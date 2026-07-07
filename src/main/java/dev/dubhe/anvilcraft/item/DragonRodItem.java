package dev.dubhe.anvilcraft.item;

import dev.anvilcraft.lib.v2.util.InventoryUtil;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import dev.dubhe.anvilcraft.util.DevourUtil;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
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
import net.neoforged.neoforge.items.IItemHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class DragonRodItem extends Item {
    public static final int DEFAULT_RANGE = 3;
    private final int enchantmentValue;
    private static final Map<UUID, Long> LAST_TRANSCENDENCE_DEVOUR_TICK = new HashMap<>();
    private static final Set<UUID> CONTINUOUS_DEVOUR_PLAYERS = new HashSet<>();

    public DragonRodItem(Properties properties, int enchantmentValue) {
        super(properties.component(ModComponents.DEVOUR_RANGE, DEFAULT_RANGE).rarity(Rarity.UNCOMMON));
        this.enchantmentValue = enchantmentValue;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        if (stack.is(ModItems.DRAGON_ROD)) {
            return repairCandidate.is(Items.IRON_INGOT);
        } else if (stack.is(ModItems.ROYAL_DRAGON_ROD)) {
            return repairCandidate.is(ModItems.ROYAL_STEEL_INGOT);
        } else if (stack.is(ModItems.EMBER_DRAGON_ROD)) {
            return repairCandidate.is(ModItems.EMBER_METAL_INGOT);
        } else if (stack.is(ModItems.TRANSCENDENCE_DRAGON_ROD)) {
            return repairCandidate.is(ModItems.TRANSCENDIUM_INGOT);
        }
        return super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return this.enchantmentValue;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        InteractionResultHolder<ItemStack> superHolder = super.use(level, player, usedHand);
        ItemStack dragonRod = superHolder.getObject();
        if (!dragonRod.is(this)) return superHolder;
        switch (dragonRod.get(ModComponents.DEVOUR_RANGE)) {
            case 3 -> dragonRod.set(ModComponents.DEVOUR_RANGE, 5);
            case 5 -> dragonRod.set(ModComponents.DEVOUR_RANGE, 7);
            case 7 -> dragonRod.set(ModComponents.DEVOUR_RANGE, 9);
            case 9 -> dragonRod.set(ModComponents.DEVOUR_RANGE, 3);
            case null, default -> {
                log.warn("A dragon rod in player {} dose not have devour range, use default", player);
                dragonRod.set(ModComponents.DEVOUR_RANGE, 3);
            }
        }
        return new InteractionResultHolder<>(superHolder.getResult(), dragonRod);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        return false;
    }

    @SuppressWarnings("DataFlowIssue")
    public static void devourBlock(
        ServerLevel level, Player player, InteractionHand hand,
        BlockPos centerPos, BlockState centerState, Direction clickedSide
    ) {
        if (centerState.is(ModBlockTags.DEVOUR_BLACKLIST)) return;
        if (centerState.getDestroySpeed(level, centerPos) < 0.0F) return;
        ItemStack dragonRod = player.getItemInHand(hand);
        if (!canDevour(player, dragonRod)) return;
        int range = dragonRod.getOrDefault(ModComponents.DEVOUR_RANGE, -1);
        if (range == -1) return;
        range = (range - 1) / 2;
        Iterable<BlockPos> devouringPoses = DevourUtil.getDevourPosList(
                level,
                centerPos,
                clickedSide,
                range,
                0
        );

        for (BlockPos devouringPos : devouringPoses) {
            BlockState devouringState = level.getBlockState(devouringPos);
            if (!DevourUtil.shouldDevour(devouringState)) continue;

            if (devouringState.is(ModBlockTags.BLOCK_DEVOURER_PROBABILITY_DROPPING)
                && level.random.nextDouble() > 0.05) {
                level.destroyBlock(devouringPos, false);
                continue;
            }

            if (!player.getAbilities().instabuild) {
                int expCount = EnchantmentHelper.processBlockExperience(
                    level,
                    dragonRod,
                    devouringState.getExpDrop(level, devouringPos, level.getBlockEntity(devouringPos), player, dragonRod)
                );
                player.giveExperiencePoints(expCount);
                List<ItemStack> dropList = BreakBlockUtil.dropWithTool(level, devouringPos, dragonRod);
                Inventory inventory = player.getInventory();
                for (ItemStack drop : dropList) {
                    if (drop.isEmpty()) continue;
                    ItemStack remaining = InventoryUtil.insertItem(inventory, drop);
                    if (!remaining.isEmpty()) {
                        Block.popResource(level, devouringPos, remaining);
                    }
                }
                // 特判雕纹书架一类
                IItemHandler source = level.getCapability(Capabilities.ItemHandler.BLOCK, devouringPos, null);
                if (source != null && dropList.isEmpty()) {
                    for (IntListIterator it = IntIterators.fromTo(0, source.getSlots()); it.hasNext(); ) {
                        int slot = it.nextInt();
                        ItemStack stack = source.getStackInSlot(slot);
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

        int cooldown = calculateCooldown(player);
        player.getCooldowns().addCooldown(ModItems.DRAGON_ROD.asItem(), cooldown);
        player.getCooldowns().addCooldown(ModItems.ROYAL_DRAGON_ROD.asItem(), cooldown);
        player.getCooldowns().addCooldown(ModItems.EMBER_DRAGON_ROD.asItem(), cooldown);
        // 超限龙杖：首次使用有0.5s启动延迟，之后持续快速破坏
        if (dragonRod.is(ModItems.TRANSCENDENCE_DRAGON_ROD)) {
            long currentTick = level.getGameTime();
            long lastTick = LAST_TRANSCENDENCE_DEVOUR_TICK.getOrDefault(player.getUUID(), 0L);
            boolean isWarmedUp = (currentTick - lastTick) < 15; // 0.75s内再次使用=已预热
            player.getCooldowns().addCooldown(ModItems.TRANSCENDENCE_DRAGON_ROD.asItem(), isWarmedUp ? 0 : 10);
            LAST_TRANSCENDENCE_DEVOUR_TICK.put(player.getUUID(), currentTick);
            if (isWarmedUp) {
                // 预热完成，进入服务端持续破坏模式，绕过原版攻击速度限制
                CONTINUOUS_DEVOUR_PLAYERS.add(player.getUUID());
            }
        } else {
            player.getCooldowns().addCooldown(ModItems.TRANSCENDENCE_DRAGON_ROD.asItem(), 0);
        }
        if (!(dragonRod.getItem() instanceof DragonRodItem)) {
            player.getCooldowns().addCooldown(dragonRod.getItem(), cooldown);
        }

        dragonRod.hurtAndBreak(calculateDamage(dragonRod), level, player, item -> {
            player.onEquippedItemBroken(item, LivingEntity.getSlotForHand(hand));
            EventHooks.onPlayerDestroyItem(player, dragonRod, hand);
        });
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean canDevour(Player player, ItemStack dragonRod) {
        return dragonRod.getDamageValue() < dragonRod.getMaxDamage() - 1
            && !player.getCooldowns().isOnCooldown(dragonRod.getItem());
    }

    public static int calculateDamage(ItemStack dragonRod) {
        int range = dragonRod.getOrDefault(ModComponents.DEVOUR_RANGE, 0);
        int damage = switch (range) {
            case 5 -> 1;
            case 7 -> 2;
            case 9 -> 4;
            default -> 0;
        };
        return Mth.clamp(dragonRod.getMaxDamage() - dragonRod.getDamageValue(), 1, damage);
    }

    public static int calculateCooldown(Player player) {
        int cooldown = 20;
        if (player.hasEffect(MobEffects.DIG_SPEED)) {
            cooldown -= Objects.requireNonNull(player.getEffect(MobEffects.DIG_SPEED)).getAmplifier() * 4;
        }
        if (player.hasEffect(MobEffects.DIG_SLOWDOWN)) {
            cooldown += Objects.requireNonNull(player.getEffect(MobEffects.DIG_SLOWDOWN)).getAmplifier() * 60;
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
        BlockState targetState = player.serverLevel().getBlockState(targetPos);
        if (targetState.isAir()) return;
        if (!DevourUtil.canDevour(targetState)) return;
        devourBlock(player.serverLevel(), player, hand, targetPos, targetState, blockHit.getDirection());
    }
}
