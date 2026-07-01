package dev.dubhe.anvilcraft.block.sliding;

import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.block.entity.DetectorSlidingRailBlockEntity;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

public class DetectorSlidingRailBlock extends BaseSlidingRailBlock implements IHammerChangeable, IMoveableEntityBlock {
    public static final VoxelShape AABB_X = Stream.of(
            Block.box(0, 0, 0, 16, 6, 16),
            Block.box(0, 6, 11, 16, 16, 16),
            Block.box(0, 6, 0, 16, 16, 5)
        )
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    public static final VoxelShape AABB_Z = Stream.of(
            Block.box(0, 0, 0, 16, 6, 16),
            Block.box(11, 6, 0, 16, 16, 16),
            Block.box(0, 6, 0, 5, 16, 16)
        )
        .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR))
        .get();
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public DetectorSlidingRailBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        if (
            context.getPlayer() != null
            && context.getPlayer().isShiftKeyDown()
        ) {
            facing = facing.getOpposite();
        }
        return this.defaultBlockState().setValue(FACING, facing).setValue(POWERED, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return switch (blockState.getValue(FACING).getAxis()) {
            case X -> AABB_X;
            case Z -> AABB_Z;
            default -> super.getShape(blockState, blockGetter, blockPos, collisionContext);
        };
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (
            state.getValue(POWERED)
            && level.getEntitiesOfClass(SlidingBlockEntity.class, new AABB(pos.above())).isEmpty()
            && level.getEntitiesOfClass(ItemEntity.class, new AABB(pos)).isEmpty()
        ) {
            level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
            level.getBlockEntity(pos, ModBlockEntities.DETECTOR_SLIDING_RAIL.get()).ifPresent(DetectorSlidingRailBlockEntity::cleanPower);
        }
        super.tick(state, level, pos, random);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        if (!state.getValue(POWERED)) return 0;
        return side == Direction.DOWN ? 0 : 15;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos, ModBlockEntities.DETECTOR_SLIDING_RAIL.get())
            .map(DetectorSlidingRailBlockEntity::getPower)
            .orElse(0);
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        BlockState bs = level.getBlockState(blockPos);
        level.setBlockAndUpdate(blockPos, bs.cycle(FACING));
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }

    @Override
    public void onSlidingAbove(Level level, BlockPos pos, BlockState state, SlidingBlockEntity entity) {
        Optional<DetectorSlidingRailBlockEntity> blockEntity = level.getBlockEntity(pos, ModBlockEntities.DETECTOR_SLIDING_RAIL.get());
        blockEntity.ifPresent(detector -> detector.updatePower(entity.getBlockCount()));
        level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, 20);
    }

    public void onItemEntitySlidingAbove(Level level, BlockPos pos, BlockState state) {
        Optional<DetectorSlidingRailBlockEntity> blockEntity = level.getBlockEntity(pos, ModBlockEntities.DETECTOR_SLIDING_RAIL.get());
        blockEntity.ifPresent(detector -> detector.updatePower(1));
        level.setBlock(pos, state.setValue(POWERED, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, this, 20);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.DETECTOR_SLIDING_RAIL.create(pos, state);
    }

    @Override
    public void notifyMoved(Level level, BlockPos pos, BlockState state, BlockEntity be) {
        level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
        if (be instanceof DetectorSlidingRailBlockEntity detector) {
            detector.cleanPower();
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
