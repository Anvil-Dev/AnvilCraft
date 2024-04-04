package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;

import java.util.stream.Stream;

public class ChuteBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING_HOPPER;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public static final VoxelShape AABB = Shapes.join(Block.box(2, 0, 2, 14, 12, 14), Block.box(0, 12, 0, 16, 16, 16), BooleanOp.OR);
    public static final VoxelShape AABB_W = Stream.of(
            Block.box(0, 12, 0, 16, 16, 16),
            Block.box(2, 8, 2, 14, 12, 14),
            Block.box(0, 2, 3, 9, 12, 13),
            Block.box(9, 3, 3, 10, 8, 13),
            Block.box(10, 4, 3, 11, 8, 13),
            Block.box(11, 5, 3, 12, 8, 13),
            Block.box(12, 6, 3, 13, 8, 13),
            Block.box(13, 7, 3, 14, 8, 13)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape AABB_E = Stream.of(
            Block.box(0, 12, 0, 16, 16, 16),
            Block.box(2, 8, 2, 14, 12, 14),
            Block.box(7, 2, 3, 16, 12, 13),
            Block.box(6, 3, 3, 7, 8, 13),
            Block.box(5, 4, 3, 6, 8, 13),
            Block.box(4, 5, 3, 5, 8, 13),
            Block.box(3, 6, 3, 4, 8, 13),
            Block.box(2, 7, 3, 3, 8, 13)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape AABB_S = Stream.of(
            Block.box(0, 12, 0, 16, 16, 16),
            Block.box(2, 8, 2, 14, 12, 14),
            Block.box(3, 2, 7, 13, 12, 16),
            Block.box(3, 3, 6, 13, 8, 7),
            Block.box(3, 4, 5, 13, 8, 6),
            Block.box(3, 5, 4, 13, 8, 5),
            Block.box(3, 6, 3, 13, 8, 4),
            Block.box(3, 7, 2, 13, 8, 3)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape AABB_N = Stream.of(
            Block.box(0, 12, 0, 16, 16, 16),
            Block.box(2, 8, 2, 14, 12, 14),
            Block.box(3, 2, 0, 13, 12, 9),
            Block.box(3, 3, 9, 13, 8, 10),
            Block.box(3, 4, 10, 13, 8, 11),
            Block.box(3, 5, 11, 13, 8, 12),
            Block.box(3, 6, 12, 13, 8, 13),
            Block.box(3, 7, 13, 13, 8, 14)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    public ChuteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(ENABLED, Boolean.TRUE));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ChuteBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        Direction direction = context.getClickedFace().getOpposite();
        return this.defaultBlockState()
                .setValue(FACING, (direction.getAxis() == Direction.Axis.Y ? Direction.DOWN : direction))
                .setValue(ENABLED, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
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

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(@NotNull BlockState state, @Nonnull Level level, @NotNull BlockPos pos, @NotNull Block neighborBlock, @NotNull BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;
        boolean bl = state.getValue(ENABLED);
        if (bl == level.hasNeighborSignal(pos)) {
            if (!bl) level.scheduleTick(pos, this, 4);
            else level.setBlock(pos, state.cycle(ENABLED), 2);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(@Nonnull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (!state.getValue(ENABLED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(ENABLED), 2);
        }
    }

    @Override
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> blockEntityType) {
        return (level1, pos, state1, e) -> ChuteBlockEntity.tick(level1, pos, e);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull VoxelShape getShape(@Nonnull BlockState blockState, @Nonnull BlockGetter blockGetter, @Nonnull BlockPos blockPos, @Nonnull CollisionContext collisionContext) {
        return switch (blockState.getValue(FACING)) {
            case NORTH -> AABB_N;
            case SOUTH -> AABB_S;
            case WEST -> AABB_W;
            case EAST -> AABB_E;
            default -> AABB;
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nonnull InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ChuteBlockEntity entity && player instanceof ServerPlayer serverPlayer) {
            ModMenuTypes.open(serverPlayer, entity);
            new MachineOutputDirectionPack(entity.getDirection()).send(serverPlayer);
            new MachineRecordMaterialPack(entity.isRecord()).send(serverPlayer);
            for (int i = 0; i < entity.getDisabled().size(); i++) {
                new SlotDisableChangePack(i, entity.getDisabled().get(i)).send(serverPlayer);
                new SlotFilterChangePack(i, entity.getFilter().get(i)).send(serverPlayer);
            }
        }
        return InteractionResult.CONSUME;
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

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState blockState, @NotNull Level level, BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof ChuteBlockEntity chuteBlockEntity) {
            return chuteBlockEntity.getRedstoneSignal();
        }
        return 0;
    }
}
