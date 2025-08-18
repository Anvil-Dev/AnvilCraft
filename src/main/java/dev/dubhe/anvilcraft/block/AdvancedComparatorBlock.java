package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity.Mode;
import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity.State;
import dev.dubhe.anvilcraft.block.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class AdvancedComparatorBlock extends HorizontalDirectionalBlock implements IMoveableEntityBlock, IHammerRemovable {
    public static final MapCodec<AdvancedComparatorBlock> CODEC = simpleCodec(AdvancedComparatorBlock::new);

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty INPUT = BooleanProperty.create("input");
    public static final IntegerProperty POWER = IntegerProperty.create("power", 0, 15);
    public static final EnumProperty<Mode> MODE = EnumProperty.create("mode", Mode.class);

    protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);

    public AdvancedComparatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(INPUT, false)
                .setValue(POWER, 0)
                .setValue(MODE, Mode.HYSTERESIS)
                .setValue(POWERED, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, INPUT, POWER, MODE, POWERED);
    }

    @Override
    protected MapCodec<? extends AdvancedComparatorBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AdvancedComparatorBlockEntity(pos, state);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        if (direction == null) return false;
        if (!(state.getBlock() instanceof AdvancedComparatorBlock)) return false;
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
        level.scheduleTick(pos, this, getDelay());
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
        level.scheduleTick(pos, this, getDelay());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        level.scheduleTick(pos, this, getDelay());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!isMoving && !state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, false);
        }
        Direction facing = state.getValue(FACING);
        BlockPos front = pos.relative(facing.getOpposite());
        if (EventHooks.onNeighborNotify(level, pos, level.getBlockState(pos), EnumSet.of(facing.getOpposite()), false)
            .isCanceled()
        ) return;
        level.neighborChanged(front, this, pos);
        level.updateNeighborsAtExceptFromFacing(front, this, facing);
    }

    public void update(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        BlockEntity blockentity = level.getBlockEntity(pos);
        if (!(blockentity instanceof AdvancedComparatorBlockEntity comparator)) return;
        Mode mode = comparator.getCompareMode();
        int highLimit = comparator.getHighLimit();
        int lowLimit = comparator.getLowLimit();
        int inputtingSignal = comparator.getInputtingSignal();
        if (mode == Mode.WINDOW && highLimit == lowLimit && inputtingSignal == highLimit) comparator.setState(State.OUTPUT_HIGH);
        switch (comparator.getState()) {
            case OUTPUT_LOW -> {
                if (mode == Mode.HYSTERESIS) {
                    if (inputtingSignal >= highLimit)
                        comparator.setState(State.OUTPUT_HIGH);
                } else if (mode == Mode.WINDOW) {
                    if (inputtingSignal >= lowLimit && inputtingSignal <= highLimit)
                        comparator.setState(State.OUTPUT_HIGH);
                }
            }
            case OUTPUT_HIGH -> {
                if (mode == Mode.HYSTERESIS) {
                    if (inputtingSignal < lowLimit)
                        comparator.setState(State.OUTPUT_LOW);
                } else if (mode == Mode.WINDOW) {
                    if (inputtingSignal < lowLimit || inputtingSignal > highLimit)
                        comparator.setState(State.OUTPUT_LOW);
                }
            }
        }
        comparator.setChanged();
        this.updateBlockAndNeighbours(level, pos, state, comparator);
    }

    public static int getInputSignal(Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING);
        BlockPos blockpos = pos.relative(direction);
        BlockState blockstate = level.getBlockState(blockpos);
        int i = level.getSignal(blockpos, direction);
        if (blockstate.hasAnalogOutputSignal()) {
            i = blockstate.getAnalogOutputSignal(level, blockpos);
        } else if (i < 15 && blockstate.isRedstoneConductor(level, blockpos)) {
            blockpos = blockpos.relative(direction);
            blockstate = level.getBlockState(blockpos);
            ItemFrame itemframe = getItemFrame(level, direction, blockpos);
            int j = Math.max(itemframe.getAnalogOutput(), blockstate.hasAnalogOutputSignal() ? blockstate.getAnalogOutputSignal(level, blockpos) : Integer.MIN_VALUE);
            if (j != Integer.MIN_VALUE) {
                i = j;
            }
        }
        return i;
    }

    private static ItemFrame getItemFrame(Level level, Direction facing, BlockPos pos) {
        List<ItemFrame> list = level.getEntitiesOfClass(ItemFrame.class, new AABB(
            pos.getX(), pos.getY(), pos.getZ(), (pos.getX() + 1), (pos.getY() + 1), (pos.getZ() + 1)), (frame) -> frame != null && frame.getDirection() == facing);
        return list.getFirst();
    }

    public static int getAlternateSignal(SignalGetter level, BlockPos pos, BlockState state, boolean isHigh) {
        Direction direction = state.getValue(FACING);
        Direction right = direction.getClockWise();
        Direction left = direction.getCounterClockWise();
        return isHigh ? Math.max(level.getSignal(pos.relative(right), right), level.getSignal(pos.relative(left), left))
            : Math.min(level.getSignal(pos.relative(right), right), level.getSignal(pos.relative(left), left));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Optional<AdvancedComparatorBlockEntity> optional = level.getBlockEntity(pos, ModBlockEntities.ADVANCED_COMPARATOR.get());
        if (level.isClientSide || optional.isEmpty()) return;
        AdvancedComparatorBlockEntity blockEntity = optional.get();
        blockEntity.updateInputtingSignal(level, pos, state);
        this.updateBlockAndNeighbours(level, pos, state, blockEntity);
        this.update(level, pos, state);
        level.scheduleTick(pos, this, getDelay());
    }

    protected void updateBlockAndNeighbours(Level level, BlockPos pos, BlockState state, AdvancedComparatorBlockEntity blockEntity) {
        Direction direction = state.getValue(AdvancedComparatorBlock.FACING).getOpposite();
        BlockPos neighbourPos = pos.relative(direction);
        boolean shouldPower = blockEntity.isOutputting();
        int inputtingSignal = blockEntity.getInputtingSignal();
        Mode mode = blockEntity.getCompareMode();
        level.setBlockAndUpdate(pos,
            state.setValue(AdvancedComparatorBlock.POWERED, shouldPower)
                .setValue(AdvancedComparatorBlock.INPUT, inputtingSignal > 0)
                .setValue(AdvancedComparatorBlock.POWER, inputtingSignal)
                .setValue(AdvancedComparatorBlock.MODE, mode));
        level.neighborChanged(neighbourPos, state.getBlock(), pos);
        level.updateNeighborsAtExceptFromFacing(neighbourPos, state.getBlock(), direction.getOpposite());
    }

    @Override
    public boolean getWeakChanges(BlockState state, LevelReader world, BlockPos pos) {
        return true;
    }

    public static int getDelay() {
        return 1;
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity blockentity = level.getBlockEntity(pos);
        return blockentity != null && blockentity.triggerEvent(id, param);
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
        if (be instanceof AdvancedComparatorBlockEntity blockEntity && pPlayer instanceof ServerPlayer sp) {
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
            if (level.getBlockEntity(pos) instanceof AdvancedComparatorBlockEntity be && player.getItemInHand(hand).is(ModItems.DISK)) {
                return Util.interactionResultConverter()
                    .apply(be.useDisk(level, serverPlayer, hand, serverPlayer.getItemInHand(hand), hitResult));
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }
}
