package dev.dubhe.anvilcraft.block;

import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import static dev.dubhe.anvilcraft.block.PropelPistonBlock.createTickerHelper;

public class LargeLaserBlock extends FlexibleMultiPartBlock<DirectionCube3x3PartHalf, DirectionProperty, Direction>
    implements IHammerRemovable, IHammerChangeable, MultiPartBlockEntity<DirectionCube3x3PartHalf, LargeLaserBlock> {
    public static final EnumProperty<DirectionCube3x3PartHalf> HALF = EnumProperty.create("half", DirectionCube3x3PartHalf.class);
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;
    public static final EnumProperty<IPowerComponent.Switch> SWITCH = IPowerComponent.SWITCH;

    private static final VoxelShape DOWN_COLLISION_SHAPE = ShapeUtil.merge(
        new AABB(0, -7, -16, 16, 32, 32),
        new AABB(-16, -7, 0, 32, 32, 16),
        new AABB(-10, -7, -10, 0, 32, 26),
        new AABB(16, -7, -10, 26, 32, 26),
        new AABB(3, -16, 3, 13, -10, 13),
        new AABB(-5, -10, -5, 21, -7, 21)
    );
    private static final Map<Direction, Map<DirectionCube3x3PartHalf, VoxelShape>> COLLISION_SHAPES = makeCollisionShapes();

    public LargeLaserBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
            .any()
            .setValue(HALF, DirectionCube3x3PartHalf.BOTTOM_CENTER)
            .setValue(FACING, Direction.DOWN)
            .setValue(OVERLOAD, true)
            .setValue(SWITCH, IPowerComponent.Switch.ON)
        );
    }

    private static Map<Direction, Map<DirectionCube3x3PartHalf, VoxelShape>> makeCollisionShapes() {
        Map<Direction, Map<DirectionCube3x3PartHalf, VoxelShape>> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.DOWN, makePartShapes(DOWN_COLLISION_SHAPE));
        shapes.put(Direction.UP, makePartShapes(ShapeUtil.rotate(Direction.Axis.X, 180, DOWN_COLLISION_SHAPE)));
        shapes.put(Direction.SOUTH, makePartShapes(ShapeUtil.rotate(Direction.Axis.X, 90, DOWN_COLLISION_SHAPE)));
        shapes.put(Direction.NORTH, makePartShapes(ShapeUtil.rotate(Direction.Axis.X, 270, DOWN_COLLISION_SHAPE)));
        shapes.put(Direction.WEST, makePartShapes(ShapeUtil.rotate(
            Direction.Axis.Y,
            270,
            ShapeUtil.rotate(Direction.Axis.X, 90, DOWN_COLLISION_SHAPE)
        )));
        shapes.put(Direction.EAST, makePartShapes(ShapeUtil.rotate(
            Direction.Axis.Y,
            270,
            ShapeUtil.rotate(Direction.Axis.X, 270, DOWN_COLLISION_SHAPE)
        )));
        return shapes;
    }

    private static Map<DirectionCube3x3PartHalf, VoxelShape> makePartShapes(VoxelShape shape) {
        Map<DirectionCube3x3PartHalf, VoxelShape> shapes = new EnumMap<>(DirectionCube3x3PartHalf.class);
        for (DirectionCube3x3PartHalf part : DirectionCube3x3PartHalf.values()) {
            ArrayList<AABB> partBoxes = new ArrayList<>();
            for (AABB box : shape.toAabbs()) {
                AABB clipped = clipToPart(scale16(box), part);
                if (clipped != null) partBoxes.add(clipped);
            }
            shapes.put(
                part,
                partBoxes.isEmpty()
                    ? Shapes.empty()
                    : ShapeUtil.merge(partBoxes.toArray(AABB[]::new))
            );
        }
        return shapes;
    }

    private static AABB scale16(AABB box) {
        return new AABB(box.getMinPosition().scale(16), box.getMaxPosition().scale(16));
    }

    @Nullable
    private static AABB clipToPart(AABB box, DirectionCube3x3PartHalf part) {
        double originX = part.getOffsetX() * 16.0;
        double originY = (part.getOffsetY() - DirectionCube3x3PartHalf.MID_CENTER.getOffsetY()) * 16.0;
        double originZ = part.getOffsetZ() * 16.0;
        double minX = Math.max(box.minX, originX);
        double minY = Math.max(box.minY, originY);
        double minZ = Math.max(box.minZ, originZ);
        double maxX = Math.min(box.maxX, originX + 16.0);
        double maxY = Math.min(box.maxY, originY + 16.0);
        double maxZ = Math.min(box.maxZ, originZ + 16.0);
        if (minX >= maxX || minY >= maxY || minZ >= maxZ) return null;
        return new AABB(
            minX - originX,
            minY - originY,
            minZ - originZ,
            maxX - originX,
            maxY - originY,
            maxZ - originZ
        );
    }

    @Override
    public Property<DirectionCube3x3PartHalf> getPart() {
        return HALF;
    }

    @Override
    public DirectionCube3x3PartHalf[] getParts() {
        return DirectionCube3x3PartHalf.values();
    }

    @Override
    public DirectionProperty getAdditionalProperty() {
        return FACING;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide) return null;
        return createTickerHelper(
            blockEntityType, ModBlockEntities.LARGE_LASER.get(), (level1, pos, state1, entity) -> entity.tick(level1));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(
                FACING,
                context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                    ? context.getNearestLookingDirection().getOpposite()
                    : context.getNearestLookingDirection()
            );
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        boolean isSignal = Arrays.stream(getParts()).anyMatch((half) -> level.hasNeighborSignal(
            pos.subtract(state.getValue(this.getPart()).getOffset()).offset(half.getOffset())
        ));
        if (isSignal && state.getValue(SWITCH) == IPowerComponent.Switch.ON) {
            this.updateState(level, pos, SWITCH, IPowerComponent.Switch.OFF, 3);
        } else if (!isSignal && state.getValue(SWITCH) == IPowerComponent.Switch.OFF) {
            this.updateState(level, pos, SWITCH, IPowerComponent.Switch.ON, 3);
            if (level.getBlockEntity(getMainPartPos(pos, state)) instanceof IPowerConsumer powerConsumer) {
                if (powerConsumer.getGrid() == null) {
                    return;
                }
                powerConsumer.getGrid().flush();
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING, OVERLOAD, SWITCH);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HALF, state.getValue(HALF).rotate(rotation))
            .setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(HALF, state.getValue(HALF).mirror(mirror))
            .setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public VoxelShape getPartShape(BlockState state) {
        return COLLISION_SHAPES.get(state.getValue(FACING)).get(state.getValue(HALF));
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        this.change(blockPos, level, (state) -> state.cycle(FACING));
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }

    @Override
    public LargeLaserBlock getMultiBlock() {
        return this;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.LARGE_LASER.create(pos, state);
    }
}
