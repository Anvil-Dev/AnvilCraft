package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.entity.TransmissionPoleBlockEntity;
import dev.dubhe.anvilcraft.block.state.Vertical3PartHalf;
import dev.dubhe.anvilcraft.init.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class TransmissionPoleBlock extends AbstractMultiplePartBlock<Vertical3PartHalf>
        implements IHammerRemovable, IHasMultiBlock, EntityBlock {
    public static final EnumProperty<Vertical3PartHalf> HALF = EnumProperty.create("half", Vertical3PartHalf.class);
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;
    public static final EnumProperty<IPowerComponent.Switch> SWITCH = IPowerComponent.SWITCH;
    public static final VoxelShape TRANSMISSION_POLE_TOP =
            Shapes.or(Block.box(3, 5, 3, 13, 16, 13), Block.box(6, 0, 6, 10, 5, 10));

    public static final VoxelShape TRANSMISSION_POLE_MID = Block.box(6, 0, 6, 10, 16, 10);

    public static final VoxelShape TRANSMISSION_POLE_BASE =
            Shapes.or(Block.box(3, 4, 3, 13, 10, 13), Block.box(0, 0, 0, 16, 4, 16), Block.box(6, 10, 6, 10, 16, 10));

    /**
     * @param properties 属性
     */
    public TransmissionPoleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
                .any()
                .setValue(HALF, Vertical3PartHalf.BOTTOM)
                .setValue(OVERLOAD, true)
                .setValue(SWITCH, IPowerComponent.Switch.ON));
    }

    @Override
    public @NotNull Property<Vertical3PartHalf> getPart() {
        return TransmissionPoleBlock.HALF;
    }

    @Override
    public Vertical3PartHalf[] getParts() {
        return Vertical3PartHalf.values();
    }

    @Override
    @Nullable public BlockState getPlacementState(@NotNull BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        IPowerComponent.Switch sw =
                level.hasNeighborSignal(pos) ? IPowerComponent.Switch.OFF : IPowerComponent.Switch.ON;
        return this.defaultBlockState()
                .setValue(HALF, Vertical3PartHalf.BOTTOM)
                .setValue(OVERLOAD, true)
                .setValue(SWITCH, sw);
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF).add(OVERLOAD).add(SWITCH);
    }

    @Override
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull VoxelShape getShape(
            @NotNull BlockState state,
            @NotNull BlockGetter level,
            @NotNull BlockPos pos,
            @NotNull CollisionContext context) {
        if (state.getValue(HALF) == Vertical3PartHalf.BOTTOM) return TRANSMISSION_POLE_BASE;
        if (state.getValue(HALF) == Vertical3PartHalf.MID) return TRANSMISSION_POLE_MID;
        if (state.getValue(HALF) == Vertical3PartHalf.TOP) return TRANSMISSION_POLE_TOP;
        return super.getShape(state, level, pos, context);
    }

    @Override
    protected @NotNull BlockState placedState(@NotNull Vertical3PartHalf part, @NotNull BlockState state) {
        return super.placedState(part, state).setValue(SWITCH, IPowerComponent.Switch.ON);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new TransmissionPoleBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (level1, pos, state1, entity) -> {
            if (entity instanceof TransmissionPoleBlockEntity be) be.tick(level1, pos);
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Block neighborBlock,
            @NotNull BlockPos neighborPos,
            boolean movedByPiston) {
        if (level.isClientSide) return;
        if (state.getValue(HALF) != Vertical3PartHalf.BOTTOM) return;
        BlockPos topPos = pos.above(2);
        BlockState topState = level.getBlockState(topPos);
        if (!topState.is(ModBlocks.TRANSMISSION_POLE.get())) return;
        if (topState.getValue(HALF) != Vertical3PartHalf.TOP) return;
        IPowerComponent.Switch sw = state.getValue(SWITCH);
        boolean bl = sw == IPowerComponent.Switch.ON;
        if (bl == level.hasNeighborSignal(pos)) {
            if (bl) {
                state = state.setValue(SWITCH, IPowerComponent.Switch.OFF);
                topState = topState.setValue(SWITCH, IPowerComponent.Switch.OFF);
            } else {
                state = state.setValue(SWITCH, IPowerComponent.Switch.ON);
                topState = topState.setValue(SWITCH, IPowerComponent.Switch.ON);
            }
            level.setBlockAndUpdate(pos, state);
            level.setBlockAndUpdate(topPos, topState);
        }
    }

    @Override
    public void onPlace(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {}

    @Override
    public void onRemove(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {}
}
