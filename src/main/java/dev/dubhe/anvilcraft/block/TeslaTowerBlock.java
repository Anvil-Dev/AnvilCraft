package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.IHasMultiBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.entity.TeslaTowerBlockEntity;
import dev.dubhe.anvilcraft.block.state.Vertical4PartHalf;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.network.TeslaFilterSyncPacket;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TeslaTowerBlock
    extends AbstractMultiplePartBlock<Vertical4PartHalf>
    implements IHammerRemovable, IHasMultiBlock, EntityBlock {
    public static final EnumProperty<Vertical4PartHalf> HALF = EnumProperty.create("half", Vertical4PartHalf.class);
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;
    public static final EnumProperty<IPowerComponent.Switch> SWITCH = IPowerComponent.SWITCH;
    public static final VoxelShape BOTTOM_SHAPE = Shapes.join(Block.box(0, 0, 0, 16, 4, 16), Block.box(1, 4, 1, 15, 16, 15), BooleanOp.OR);
    public static final VoxelShape LOWER_SHAPE = Shapes.join(Block.box(5, 8, 5, 11, 16, 11), Block.box(1, 0, 1, 15, 8, 15), BooleanOp.OR);
    public static final VoxelShape UPPER_SHAPE = Shapes.join(Block.box(6, 8, 6, 10, 16, 10), Block.box(4, 0, 4, 12, 8, 12), BooleanOp.OR);
    public static final VoxelShape TOP_SHAPE = Shapes.join(Block.box(3, 6, 3, 13, 16, 13), Block.box(6, 0, 6, 10, 8, 10), BooleanOp.OR);

    /**
     * @param properties 属性
     */
    public TeslaTowerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
            .any()
            .setValue(HALF, Vertical4PartHalf.BOTTOM)
            .setValue(OVERLOAD, true)
            .setValue(SWITCH, IPowerComponent.Switch.ON));
    }

    @Override
    public Property<Vertical4PartHalf> getPart() {
        return TeslaTowerBlock.HALF;
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
        return switch (state.getValue(HALF)){
            case BOTTOM -> BOTTOM_SHAPE;
            case MID_LOWER -> LOWER_SHAPE;
            case MID_UPPER -> UPPER_SHAPE;
            case TOP -> TOP_SHAPE;
        };
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
        return new TeslaTowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (level1, pos, state1, entity) -> {
            if (entity instanceof TeslaTowerBlockEntity entity1) entity1.tick();
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
        if (!topState.is(ModBlocks.TESLA_TOWER.get())) return;
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

    @Override
    protected InteractionResult useWithoutItem(
            BlockState pState,
            Level pLevel,
            BlockPos pPos,
            Player pPlayer,
            BlockHitResult pHitResult
    ) {
        if (pLevel.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (be instanceof TeslaTowerBlockEntity teslaTowerBlockEntity && pPlayer instanceof ServerPlayer sp) {
            if (sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) return InteractionResult.PASS;
            ModMenuTypes.open(sp, teslaTowerBlockEntity, pPos);
            PacketDistributor.sendToPlayer(sp, new TeslaFilterSyncPacket(teslaTowerBlockEntity.getWhiteList()));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }
}