package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.init.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class LevitatingBlockEntity extends FallingBlockEntity {

    public LevitatingBlockEntity(EntityType<? extends FallingBlockEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(false);
        this.refreshDimensions();
    }

    private LevitatingBlockEntity(Level level, double x, double y, double z, BlockState state) {
        this(ModEntities.LEVITATING_BLOCK.get(), level);
        this.blockState = state;
        this.blocksBuilding = true;
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setStartPos(this.blockPosition());
    }

    public static LevitatingBlockEntity levitate(Level level, BlockPos pos, BlockState blockState) {
        LevitatingBlockEntity levitating = new LevitatingBlockEntity(
            level,
            (double) pos.getX() + 0.5D,
            pos.getY(),
            (double) pos.getZ() + 0.5D,
            blockState
        );
        // 设置原位置为空气
        level.setBlock(pos, blockState.getFluidState().createLegacyBlock(), 3);
        level.addFreshEntity(levitating);
        return levitating;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.blockPosition().getY() > this.level().getMaxBuildHeight() + 64) {
            this.discard();
        }
    }
}