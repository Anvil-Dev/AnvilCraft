package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.CrabTrapBlock;
import dev.dubhe.anvilcraft.block.entity.CrabTrapBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

public class HitCrabTrapBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilFallOnLandEvent event
    ) {
        if (!hitBlockState.hasBlockEntity()) return false;
        CrabTrapBlockEntity blockEntity = (CrabTrapBlockEntity) level.getBlockEntity(hitBlockPos);
        Direction face = hitBlockState.getValue(CrabTrapBlock.FACING);
        Vec3 dropPos = hitBlockPos.above().relative(face).getCenter().relative(face.getOpposite(), 0.5);
        if (blockEntity == null) return false;
        IItemHandler depository = blockEntity.getItemHandler();
        for (int i = 0; i < depository.getSlots(); i++) {
            ItemStack stack = depository.getStackInSlot(i);
            ItemEntity itemEntity = new ItemEntity(level, dropPos.x, dropPos.y - 0.4, dropPos.z, stack, 0, 0, 0);
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
            depository.extractItem(i, stack.getCount(), false);
        }
        return false;
    }
}
