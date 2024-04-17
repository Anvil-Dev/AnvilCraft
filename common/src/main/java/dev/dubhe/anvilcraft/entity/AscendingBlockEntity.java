package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.init.ModEntities;
import dev.dubhe.anvilcraft.mixin.accessor.FallingBlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class AscendingBlockEntity extends FallingBlockEntity {
    public AscendingBlockEntity(EntityType<? extends AscendingBlockEntity> entityType, Level level) {
        super(entityType, level);
    }

    private AscendingBlockEntity(Level level, double x, double y, double z, BlockState state) {
        this(ModEntities.ASCENDING_BLOCK_ENTITY.get(), level);
        ((FallingBlockEntityAccessor) this).setBlockState(state);
        this.blocksBuilding = true;
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setStartPos(this.blockPosition());
    }

    @Override
    public void tick() {
        if (this.getBlockState().isAir()) {
            System.out.println("this.getBlockState() = " + this.getBlockState());
            this.discard();
            return;
        }
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.4, 0.0));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.level().isClientSide) return;
        BlockPos current = this.blockPosition();
        BlockPos up = current.above();
        if (!this.level().getBlockState(up).isAir()) {
            this.level().setBlockAndUpdate(current, this.getBlockState());
            this.discard();
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
    }


    public static AscendingBlockEntity ascend(Level level, BlockPos pos, BlockState blockState) {
        AscendingBlockEntity ascendingBlockEntity = new AscendingBlockEntity(
                level,
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                        ? blockState.setValue(BlockStateProperties.WATERLOGGED, false)
                        : blockState
        );
        level.setBlock(pos, blockState.getFluidState().createLegacyBlock(), 3);
        level.addFreshEntity(ascendingBlockEntity);
        return ascendingBlockEntity;
    }
}
