package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
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
import net.minecraft.world.level.block.Blocks;
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
import org.jetbrains.annotations.Nullable;

public class SimpleMagneticChuteBlock
    extends BaseEntityBlock
    implements SimpleWaterloggedBlock, IHammerChangeable, IHammerRemovable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty HEAD = BooleanProperty.create("head");

    public static final VoxelShape SHAPE_UP = Block.box(4, 4, 4, 12, 16, 12);
    public static final VoxelShape SHAPE_DOWN = Block.box(4, 0, 4, 12, 12, 12);
    public static final VoxelShape SHAPE_N = Block.box(4, 4, 0, 12, 12, 12);
    public static final VoxelShape SHAPE_S = Block.box(4, 4, 4, 12, 12, 16);
    public static final VoxelShape SHAPE_W = Block.box(0, 4, 4, 12, 12, 12);
    public static final VoxelShape SHAPE_E = Block.box(4, 4, 4, 16, 12, 12);

    public static final VoxelShape SHAPE_N_HEAD = Shapes.join(
        Block.box(4, 4, 0, 12, 12, 12),
        Block.box(2, 10, 2, 14, 16, 14),
        BooleanOp.OR);
    public static final VoxelShape SHAPE_S_HEAD = Shapes.join(
        Block.box(4, 4, 4, 12, 12, 16),
        Block.box(2, 10, 2, 14, 16, 14),
        BooleanOp.OR);
    public static final VoxelShape SHAPE_W_HEAD = Shapes.join(
        Block.box(0, 4, 4, 12, 12, 12),
        Block.box(2, 10, 2, 14, 16, 14),
        BooleanOp.OR);
    public static final VoxelShape SHAPE_E_HEAD = Shapes.join(Block.box(
        4, 4, 4, 16, 12, 12),
        Block.box(2, 10, 2, 14, 16, 14),
        BooleanOp.OR);

    public SimpleMagneticChuteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
            .any()
            .setValue(FACING, Direction.UP)
            .setValue(ENABLED, true)
            .setValue(WATERLOGGED, false)
            .setValue(HEAD, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(SimpleMagneticChuteBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimpleMagneticChuteBlockEntity(ModBlockEntities.SIMPLE_MAGNETIC_CHUTE.get(), pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED, WATERLOGGED, HEAD);
    }

    /**
     * 判断指定位置是否被任意相邻溜槽的输出口指向。
     * 即存在某个方向 dir 上的溜槽，其输出朝向正好是 dir 的反方向（指向本格）。
     *
     * @param level 维度
     * @param pos   被检测的位置
     * @return 若有任意溜槽指向该位置，返回 {@code true}
     */
    public static boolean isPointedByChute(BlockGetter level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState neighborState = level.getBlockState(pos.relative(dir));
            if ((neighborState.is(ModBlocks.MAGNETIC_CHUTE.get()) || neighborState.is(ModBlocks.SIMPLE_MAGNETIC_CHUTE.get()))
                && ChuteBlock.getFacing(neighborState) == dir.getOpposite()) {
                return true;
            }
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
            // 不再被任意磁性溜槽指向时，升级回正常磁性溜槽
            if (!isPointedByChute(level, pos)) {
                level.setBlockAndUpdate(pos, ModBlocks.MAGNETIC_CHUTE.get().defaultBlockState()
                    .setValue(MagneticChuteBlock.FACING, state.getValue(FACING)));
                return;
            }
            // 上方有朝下的普通溜槽/简易溜槽时，附加连接头模型
            BlockState aboveState = level.getBlockState(pos.above());
            boolean hasHead = (aboveState.is(ModBlocks.CHUTE.get()) || aboveState.is(ModBlocks.SIMPLE_CHUTE.get()))
                && aboveState.getValue(ChuteBlock.FACING) == Direction.DOWN;
            if (state.getValue(HEAD) != hasHead) {
                level.setBlockAndUpdate(pos, state.setValue(HEAD, hasHead));
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

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
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
            case NORTH -> state.getValue(HEAD) ? SHAPE_N_HEAD : SHAPE_N;
            case SOUTH -> state.getValue(HEAD) ? SHAPE_S_HEAD : SHAPE_S;
            case WEST -> state.getValue(HEAD) ? SHAPE_W_HEAD : SHAPE_W;
            case EAST -> state.getValue(HEAD) ? SHAPE_E_HEAD : SHAPE_E;
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

    @Override
    public boolean change(Player player, BlockPos pos, Level level, ItemStack anvilHammer) {
        BlockState oldState = level.getBlockState(pos);
        Direction oldFacing = oldState.getValue(FACING);
        Direction newFacing = switch (oldFacing) {
            case WEST -> Direction.UP;
            case UP -> Direction.DOWN;
            case DOWN -> Direction.NORTH;
            default -> oldFacing.getClockWise();
        };
        BlockState facingState = level.getBlockState(pos.relative(newFacing));
        if (ChuteBlock.isChuteBlock(facingState)
            && ChuteBlock.getFacing(facingState) == newFacing.getOpposite()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.levelEvent(2001, pos, Block.getId(oldState));
            Block.dropResources(oldState, level, pos);
            return true;
        }
        HammerRotateBehavior.DEFAULT.change(player, pos, level, anvilHammer);
        return true;
    }

    @Override
public Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
