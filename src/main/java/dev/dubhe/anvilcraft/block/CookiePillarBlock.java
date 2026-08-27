package dev.dubhe.anvilcraft.block;

import dev.anvilcraft.lib.v2.util.ShapeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CookiePillarBlock extends RotatedPillarBlock {
    private static final VoxelShape SHAPE_Y = ShapeUtil.cut(
        Block.box(0, 0, 0, 16, 16, 16),
        Block.box(3, 0, 3, 13, 16, 13)
    );
    private static final VoxelShape SHAPE_X = ShapeUtil.rotate(Direction.Axis.Z, 90, SHAPE_Y);
    private static final VoxelShape SHAPE_Z = ShapeUtil.rotate(Direction.Axis.X, 90, SHAPE_Y);
    private static final VoxelShape FULL = Block.box(0, 0, 0, 16, 16, 16);

    public CookiePillarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(
        BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL;
    }

    @Override
    public VoxelShape getCollisionShape(
        BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            default -> SHAPE_Y;
        };
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return switch (state.getValue(AXIS)) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            default -> SHAPE_Y;
        };
    }
}
