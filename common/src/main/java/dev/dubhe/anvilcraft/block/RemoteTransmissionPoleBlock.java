package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.entity.RemoteTransmissionPoleBlockEntity;
import dev.dubhe.anvilcraft.block.state.Vertical4PartHalf;
import dev.dubhe.anvilcraft.init.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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

public class RemoteTransmissionPoleBlock extends AbstractMultiplePartBlock<Vertical4PartHalf>
        implements IHammerRemovable, IHasMultiBlock, EntityBlock {
    public static final EnumProperty<Vertical4PartHalf> HALF =
            EnumProperty.create("half", Vertical4PartHalf.class);
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;
    public static final EnumProperty<IPowerComponent.Switch> SWITCH = IPowerComponent.SWITCH;
    public static final VoxelShape TRANSMISSION_POLE_TOP =
            Shapes.or(Block.box(1, 11, 1, 15, 13, 15), Block.box(4, 0, 4, 12, 16, 12));

    public static final VoxelShape TRANSMISSION_POLE_MID = Block.box(6, 0, 6, 10, 16, 10);

    public static final VoxelShape TRANSMISSION_POLE_BASE =
            Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(4, 4, 4, 12, 16, 12));

    /**
     * @param properties 属性
     */
    public RemoteTransmissionPoleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
                .any()
                .setValue(HALF, Vertical4PartHalf.BOTTOM)
                .setValue(OVERLOAD, true)
                .setValue(SWITCH, IPowerComponent.Switch.ON));
    }

    @Override
    protected @NotNull Property<Vertical4PartHalf> getPart() {
        return RemoteTransmissionPoleBlock.HALF;
    }

    @Override
    protected Vertical4PartHalf[] getParts() {
        return Vertical4PartHalf.values();
    }

    @Override
    @Nullable public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        IPowerComponent.Switch sw =
                level.hasNeighborSignal(pos) ? IPowerComponent.Switch.OFF : IPowerComponent.Switch.ON;
        return this.defaultBlockState()
                .setValue(HALF, Vertical4PartHalf.BOTTOM)
                .setValue(OVERLOAD, true)
                .setValue(SWITCH, sw);
    }

    @Override
    protected void createBlockStateDefinition(
            @NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF).add(OVERLOAD).add(SWITCH);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull VoxelShape getShape(
            @NotNull BlockState state,
            @NotNull BlockGetter level,
            @NotNull BlockPos pos,
            @NotNull CollisionContext context) {
        if (state.getValue(HALF) == Vertical4PartHalf.BOTTOM) return TRANSMISSION_POLE_BASE;
        if (state.getValue(HALF) == Vertical4PartHalf.MID_UPPER) return TRANSMISSION_POLE_MID;
        if (state.getValue(HALF) == Vertical4PartHalf.MID_LOWER) return TRANSMISSION_POLE_MID;
        if (state.getValue(HALF) == Vertical4PartHalf.TOP) return TRANSMISSION_POLE_TOP;
        return super.getShape(state, level, pos, context);
    }

    @Override
    public void setPlacedBy(
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            LivingEntity placer,
            @NotNull ItemStack stack) {
        BlockPos above = pos.above();
        level.setBlockAndUpdate(
                above,
                state
                        .setValue(HALF, Vertical4PartHalf.MID_LOWER)
                        .setValue(SWITCH, IPowerComponent.Switch.ON));
        above = above.above();
        level.setBlockAndUpdate(
                above,
                state
                        .setValue(HALF, Vertical4PartHalf.MID_UPPER)
                        .setValue(SWITCH, IPowerComponent.Switch.ON));
        above = above.above();
        level.setBlockAndUpdate(above, state.setValue(HALF, Vertical4PartHalf.TOP));
    }

    @Override
    public void playerWillDestroy(
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull BlockState state,
            @NotNull Player player) {
        if (level.isClientSide) return;
        onRemove(level, pos, state);
        super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new RemoteTransmissionPoleBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (level1, pos, state1, entity) -> {
            if (entity instanceof RemoteTransmissionPoleBlockEntity be) be.tick(level1, pos);
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
        if (level.isClientSide) {
            return;
        }
        if (state.getValue(HALF) != Vertical4PartHalf.BOTTOM) return;
        BlockPos topPos = pos.above(3);
        BlockState topState = level.getBlockState(topPos);
        if (!topState.is(ModBlocks.REMOTE_TRANSMISSION_POLE.get())) return;
        if (topState.getValue(HALF) != Vertical4PartHalf.TOP) return;
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
    @SuppressWarnings("deprecation")
    public boolean canSurvive(
            @NotNull BlockState state, @NotNull LevelReader level, @NotNull BlockPos pos) {
        switch (state.getValue(HALF)) {
            case TOP -> {
                BlockState state1 = level.getBlockState(pos.below());
                BlockState state2 = level.getBlockState(pos.below(2));
                BlockState state3 = level.getBlockState(pos.below(3));
                return state1.is(this) && state2.is(this) && state3.is(this);
            }
            case MID_UPPER, MID_LOWER -> {
                BlockState state2 = level.getBlockState(pos.below());
                return state2.is(this);
            }
            default -> {
                return state.isFaceSturdy(level, pos.below(), Direction.UP);
            }
        }
    }

    @Override
    public void onRemove(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {}

    @Override
    public void onPlace(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {}
}
