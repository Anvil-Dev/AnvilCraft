package dev.dubhe.anvilcraft.item;

import com.google.common.collect.Streams;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import dev.dubhe.anvilcraft.util.InventoryUtil;
import dev.dubhe.anvilcraft.util.MultiPartBlockUtil;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;
import java.util.Objects;

@Slf4j
public class DragonRodItem extends Item {
    public static final int DEFAULT_RANGE = 3;
    private final int enchantmentValue;

    public DragonRodItem(Properties properties, int enchantmentValue) {
        super(properties.component(ModComponents.DEVOUR_RANGE, DEFAULT_RANGE).rarity(Rarity.EPIC));
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
        if (centerState.getDestroySpeed(level, centerPos) == 0.0F) return;
        ItemStack dragonRod = player.getItemInHand(hand);
        if (!canDevour(player, dragonRod)) return;
        int range = dragonRod.getOrDefault(ModComponents.DEVOUR_RANGE, -1);
        if (range == -1) return;
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
            if (devouringState.getBlock().defaultDestroyTime() < 0) continue;
            if (devouringState.is(ModBlockTags.BLOCK_DEVOURER_PROBABILITY_DROPPING)
                && level.random.nextDouble() > 0.05) {
                level.destroyBlock(devouringPos, false);
                continue;
            }

            devouringPos = MultiPartBlockUtil.getChainableMainPartPos(level, devouringPos);
            devouringState = level.getBlockState(devouringPos);

            if (!player.getAbilities().instabuild) {
                List<ItemStack> dropList = BreakBlockUtil.dropWithTool(level, devouringPos, dragonRod);
                Inventory inventory = player.getInventory();
                for (ItemStack drop : dropList) {
                    if (drop.isEmpty()) continue;
                    ItemStack remaining = InventoryUtil.insertItem(inventory, drop);
                    if (!remaining.isEmpty()) {
                        Block.popResource(level, devouringPos, remaining);
                    }
                }
                //特判雕纹书架一类
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
                //特判讲台
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
        return Math.min(damage, Math.max(dragonRod.getMaxDamage() - dragonRod.getDamageValue(), 1));
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
}
