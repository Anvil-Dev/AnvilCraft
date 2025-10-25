package dev.dubhe.anvilcraft.block.sliding;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public abstract class BaseSlidingRailBlock extends Block implements ISlidingRail, IHammerRemovable {
    public BaseSlidingRailBlock(Properties properties) {
        super(properties.friction(1.0204082f));
    }

    protected BaseSlidingRailBlock(Properties properties, boolean shouldSetFriction) {
        super(shouldSetFriction ? properties.friction(1.0204082f) : properties);
    }

    @Override
    public Block self() {
        return this;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        ISlidingRail.whenOnNeighborChange(level, pos, neighbor);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;
        ISlidingRail.whenNeighborChanged(level, self(), pos, fromPos);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        ISlidingRail.whenTick(level, self(), pos);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        return ISlidingRail.whenTriggerEvent(level, pos, param);
    }

    @Override
    public boolean isStickyBlock(BlockState state) {
        return true;
    }

    @Override
    public boolean anvilcraft$canStickTo(BlockPos pos, BlockState state, BlockPos otherPos, BlockState other) {
        if (otherPos.equals(pos.above())) return false;
        if (!AnvilCraft.CONFIG.slidingRailStickToEachOther) {
            return other.isStickyBlock() && !(other.getBlock() instanceof BaseSlidingRailBlock);
        }
        if (!other.is(ModBlockTags.STICKABLE_WITH_SLIDING_RAILS)) return other.isStickyBlock();
        Direction.Axis axis = state.getOptionalValue(BlockStateProperties.AXIS)
            .or(() -> state.getOptionalValue(BlockStateProperties.FACING).map(Direction::getAxis))
            .orElse(null);
        Direction.Axis otherAxis = other.getOptionalValue(BlockStateProperties.AXIS)
            .or(() -> state.getOptionalValue(BlockStateProperties.FACING).map(Direction::getAxis))
            .orElse(null);
        return this.canStickTo(pos, axis, otherPos, otherAxis);
    }

    private boolean canStickTo(BlockPos pos, @Nullable Direction.Axis axis, BlockPos otherPos, @Nullable Direction.Axis otherAxis) {
        if (axis == Direction.Axis.Y || otherAxis == Direction.Axis.Y) return true;
        boolean axisIsNotNull = axis != null;
        if (Objects.equals(otherAxis, axis) && axisIsNotNull) {
            return pos.relative(axis, -1).equals(otherPos) || pos.relative(axis, 1).equals(otherPos);
        }
        if (axisIsNotNull && otherAxis != null) return false;
        return pos.relative(Direction.Axis.X, -1).equals(otherPos)
               || pos.relative(Direction.Axis.X, 1).equals(otherPos)
               || pos.relative(Direction.Axis.Y, -1).equals(otherPos)
               || pos.relative(Direction.Axis.Y, 1).equals(otherPos)
               || pos.relative(Direction.Axis.Z, -1).equals(otherPos)
               || pos.relative(Direction.Axis.Z, 1).equals(otherPos);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        if (entity instanceof ItemEntity) {
            AABB railBox = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
            if (railBox.intersects(entity.getBoundingBox())) {
                if (this instanceof DetectorSlidingRailBlock detectorRail) {
                    detectorRail.onItemEntitySlidingAbove(level, pos, state);
                }
            }
        }
        super.entityInside(state, level, pos, entity);
    }
}
