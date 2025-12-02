package dev.dubhe.anvilcraft.block.sliding;

import dev.dubhe.anvilcraft.api.sliding.SlidingBlockStructureResolver;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.MathUtil;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, fromPos, isMoving);
        if (level.isEmptyBlock(pos.above())) return;
        BlockState topBlock = level.getBlockState(pos.above());
        if (!PistonBaseBlock.isPushable(topBlock, level, pos, null, true, null)) return;
        Direction moveTo = MathUtil.getDirection(fromPos, pos);
        if (moveTo.getAxis() == Direction.Axis.Y) return;
        if (this.canMoveBlockTo(level, pos, topBlock, moveTo)) {
            SlidingRailStopBlock.moveBlocksAbove(level, pos, moveTo);
        } else if (this.canMoveBlockTo(level, pos, topBlock, moveTo.getCounterClockWise())) {
            SlidingRailStopBlock.moveBlocksAbove(level, pos, moveTo.getCounterClockWise());
        } else if (this.canMoveBlockTo(level, pos, topBlock, moveTo.getClockWise())) {
            SlidingRailStopBlock.moveBlocksAbove(level, pos, moveTo.getClockWise());
        }
    }

    private boolean canMoveBlockTo(Level level, BlockPos pos, BlockState topBlock, Direction moveTo) {
        if (moveTo.getAxis() == Direction.Axis.Y) return false;
        BlockPos railPos = pos.relative(moveTo);
        BlockState railState = level.getBlockState(railPos);
        return Util.castSafely(railState.getBlock(), ISlidingRail.class)
            .map(rail -> rail.canMoveBlockToTop(level, railPos, railState, topBlock, moveTo.getOpposite()))
            .orElse(false);
    }

    private boolean canMoveSlidingTo(Level level, BlockPos pos, Direction moveTo) {
        if (moveTo.getAxis() == Direction.Axis.Y) return false;
        BlockPos railPos = pos.relative(moveTo);
        BlockState railState = level.getBlockState(railPos);
        return Util.castSafely(railState.getBlock(), ISlidingRail.class)
            .map(rail -> rail.canMoveSlidingToTop(level, railPos, railState, moveTo.getOpposite()))
            .orElse(false);
    }

    private static void moveBlocksAbove(Level level, BlockPos pos, Direction moveToSide) {
        SlidingBlockStructureResolver resolver = new SlidingBlockStructureResolver(level, pos.above(), moveToSide, true);
        if (!resolver.resolve()) return;
        List<Triple<BlockPos, BlockState, Optional<CompoundTag>>> toPushes = new ArrayList<>();
        List<BlockPos> toPushPoses = new ArrayList<>(resolver.getToPush());

        for (Iterator<BlockPos> iterator = toPushPoses.iterator(); iterator.hasNext(); ) {
            BlockPos toPushPos = iterator.next();
            if (toPushPos.equals(pos)) {
                iterator.remove();
                continue;
            }
            BlockState toPushState = level.getBlockState(toPushPos);
            if (toPushState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                toPushState = toPushState.setValue(BlockStateProperties.WATERLOGGED, false);
            }
            Optional<CompoundTag> toPushEntityData = Optional.ofNullable(level.getBlockEntity(toPushPos))
                .map(entity -> entity.saveCustomOnly(level.registryAccess()));
            toPushes.add(Triple.of(toPushPos, toPushState, toPushEntityData));
        }

        List<BlockPos> toDestroys = resolver.getToDestroy();

        for (int i = toDestroys.size() - 1; i >= 0; i--) {
            BlockPos destroyingPos = toDestroys.get(i);
            BlockState destroyingState = level.getBlockState(destroyingPos);
            BlockEntity destroyingEntity = destroyingState.hasBlockEntity() ? level.getBlockEntity(destroyingPos) : null;
            Block.dropResources(destroyingState, level, destroyingPos, destroyingEntity);
            destroyingState.onDestroyedByPushReaction(level, destroyingPos, moveToSide, level.getFluidState(destroyingPos));
        }

        BlockState air = Blocks.AIR.defaultBlockState();

        for (BlockPos toPushPos : toPushPoses) {
            level.setBlock(toPushPos, air, 0b1010010);
        }

        for (var toPushEntry : toPushes) {
            BlockPos toPushPos = toPushEntry.getLeft();
            BlockState toPushState = toPushEntry.getMiddle();
            toPushState.updateIndirectNeighbourShapes(level, toPushPos, 0b0000010);
            air.updateNeighbourShapes(level, toPushPos, 0b0000010);
            air.updateIndirectNeighbourShapes(level, toPushPos, 0b0000010);
        }

        for (var toPushEntry : toPushes) {
            level.updateNeighborsAt(toPushEntry.getLeft(), air.getBlock());
        }

        SlidingBlockEntity.slid(level, pos.above(), moveToSide, toPushes);
    }
}
