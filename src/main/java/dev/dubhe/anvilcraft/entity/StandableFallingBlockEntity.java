package dev.dubhe.anvilcraft.entity;

import dev.dubhe.anvilcraft.init.ModEntities;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
public class StandableFallingBlockEntity extends FallingBlockEntity {
    public StandableFallingBlockEntity(EntityType<? extends FallingBlockEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
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
    public void tick() {
        this.setNoGravity(true);
        if (this.blockState.isAir()) {
            this.discard();
            return;
        }
        Block block = this.blockState.getBlock();
        ++this.time;
        BlockPos blockPos = this.blockPosition();

        whenStop:
        if (blockPos.getY() <= this.level().getMinBuildHeight() || blockPos.getY() > this.level().getMaxBuildHeight() + 64) {
            this.discard();
        } else if (
            (this.time <= 1 || !this.getDeltaMovement().equals(Vec3.ZERO))
                && isFree(this.level(), new BlockPos(
                this.getBlockX(), (int) Math.floor(blockPos.getBottomCenter().y - 0.04), this.getBlockZ()))
        ) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        } else {
            if (this.level().isClientSide) break whenStop;
            BlockState blockState = this.level().getBlockState(blockPos);
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
            if (blockState.is(Blocks.MOVING_PISTON)) break whenStop;

            if (this.cancelDrop) {
                this.discard();
                this.callOnBrokenAfterFall(block, blockPos);
                break whenStop;
            }
            boolean canBeReplaced = blockState.canBeReplaced(
                new DirectionalPlaceContext(this.level(), blockPos, Direction.DOWN, ItemStack.EMPTY, Direction.UP));
            boolean canSurvive = this.blockState.canSurvive(this.level(), blockPos);
            if (canBeReplaced && canSurvive) {
                if (this.blockState.hasProperty(BlockStateProperties.WATERLOGGED)
                    && this.level().getFluidState(blockPos).getType() == Fluids.WATER) {
                    this.blockState = this.blockState.setValue(BlockStateProperties.WATERLOGGED, true);
                }

                if (this.level().setBlock(blockPos, this.blockState, 3)) {
                    ((ServerLevel) this.level())
                        .getChunkSource()
                        .chunkMap
                        .broadcast(this, new ClientboundBlockUpdatePacket(blockPos, this.level().getBlockState(blockPos)));
                    this.discard();
                    if (block instanceof Fallable) {
                        ((Fallable) block).onLand(this.level(), blockPos, this.blockState, blockState, this);
                    }
                } else if (this.dropItem && this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                    this.discard();
                    this.callOnBrokenAfterFall(block, blockPos);
                    this.spawnAtLocation(block);
                }
            } else {
                this.discard();
                if (!this.dropItem || !this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) break whenStop;
                this.callOnBrokenAfterFall(block, blockPos);
                this.spawnAtLocation(block);
            }
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
    }

    @Override
    public void move(MoverType type, Vec3 motion) {
        super.move(type, motion);
        if (motion.equals(Vec3.ZERO)) return;
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
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return EntityDimensions.scalable(0.98f, 0.98f);
    }

    public static boolean isFree(Level level, BlockPos pos) {
        return FallingBlock.isFree(level.getBlockState(pos))
            || level.getBlockState(pos).getCollisionShape(level, pos).equals(Shapes.empty());
    }
}
