package dev.dubhe.anvilcraft.entity;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.List;

public class StandableFallingBlockEntity extends FallingBlockEntity {
    public StandableFallingBlockEntity(EntityType<? extends FallingBlockEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(false);
        this.refreshDimensions();
    }

    private StandableFallingBlockEntity(Level level, double x, double y, double z, BlockState state) {
        this(ModEntities.STANDABLE_FALLING_BLOCK.get(), level);
        this.blockState = state;
        this.blocksBuilding = true;
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setStartPos(this.blockPosition());
    }

    @SuppressWarnings("UnusedReturnValue")
    public static StandableFallingBlockEntity fall(Level level, BlockPos pos, BlockState blockState) {
        StandableFallingBlockEntity falling = new StandableFallingBlockEntity(
            level,
            (double) pos.getX() + 0.5,
            pos.getY(),
            (double) pos.getZ() + 0.5,
            blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                ? blockState.setValue(BlockStateProperties.WATERLOGGED, false)
                : blockState
        );
        level.setBlock(pos, blockState.getFluidState().createLegacyBlock(), 3);
        level.addFreshEntity(falling);
        return falling;
    }

    @Override
    public void move(MoverType type, Vec3 motion) {
        super.move(type, motion);
        List<Entity> list = this.level().getEntities(
            this,
            this.getBoundingBox().expandTowards(0, 1.1F, 0),
            EntitySelector.pushableBy(this)
        );
        if (list.isEmpty()) return;
        for (Entity entity : list) {
            if (entity instanceof FallingBlockEntity) continue;
            if (entity instanceof IonocraftEntity) continue;
            entity.setDeltaMovement(
                entity.getDeltaMovement().x,
                entity.getDeltaMovement().y + 0.04 <= 0
                    ? motion.y + (this.getY() - entity.getBoundingBox().minY)
                    : motion.y * 2.8,
                entity.getDeltaMovement().z
            );
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        List<Entity> list = this.level().getEntities(
            this,
            this.getBoundingBox().expandTowards(0, 1.75F, 0),
            EntitySelector.pushableBy(this)
        );
        if (list.isEmpty()) return;
        for (Entity entity : list) {
            if (entity instanceof FallingBlockEntity) continue;
            if (entity instanceof IonocraftEntity) continue;
            entity.setDeltaMovement(
                entity.getDeltaMovement().x,
                entity.getGravity(),
                entity.getDeltaMovement().z
            );
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return super.canCollideWith(entity)
            && !Util.instanceOfAny(entity, FallingBlockEntity.class, IonocraftEntity.class);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(0.98f, 0.98f);
    }

    public static boolean isFree(Level level, BlockPos pos) {
        return FallingBlock.isFree(level.getBlockState(pos))
            || level.getBlockState(pos).getCollisionShape(level, pos).equals(Shapes.empty());
    }
}
