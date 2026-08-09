package dev.dubhe.anvilcraft.block.power.ring;

import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.WaterloggedFlexibleMultiPartBlock;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class DeflectionRingBlock
    extends WaterloggedFlexibleMultiPartBlock<DirectionCube3x3PartHalf, EnumProperty<Direction>, Direction>
    implements MultiPartBlockEntity<DirectionCube3x3PartHalf, DeflectionRingBlock>, IHammerRemovable, IHammerChangeable {
    public static final EnumProperty<DirectionCube3x3PartHalf> HALF = EnumProperty.create("half", DirectionCube3x3PartHalf.class);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;
    public static final EnumProperty<IPowerComponent.Switch> SWITCH = IPowerComponent.SWITCH;

    private static final VoxelShape Y_AXIS_COLLISION_SHAPE = ShapeUtil.merge(
        new AABB(-16, -16, -3, 32, 0, 19),
        new AABB(-11, -16, -11, 27, 0, 27),
        new AABB(-16, 16, -3, 32, 32, 19),
        new AABB(-11, 16, -11, 27, 32, 27),
        new AABB(-3, -16, -16, 19, 0, 32),
        new AABB(-3, 16, -16, 19, 32, 32)
    );
    private static final Map<Direction.Axis, Map<DirectionCube3x3PartHalf, VoxelShape>> COLLISION_SHAPES =
        DeflectionRingBlock.makeCollisionShapes();

    public DeflectionRingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
            .any()
            .setValue(DeflectionRingBlock.HALF, DirectionCube3x3PartHalf.BOTTOM_CENTER)
            .setValue(DeflectionRingBlock.FACING, Direction.NORTH)
            .setValue(DeflectionRingBlock.OVERLOAD, true)
            .setValue(DeflectionRingBlock.SWITCH, IPowerComponent.Switch.ON));
    }

    private static Map<Direction.Axis, Map<DirectionCube3x3PartHalf, VoxelShape>> makeCollisionShapes() {
        Map<Direction.Axis, Map<DirectionCube3x3PartHalf, VoxelShape>> shapes = new EnumMap<>(Direction.Axis.class);
        shapes.put(Direction.Axis.Y, DeflectionRingBlock.makePartShapes(DeflectionRingBlock.Y_AXIS_COLLISION_SHAPE));
        shapes.put(
            Direction.Axis.Z,
            DeflectionRingBlock.makePartShapes(
                ShapeUtil.rotate(Direction.Axis.X, 90, DeflectionRingBlock.Y_AXIS_COLLISION_SHAPE)
            )
        );
        shapes.put(
            Direction.Axis.X,
            DeflectionRingBlock.makePartShapes(
                ShapeUtil.rotate(Direction.Axis.Z, 90, DeflectionRingBlock.Y_AXIS_COLLISION_SHAPE)
            )
        );
        return shapes;
    }

    private static Map<DirectionCube3x3PartHalf, VoxelShape> makePartShapes(VoxelShape shape) {
        Map<DirectionCube3x3PartHalf, VoxelShape> shapes = new EnumMap<>(DirectionCube3x3PartHalf.class);
        for (DirectionCube3x3PartHalf part : DirectionCube3x3PartHalf.values()) {
            ArrayList<AABB> partBoxes = new ArrayList<>();
            for (AABB box : shape.toAabbs()) {
                AABB clipped = DeflectionRingBlock.clipToPart(DeflectionRingBlock.scale16(box), part);
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
        return DeflectionRingBlock.HALF;
    }

    @Override
    public DirectionCube3x3PartHalf[] getParts() {
        return DirectionCube3x3PartHalf.values();
    }

    @Override
    public EnumProperty<Direction> getAdditionalProperty() {
        return DeflectionRingBlock.FACING;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DeflectionRingBlock.OVERLOAD, DeflectionRingBlock.SWITCH);
    }

    @Override
    public BlockState placedState(DirectionCube3x3PartHalf part, BlockState state) {
        return state
            .setValue(this.getPart(), part);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(
            DeflectionRingBlock.FACING,
            context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
            ? context.getNearestLookingDirection().getOpposite()
            : context.getNearestLookingDirection()
        );
        return this.waterloggedStateForPlacement(context, state);
    }

    public boolean isChannelWaterlogged(Level level, BlockPos mainPos, BlockState mainState) {
        Direction.Axis axis = mainState.getValue(DeflectionRingBlock.FACING).getAxis();
        for (DirectionCube3x3PartHalf part : this.getParts()) {
            if (!DeflectionRingBlock.isChannelPart(part, axis)) continue;
            BlockState partState = level.getBlockState(mainPos.offset(this.offsetFrom(mainState, part)));
            if (partState.is(this) && partState.getValue(WaterloggedFlexibleMultiPartBlock.WATERLOGGED)) return true;
        }
        return false;
    }

    private static boolean isChannelPart(DirectionCube3x3PartHalf part, Direction.Axis axis) {
        return switch (axis) {
            case X -> part.getOffsetX() == 0;
            case Y -> part.getOffsetY() == 1;
            case Z -> part.getOffsetZ() == 0;
        };
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        @Nullable Orientation orientation,
        boolean movedByPiston
    ) {
        boolean isSignal = Arrays.stream(this.getParts())
            .anyMatch(it -> level.hasNeighborSignal(
                pos.subtract(state.getValue(this.getPart()).getOffset()).offset(it.getOffset())
            ));
        if (isSignal && state.getValue(DeflectionRingBlock.SWITCH) == IPowerComponent.Switch.ON) {
            this.updateState(level, pos, DeflectionRingBlock.SWITCH, IPowerComponent.Switch.OFF, 3);
        } else if (!isSignal && state.getValue(DeflectionRingBlock.SWITCH) == IPowerComponent.Switch.OFF) {
            this.updateState(level, pos, DeflectionRingBlock.SWITCH, IPowerComponent.Switch.ON, 3);
            BlockPos centerPos = pos.subtract(state.getValue(DeflectionRingBlock.HALF).getOffset()).offset(0, 1, 0);
            if (level.getBlockEntity(centerPos) instanceof IPowerConsumer powerConsumer) {
                if (powerConsumer.getGrid() == null) return;
                powerConsumer.getGrid().flush();
            }
        }
    }

    @Override
    protected VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        if (context.isHoldingItem(state.getBlock().asItem())) {
            return Shapes.block();
        }
        return DeflectionRingBlock.getPreciseShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return DeflectionRingBlock.getPreciseShape(state);
    }

    private static VoxelShape getPreciseShape(BlockState state) {
        return DeflectionRingBlock.COLLISION_SHAPES
            .get(state.getValue(DeflectionRingBlock.FACING).getAxis())
            .get(state.getValue(DeflectionRingBlock.HALF));
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public DeflectionRingBlock getMultiBlock() {
        return this;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.DEFLECTION_RING.create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        return (level1, pos, state1, entity) -> {
            if (entity instanceof DeflectionRingBlockEntity be) be.tick();
        };
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos, Direction direction) {
        if (!(blockState.getBlock() instanceof AbstractMultiPartBlock<?> abstractMultiPartBlock)) return 0;
        BlockPos blockPos = abstractMultiPartBlock.getMainPartPos(pos, blockState);
        if (!(level.getBlockEntity(blockPos) instanceof DeflectionRingBlockEntity blockEntity)) return 0;
        if (blockEntity.getLastEntitySpeed() <= 0) return 0;
        return Math.max(0, (int) (Math.log(blockEntity.getLastEntitySpeed()) / Math.log(2) * 2) - 1);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter getter, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        this.change(blockPos, level, state -> state.cycle(DeflectionRingBlock.FACING));
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return DeflectionRingBlock.FACING;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(DeflectionRingBlock.HALF, state.getValue(DeflectionRingBlock.HALF).rotate(rotation))
            .setValue(DeflectionRingBlock.FACING, rotation.rotate(state.getValue(DeflectionRingBlock.FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(DeflectionRingBlock.HALF, state.getValue(DeflectionRingBlock.HALF).mirror(mirror))
            .setValue(DeflectionRingBlock.FACING, mirror.mirror(state.getValue(DeflectionRingBlock.FACING)));
    }
}
