package dev.dubhe.anvilcraft.block.placement;

import dev.dubhe.anvilcraft.block.UseItemOnBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;

/** 加工台变体的物品建造路径与玩家升级冲压台消耗相同的两件材料。 */
public final class ProcessingTablePlacement {
    private ProcessingTablePlacement() {
    }

    public static boolean isConverted(BlockState state) {
        return !UseItemOnBlock.materialFor(state).isEmpty();
    }

    public static ItemStack baseMaterial(BlockState state) {
        return isConverted(state) ? ModBlocks.STAMPING_PLATFORM.asStack() : new ItemStack(state.getBlock().asItem());
    }

    public static boolean matches(ItemStack stack) {
        return stack.is(ModBlocks.STAMPING_PLATFORM.asItem());
    }

    public static boolean placeFromHandler(
        ServerLevel level, BlockPos target, BlockState state, IItemHandler handler, int slot,
        ItemStack expected, BlockPos source, @Nullable Direction facing
    ) {
        if (!matches(expected) || !BlockPlacementUtil.isTargetAvailable(level, target)) return false;
        ItemStack simulated = handler.extractItem(slot, 1, true);
        if (simulated.getCount() != 1 || !ItemStack.isSameItemSameComponents(simulated, expected)) return false;
        int upgradeSlot = upgradeSlot(handler, state);
        if (upgradeSlot < 0) return false;
        ItemStack base = handler.extractItem(slot, 1, false);
        if (base.getCount() != 1 || !ItemStack.isSameItemSameComponents(base, expected)) {
            refund(level, source, handler, base);
            return false;
        }
        ItemStack upgrade = handler.extractItem(upgradeSlot, 1, false);
        if (upgrade.getCount() != 1 || !upgrade.is(UseItemOnBlock.materialFor(state).getItem())) {
            refund(level, source, handler, base);
            refund(level, source, handler, upgrade);
            return false;
        }
        boolean placed = false;
        try {
            placed = place(level, target, state, base, facing);
            return placed;
        } finally {
            if (!placed) {
                refund(level, source, handler, base);
                refund(level, source, handler, upgrade);
            }
        }
    }

    public static int upgradeSlot(IItemHandler handler, BlockState state) {
        ItemStack upgrade = UseItemOnBlock.materialFor(state);
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack simulated = handler.extractItem(slot, 1, true);
            if (simulated.getCount() == 1 && simulated.is(upgrade.getItem())) return slot;
        }
        return -1;
    }

    @Nullable
    public static ItemEntity upgradeEntity(ServerLevel level, ItemEntity base, BlockState state) {
        ItemStack upgrade = UseItemOnBlock.materialFor(state);
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(base.blockPosition()),
            entity -> entity.isAlive() && entity.getItem().is(upgrade.getItem())).stream().findFirst().orElse(null);
    }

    public static boolean placeFromEntities(
        ServerLevel level, BlockPos target, BlockState state, ItemEntity base, @Nullable Direction facing
    ) {
        if (!matches(base.getItem()) || !BlockPlacementUtil.isTargetAvailable(level, target)) return false;
        ItemEntity upgrade = upgradeEntity(level, base, state);
        if (upgrade == null) return false;
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
                if (base.getItem().isEmpty()) base.discard();
                if (upgrade.getItem().isEmpty()) upgrade.discard();
            } else {
                restoreEntity(level, base, baseItem);
                restoreEntity(level, upgrade, upgradeItem);
            }
        }
    }

    private static boolean place(ServerLevel level, BlockPos pos, BlockState state, ItemStack base, @Nullable Direction facing) {
        ItemStack assembled = base.transmuteCopy(state.getBlock().asItem());
        return BlockPlacementUtil.placeBlock(level, pos, assembled, state, facing).isEmpty();
    }

    private static void refund(ServerLevel level, BlockPos source, IItemHandler handler, ItemStack stack) {
        ItemStack remaining = ItemHandlerHelper.insertItemStacked(handler, stack, false);
        if (!remaining.isEmpty()) Containers.dropItemStack(level, source.getX() + 0.5, source.getY() + 0.5, source.getZ() + 0.5, remaining);
    }

    private static void restoreEntity(ServerLevel level, ItemEntity entity, ItemStack stack) {
        if (entity.isAlive() && (entity.getItem().isEmpty() || ItemStack.isSameItemSameComponents(entity.getItem(), stack))) {
            entity.setItem(stack.copyWithCount(entity.getItem().getCount() + stack.getCount()));
        } else {
            Containers.dropItemStack(level, entity.getX(), entity.getY(), entity.getZ(), stack);
        }
    }
}
