package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PulseGeneratorBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);
    public static final MapCodec<PulseGeneratorBlock> CODEC = simpleCodec(PulseGeneratorBlock::new);

    public PulseGeneratorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, Boolean.FALSE)
        );
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PulseGeneratorBlockEntity(pos, state);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        if (direction == null) return false;
        if (!(state.getBlock() instanceof PulseGeneratorBlock)) return false;
        return state.getValue(FACING).getAxis().equals(direction.getAxis());
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getSignal(level, pos, direction);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(FACING) == direction && state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        this.update(level, pos, state);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        this.update(level, pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        this.update(level, pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, false);
        }
    }

    protected void update(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide) {
            boolean nowInputting = this.getInputSignal(level, pos, state) > 0;

            BlockEntity blockentity = level.getBlockEntity(pos);
            if (blockentity instanceof PulseGeneratorBlockEntity generator) {
                if (PulseGeneratorBlockEntity.canStart(generator, nowInputting)) {
                    generator.start(level, pos, state);
                }

                generator.setInputtingSignal(nowInputting);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(POWERED)) {
            Direction direction = state.getValue(FACING).getOpposite();
            double d0 = (double) pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            double d1 = (double) pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
            double d2 = (double) pos.getZ() + 0.6 + (random.nextDouble() - 0.5) * 0.2;
            double d3 = 0.375 + (random.nextDouble() - 0.5) * 0.2;
            double d4 = direction.getStepX() * d3;
            double d5 = direction.getStepZ() * d3;
            level.addParticle(DustParticleOptions.REDSTONE, d0 + d4, d1, d2 + d5, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state,
        BlockEntityType<T> type
    ) {
        if (level.isClientSide()) return null;
        if (type != ModBlockEntities.PULSE_GENERATOR.get()) return null;
        return (level1, pos, state1, blockEntity) ->
            PulseGeneratorBlockEntity.tick(level1, pos, state1, (PulseGeneratorBlockEntity) blockEntity);
    }

    protected int getInputSignal(Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING);
        BlockPos blockpos = pos.relative(direction);
        int i = level.getSignal(blockpos, direction);
        if (i >= 15) {
            return i;
        } else {
            BlockState blockstate = level.getBlockState(blockpos);
            return Math.max(i, blockstate.is(Blocks.REDSTONE_WIRE) ? blockstate.getValue(RedStoneWireBlock.POWER) : 0);
        }
    }

    public static void updateNeighborsInFront(Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING);
        BlockPos blockpos = pos.relative(direction.getOpposite());
        if (net.neoforged.neoforge.event.EventHooks.onNeighborNotify(
            level, pos, level.getBlockState(pos), java.util.EnumSet.of(direction.getOpposite()), false).isCanceled())
            return;
        level.neighborChanged(blockpos, state.getBlock(), pos);
        level.blockEvent(blockpos, level.getBlockState(blockpos).getBlock(), 0, direction.get3DDataValue());
        level.updateNeighborsAtExceptFromFacing(blockpos, state.getBlock(), direction);
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
        if (be instanceof PulseGeneratorBlockEntity blockEntity && pPlayer instanceof ServerPlayer sp) {
            if (sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) return InteractionResult.PASS;
            sp.openMenu(blockEntity, buf -> {
                buf.writeBlockPos(pPos);
                buf.writeNbt(blockEntity.constructDataNbt());
            });
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }
}
