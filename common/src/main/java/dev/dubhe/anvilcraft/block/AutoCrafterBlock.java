package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.entity.AutoCrafterBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.network.MachineOutputDirectionPack;
import dev.dubhe.anvilcraft.network.MachineRecordMaterialPack;
import dev.dubhe.anvilcraft.network.SlotDisableChangePack;
import dev.dubhe.anvilcraft.network.SlotFilterChangePack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AutoCrafterBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

    public AutoCrafterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(@Nonnull BlockState blockState) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(@Nonnull BlockState blockState, @Nonnull Level level, @Nonnull BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof AutoCrafterBlockEntity crafterBlockEntity) {
            return crafterBlockEntity.getRedstoneSignal();
        }
        return 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AutoCrafterBlockEntity entity) {
            player.openMenu(entity);
            if (player instanceof ServerPlayer serverPlayer) {
                new MachineOutputDirectionPack(entity.getDirection()).send(serverPlayer);
                new MachineRecordMaterialPack(entity.isRecord()).send(serverPlayer);
                for (int i = 0; i < entity.getDisabled().size(); i++) {
                    new SlotDisableChangePack(i, entity.getDisabled().get(i)).send(serverPlayer);
                    new SlotFilterChangePack(i, entity.getFilter().get(i)).send(serverPlayer);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull BlockState newState, boolean movedByPiston) {
        if (state.is(newState.getBlock())) return;
        if (level.getBlockEntity(pos) instanceof Container container) {
            Containers.dropContents(level, pos, container);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new AutoCrafterBlockEntity(ModBlockEntities.AUTO_CRAFTER.get(),pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> blockEntityType) {
        return (level1, pos, state1, e) -> AutoCrafterBlockEntity.tick(level1, pos, e);
    }

    @Override
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite()).setValue(LIT, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT).add(FACING);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Block neighborBlock, @Nonnull BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        boolean bl = state.getValue(LIT);
        if (bl != level.hasNeighborSignal(pos)) {
            if (bl) {
                level.scheduleTick(pos, this, 4);
            } else {
                level.setBlock(pos, state.cycle(LIT), 2);
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        if (state.getValue(LIT) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(LIT), 2);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull BlockState rotate(@Nonnull BlockState state, @Nonnull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull BlockState mirror(@Nonnull BlockState state, @Nonnull Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
