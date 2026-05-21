package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.block.entity.AccelerationRingBlockEntity;
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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class AccelerationRingBlock extends FlexibleMultiPartBlock<DirectionCube3x3PartHalf, DirectionProperty, Direction>
    implements MultiPartBlockEntity<DirectionCube3x3PartHalf, AccelerationRingBlock>, IHammerRemovable, IHammerChangeable {
    public static final EnumProperty<DirectionCube3x3PartHalf> HALF = EnumProperty.create("half", DirectionCube3x3PartHalf.class);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;
    public static final EnumProperty<IPowerComponent.Switch> SWITCH = IPowerComponent.SWITCH;

    public AccelerationRingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
            .any()
            .setValue(HALF, DirectionCube3x3PartHalf.BOTTOM_CENTER)
            .setValue(FACING, Direction.NORTH)
            .setValue(OVERLOAD, true)
            .setValue(SWITCH, IPowerComponent.Switch.ON));
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

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING, OVERLOAD, SWITCH);
    }

    @Override
    public BlockState placedState(DirectionCube3x3PartHalf part, BlockState state) {
        return state
            .setValue(this.getPart(), part);
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
    public void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        boolean isSignal = Arrays.stream(getParts())
            .anyMatch(it -> level.hasNeighborSignal(
                pos.subtract(state.getValue(this.getPart()).getOffset())
                    .offset(it.getOffset())
            ));
        if (isSignal && state.getValue(SWITCH) == IPowerComponent.Switch.ON) {
            updateState(level, pos, SWITCH, IPowerComponent.Switch.OFF, 3);
        } else if (!isSignal && state.getValue(SWITCH) == IPowerComponent.Switch.OFF) {
            updateState(level, pos, SWITCH, IPowerComponent.Switch.ON, 3);
            BlockPos centerPos = pos.subtract(state.getValue(HALF).getOffset()).offset(0, 1, 0);
            if (level.getBlockEntity(centerPos) instanceof IPowerConsumer powerConsumer) {
                if (powerConsumer.getGrid() == null) return;
                powerConsumer.getGrid().flush();
            }
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING).getAxis()) {
            case Z -> switch (state.getValue(HALF)) {
                case MID_CENTER, MID_S, MID_N -> Shapes.empty();
                default -> Shapes.block();
            };
            case X -> switch (state.getValue(HALF)) {
                case MID_CENTER, MID_E, MID_W -> Shapes.empty();
                default -> Shapes.block();
            };
            case Y -> switch (state.getValue(HALF)) {
                case BOTTOM_CENTER, MID_CENTER, TOP_CENTER -> Shapes.empty();
                default -> Shapes.block();
            };
        };
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public AccelerationRingBlock getMultiBlock() {
        return this;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.ACCELERATION_RING.create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (level1, pos, state1, entity) -> {
            if (entity instanceof AccelerationRingBlockEntity be) be.tick();
        };
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter getter, BlockPos pos) {
        return 1.0F;
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
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HALF, state.getValue(HALF).rotate(rotation))
            .setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(HALF, state.getValue(HALF).mirror(mirror))
            .setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
