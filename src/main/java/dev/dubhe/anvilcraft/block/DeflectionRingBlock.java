package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.FlexibleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class DeflectionRingBlock extends FlexibleMultiPartBlock<DirectionCube3x3PartHalf, DirectionProperty, Direction> implements EntityBlock {
    public static final EnumProperty<DirectionCube3x3PartHalf> HALF = EnumProperty.create("half", DirectionCube3x3PartHalf.class);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;
    public static final EnumProperty<IPowerComponent.Switch> SWITCH = IPowerComponent.SWITCH;

    public DeflectionRingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
            .any()
            .setValue(HALF, DirectionCube3x3PartHalf.BOTTOM_CENTER)
            .setValue(FACING, Direction.NORTH)
            .setValue(OVERLOAD, true)
            .setValue(SWITCH, IPowerComponent.Switch.ON));
    }

    @Override
    public @NotNull Property<DirectionCube3x3PartHalf> getPart() {
        return HALF;
    }

    @Override
    public DirectionCube3x3PartHalf @NotNull [] getParts() {
        return DirectionCube3x3PartHalf.values();
    }

    @Override
    public @NotNull DirectionProperty getAdditionalProperty() {
        return FACING;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING, OVERLOAD, SWITCH);
    }

    @Override
    protected @NotNull BlockState placedState(@NotNull DirectionCube3x3PartHalf part, BlockState state) {
        return state
            .setValue(this.getPart(), part);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getPlayer() != null && context.getPlayer().isShiftKeyDown() ? context.getNearestLookingDirection().getOpposite() : context.getNearestLookingDirection());
    }


    @Override
    public void neighborChanged(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Block neighborBlock,
        @NotNull BlockPos neighborPos,
        boolean movedByPiston
    ) {
        boolean isSignal = Arrays.stream(getParts()).anyMatch(it -> level.hasNeighborSignal(pos.subtract(state.getValue(getPart()).getOffset()).offset(it.getOffset())));
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
    protected @NotNull VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return switch (state.getValue(FACING).getAxis()) {
            case Z -> state.getValue(HALF).getOffset().getZ() == 0 ? Shapes.empty() : Shapes.block();
            case X -> state.getValue(HALF).getOffset().getX() == 0 ? Shapes.empty() : Shapes.block();
            case Y -> state.getValue(HALF).getOffset().getY() == 1 ? Shapes.empty() : Shapes.block();
        };
    }

    @Override
    protected @NotNull VoxelShape getInteractionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new DeflectionRingBlockEntity(blockPos, blockState);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        return (level1, pos, state1, entity) -> {
            if (entity instanceof DeflectionRingBlockEntity be) be.tick();
        };
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
        if (!(blockState.getBlock() instanceof AbstractMultiPartBlock<?> abstractMultiPartBlock)) return 0;
        BlockPos blockPos = abstractMultiPartBlock.getMainPartPos(pos, blockState);
        if (!(level.getBlockEntity(blockPos) instanceof DeflectionRingBlockEntity blockEntity)) return 0;
        if (blockEntity.getLastEntitySpeed() <= 0) return 0;
        return Math.max(0, (int) (Math.log(blockEntity.getLastEntitySpeed()) / Math.log(2) * 2) - 1);
    }

    @Override
    protected float getShadeBrightness(@NotNull BlockState state, @NotNull BlockGetter getter, @NotNull BlockPos pos) {
        return 1.0F;
    }
}
