package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.depository.ItemDepository;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeableBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.SimpleChuteBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class SimpleChuteBlock extends BaseEntityBlock implements
    SimpleWaterloggedBlock, IHammerChangeable, IHammerRemovable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING_HOPPER;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty TALL = BooleanProperty.create("tall");

    public static final VoxelShape AABB = Block.box(2, 0, 2, 14, 12, 14);
    public static final VoxelShape AABB_TALL = Block.box(2, 0, 2, 14, 16, 14);
    public static final VoxelShape AABB_N = Block.box(4, 4, 0, 12, 12, 12);
    public static final VoxelShape AABB_TALL_N = Shapes.join(Block.box(4, 4, 0, 12, 12, 12),
        Block.box(2, 8, 2, 14, 16, 14), BooleanOp.OR);
    public static final VoxelShape AABB_E = Block.box(4, 4, 4, 16, 12, 12);
    public static final VoxelShape AABB_TALL_E = Shapes.join(Block.box(4, 4, 4, 16, 12, 12),
        Block.box(2, 8, 2, 14, 16, 14), BooleanOp.OR);
    public static final VoxelShape AABB_S = Block.box(4, 4, 4, 12, 12, 16);
    public static final VoxelShape AABB_TALL_S = Shapes.join(Block.box(4, 4, 4, 12, 12, 16),
        Block.box(2, 8, 2, 14, 16, 14), BooleanOp.OR);
    public static final VoxelShape AABB_W = Block.box(0, 4, 4, 12, 12, 12);
    public static final VoxelShape AABB_TALL_W = Shapes.join(Block.box(0, 4, 4, 12, 12, 12),
        Block.box(2, 8, 2, 14, 16, 14), BooleanOp.OR);

    /**
     * @param properties 方块属性
     */
    public SimpleChuteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.DOWN)
                .setValue(WATERLOGGED, false)
                .setValue(ENABLED, true)
                .setValue(TALL, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return SimpleChuteBlockEntity.createBlockEntity(ModBlockEntities.SIMPLE_CHUTE.get(), pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, ENABLED, TALL);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(
        @NotNull BlockState state, @Nonnull Level level,
        @NotNull BlockPos pos, @NotNull Block neighborBlock,
        @NotNull BlockPos neighborPos, boolean movedByPiston
    ) {
        if (level.isClientSide) return;
        boolean bl = state.getValue(ENABLED);
        if (bl == level.hasNeighborSignal(pos)) {
            if (!bl) level.scheduleTick(pos, this, 4);
            else level.setBlock(pos, state.cycle(ENABLED), 2);
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

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(
        @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
        @NotNull BlockState newState, boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof SimpleChuteBlockEntity entity) {
                Vec3 vec3 = entity.getBlockPos().getCenter();
                ItemDepository depository = entity.getDepository();
                for (int slot = 0; slot < depository.getSlots(); slot++) {
                    Containers.dropItemStack(level, vec3.x, vec3.y, vec3.z, depository.getStack(slot));
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        BlockState facingState = level.getBlockState(pos.relative(state.getValue(FACING)));
        if (facingState.is(ModBlocks.SIMPLE_CHUTE.get())
                && !newState.is(ModBlocks.CHUTE.get())
                && !ChuteBlock.hasChuteFacing(level, pos.relative(state.getValue(FACING)))) {
            BlockState newBlockState = ModBlocks.CHUTE.getDefaultState();
            newBlockState = newBlockState
                    .setValue(FACING, facingState.getValue(SimpleChuteBlock.FACING))
                    .setValue(ENABLED, facingState.getValue(SimpleChuteBlock.ENABLED));
            level.setBlockAndUpdate(pos.relative(state.getValue(FACING)), newBlockState);
        }
        BlockState downState = level.getBlockState(pos.relative(Direction.DOWN));
        if (state.getValue(FACING) == Direction.DOWN
                && downState.is(ModBlocks.SIMPLE_CHUTE.get())
                && !newState.is(ModBlocks.SIMPLE_CHUTE.get())
                && !newState.is(ModBlocks.CHUTE.get())) {
            BlockState newBlockState = ModBlocks.SIMPLE_CHUTE.getDefaultState();
            newBlockState = newBlockState
                    .setValue(FACING, downState.getValue(FACING))
                    .setValue(ENABLED, downState.getValue(ENABLED))
                    .setValue(TALL, false);
            level.setBlockAndUpdate(pos.relative(Direction.DOWN), newBlockState);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide) return null;
        return createTickerHelper(blockEntityType, ModBlockEntities.SIMPLE_CHUTE.get(),
                ((level1, blockPos, blockState, blockEntity) -> blockEntity.tick()));
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull VoxelShape getShape(
        @Nonnull BlockState blockState, @Nonnull BlockGetter blockGetter,
        @Nonnull BlockPos blockPos, @Nonnull CollisionContext collisionContext
    ) {
        if (!blockState.getValue(TALL)) {
            return switch (blockState.getValue(FACING)) {
                case NORTH -> AABB_N;
                case EAST -> AABB_E;
                case SOUTH -> AABB_S;
                case WEST -> AABB_W;
                default -> AABB;
            };
        } else {
            return switch (blockState.getValue(FACING)) {
                case NORTH -> AABB_TALL_N;
                case EAST -> AABB_TALL_E;
                case SOUTH -> AABB_TALL_S;
                case WEST -> AABB_TALL_W;
                default -> AABB_TALL;
            };
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState blockState) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(@NotNull BlockState blockState, @NotNull Level level, @NotNull BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof SimpleChuteBlockEntity chuteBlockEntity) {
            return chuteBlockEntity.getRedstoneSignal();
        }
        return 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull FluidState getFluidState(@NotNull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean change(Player player, BlockPos pos, @NotNull Level level, ItemStack anvilHammer) {
        IHammerChangeableBlock.DEFAULT.change(player, pos, level, anvilHammer);
        BlockState state = level.getBlockState(pos);
        BlockState facingState = level.getBlockState(pos.relative(state.getValue(FACING)));
        if (facingState.is(ModBlocks.CHUTE.get()) || facingState.is(ModBlocks.SIMPLE_CHUTE.get())) {
            if (facingState.getValue(FACING).getOpposite() == state.getValue(FACING)) {
                level.destroyBlock(pos, true);
                return true;
            }
        }
        return true;
    }
}
