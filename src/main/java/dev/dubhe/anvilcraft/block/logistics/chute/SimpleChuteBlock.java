package dev.dubhe.anvilcraft.block.logistics.chute;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.HammerRotateBehavior;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.ChutePlates;
import dev.dubhe.anvilcraft.block.entity.SimpleChuteBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class SimpleChuteBlock
    extends BaseEntityBlock
    implements SimpleWaterloggedBlock, IHammerChangeable, IHammerRemovable {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING_HOPPER;
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

    public SimpleChuteBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition
            .any()
            .setValue(SimpleChuteBlock.FACING, Direction.DOWN)
            .setValue(SimpleChuteBlock.WATERLOGGED, false)
            .setValue(SimpleChuteBlock.ENABLED, true)
            .setValue(SimpleChuteBlock.TALL, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockBehaviour.simpleCodec(SimpleChuteBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimpleChuteBlockEntity(ModBlockEntities.SIMPLE_CHUTE.get(), pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SimpleChuteBlock.FACING, SimpleChuteBlock.WATERLOGGED, SimpleChuteBlock.ENABLED, SimpleChuteBlock.TALL);
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess ticks,
        BlockPos pos,
        Direction directionToNeighbour,
        BlockPos neighbourPos,
        BlockState neighbourState,
        RandomSource random
    ) {
        if (state.getValue(SimpleChuteBlock.WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (level.isClientSide()) return state;
        BlockState newState = this.getState(level, pos, state.getValue(SimpleChuteBlock.FACING));
        if (newState != null && newState != state) state = newState;

        state = this.checkPoweredState(level, pos, state);
        return state;
    }

    private BlockState checkPoweredState(LevelReader level, BlockPos pos, BlockState state) {
        boolean flag = !level.hasNeighborSignal(pos);
        if (flag == state.getValue(SimpleChuteBlock.ENABLED)) return state;
        return state.setValue(SimpleChuteBlock.ENABLED, flag);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return ChutePlates.withPlateDrops(super.getDrops(state, params), params);
    }

    @Override
    public ItemStack getCloneItemStack(
        LevelReader level,
        BlockPos pos,
        BlockState state,
        boolean includeData,
        Player player
    ) {
        return new ItemStack(ModBlocks.CHUTE);
    }

    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random
    ) {
        if (!state.getValue(SimpleChuteBlock.ENABLED) && !level.hasNeighborSignal(pos)) {
            level.setBlock(pos, state.cycle(SimpleChuteBlock.ENABLED), 2);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void affectNeighborsAfterRemoval(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        boolean movedByPiston
    ) {
        level.updateNeighbourForOutputSignal(pos, this);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) return null;
        return BaseEntityBlock.createTickerHelper(
            blockEntityType,
            ModBlockEntities.SIMPLE_CHUTE.get(),
            ((_, _, _, be) -> be.tick())
        );
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return level.getBlockEntity(pos) instanceof ChutePlates.Holder chute
            ? chute.getChutePlates().takePlate(player, hit) : super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ChutePlates.withPlates(level, pos, getChuteShape(state));
    }

    private static VoxelShape getChuteShape(BlockState state) {
        Direction facing = state.getValue(SimpleChuteBlock.FACING);
        if (!state.getValue(SimpleChuteBlock.TALL)) {
            return switch (facing) {
                case NORTH -> SimpleChuteBlock.AABB_N;
                case EAST -> SimpleChuteBlock.AABB_E;
                case SOUTH -> SimpleChuteBlock.AABB_S;
                case WEST -> SimpleChuteBlock.AABB_W;
                default -> SimpleChuteBlock.AABB;
            };
        } else {
            return switch (facing) {
                case NORTH -> SimpleChuteBlock.AABB_TALL_N;
                case EAST -> SimpleChuteBlock.AABB_TALL_E;
                case SOUTH -> SimpleChuteBlock.AABB_TALL_S;
                case WEST -> SimpleChuteBlock.AABB_TALL_W;
                default -> SimpleChuteBlock.AABB_TALL;
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
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SimpleChuteBlockEntity chuteBlockEntity) {
            return chuteBlockEntity.getRedstoneSignal();
        }
        return 0;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(SimpleChuteBlock.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean change(Player player, BlockPos pos, Level level, ItemStack anvilHammer) {
        BlockState oldState = level.getBlockState(pos);
        Direction oldFacing = oldState.getValue(SimpleChuteBlock.FACING);
        Direction newFacing = switch (oldFacing) {
            case WEST -> Direction.DOWN;
            case DOWN -> Direction.NORTH;
            default -> oldFacing.getClockWise();
        };
        BlockState facingState = level.getBlockState(pos.relative(newFacing));
        if (ChuteBlock.outputsToward(facingState, newFacing.getOpposite())) {
            ChuteBlock.destroyFacingConflict(level, pos, player);
            return true;
        }
        HammerRotateBehavior.DEFAULT.change(player, pos, level, anvilHammer);
        return true;
    }

    @Override
    public Property<?> getChangeableProperty(BlockState blockState) {
        return SimpleChuteBlock.FACING;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(SimpleChuteBlock.FACING, rotation.rotate(state.getValue(SimpleChuteBlock.FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(SimpleChuteBlock.FACING)));
    }

    @Nullable
    BlockState getState(LevelReader level, BlockPos pos, Direction facing) {
        boolean connected = false;
        boolean tall = false;
        BlockState result = level.getBlockState(pos);
        ChutePlates chute = ChutePlates.at(level, pos);
        for (Direction dir : Direction.values()) {
            if (chute != null && chute.getPlates().contains(dir)) {
                continue;
            }
            BlockState source = level.getBlockState(pos.relative(dir));
            if (ChuteBlock.outputsToward(source, dir.getOpposite())) {
                connected = true;
                if (dir == Direction.UP) {
                    tall = !source.is(ModBlocks.MAGNETIC_CHUTE.get());
                }
            }
        }
        if (!connected) {
            result = ModBlocks.CHUTE.getDefaultState()
                .setValue(SimpleChuteBlock.FACING, facing)
                .setValue(SimpleChuteBlock.ENABLED, !level.hasNeighborSignal(pos));
        } else {
            result = result.setValue(SimpleChuteBlock.TALL, tall);
        }
        return result;
    }

    // 防止流体流动时破坏溜槽
    @Override
    public boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }

    // 防止玩家使用桶放置流体时直接替换掉溜槽
    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return false;
    }
}
