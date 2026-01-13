package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.util.GravityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BlackHoleBlockEntity extends BlockEntity {
    public BlackHoleBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.BLACK_HOLE.get(), pos, blockState);
    }

    public static BlackHoleBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new BlackHoleBlockEntity(pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide) {
            GravityManager.GravitySourceType type = GravityManager.GravitySourceManager.getType(this.getBlockState().getBlock());
            if (type != null) {
                GravityManager.GravitySourceManager.addSource(this.level, this.worldPosition, type);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide) {
            GravityManager.GravitySourceManager.removeSource(this.level, this.worldPosition);
        }
    }
}