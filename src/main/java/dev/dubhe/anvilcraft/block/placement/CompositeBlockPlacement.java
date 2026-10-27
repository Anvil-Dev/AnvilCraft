package dev.dubhe.anvilcraft.block.placement;

import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.block.UseItemOnBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/** 将基础物品与附加材料组合为蓝图方块，放置失败时返还两件材料。 */
public final class CompositeBlockPlacement {
    private CompositeBlockPlacement() {
    }

    public static boolean isConverted(BlockState state) {
        return !materialFor(state).isEmpty();
    }

    public static ItemStack baseMaterial(BlockState state) {
        if (state.getBlock() instanceof CandleCakeBlock) {
            return new ItemStack(Items.CAKE);
        }
        if (state.getBlock() instanceof FlowerPotBlock pot) {
            return new ItemStack(pot.getEmptyPot().asItem());
        }
        return isConverted(state) ? ModBlocks.STAMPING_PLATFORM.asStack() : new ItemStack(state.getBlock().asItem());
    }

    private static ItemStack materialFor(BlockState state) {
        if (state.getBlock() instanceof CandleCakeBlock cake) {
            return new ItemStack(cake.candleBlock.asItem());
        }
        if (state.getBlock() instanceof FlowerPotBlock pot) {
            return new ItemStack(pot.getPotted().asItem());
        }
        return new ItemStack(UseItemOnBlock.upgradeItem(state));
    }

    public static boolean matches(ItemStack stack, BlockState state) {
        return stack.is(baseMaterial(state).getItem());
    }

    public static boolean placeFromHandler(
        ServerLevel level, BlockPos target, BlockState state, ResourceHandler<ItemResource> handler, int slot,
        ItemStack expected, BlockPos source, @Nullable Direction facing
    ) {
        if (!isConverted(state) || !matches(expected, state) || !BlockPlacementUtil.isTargetAvailable(level, target)) {
            return false;
        }
        if (slot < 0 || slot >= handler.size()) {
            return false;
        }
        int upgradeSlot = upgradeSlot(handler, state);
        if (upgradeSlot < 0) {
            return false;
        }
        ItemResource base = ItemResource.of(expected);
        ItemResource upgrade = handler.getResource(upgradeSlot);
        try (Transaction transaction = Transaction.openRoot()) {
            if (handler.extract(slot, base, 1, transaction) != 1
                || handler.extract(upgradeSlot, upgrade, 1, transaction) != 1) {
                return false;
            }
            if (!place(level, target, state, base.toStack(), facing)) {
                return false;
            }
            transaction.commit();
            return true;
        }
    }

    public static int upgradeSlot(ResourceHandler<ItemResource> handler, BlockState state) {
        ItemStack upgrade = materialFor(state);
        if (upgrade.isEmpty()) {
            return -1;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            for (int slot = 0; slot < handler.size(); slot++) {
                ItemResource resource = handler.getResource(slot);
                if (resource.is(upgrade.getItem()) && handler.extract(slot, resource, 1, transaction) == 1) {
                    return slot;
                }
            }
        }
        return -1;
    }

    @Nullable
    public static ItemEntity upgradeEntity(ServerLevel level, ItemEntity base, BlockState state) {
        ItemStack upgrade = materialFor(state);
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(base.blockPosition()),
            entity -> entity.isAlive() && entity.getItem().is(upgrade.getItem())).stream().findFirst().orElse(null);
    }

    public static boolean placeFromEntities(
        ServerLevel level, BlockPos target, BlockState state, ItemEntity base, @Nullable Direction facing
    ) {
        if (!isConverted(state) || !matches(base.getItem(), state) || !BlockPlacementUtil.isTargetAvailable(level, target)) {
            return false;
        }
        ItemEntity upgrade = upgradeEntity(level, base, state);
        if (upgrade == null) {
            return false;
        }
        ItemStack baseItem = base.getItem().copyWithCount(1);
        ItemStack upgradeItem = upgrade.getItem().copyWithCount(1);
        base.setItem(base.getItem().copyWithCount(base.getItem().getCount() - 1));
        upgrade.setItem(upgrade.getItem().copyWithCount(upgrade.getItem().getCount() - 1));
        boolean placed = false;
        try {
            placed = place(level, target, state, baseItem, facing);
            return placed;
        } finally {
            if (placed) {
                if (base.getItem().isEmpty()) {
                    base.discard();
                }
                if (upgrade.getItem().isEmpty()) {
                    upgrade.discard();
                }
            } else {
                restoreEntity(level, base, baseItem);
                restoreEntity(level, upgrade, upgradeItem);
            }
        }
    }

    private static boolean place(ServerLevel level, BlockPos pos, BlockState state, ItemStack base, @Nullable Direction facing) {
        if (state.getBlock() instanceof FlowerPotBlock || state.getBlock() instanceof CandleCakeBlock) {
            return placeDecorated(level, pos, state, base, facing);
        }
        ItemStack assembled = base.transmuteCopy(state.getBlock().asItem());
        return BlockPlacementUtil.placeBlock(level, pos, assembled, state, facing).isEmpty();
    }

    private static boolean placeDecorated(
        ServerLevel level, BlockPos pos, BlockState state, ItemStack base, @Nullable Direction facing
    ) {
        BlockState previous = level.getBlockState(pos);
        boolean placed = false;
        ServerPlayer player = AnvilCraftFakePlayers.getBlockPlacer().offerPlayer(level);
        try {
            if (!BlockPlacementUtil.placeBlock(level, pos, base.copy(), null, facing).isEmpty()) {
                return false;
            }
            ItemStack decoration = materialFor(state);
            player.setItemInHand(InteractionHand.MAIN_HAND, decoration);
            BlockHitResult hit = new BlockHitResult(pos.getCenter(), Direction.UP, pos, false);
            level.getBlockState(pos).useItemOn(decoration, level, player, InteractionHand.MAIN_HAND, hit);
            placed = decoration.isEmpty() && level.getBlockState(pos).is(state.getBlock())
                && level.getBlockState(pos).canSurvive(level, pos);
            return placed;
        } finally {
            AnvilCraftFakePlayers.getBlockPlacer().disable(player);
            if (!placed) {
                level.setBlock(pos, previous, Block.UPDATE_ALL);
            }
        }
    }

    private static void restoreEntity(ServerLevel level, ItemEntity entity, ItemStack stack) {
        if (entity.isAlive() && (entity.getItem().isEmpty() || ItemStack.isSameItemSameComponents(entity.getItem(), stack))) {
            entity.setItem(stack.copyWithCount(entity.getItem().getCount() + stack.getCount()));
        } else {
            Containers.dropItemStack(level, entity.getX(), entity.getY(), entity.getZ(), stack);
        }
    }
}
