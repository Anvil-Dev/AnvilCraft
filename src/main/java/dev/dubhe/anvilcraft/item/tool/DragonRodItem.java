package dev.dubhe.anvilcraft.item.tool;

import com.google.common.collect.Streams;
import dev.anvilcraft.lib.v2.util.InventoryUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.utility.BlockDevourerBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.DevourRange;
import dev.dubhe.anvilcraft.util.BlockMiningEffect;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import dev.dubhe.anvilcraft.util.InfiniteFluidTankBreakProtection;
import dev.dubhe.anvilcraft.util.ItemResourceHelper;
import dev.dubhe.anvilcraft.util.MultiPartBlockUtil;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class DragonRodItem extends Item {
    public static final Identifier COOLDOWN_GROUP = AnvilCraft.of("dragon_rods");
    private final BlockMiningEffect miningEffect;
    private static final Map<UUID, Long> LAST_TRANSCENDENCE_DEVOUR_TICK = new HashMap<>();
    private static final Set<UUID> CONTINUOUS_DEVOUR_PLAYERS = new HashSet<>();

    public DragonRodItem(Properties properties) {
        this(properties, DevourRange.THREE, BlockMiningEffect.NORMAL);
    }

    public DragonRodItem(Properties properties, DevourRange defaultRange, BlockMiningEffect miningEffect) {
        super(properties
            .component(ModComponents.DEVOUR_RANGE, defaultRange)
            .rarity(Rarity.UNCOMMON)
        );
        this.miningEffect = miningEffect;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        ItemStack dragonRod = player.getItemInHand(usedHand);
        if (!dragonRod.is(this)) return super.use(level, player, usedHand);
        dragonRod.set(
            ModComponents.DEVOUR_RANGE,
            dragonRod.getOrDefault(ModComponents.DEVOUR_RANGE, DevourRange.THREE).getNext()
        );
        return super.use(level, player, usedHand);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        return false;
    }

    @SuppressWarnings("DataFlowIssue")
    public static void devourBlock(
        ServerLevel level,
        Player player,
        InteractionHand hand,
        BlockPos centerPos,
        BlockState centerState,
        Direction clickedSide
    ) {
        if (centerState.is(ModBlockTags.DEVOUR_BLACKLIST)) return;
        if (centerState.getDestroySpeed(level, centerPos) < 0.0F) return;
        ItemStack dragonRod = player.getItemInHand(hand);
        if (!canDevour(player, dragonRod)) return;

        int range = dragonRod.getOrDefault(ModComponents.DEVOUR_RANGE, DevourRange.THREE).getRange();
        range = (range - 1) / 2;
        Iterable<BlockPos> devouringPoses;
        switch (clickedSide) {
            case DOWN, UP -> devouringPoses = BlockPos.betweenClosed(
                centerPos.relative(Direction.NORTH, range).relative(Direction.WEST, range),
                centerPos.relative(Direction.SOUTH, range).relative(Direction.EAST, range)
            );
            case NORTH, SOUTH -> devouringPoses = BlockPos.betweenClosed(
                centerPos.relative(Direction.UP, range).relative(Direction.WEST, range),
                centerPos.relative(Direction.DOWN, range).relative(Direction.EAST, range)
            );
            case WEST, EAST -> devouringPoses = BlockPos.betweenClosed(
                centerPos.relative(Direction.UP, range).relative(Direction.NORTH, range),
                centerPos.relative(Direction.DOWN, range).relative(Direction.SOUTH, range)
            );
            default -> devouringPoses = List.of(centerPos);
        }
        devouringPoses = Streams.stream(devouringPoses).map(BlockPos::immutable).toList();

        boolean infiniteFluidTankBlocked = false;
        for (BlockPos devouringPos : devouringPoses) {
            BlockState devouringState = level.getBlockState(devouringPos);
            if (devouringState.isAir()) continue;
            if (!BlockDevourerBlock.canDevour(devouringState)) continue;
            if (InfiniteFluidTankBreakProtection.isProtected(level, devouringPos)) {
                infiniteFluidTankBlocked = true;
                continue;
            }
            BlockMiningEffect miningEffect = dragonRod.getItem() instanceof DragonRodItem item
                                             ? item.miningEffect
                                             : BlockMiningEffect.NORMAL;
            if (!miningEffect.isDisintegration()
                && devouringState.is(ModBlockTags.BLOCK_DEVOURER_PROBABILITY_DROPPING)
                && level.getRandom().nextDouble() > 0.05) {
                level.destroyBlock(devouringPos, false);
                continue;
            }

            devouringPos = MultiPartBlockUtil.getChainableMainPartPos(level, devouringPos);
            devouringState = level.getBlockState(devouringPos);

            if (!player.getAbilities().instabuild) {
                ItemStack miningTool = miningEffect.applyTo(level, dragonRod);
                int expCount = EnchantmentHelper.processBlockExperience(
                    level,
                    miningTool,
                    devouringState.getExpDrop(
                        level,
                        devouringPos,
                        level.getBlockEntity(devouringPos),
                        player,
                        miningTool
                    )
                );
                player.giveExperiencePoints(expCount);
                List<ItemStack> dropList = BreakBlockUtil.dropWithTool(level, devouringPos, miningTool);
                Inventory inventory = player.getInventory();
                for (ItemStack drop : dropList) {
                    if (drop.isEmpty()) continue;
                    ItemStack remaining = InventoryUtil.insertItem(inventory, drop);
                    if (!remaining.isEmpty()) {
                        Block.popResource(level, devouringPos, remaining);
                    }
                }
                // 特判雕纹书架一类
                ResourceHandler<ItemResource> source = level.getCapability(Capabilities.Item.BLOCK, devouringPos, null);
                if (!miningEffect.isDisintegration() && source != null && dropList.isEmpty()) {
                    for (IntListIterator it = IntIterators.fromTo(0, source.size()); it.hasNext(); ) {
                        int slot = it.nextInt();
                        ItemStack stack = ItemResourceHelper.getStackInSlot(source, slot);
                        if (stack.isEmpty()) continue;
                        stack = InventoryUtil.insertItem(inventory, stack);
                        if (!stack.isEmpty()) {
                            Block.popResource(level, devouringPos, stack);
                        }
                    }
                }
                // 特判讲台
                BlockEntity devouringBlockEntity = level.getBlockEntity(devouringPos);
                if (devouringBlockEntity instanceof LecternBlockEntity lectern) {
                    ItemStack bookStack = lectern.getBook();
                    bookStack = InventoryUtil.insertItem(inventory, bookStack);
                    lectern.setBook(bookStack);
                    if (!bookStack.isEmpty()) {
                        Block.popResource(level, devouringPos, bookStack);
                        lectern.setBook(ItemStack.EMPTY);
                    }
                }
            }
            if (!(devouringState.getBlock() instanceof DoublePlantBlock)) {
                devouringState.getBlock().playerWillDestroy(level, devouringPos, devouringState, player);
            }
            level.destroyBlock(devouringPos, false);
        }
        if (infiniteFluidTankBlocked) {
            InfiniteFluidTankBreakProtection.showToolBreakDenied(player);
        }

        if (dragonRod.is(ModItems.TRANSCENDENCE_DRAGON_ROD)) {
            long currentTick = level.getGameTime();
            Long lastTick = LAST_TRANSCENDENCE_DEVOUR_TICK.put(player.getUUID(), currentTick);
            boolean warmedUp = lastTick != null && currentTick - lastTick < 15;
            player.getCooldowns().addCooldown(DragonRodItem.COOLDOWN_GROUP, warmedUp ? 0 : 10);
            if (warmedUp) {
                CONTINUOUS_DEVOUR_PLAYERS.add(player.getUUID());
            }
        } else {
            player.getCooldowns().addCooldown(DragonRodItem.COOLDOWN_GROUP, calculateCooldown(player, dragonRod));
        }

        dragonRod.hurtAndBreak(
            calculateDamage(dragonRod), level, player, item -> {
                player.onEquippedItemBroken(item, hand.asEquipmentSlot());
                EventHooks.onPlayerDestroyItem(player, dragonRod, hand);
            }
        );
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean canDevour(Player player, ItemStack dragonRod) {
        return dragonRod.getDamageValue() < dragonRod.getMaxDamage() - 1
            && !player.getCooldowns().isOnCooldown(dragonRod);
    }

    public static int calculateDamage(ItemStack dragonRod) {
        int damage = dragonRod.getOrDefault(ModComponents.DEVOUR_RANGE, DevourRange.THREE).getDamage();
        return Math.clamp(dragonRod.getMaxDamage() - dragonRod.getDamageValue(), 1, damage);
    }

    public static int calculateCooldown(Player player, ItemStack dragonRod) {
        int cooldown;
        UseCooldown useCooldown = dragonRod.get(DataComponents.USE_COOLDOWN);
        if (useCooldown == null) {
            cooldown = 20;
        } else {
            cooldown = useCooldown.ticks();
        }
        if (player.hasEffect(MobEffects.HASTE)) {
            cooldown -= Objects.requireNonNull(player.getEffect(MobEffects.HASTE)).getAmplifier() * 4;
        }
        if (player.hasEffect(MobEffects.MINING_FATIGUE)) {
            cooldown += Objects.requireNonNull(player.getEffect(MobEffects.MINING_FATIGUE)).getAmplifier() * 60;
        }
        return Math.max(cooldown, 4);
    }

    public static void stopContinuousMode(Player player) {
        CONTINUOUS_DEVOUR_PLAYERS.remove(player.getUUID());
    }

    public static void tickContinuousDevour(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (!CONTINUOUS_DEVOUR_PLAYERS.contains(playerId)) return;

        ItemStack rod = player.getMainHandItem();
        InteractionHand hand = InteractionHand.MAIN_HAND;
        if (!rod.is(ModItems.TRANSCENDENCE_DRAGON_ROD)) {
            rod = player.getOffhandItem();
            hand = InteractionHand.OFF_HAND;
            if (!rod.is(ModItems.TRANSCENDENCE_DRAGON_ROD)) {
                CONTINUOUS_DEVOUR_PLAYERS.remove(playerId);
                return;
            }
        }
        if (!canDevour(player, rod)) {
            CONTINUOUS_DEVOUR_PLAYERS.remove(playerId);
            return;
        }
        HitResult hit = player.pick(player.blockInteractionRange(), 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) return;
        BlockPos targetPos = blockHit.getBlockPos();
        ServerLevel level = player.level();
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.isAir() || !BlockDevourerBlock.canDevour(targetState)) return;
        devourBlock(level, player, hand, targetPos, targetState, blockHit.getDirection());
    }
}
