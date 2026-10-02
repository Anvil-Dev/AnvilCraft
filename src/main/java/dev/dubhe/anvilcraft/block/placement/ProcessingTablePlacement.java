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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/** 加工台变体的物品建造路径与玩家升级冲压台消耗相同的两件材料。 */
public final class ProcessingTablePlacement {
    private ProcessingTablePlacement() {
    }

    public static boolean isConverted(BlockState state) {
        return UseItemOnBlock.upgradeItem(state) != Items.AIR;
    }

    public static ItemStack baseMaterial(BlockState state) {
        return isConverted(state) ? ModBlocks.STAMPING_PLATFORM.asStack() : new ItemStack(state.getBlock().asItem());
    }

    public static boolean matches(ItemStack stack) {
        return stack.is(ModBlocks.STAMPING_PLATFORM.asItem());
    }

    public static boolean placeFromHandler(
        ServerLevel level, BlockPos target, BlockState state, ResourceHandler<ItemResource> handler, int slot,
        ItemStack expected, BlockPos source, @Nullable Direction facing
    ) {
        if (!isConverted(state) || !matches(expected) || !BlockPlacementUtil.isTargetAvailable(level, target)) return false;
        if (slot < 0 || slot >= handler.size()) return false;
        int upgradeSlot = upgradeSlot(handler, state);
        if (upgradeSlot < 0) return false;
        ItemResource base = ItemResource.of(expected);
        ItemResource upgrade = handler.getResource(upgradeSlot);
        try (Transaction transaction = Transaction.openRoot()) {
            if (handler.extract(slot, base, 1, transaction) != 1
                || handler.extract(upgradeSlot, upgrade, 1, transaction) != 1) return false;
            if (!place(level, target, state, base.toStack(), facing)) return false;
            transaction.commit();
            return true;
        }
    }

    public static int upgradeSlot(ResourceHandler<ItemResource> handler, BlockState state) {
        ItemStack upgrade = UseItemOnBlock.materialFor(state);
        if (upgrade.isEmpty()) return -1;
        try (Transaction transaction = Transaction.openRoot()) {
            for (int slot = 0; slot < handler.size(); slot++) {
                ItemResource resource = handler.getResource(slot);
                if (resource.is(upgrade.getItem()) && handler.extract(slot, resource, 1, transaction) == 1) return slot;
            }
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
        if (!isConverted(state) || !matches(base.getItem()) || !BlockPlacementUtil.isTargetAvailable(level, target)) return false;
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

    private static void restoreEntity(ServerLevel level, ItemEntity entity, ItemStack stack) {
        if (entity.isAlive() && (entity.getItem().isEmpty() || ItemStack.isSameItemSameComponents(entity.getItem(), stack))) {
            entity.setItem(stack.copyWithCount(entity.getItem().getCount() + stack.getCount()));
        } else {
            Containers.dropItemStack(level, entity.getX(), entity.getY(), entity.getZ(), stack);
        }
    }
}
