package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.SimpleMagneticChuteBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class SimpleMagneticChuteBlock
    extends BaseEntityBlock
    implements SimpleWaterloggedBlock, IHammerRemovable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final VoxelShape SHAPE_UP = Block.box(4, 0, 4, 12, 16, 12);
    public static final VoxelShape SHAPE_DOWN = Block.box(4, 0, 4, 12, 16, 12);
    public static final VoxelShape SHAPE_N = Block.box(4, 4, 0, 12, 12, 16);
    public static final VoxelShape SHAPE_S = Block.box(4, 4, 0, 12, 12, 16);
    public static final VoxelShape SHAPE_W = Block.box(0, 4, 4, 16, 12, 12);
    public static final VoxelShape SHAPE_E = Block.box(0, 4, 4, 16, 12, 12);

    public SimpleMagneticChuteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
            .any()
            .setValue(FACING, Direction.UP)
            .setValue(ENABLED, true)
            .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(SimpleMagneticChuteBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimpleMagneticChuteBlockEntity(ModBlockEntities.SIMPLE_MAGNETIC_CHUTE.get(), pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED, WATERLOGGED);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide) return;
        // 将前方同向的磁性溜槽磁化为简易磁性溜槽，形成磁化传播链
        Direction facing = state.getValue(FACING);
        BlockPos forwardPos = pos.relative(facing);
        BlockState forwardState = level.getBlockState(forwardPos);
        if (forwardState.is(ModBlocks.MAGNETIC_CHUTE.get())
            && forwardState.getValue(MagneticChuteBlock.FACING) == facing) {
            level.setBlockAndUpdate(forwardPos, state);
        }
    }

    /**
     * 判断指定方块状态能否作为简易磁性溜槽的"支撑源"（即位于输入侧、朝向相同方向输送物品的溜槽）。
     * 支持磁性溜槽、简易磁性溜槽，以及朝向相同的普通溜槽 / 简易溜槽。
     */
    public static boolean isMagnetizeSupport(BlockState state, Direction facing) {
        if (state.is(ModBlocks.SIMPLE_MAGNETIC_CHUTE.get())) {
            return state.getValue(FACING) == facing;
        }
        if (state.is(ModBlocks.MAGNETIC_CHUTE.get())) {
            return state.getValue(MagneticChuteBlock.FACING) == facing;
        }
        if (state.is(ModBlocks.CHUTE.get()) || state.is(ModBlocks.SIMPLE_CHUTE.get())) {
            return ChuteBlock.getFacing(state) == facing;
        }
        return false;
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        if (!level.isClientSide) {
            Direction facing = state.getValue(FACING);
            BlockPos backPos = pos.relative(facing.getOpposite());
            if (neighborPos.equals(backPos)) {
                // 后方支撑源被拆除时，自己变回正常磁性溜槽
                BlockState backState = level.getBlockState(backPos);
                if (!isMagnetizeSupport(backState, facing)) {
                    level.setBlockAndUpdate(pos, ModBlocks.MAGNETIC_CHUTE.get().defaultBlockState()
                        .setValue(MagneticChuteBlock.FACING, facing));
                }
            }
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        Direction facing,
        BlockState facingState,
        LevelAccessor level,
        BlockPos currentPos,
        BlockPos facingPos
    ) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public ItemStack getCloneItemStack(
        BlockState state,
        HitResult target,
        LevelReader level,
        BlockPos pos,
        Player player
    ) {
        return new ItemStack(ModBlocks.MAGNETIC_CHUTE);
    }

    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random
    ) {
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof SimpleMagneticChuteBlockEntity oldEntity) {
                IItemHandler oldHandler = oldEntity.getItemHandler();
                Vec3 vec3 = oldEntity.getBlockPos().getCenter();
                for (int slot = 0; slot < oldHandler.getSlots(); slot++) {
                    Containers.dropItemStack(level, vec3.x, vec3.y, vec3.z, oldHandler.getStackInSlot(slot));
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) return null;
        return createTickerHelper(
            blockEntityType,
            ModBlockEntities.SIMPLE_MAGNETIC_CHUTE.get(),
            ((level1, blockPos, blockState, blockEntity) -> blockEntity.tick()));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> SHAPE_DOWN;
            case NORTH -> SHAPE_N;
            case SOUTH -> SHAPE_S;
            case WEST -> SHAPE_W;
            case EAST -> SHAPE_E;
            default -> SHAPE_UP;
        };
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
        if (blockEntity instanceof SimpleMagneticChuteBlockEntity chuteBlockEntity) {
            return chuteBlockEntity.getRedstoneSignal();
        }
        return 0;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    // 防止流体流动时破坏溜槽
    @Override
    public boolean canBeReplaced(BlockState state, net.minecraft.world.level.material.Fluid fluid) {
        return false;
    }

    // 防止玩家使用桶放置流体时直接替换掉溜槽
    @Override
    public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
        return false;
    }
}
