package dev.dubhe.anvilcraft.block.sliding;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.stream.Stream;

public class SlidingRailStopBlock extends BaseSlidingRailBlock {
    public static final VoxelShape SHAPE = Stream.of(
        Block.box(11, 6, 11, 16, 16, 16),
        Block.box(0, 0, 0, 16, 6, 16),
        Block.box(11, 6, 0, 16, 16, 5),
        Block.box(0, 6, 0, 5, 16, 5),
        Block.box(0, 6, 11, 5, 16, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    public SlidingRailStopBlock(Properties properties) {
        super(properties, false);
    }

    @Override
    protected VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        ISlidingRail.absorbEntity(pos, entity);
        if (entity.getType() != EntityType.ITEM) return;
        double dx = entity.getX() - Math.floor(entity.getX());
        if (!MathUtil.isInRange(dx, 0.374, 0.626)) return;
        double dz = entity.getZ() - Math.floor(entity.getZ());
        if (!MathUtil.isInRange(dz, 0.374, 0.626)) return;

        Direction side = entity.getMotionDirection();
        if (side.getAxis() == Direction.Axis.Y) side = Direction.NORTH;

        BlockPos pos1 = pos.relative(side);
        BlockState state1 = level.getBlockState(pos1);
        if (this.tryTeleportToSlidingRail(pos1, state1, side, entity)) return;
        pos1 = pos.relative(side.getCounterClockWise());
        state1 = level.getBlockState(pos1);
        if (this.tryTeleportToSlidingRail(pos1, state1, side.getCounterClockWise(), entity)) return;
        pos1 = pos.relative(side.getClockWise());
        state1 = level.getBlockState(pos1);
        if (this.tryTeleportToSlidingRail(pos1, state1, side.getClockWise(), entity)) return;
        pos1 = pos.relative(side.getOpposite());
        state1 = level.getBlockState(pos1);
        this.tryTeleportToSlidingRail(pos1, state1, side.getOpposite(), entity);
    }

    private boolean tryTeleportToSlidingRail(BlockPos pos, BlockState state, Direction direction, Entity item) {
        if (!state.is(ModBlocks.POWERED_SLIDING_RAIL)) return false;
        if (state.getOptionalValue(PoweredSlidingRailBlock.FACING).map(dir -> dir != direction).orElse(false)) return false;
        if (!state.getOptionalValue(BlockStateProperties.POWERED).orElse(false)) return false;
        item.setPos(pos.getBottomCenter().add(0, 0.375, 0));
        return true;
    }

    @Override
    public void onSlidingAbove(Level level, BlockPos pos, BlockState state, SlidingBlockEntity entity) {
        Direction moveTo = entity.getMoveDirection();
        if (this.canMoveSlidingTo(level, pos, moveTo)) {
            return;
        } else if (this.canMoveSlidingTo(level, pos, moveTo.getCounterClockWise())) {
            entity.setMoveDirection(moveTo.getCounterClockWise());
            return;
        } else if (this.canMoveSlidingTo(level, pos, moveTo.getClockWise())) {
            entity.setMoveDirection(moveTo.getClockWise());
            return;
        } else if (this.canMoveSlidingTo(level, pos, moveTo.getOpposite())) {
            entity.setMoveDirection(moveTo.getOpposite());
            return;
        }
        ISlidingRail.stopSlidingBlock(entity);
    }

    private boolean canMoveSlidingTo(Level level, BlockPos pos, Direction moveTo) {
        if (moveTo.getAxis() == Direction.Axis.Y) return false;
        BlockPos railPos = pos.relative(moveTo);
        BlockState railState = level.getBlockState(railPos);
        return Util.castSafely(railState.getBlock(), ISlidingRail.class)
            .map(rail -> rail.canMoveSlidingToTop(level, railPos, railState, moveTo.getOpposite()))
            .orElse(false);
    }
}
