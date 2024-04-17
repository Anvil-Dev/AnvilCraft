package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.depository.FilteredItemDepository;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeableBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.network.MachineEnableFilterPack;
import dev.dubhe.anvilcraft.network.MachineOutputDirectionPack;
import dev.dubhe.anvilcraft.network.SlotDisableChangePack;
import dev.dubhe.anvilcraft.network.SlotFilterChangePack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class ChuteBlock extends BaseEntityBlock implements IHammerChangeableBlock, IHammerRemovable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING_HOPPER;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    public static final VoxelShape AABB = Shapes.join(Block.box(0, 12, 0, 16, 16, 16),
        Block.box(2, 0, 2, 14, 12, 14), BooleanOp.OR);
    public static final VoxelShape AABB_W = Stream.of(Block.box(2, 8, 2, 14, 12, 14),
            Block.box(0, 4, 4, 12, 12, 12),
            Block.box(0, 12, 0, 16, 16, 16))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape AABB_E = Stream.of(Block.box(2, 8, 2, 14, 12, 14),
            Block.box(4, 4, 4, 16, 12, 12),
            Block.box(0, 12, 0, 16, 16, 16))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape AABB_S = Stream.of(Block.box(2, 8, 2, 14, 12, 14),
            Block.box(4, 4, 4, 12, 12, 16),
            Block.box(0, 12, 0, 16, 16, 16))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape AABB_N = Stream.of(Block.box(2, 8, 2, 14, 12, 14),
            Block.box(4, 4, 0, 12, 12, 12),
            Block.box(0, 12, 0, 16, 16, 16))
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    /**
     * 溜槽方块
     *
     * @param properties 方块属性
     */
    public ChuteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.DOWN)
                .setValue(ENABLED, Boolean.TRUE)
        );
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return ChuteBlockEntity.createBlockEntity(ModBlockEntities.CHUTE.get(), pos, state);
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        Direction direction = context.getClickedFace().getOpposite();
        return this.defaultBlockState()
            .setValue(FACING, (direction.getAxis() == Direction.Axis.Y ? Direction.DOWN : direction))
            .setValue(ENABLED, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull BlockState rotate(@Nonnull BlockState state, @Nonnull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull BlockState mirror(@Nonnull BlockState state, @Nonnull Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(
        @NotNull BlockState state,
        @Nonnull Level level,
        @NotNull BlockPos pos,
        @NotNull Block neighborBlock,
        @NotNull BlockPos neighborPos,
        boolean movedByPiston
    ) {
        if (level.isClientSide) return;
        boolean bl = state.getValue(ENABLED);
        if (bl == level.hasNeighborSignal(pos)) {
            if (!bl) level.scheduleTick(pos, this, 4);
            else level.setBlock(pos, state.cycle(ENABLED), 2);
        }
        if (!neighborPos.equals(pos.relative(state.getValue(FACING)))) return;
        BlockState blockState = level.getBlockState(neighborPos);
        if (!blockState.is(ModBlocks.CHUTE.get())) return;
        if (hasChuteFacing(level, neighborPos)) {
            BlockState newState = ModBlocks.SIMPLE_CHUTE.getDefaultState();
            newState = newState.setValue(SimpleChuteBlock.FACING, blockState.getValue(FACING))
                .setValue(SimpleChuteBlock.ENABLED, blockState.getValue(ENABLED));
            BlockState upState = level.getBlockState(neighborPos.relative(Direction.UP));
            if (upState.is(ModBlocks.SIMPLE_CHUTE.get()) || upState.is(ModBlocks.CHUTE.get())) {
                if (upState.getValue(FACING) == Direction.DOWN) {
                    newState = newState.setValue(SimpleChuteBlock.TALL, true);
                }
            }
            level.setBlockAndUpdate(neighborPos, newState);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void tick(
        @NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random
    ) {
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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type
    ) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(
            type, ModBlockEntities.CHUTE.get(),
            ((level1, blockPos, blockState, blockEntity) -> blockEntity.tick()));
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull VoxelShape getShape(
        @Nonnull BlockState blockState,
        @Nonnull BlockGetter blockGetter,
        @Nonnull BlockPos blockPos,
        @Nonnull CollisionContext collisionContext
    ) {
        return switch (blockState.getValue(FACING)) {
            case NORTH -> AABB_N;
            case SOUTH -> AABB_S;
            case WEST -> AABB_W;
            case EAST -> AABB_E;
            default -> AABB;
        };
    }

    @SuppressWarnings({"deprecation", "DuplicatedCode", "UnreachableCode"})
    @Override
    public @Nonnull InteractionResult use(
        @Nonnull BlockState state,
        @Nonnull Level level,
        @Nonnull BlockPos pos,
        @Nonnull Player player,
        @Nonnull InteractionHand hand,
        @Nonnull BlockHitResult hit
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ChuteBlockEntity entity) {
            if (player instanceof ServerPlayer serverPlayer) {
                ModMenuTypes.open(serverPlayer, entity, pos);
                new MachineOutputDirectionPack(entity.getDirection()).send(serverPlayer);
                new MachineEnableFilterPack(entity.isFilterEnabled()).send(serverPlayer);
                for (int i = 0; i < entity.getFilteredItems().size(); i++) {
                    new SlotDisableChangePack(i, entity.getDepository().getDisabled().get(i)).send(serverPlayer);
                    new SlotFilterChangePack(i, entity.getFilter(i)).send(serverPlayer);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState newState,
        boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ChuteBlockEntity entity) {
                Vec3 vec3 = entity.getBlockPos().getCenter();
                FilteredItemDepository depository = entity.getDepository();
                for (int slot = 0; slot < depository.getSlots(); slot++) {
                    Containers.dropItemStack(level, vec3.x, vec3.y, vec3.z, depository.getStack(slot));
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        BlockState facingState = level.getBlockState(pos.relative(state.getValue(FACING)));
        if (facingState.is(ModBlocks.SIMPLE_CHUTE.get())
            && !newState.is(ModBlocks.SIMPLE_CHUTE.get())
            && !hasChuteFacing(level, pos.relative(state.getValue(FACING)))) {
            BlockState newBlockState = ModBlocks.CHUTE.getDefaultState();
            newBlockState = newBlockState
                .setValue(FACING, facingState.getValue(SimpleChuteBlock.FACING))
                .setValue(ENABLED, facingState.getValue(SimpleChuteBlock.ENABLED));
            level.setBlockAndUpdate(pos.relative(state.getValue(FACING)), newBlockState);
        }
        BlockState downState = level.getBlockState(pos.relative(Direction.DOWN));
        if (state.getValue(FACING) == Direction.DOWN
            && downState.is(ModBlocks.SIMPLE_CHUTE.get())
            && !newState.is(ModBlocks.SIMPLE_CHUTE.get())) {
            BlockState newBlockState = ModBlocks.SIMPLE_CHUTE.getDefaultState();
            newBlockState = newBlockState
                .setValue(FACING, downState.getValue(FACING))
                .setValue(ENABLED, downState.getValue(ENABLED))
                .setValue(SimpleChuteBlock.TALL, false);
            level.setBlockAndUpdate(pos.relative(Direction.DOWN), newBlockState);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull BlockState oldState,
        boolean movedByPiston
    ) {
        BlockState facingState = level.getBlockState(pos.relative(state.getValue(FACING)));
        if (facingState.is(ModBlocks.CHUTE.get()) || facingState.is(ModBlocks.SIMPLE_CHUTE.get())) {
            if (facingState.getValue(FACING).getOpposite() == state.getValue(FACING)) {
                level.destroyBlock(pos, true);
                return;
            }
            BlockState newState = ModBlocks.SIMPLE_CHUTE.getDefaultState();
            newState = newState
                .setValue(SimpleChuteBlock.FACING, facingState.getValue(FACING))
                .setValue(SimpleChuteBlock.ENABLED, facingState.getValue(ENABLED));
            BlockState facingUpState = level.getBlockState(pos.relative(state.getValue(FACING)).relative(Direction.UP));
            if (state.getValue(FACING) == Direction.DOWN
                || ((facingUpState.is(ModBlocks.SIMPLE_CHUTE.get())
                || facingUpState.is(ModBlocks.CHUTE.get())) && facingUpState.getValue(FACING) == Direction.DOWN)) {
                newState = newState.setValue(SimpleChuteBlock.TALL, true);
            } else {
                newState = newState.setValue(SimpleChuteBlock.TALL, false);
            }
            level.setBlockAndUpdate(pos.relative(state.getValue(FACING)), newState);
        }
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(@NotNull BlockState blockState) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof ChuteBlockEntity chuteBlockEntity) {
            return chuteBlockEntity.getRedstoneSignal();
        }
        return 0;
    }

    /**
     * 判断是否有溜槽指向
     *
     * @param level 世界
     * @param pos   位置
     * @return 是否有溜槽指向
     */
    public static boolean hasChuteFacing(Level level, BlockPos pos) {
        for (Direction face : Direction.values()) {
            if (face == Direction.DOWN) continue;
            BlockState facingState = level.getBlockState(pos.relative(face));
            if (facingState.is(ModBlocks.SIMPLE_CHUTE.get()) || facingState.is(ModBlocks.CHUTE.get())) {
                if (facingState.getValue(FACING) == face.getOpposite()) {
                    return true;
                }
            }
        }
        return false;
    }
}
