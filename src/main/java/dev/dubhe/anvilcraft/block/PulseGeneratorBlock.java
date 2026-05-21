package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Supplier;

public class PulseGeneratorBlock extends HorizontalDirectionalBlock implements IHammerChangeable, IHammerRemovable, IMoveableEntityBlock {
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
        this.update(level, pos, () -> state);
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
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (oldState.getBlock() == state.getBlock()) return;
        this.update(level, pos, () -> state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getBlockTicks().clearArea(new BoundingBox(pos));
            }
            super.onRemove(state, level, pos, newState, false);
        }
        Direction facing = state.getValue(FACING);
        BlockPos front = pos.relative(facing.getOpposite());
        if (EventHooks.onNeighborNotify(level, pos, level.getBlockState(pos), EnumSet.of(facing.getOpposite()), false).isCanceled()) return;
        level.neighborChanged(front, this, pos);
        level.updateNeighborsAtExceptFromFacing(front, this, facing);
    }

    public void update(Level level, BlockPos pos, Supplier<BlockState> stateGetter) {
        if (level.isClientSide) return;

        BlockEntity blockentity = level.getBlockEntity(pos);
        if (!(blockentity instanceof PulseGeneratorBlockEntity generator)) return;
        boolean lastInputting = generator.isInputtingSignal();
        boolean nowInputting = PulseGeneratorBlock.getInputSignal(level, pos, stateGetter.get()) > 0;
        generator.setInputtingSignal(nowInputting);
        boolean canStart = switch (generator.getStartMode()) {
            case RISING_EDGE -> !lastInputting && nowInputting;
            case FALLING_EDGE -> lastInputting && !nowInputting;
            case LOOP -> !generator.isLocked();
        } && !generator.isProcessing();

        if (canStart) {
            this.startWaiting(level, pos, stateGetter, generator);
            this.updateBlockAndNeighbours(level, pos, stateGetter, generator);
        }

        if (generator.getStartMode() == PulseGeneratorBlockEntity.Mode.LOOP) {
            if (generator.isLocked() && !generator.isInputtingSignal()) {
                this.startWaiting(level, pos, stateGetter, generator);
                generator.setLocked(false);
            } else {
                generator.setLocked(generator.isInputtingSignal());
            }
        }
        if (generator.isLocked()) {
            generator.setState(PulseGeneratorBlockEntity.State.DEFAULT);
            this.updateBlockAndNeighbours(level, pos, stateGetter, generator);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Optional<PulseGeneratorBlockEntity> generatorOp = level.getBlockEntity(pos, ModBlockEntities.PULSE_GENERATOR.get());
        if (generatorOp.isEmpty()) return;
        PulseGeneratorBlockEntity generator = generatorOp.get();
        if (!generator.isLocked()) {
            switch (generator.getState()) {
                case WAITING -> this.startOutputting(level, pos, () -> state, generator);
                case OUTPUTTING -> this.checkOnSignalEnd(level, pos, () -> state, generator);
                case DEFAULT -> this.updateBlockAndNeighbours(level, pos, () -> state, generator);
                default -> {
                }
            }
        }
    }

    public void startWaiting(Level level, BlockPos pos, Supplier<BlockState> stateGetter, PulseGeneratorBlockEntity generator) {
        generator.setState(PulseGeneratorBlockEntity.State.WAITING);
        if (generator.getWaitingTime() == 1
            && generator.getSignalDuration() == 0) {
            generator.setState(PulseGeneratorBlockEntity.State.OUTPUTTING);
            level.scheduleTick(pos, this, 1, TickPriority.LOW);
            this.updateBlockAndNeighbours(level, pos, stateGetter, generator);
            return;
        }
        if (generator.getWaitingTime() != 0) {
            level.scheduleTick(pos, this, generator.getWaitingTime(), TickPriority.LOW);
        } else {
            this.startOutputting(level, pos, stateGetter, generator);
        }
    }

    protected void startOutputting(Level level, BlockPos pos, Supplier<BlockState> stateGetter, PulseGeneratorBlockEntity generator) {
        generator.setState(PulseGeneratorBlockEntity.State.OUTPUTTING);
        if (generator.getSignalDuration() != 0) {
            level.scheduleTick(pos, this, generator.getSignalDuration(), TickPriority.LOW);
            this.updateBlockAndNeighbours(level, pos, stateGetter, generator);
        } else {
            this.updateBlockAndNeighbours(level, pos, stateGetter, generator);
            this.checkOnSignalEnd(level, pos, stateGetter, generator);
        }
    }

    protected void checkOnSignalEnd(Level level, BlockPos pos, Supplier<BlockState> stateGetter, PulseGeneratorBlockEntity generator) {
        generator.setState(PulseGeneratorBlockEntity.State.DEFAULT);
        this.updateBlockAndNeighbours(level, pos, stateGetter, generator);

        if (generator.getStartMode() == PulseGeneratorBlockEntity.Mode.LOOP) {
            this.startWaiting(level, pos, stateGetter, generator);
        }
    }

    protected void updateBlockAndNeighbours(
        Level level,
        BlockPos pos,
        Supplier<BlockState> stateGetter,
        PulseGeneratorBlockEntity generator
    ) {
        BlockState state = stateGetter.get();
        boolean powered = state.getValue(POWERED);
        boolean shouldPower = generator.isOutputting();
        if (powered == shouldPower) return;
        Direction direction = state.getValue(FACING).getOpposite();
        BlockPos neighbourPos = pos.relative(direction);
        BlockState newState = state.setValue(POWERED, shouldPower);
        level.setBlockAndUpdate(pos, newState);
        // noinspection deprecation
        generator.setBlockState(newState);
        level.neighborChanged(neighbourPos, state.getBlock(), pos);
        level.updateNeighborsAtExceptFromFacing(neighbourPos, state.getBlock(), direction.getOpposite());
        if (generator.getSignalDuration() == 0) {
            level.scheduleTick(pos, this, 1, TickPriority.LOW);
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

    public static int getInputSignal(Level level, BlockPos pos, BlockState state) {
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

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hitResult
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PulseGeneratorBlockEntity blockEntity && player instanceof ServerPlayer sp) {
            if (sp.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) return InteractionResult.PASS;
            sp.openMenu(
                blockEntity, buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeNbt(blockEntity.constructDataNbt());
                }
            );
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof PulseGeneratorBlockEntity be && player.getItemInHand(hand).is(ModItems.DISK)) {
                return Util.interactionResultConverter()
                    .apply(be.useDisk(level, serverPlayer, hand, serverPlayer.getItemInHand(hand), hitResult));
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        return level.setBlockAndUpdate(blockPos, level.getBlockState(blockPos).cycle(FACING));
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }

    @Override
    public CompoundTag clearData(Level level, BlockPos pos) {
        CompoundTag[] data = new CompoundTag[1];
        level.getBlockEntity(pos, ModBlockEntities.PULSE_GENERATOR.get())
            .ifPresent(be -> data[0] = be.exportMoveData());
        return data[0];
    }

    @Override
    public void setData(Level level, BlockPos pos, CompoundTag tag) {
        level.getBlockEntity(pos, ModBlockEntities.PULSE_GENERATOR.get())
            .ifPresent(be -> be.applyMoveData(level, pos, level.getBlockState(pos), tag));
    }
}

