package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.block.entity.AdvancedRepeaterBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class AdvancedRepeaterBlock extends DiodeBlock implements EntityBlock {
    protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);
    public static final MapCodec<AdvancedRepeaterBlock> CODEC = simpleCodec(AdvancedRepeaterBlock::new);

    public AdvancedRepeaterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, Boolean.FALSE)
        );
    }

    @Override
    protected MapCodec<? extends DiodeBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedRepeaterBlockEntity(pos, state);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        if (direction == null) return false;
        if (!(state.getBlock() instanceof AdvancedRepeaterBlock)) return false;
        return state.getValue(FACING).getAxis().equals(direction.getAxis());
    }

    @Override
    protected int getOutputSignal(BlockGetter level, BlockPos pos, BlockState state) {
        BlockEntity blockentity = level.getBlockEntity(pos);
        return blockentity instanceof AdvancedRepeaterBlockEntity repeater ? repeater.getOutputSignal() : 0;
    }

    @Override
    protected boolean shouldTurnOn(Level level, BlockPos pos, BlockState state) {
        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof AdvancedRepeaterBlockEntity repeater && repeater.isProcessing()) {
            return repeater.isOutputting();
        } else {
            return false;
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        level.scheduleTick(pos, this, 1, TickPriority.HIGH);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean nowInputting = this.getInputSignal(level, pos, state) > 0;

        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity instanceof AdvancedRepeaterBlockEntity repeater) {
            if (AdvancedRepeaterBlockEntity.canStart(repeater, nowInputting)) {
                repeater.start();
            }

            repeater.setInputtingSignal(nowInputting);
        }

        boolean powered = state.getValue(POWERED);
        if (powered) {
            this.updateNeighborsInFront(level, pos, state);
        }
    }

    @Override
    protected void checkTickOnNeighbor(Level level, BlockPos pos, BlockState state) {
        boolean powered = state.getValue(POWERED);
        boolean shouldBePowered = super.shouldTurnOn(level, pos, state);
        if (
            (powered != shouldBePowered
             || powered != AdvancedRepeaterBlockEntity.canStart(level.getBlockEntity(pos), shouldBePowered)
             || ((AdvancedRepeaterBlockEntity) Objects.requireNonNull(level.getBlockEntity(pos))).isInputtingSignal() != shouldBePowered)
            && !level.getBlockTicks().willTickThisTick(pos, this)
        ) {
            TickPriority tickpriority = TickPriority.HIGH;
            if (this.shouldPrioritize(level, pos, state)) {
                tickpriority = TickPriority.EXTREMELY_HIGH;
            } else if (powered) {
                tickpriority = TickPriority.VERY_HIGH;
            }

            level.scheduleTick(pos, this, 1, tickpriority);
        }
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state,
        BlockEntityType<T> type
    ) {
        if (level.isClientSide()) return null;
        if (type != ModBlockEntities.ADVANCED_REPEATER.get()) return null;
        return (level1, pos, state1, blockEntity) ->
            AdvancedRepeaterBlockEntity.tick(level1, pos, state1, (AdvancedRepeaterBlockEntity) blockEntity);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    protected int getDelay(BlockState state) {
        return 1;
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
        if (be instanceof AdvancedRepeaterBlockEntity blockEntity && pPlayer instanceof ServerPlayer sp) {
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
