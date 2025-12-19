package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.init.entity.ModEntities;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class StandableLevitatingBlockEntity extends LevitatingBlockEntity {
    public StandableLevitatingBlockEntity(EntityType<? extends FallingBlockEntity> entityType, Level level) {
        super(entityType, level);
    }

    private StandableLevitatingBlockEntity(Level level, double x, double y, double z, BlockState state) {
        this(ModEntities.STANDABLE_LEVITATING_BLOCK.get(), level);
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
    public static StandableLevitatingBlockEntity levitate(Level level, BlockPos pos, BlockState blockState) {
        StandableLevitatingBlockEntity levitating = new StandableLevitatingBlockEntity(
            level,
            (double) pos.getX() + 0.5,
            pos.getY(),
            (double) pos.getZ() + 0.5,
            blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                ? blockState.setValue(BlockStateProperties.WATERLOGGED, false)
                : blockState
        );
        level.setBlock(pos, blockState.getFluidState().createLegacyBlock(), 3);
        level.addFreshEntity(levitating);
        return levitating;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void move(MoverType type, Vec3 motion) {
        super.move(type, motion);
        List<Entity> list = this.level().getEntities(
            this,
            this.getBoundingBox().expandTowards(0, 0.5F, 0),
            EntitySelector.pushableBy(this)
        );
        if (list.isEmpty()) return;
        for (Entity entity : list) {
            if (entity instanceof FallingBlockEntity) continue;
            if (entity instanceof IonocraftEntity) continue;
            entity.setDeltaMovement(
                entity.getDeltaMovement().x,
                entity.getDeltaMovement().y + 0.04 > 0
                    ? motion.y - (entity.getBoundingBox().minY - this.getBoundingBox().maxY) + 0.0392
                    : motion.y * 1.5,
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
}
