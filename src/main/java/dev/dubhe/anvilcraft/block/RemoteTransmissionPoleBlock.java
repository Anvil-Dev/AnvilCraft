package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.entity.RemoteTransmissionPoleBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Vertical4PartHalf;
import dev.dubhe.anvilcraft.init.ModBlocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RemoteTransmissionPoleBlock
    extends SimpleMultiPartBlock<Vertical4PartHalf>
    implements IHammerRemovable, IHasMultiBlock, EntityBlock {
    public static final EnumProperty<Vertical4PartHalf> HALF = EnumProperty.create("half", Vertical4PartHalf.class);
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
    public Property<Vertical4PartHalf> getPart() {
        return RemoteTransmissionPoleBlock.HALF;
    }

    @Override
    public Vertical4PartHalf[] getParts() {
        return Vertical4PartHalf.values();
    }

    @Override
    @Nullable
    public BlockState getPlacementState(BlockPlaceContext context) {
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF).add(OVERLOAD).add(SWITCH);
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
        CollisionContext context) {
        return switch (state.getValue(HALF)) {
            case BOTTOM -> TRANSMISSION_POLE_BASE;
            case MID_UPPER, MID_LOWER -> TRANSMISSION_POLE_MID;
            case TOP -> TRANSMISSION_POLE_TOP;
            default -> super.getShape(state, level, pos, context);
        };
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    protected BlockState placedState(Vertical4PartHalf part, BlockState state) {
        return super.placedState(part, state).setValue(SWITCH, IPowerComponent.Switch.ON);
    }

    @Override
    public BlockState playerWillDestroy(
        Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) return state;
        onRemove(level, pos, state);
        super.playerWillDestroy(level, pos, state, player);
        return state;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RemoteTransmissionPoleBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (level1, pos, state1, entity) -> {
            if (entity instanceof RemoteTransmissionPoleBlockEntity be) be.tick(level1, pos);
        };
    }

    @Override

    public void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
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
    public void onRemove(Level level, BlockPos pos, BlockState state) {
    }

    @Override
    public void onPlace(Level level, BlockPos pos, BlockState state) {
    }
}
