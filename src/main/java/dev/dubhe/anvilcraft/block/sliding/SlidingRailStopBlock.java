package dev.dubhe.anvilcraft.block.sliding;

import dev.dubhe.anvilcraft.block.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.util.MathUtil;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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
        if (!MathUtil.isInRange(entity.getX() - Math.floor(entity.getX()), 0.374, 0.626)) return;
        if (!MathUtil.isInRange(entity.getZ() - Math.floor(entity.getZ()), 0.374, 0.626)) return;
        for (Direction side : Direction.values()) {
            if (side.getAxis() == Direction.Axis.Y) continue;
            BlockPos pos1 = pos.relative(side);
            BlockState state1 = level.getBlockState(pos1);
            if (!state1.is(ModBlocks.POWERED_SLIDING_RAIL)) continue;
            if (!state1.getOptionalValue(BlockStateProperties.POWERED).orElse(false)) continue;
            entity.setPos(pos1.getBottomCenter().add(0, 0.375, 0));
            break;
        }
    }

    @Override
    public void onSlidingAbove(Level level, BlockPos pos, BlockState state, SlidingBlockEntity entity) {
        ISlidingRail.stopSlidingBlock(entity);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, fromPos, isMoving);
        if (level.isEmptyBlock(pos.above())) return;
        BlockState topBlock = level.getBlockState(pos.above());
        if (topBlock.getPistonPushReaction() == PushReaction.BLOCK
            || topBlock.getPistonPushReaction() == PushReaction.IGNORE
            || (topBlock.getBlock() instanceof EntityBlock entityBlock && !(entityBlock instanceof IMoveableEntityBlock))
        ) return;
        BlockPos moveToPos = null;
        for (Direction side : Direction.values()) {
            if (side.getAxis() == Direction.Axis.Y) continue;
            BlockPos railPos = pos.relative(side);
            BlockState railState = level.getBlockState(railPos);
            boolean canMove = Util.castSafely(railState.getBlock(), ISlidingRail.class)
                .map(rail -> rail.canMoveBlockToTop(level, railPos, railState, topBlock, side.getOpposite()))
                .orElse(false);
            if (!canMove) continue;
            moveToPos = railPos.above();
            break;
        }
        if (moveToPos == null) return;
        level.removeBlock(pos.above(), true);
        level.setBlock(moveToPos, topBlock, 0b1000011);
    }
}
