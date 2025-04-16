package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.SimpleChuteBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

import static dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil.exportToTarget;
import static dev.dubhe.anvilcraft.block.ChuteBlock.getFacing;
import static dev.dubhe.anvilcraft.block.ChuteBlock.isChuteBlock;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SimpleChuteBlock
    extends BaseEntityBlock
    implements SimpleWaterloggedBlock, IHammerChangeable, IHammerRemovable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING_HOPPER;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty TALL = BooleanProperty.create("tall");

    public static final VoxelShape AABB = Block.box(2, 0, 2, 14, 12, 14);
    public static final VoxelShape AABB_TALL = Block.box(2, 0, 2, 14, 16, 14);
    public static final VoxelShape AABB_N = Block.box(4, 4, 0, 12, 12, 12);
    public static final VoxelShape AABB_TALL_N =
        Shapes.join(Block.box(4, 4, 0, 12, 12, 12), Block.box(2, 8, 2, 14, 16, 14), BooleanOp.OR);
    public static final VoxelShape AABB_E = Block.box(4, 4, 4, 16, 12, 12);
    public static final VoxelShape AABB_TALL_E =
        Shapes.join(Block.box(4, 4, 4, 16, 12, 12), Block.box(2, 8, 2, 14, 16, 14), BooleanOp.OR);
    public static final VoxelShape AABB_S = Block.box(4, 4, 4, 12, 12, 16);
    public static final VoxelShape AABB_TALL_S =
        Shapes.join(Block.box(4, 4, 4, 12, 12, 16), Block.box(2, 8, 2, 14, 16, 14), BooleanOp.OR);
    public static final VoxelShape AABB_W = Block.box(0, 4, 4, 12, 12, 12);
    public static final VoxelShape AABB_TALL_W =
        Shapes.join(Block.box(0, 4, 4, 12, 12, 12), Block.box(2, 8, 2, 14, 16, 14), BooleanOp.OR);

    /**
     * @param properties 方块属性
     */
    public SimpleChuteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
            .any()
            .setValue(FACING, Direction.DOWN)
            .setValue(WATERLOGGED, false)
            .setValue(ENABLED, true)
            .setValue(TALL, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(SimpleChuteBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimpleChuteBlockEntity(ModBlockEntities.SIMPLE_CHUTE.get(), pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, ENABLED, TALL);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;
        BlockState neighborState = level.getBlockState(neighborPos);
        Block neighborBlock1 = neighborState.getBlock();
        if (isChuteBlock(neighborBlock) || isChuteBlock(neighborBlock1)) {
            BlockState newState = getState(level, pos, state.getValue(FACING));
            if (newState != null && newState != state)
                level.setBlockAndUpdate(pos, newState);
        }
        this.checkPoweredState(level, pos, state);
    }

    private void checkPoweredState(Level level, BlockPos pos, BlockState state) {
        boolean flag = !level.hasNeighborSignal(pos);
        if (flag != state.getValue(ENABLED)) {
            level.setBlock(pos, state.setValue(ENABLED, flag), 2);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    @MethodsReturnNonnullByDefault
    @ParametersAreNonnullByDefault
    public ItemStack getCloneItemStack(
        BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(ModBlocks.CHUTE);
    }


    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random) {
        if (!state.getValue(ENABLED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(ENABLED), 2);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof SimpleChuteBlockEntity oldEntity) {
                IItemHandler oldHandler = oldEntity.getItemHandler();
                if (newState.is(ModBlocks.CHUTE.get())) {
                    level.removeBlockEntity(pos);
                    level.setBlock(pos, newState, 2);
                    IItemHandler newHandler = null;
                    if (level.getBlockEntity(pos) instanceof ChuteBlockEntity newEntity) {
                        newHandler = newEntity.getItemHandler();
                    }
                    exportToTarget(oldHandler, 64, stack -> true, newHandler);
                } else level.removeBlockEntity(pos);
                Vec3 vec3 = oldEntity.getBlockPos().getCenter();
                for (int slot = 0; slot < oldHandler.getSlots(); slot++) {
                    Containers.dropItemStack(level, vec3.x, vec3.y, vec3.z, oldHandler.getStackInSlot(slot));
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }

    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) return null;
        return createTickerHelper(
            blockEntityType,
            ModBlockEntities.SIMPLE_CHUTE.get(),
            ((level1, blockPos, blockState, blockEntity) -> blockEntity.tick()));
    }


    @Override
    public VoxelShape getShape(
        BlockState blockState,
        BlockGetter blockGetter,
        BlockPos blockPos,
        CollisionContext collisionContext) {
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

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState blockState) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity instanceof SimpleChuteBlockEntity chuteBlockEntity) {
            return chuteBlockEntity.getRedstoneSignal();
        }
        return 0;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean change(Player player, BlockPos pos, Level level, ItemStack anvilHammer) {
        HammerRotateBehavior.DEFAULT.change(player, pos, level, anvilHammer);
        BlockState state = level.getBlockState(pos);
        BlockState facingState = level.getBlockState(pos.relative(state.getValue(FACING)));
        if (facingState.is(ModBlocks.CHUTE.get()) || facingState.is(ModBlocks.SIMPLE_CHUTE.get())) {
            if (facingState.getValue(FACING).getOpposite() == state.getValue(FACING)) {
                return this.change(player, pos, level, anvilHammer);
            }
        }
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }

    @Override
    public @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    BlockState getState(Level level, BlockPos pos, Direction facing) {
        boolean success = false;
        boolean tall = false;
        BlockState result = level.getBlockState(pos);
        //遍历六个方向 获取指向自己的溜槽
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (isChuteBlock(neighborState)) {
                if (getFacing(neighborState) == dir.getOpposite()) {
                    success = true;
                    if (dir == Direction.UP) {
                        tall = !neighborState.is(ModBlocks.MAGNETIC_CHUTE.get());
                    }
                }

            }
        }
        if (!success)
            result = ModBlocks.CHUTE.getDefaultState()
                .setValue(FACING, facing)
                .setValue(ENABLED, !level.hasNeighborSignal(pos));
        else
            result = result.setValue(TALL, tall);
        return result;
    }
}
