package dev.dubhe.anvilcraft.block;

import com.google.common.collect.ImmutableMap;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.WorkshopCube;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class WorkshopBlock extends SimpleMultiPartBlock<Cube3x3PartHalf> {
    public static final EnumProperty<Cube3x3PartHalf> PART = EnumProperty.create("part", Cube3x3PartHalf.class);
    public static final EnumProperty<WorkshopCube> CUBE = EnumProperty.create("cube", WorkshopCube.class);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final Map<Direction, Map<Cube3x3PartHalf, VoxelShape>> SHAPES;

    static {
        ImmutableMap.Builder<Direction, Map<Cube3x3PartHalf, VoxelShape>> directionBuilder = ImmutableMap.builder();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            VoxelShape baseShape = makeShape(direction);
            ImmutableMap.Builder<Cube3x3PartHalf, VoxelShape> partBuilder = ImmutableMap.builder();
            for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
                partBuilder.put(part, baseShape.move(-part.getOffsetX(), 1 - part.getOffsetY(), -part.getOffsetZ()));
            }
            directionBuilder.put(direction, partBuilder.build());
        }
        SHAPES = directionBuilder.build();
    }

    public WorkshopBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(PART, Cube3x3PartHalf.BOTTOM_CENTER)
            .setValue(CUBE, WorkshopCube.CORNER)
            .setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState placedState(Cube3x3PartHalf part, BlockState state) {
        return super.placedState(part, state)
            .setValue(CUBE, part == Cube3x3PartHalf.MID_CENTER ? WorkshopCube.CENTER : WorkshopCube.CORNER);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public Property<Cube3x3PartHalf> getPart() {
        return PART;
    }

    @Override
    public Cube3x3PartHalf[] getParts() {
        return Cube3x3PartHalf.values();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, CUBE, FACING);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
            .setValue(PART, state.getValue(PART).rotate(rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)))
            .setValue(PART, state.getValue(PART).mirror(mirror));
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING)).get(state.getValue(PART));
    }

    public static VoxelShape makeShape(Direction direction) {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, box(-1, -1, -1, 2, -0.5, 2, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(0.0625, -0.5, 1.125, 0.1875, 0.1875, 1.8125, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(1.125, -0.5, 1.125, 2, 0.1875, 1.8125, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-1, -0.5, 1.8125, 2, 0.1875, 2, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-1, 0.1875, 1.125, 2, 0.375, 2, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-0.5, 0.5625, 1.875, 2, 1.5625, 2, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(1.8125, 0.375, 1.875, 1.9375, 0.5625, 2, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-0.4375, 0.375, 1.875, -0.3125, 0.5625, 2, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(0.3125, 0.375, 1.875, 0.4375, 0.5625, 2, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(1.0625, 0.375, 1.875, 1.1875, 0.5625, 2, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(0.1875, -0.125, 1.1875, 1.125, 0, 1.8125, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(0.1875, -0.4375, 1.1875, 1.125, -0.3125, 1.8125, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-1, -0.5, 1.125, -0.875, 0.1875, 1.8125, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-0.875, -0.375, -0.875, 0, 0.125, 1.375, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-1, 0.3125, -1, -0.875, 1.5625, 1, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-1, -0.5, -0.875, -0.875, 0.3125, -0.75, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-1, -0.5, 0.75, -0.875, 0.3125, 0.875, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-0.1875, -0.5, -0.875, 0, -0.375, -0.6875, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-0.875, -0.5, -0.875, -0.6875, -0.375, -0.6875, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-0.1875, -0.5, -0.1875, 0, -0.375, 0, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-0.875, -0.5, -0.1875, -0.6875, -0.375, 0, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-0.1875, -0.5, 0.5, 0, -0.375, 0.6875, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-0.875, -0.5, 0.5, -0.6875, -0.375, 0.6875, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-0.1875, -0.5, 1.1875, 0, -0.375, 1.375, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-0.875, -0.5, 1.1875, -0.6875, -0.375, 1.375, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-0.6875, 0.125, -0.75, -0.1875, 0.625, 0.125, direction), BooleanOp.OR);
        shape = Shapes.join(shape, box(-0.4375, 0.125, -0.6875, -0.4375, 0.5, 0.0625, direction), BooleanOp.OR);
        return shape;
    }

    @SuppressWarnings("checkstyle:MultipleVariableDeclarations")
    private static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2, Direction direction) {
        double cx = 0.5;
        double cz = 0.5;

        double x1r = x1 - cx;
        double z1r = z1 - cz;
        double x2r = x2 - cx;
        double z2r = z2 - cz;

        double nx1, nz1, nx2, nz2;

        switch (direction) {
            case EAST:
                nx1 = -z1r;
                nz1 = x1r;
                nx2 = -z2r;
                nz2 = x2r;
                break;
            case SOUTH:
                nx1 = -x1r;
                nz1 = -z1r;
                nx2 = -x2r;
                nz2 = -z2r;
                break;
            case WEST:
                nx1 = z1r;
                nz1 = -x1r;
                nx2 = z2r;
                nz2 = -x2r;
                break;
            default:
                nx1 = x1r;
                nz1 = z1r;
                nx2 = x2r;
                nz2 = z2r;
                break;
        }

        nx1 += cx;
        nz1 += cz;
        nx2 += cx;
        nz2 += cz;

        return Shapes.box(Math.min(nx1, nx2), y1, Math.min(nz1, nz2), Math.max(nx1, nx2), y2, Math.max(nz1, nz2));
    }
}