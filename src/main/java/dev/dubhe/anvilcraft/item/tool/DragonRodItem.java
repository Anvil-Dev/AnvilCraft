package dev.dubhe.anvilcraft.item.tool;

import com.google.common.collect.Streams;
import dev.anvilcraft.lib.v2.util.InventoryUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.utility.BlockDevourerBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.DevourRange;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;
import java.util.Objects;

@Slf4j
public class DragonRodItem extends Item {
    public static final Identifier COOLDOWN_GROUP = AnvilCraft.of("dragon_rods");

    public DragonRodItem(Properties properties) {
        super(
            properties
                .component(ModComponents.DEVOUR_RANGE, DevourRange.THREE)
                .rarity(Rarity.UNCOMMON)
        );
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        ItemStack dragonRod = player.getItemInHand(usedHand);
        if (!dragonRod.is(this)) return super.use(level, player, usedHand);
        dragonRod.set(ModComponents.DEVOUR_RANGE, dragonRod.getOrDefault(ModComponents.DEVOUR_RANGE, DevourRange.THREE).getNext());
        return super.use(level, player, usedHand);
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

        for (BlockPos devouringPos : devouringPoses) {
            BlockState devouringState = level.getBlockState(devouringPos);
            if (devouringState.isAir()) continue;
            if (!BlockDevourerBlock.canDevour(devouringState)) continue;
            if (devouringState.is(ModBlockTags.BLOCK_DEVOURER_PROBABILITY_DROPPING)
                && level.getRandom().nextDouble() > 0.05) {
                level.destroyBlock(devouringPos, false);
                continue;
            }

            devouringPos = MultiPartBlockUtil.getChainableMainPartPos(level, devouringPos);
            devouringState = level.getBlockState(devouringPos);

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
                ResourceHandler<ItemResource> source = level.getCapability(Capabilities.Item.BLOCK, devouringPos, null);
                if (source != null && dropList.isEmpty()) {
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

        int cooldown = calculateCooldown(player, dragonRod);
        player.getCooldowns().addCooldown(DragonRodItem.COOLDOWN_GROUP, cooldown);

        dragonRod.hurtAndBreak(calculateDamage(dragonRod), level, player, item -> {
            player.onEquippedItemBroken(item, hand.asEquipmentSlot());
            EventHooks.onPlayerDestroyItem(player, dragonRod, hand);
        });
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
        return Math.max(cooldown, 80);
    }
}
