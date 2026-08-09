package dev.dubhe.anvilcraft.block.power.transmitting;

import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.api.block.entity.ITickable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.entity.TransmissionPoleBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.MultiPartBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Vertical3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class TransmissionPoleBlock
    extends SimpleMultiPartBlock<Vertical3PartHalf>
    implements MultiPartBlockEntity<Vertical3PartHalf, TransmissionPoleBlock>, IHammerRemovable, IHasMultiBlock {
    public static final EnumProperty<Vertical3PartHalf> HALF = EnumProperty.create("half", Vertical3PartHalf.class);
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;
    public static final EnumProperty<IPowerComponent.Switch> SWITCH = IPowerComponent.SWITCH;
    public static final VoxelShape TRANSMISSION_POLE_TOP =
        Shapes.or(Block.box(3, 5, 3, 13, 16, 13), Block.box(6, 0, 6, 10, 5, 10));

    public static final VoxelShape TRANSMISSION_POLE_MID = Block.box(6, 0, 6, 10, 16, 10);

    public static final VoxelShape TRANSMISSION_POLE_BASE =
        Shapes.or(Block.box(3, 4, 3, 13, 10, 13), Block.box(0, 0, 0, 16, 4, 16), Block.box(6, 10, 6, 10, 16, 10));

    public TransmissionPoleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
            .any()
            .setValue(TransmissionPoleBlock.HALF, Vertical3PartHalf.BOTTOM)
            .setValue(TransmissionPoleBlock.OVERLOAD, true)
            .setValue(TransmissionPoleBlock.SWITCH, IPowerComponent.Switch.ON));
    }

    @Override
    public Property<Vertical3PartHalf> getPart() {
        return TransmissionPoleBlock.HALF;
    }

    @Override
    public Vertical3PartHalf[] getParts() {
        return Vertical3PartHalf.values();
    }

    @Override
    @Nullable
    public BlockState getPlacementState(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        IPowerComponent.Switch sw =
            level.hasNeighborSignal(pos) ? IPowerComponent.Switch.OFF : IPowerComponent.Switch.ON;
        return this.defaultBlockState()
            .setValue(TransmissionPoleBlock.HALF, Vertical3PartHalf.BOTTOM)
            .setValue(TransmissionPoleBlock.OVERLOAD, true)
            .setValue(TransmissionPoleBlock.SWITCH, sw);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TransmissionPoleBlock.HALF).add(TransmissionPoleBlock.OVERLOAD).add(TransmissionPoleBlock.SWITCH);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        if (state.getValue(TransmissionPoleBlock.HALF) == Vertical3PartHalf.BOTTOM) return TransmissionPoleBlock.TRANSMISSION_POLE_BASE;
        if (state.getValue(TransmissionPoleBlock.HALF) == Vertical3PartHalf.MID) return TransmissionPoleBlock.TRANSMISSION_POLE_MID;
        if (state.getValue(TransmissionPoleBlock.HALF) == Vertical3PartHalf.TOP) return TransmissionPoleBlock.TRANSMISSION_POLE_TOP;
        return super.getShape(state, level, pos, context);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public BlockState placedState(Vertical3PartHalf part, BlockState state) {
        return super.placedState(part, state).setValue(TransmissionPoleBlock.SWITCH, IPowerComponent.Switch.ON);
    }

    @Override
    public TransmissionPoleBlock getMultiBlock() {
        return this;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TransmissionPoleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ITickable.tickServerOnly(level, ModBlockEntities.TRANSMISSION_POLE.get(), type);
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
        if (level.isClientSide()) return;
        if (state.getValue(TransmissionPoleBlock.HALF) != Vertical3PartHalf.BOTTOM) return;
        BlockPos topPos = pos.above(2);
        BlockState topState = level.getBlockState(topPos);
        if (!topState.is(ModBlocks.TRANSMISSION_POLE.get())) return;
        if (topState.getValue(TransmissionPoleBlock.HALF) != Vertical3PartHalf.TOP) return;
        IPowerComponent.Switch sw = state.getValue(TransmissionPoleBlock.SWITCH);
        boolean bl = sw == IPowerComponent.Switch.ON;
        if (bl == level.hasNeighborSignal(pos)) {
            if (bl) {
                state = state.setValue(TransmissionPoleBlock.SWITCH, IPowerComponent.Switch.OFF);
                topState = topState.setValue(TransmissionPoleBlock.SWITCH, IPowerComponent.Switch.OFF);
            } else {
                state = state.setValue(TransmissionPoleBlock.SWITCH, IPowerComponent.Switch.ON);
                topState = topState.setValue(TransmissionPoleBlock.SWITCH, IPowerComponent.Switch.ON);
            }
            level.setBlockAndUpdate(pos, state);
            level.setBlockAndUpdate(topPos, topState);
        }
    }

    @Override
    public void onPlace(Level level, BlockPos pos, BlockState state) {
    }

    @Override
    public void onRemove(Level level, BlockPos pos, BlockState state) {
    }
}
