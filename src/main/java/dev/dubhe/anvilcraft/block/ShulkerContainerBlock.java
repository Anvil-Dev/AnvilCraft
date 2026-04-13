package dev.dubhe.anvilcraft.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.state.OpenedCube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.util.ShapeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShulkerContainerBlock
    extends FlexibleMultiPartBlock<OpenedCube3x3PartHalf, BooleanProperty, Boolean>
    implements MultiPartBlockEntity<OpenedCube3x3PartHalf, ShulkerContainerBlock>, IHammerRemovable { // TODO: 实现潜影集装箱功能
    public static final EnumProperty<OpenedCube3x3PartHalf> HALF = EnumProperty.create("half", OpenedCube3x3PartHalf.class);
    public static final BooleanProperty OPENED = BooleanProperty.create("opened");

    private static final ImmutableMap<Direction, ImmutableList<Vec3i>> UPDATE_OFFSET = ImmutableMap.of(
        Direction.DOWN,
        ImmutableList.of(
            new Vec3i(-1, 3, -1),
            new Vec3i(-1, 3, 0),
            new Vec3i(-1, 3, 1),
            new Vec3i(0, 3, -1),
            new Vec3i(0, 3, 0),
            new Vec3i(0, 3, 1),
            new Vec3i(1, 3, -1),
            new Vec3i(1, 3, 0),
            new Vec3i(1, 3, 1)
        ),
        Direction.UP,
        ImmutableList.of(
            new Vec3i(-1, -1, -1),
            new Vec3i(-1, -1, 0),
            new Vec3i(-1, -1, 1),
            new Vec3i(0, -1, -1),
            new Vec3i(0, -1, 0),
            new Vec3i(0, -1, 1),
            new Vec3i(1, -1, -1),
            new Vec3i(1, -1, 0),
            new Vec3i(1, -1, 1)
        ),
        Direction.EAST,
        ImmutableList.of(
            new Vec3i(-2, 0, -1),
            new Vec3i(-2, 0, 0),
            new Vec3i(-2, 0, 1),
            new Vec3i(-2, 1, -1),
            new Vec3i(-2, 1, 0),
            new Vec3i(-2, 1, 1),
            new Vec3i(-2, 2, -1),
            new Vec3i(-2, 2, 0),
            new Vec3i(-2, 2, 1)
        ),
        Direction.WEST,
        ImmutableList.of(
            new Vec3i(2, 0, -1),
            new Vec3i(2, 0, 0),
            new Vec3i(2, 0, 1),
            new Vec3i(2, 1, -1),
            new Vec3i(2, 1, 0),
            new Vec3i(2, 1, 1),
            new Vec3i(2, 2, -1),
            new Vec3i(2, 2, 0),
            new Vec3i(2, 2, 1)
        ),
        Direction.SOUTH,
        ImmutableList.of(
            new Vec3i(-1, 0, -2),
            new Vec3i(0, 0, -2),
            new Vec3i(1, 0, -2),
            new Vec3i(-1, 1, -2),
            new Vec3i(0, 1, -2),
            new Vec3i(1, 1, -2),
            new Vec3i(-1, 2, -2),
            new Vec3i(0, 2, -2),
            new Vec3i(1, 2, -2)
        ),
        Direction.NORTH,
        ImmutableList.of(
            new Vec3i(-1, 0, 2),
            new Vec3i(0, 0, 2),
            new Vec3i(1, 0, 2),
            new Vec3i(-1, 1, 2),
            new Vec3i(0, 1, 2),
            new Vec3i(1, 1, 2),
            new Vec3i(-1, 2, 2),
            new Vec3i(0, 2, 2),
            new Vec3i(1, 2, 2)
        )
    );

    public ShulkerContainerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(HALF, OpenedCube3x3PartHalf.BOTTOM_CENTER)
                .setValue(OPENED, false)
        );
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(HALF)) {
            case BOTTOM_CENTER -> BOTTOM_CENTER;
            case BOTTOM_W -> BOTTOM_W;
            case BOTTOM_E -> BOTTOM_E;
            case BOTTOM_N -> BOTTOM_N;
            case BOTTOM_S -> BOTTOM_S;
            case BOTTOM_NW -> BOTTOM_NW;
            case BOTTOM_SW -> BOTTOM_SW;
            case BOTTOM_NE -> BOTTOM_NE;
            case BOTTOM_SE -> BOTTOM_SE;
            case MID_CENTER -> MID_CENTER;
            case MID_W -> MID_W;
            case MID_E -> MID_E;
            case MID_N -> MID_N;
            case MID_S -> MID_S;
            case MID_NW -> MID_NW;
            case MID_SW -> MID_SW;
            case MID_NE -> MID_NE;
            case MID_SE -> MID_SE;
            case TOP_CENTER -> TOP_CENTER;
            case TOP_W -> TOP_W;
            case TOP_E -> TOP_E;
            case TOP_N -> TOP_N;
            case TOP_S -> TOP_S;
            case TOP_NW -> TOP_NW;
            case TOP_SW -> TOP_SW;
            case TOP_NE -> TOP_NE;
            case TOP_SE -> TOP_SE;
        };
    }

    @Override
    public void removePartsAndUpdate(Level level, BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        if (!blockState.is(this)) return;
        BlockPos bottomCenterPos = this.getMainPartPos(pos, blockState).below();
        for (OpenedCube3x3PartHalf part : this.getParts()) {
            BlockPos bp = bottomCenterPos.offset(part.getOffset());
            level.setBlock(bp, level.getBlockState(bp).getFluidState().createLegacyBlock(), 3, 0);
        }
        ShulkerContainerBlock.UPDATE_OFFSET.forEach((direction, offsetList) -> offsetList.forEach(offset -> {
            BlockPos updatedPos = bottomCenterPos.offset(offset);
            BlockPos fromPos = updatedPos.relative(direction);
            level.neighborShapeChanged(
                direction,
                level.getBlockState(fromPos),
                updatedPos,
                fromPos,
                3,
                512
            );
        }));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HALF, state.getValue(HALF).rotate(rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(HALF, state.getValue(HALF).mirror(mirror));
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter getter, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public Property<OpenedCube3x3PartHalf> getPart() {
        return ShulkerContainerBlock.HALF;
    }

    @Override
    public OpenedCube3x3PartHalf[] getParts() {
        return OpenedCube3x3PartHalf.values();
    }

    @Override
    public BooleanProperty getAdditionalProperty() {
        return ShulkerContainerBlock.OPENED;
    }

    @Override
    public ShulkerContainerBlock getMultiBlock() {
        return this;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.SHULKER_CONTAINER.create(pos, state);
    }

    // region VoxelShapes
    protected static final VoxelShape MID_CENTER = Shapes.block();

    protected static final VoxelShape BOTTOM_CENTER = Block.box(0, 2, 0, 16, 16, 16);
    protected static final VoxelShape TOP_CENTER = ShapeUtil.rotate(Direction.Axis.X, 180, BOTTOM_CENTER);
    protected static final VoxelShape MID_N = ShapeUtil.rotate(Direction.Axis.X, 270, BOTTOM_CENTER);
    protected static final VoxelShape MID_W = ShapeUtil.rotate(Direction.Axis.Y, 90, MID_N);
    protected static final VoxelShape MID_S = ShapeUtil.rotate(Direction.Axis.Y, 180, MID_N);
    protected static final VoxelShape MID_E = ShapeUtil.rotate(Direction.Axis.Y, 270, MID_N);

    protected static final VoxelShape BOTTOM_N = ShapeUtil.merge(
        new AABB(0, 2, 2, 16, 16, 16),
        new AABB(0, 0, 0, 16, 5, 5),
        new AABB(0, 0, 0, 4, 8, 8),
        new AABB(12, 0, 0, 16, 8, 8)
    );
    protected static final VoxelShape BOTTOM_W = ShapeUtil.rotate(Direction.Axis.Y, 90, BOTTOM_N);
    protected static final VoxelShape BOTTOM_S = ShapeUtil.rotate(Direction.Axis.Y, 180, BOTTOM_N);
    protected static final VoxelShape BOTTOM_E = ShapeUtil.rotate(Direction.Axis.Y, 270, BOTTOM_N);

    protected static final VoxelShape BOTTOM_NW = ShapeUtil.merge(
        new AABB(2, 2, 2, 16, 16, 16),
        new AABB(0, 0, 0, 12, 8, 8),
        new AABB(0, 8, 0, 8, 12, 8),
        new AABB(0, 0, 8, 8, 8, 12)
    );
    protected static final VoxelShape BOTTOM_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, BOTTOM_NW);
    protected static final VoxelShape BOTTOM_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, BOTTOM_NW);
    protected static final VoxelShape BOTTOM_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, BOTTOM_NW);

    protected static final VoxelShape MID_NW = ShapeUtil.rotate(Direction.Axis.Z, 90, BOTTOM_N);
    protected static final VoxelShape MID_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, MID_NW);
    protected static final VoxelShape MID_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, MID_NW);
    protected static final VoxelShape MID_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, MID_NW);

    protected static final VoxelShape TOP_N = ShapeUtil.rotate(Direction.Axis.Z, 180, BOTTOM_N);
    protected static final VoxelShape TOP_W = ShapeUtil.rotate(Direction.Axis.Y, 90, TOP_N);
    protected static final VoxelShape TOP_S = ShapeUtil.rotate(Direction.Axis.Y, 180, TOP_N);
    protected static final VoxelShape TOP_E = ShapeUtil.rotate(Direction.Axis.Y, 270, TOP_N);

    protected static final VoxelShape TOP_NW = ShapeUtil.rotate(Direction.Axis.X, 270, BOTTOM_NW);
    protected static final VoxelShape TOP_SW = ShapeUtil.rotate(Direction.Axis.Y, 90, TOP_NW);
    protected static final VoxelShape TOP_SE = ShapeUtil.rotate(Direction.Axis.Y, 180, TOP_NW);
    protected static final VoxelShape TOP_NE = ShapeUtil.rotate(Direction.Axis.Y, 270, TOP_NW);
    // endregion
}