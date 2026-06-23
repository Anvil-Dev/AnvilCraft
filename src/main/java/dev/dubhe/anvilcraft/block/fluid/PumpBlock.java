package dev.dubhe.anvilcraft.block.fluid;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.fluid.PumpBlockEntity;
import dev.dubhe.anvilcraft.block.state.Orientation;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 泵（Pump），管道系统的主动流体输送设备。消耗 32kW 电力，
 * 输入端等效高度 +10，输出端 -10。12 向放置，铁砧锤右键反转方向，红石可关闭。
 */
public class PumpBlock extends BetterBaseEntityBlock implements IHammerRemovable, IHammerChangeable {
    public static final EnumProperty<Orientation> ORIENTATION = EnumProperty.create("orientation", Orientation.class);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;

    private static final VoxelShape SHAPE_Z = box(3, 3, 0, 13, 13, 16);
    private static final VoxelShape SHAPE_X = box(0, 3, 3, 16, 13, 13);
    private static final VoxelShape SHAPE_Y = box(3, 0, 3, 13, 16, 13);

    public PumpBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(ORIENTATION, Orientation.NORTH_UP)
            .setValue(POWERED, false)
            .setValue(OVERLOAD, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORIENTATION, POWERED, OVERLOAD);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(ORIENTATION).getDirection().getAxis()) {
            case X -> SHAPE_X;
            case Y -> SHAPE_Y;
            default -> SHAPE_Z;
        };
    }

    @Override
    protected MapCodec<PumpBlock> codec() {
        return simpleCodec(PumpBlock::new);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction lookDir = context.getNearestLookingDirection();
        Direction horizontalDir = context.getHorizontalDirection();
        Player player = context.getPlayer();
        boolean shiftDown = player != null && player.isShiftKeyDown();
        if (lookDir.getAxis() == Direction.Axis.Y) {
            if (!shiftDown) lookDir = lookDir.getOpposite();
        } else {
            if (!shiftDown) horizontalDir = horizontalDir.getOpposite();
        }
        Orientation orientation = switch (lookDir) {
            case UP -> switch (horizontalDir) {
                case SOUTH -> Orientation.UP_SOUTH;
                case WEST -> Orientation.UP_WEST;
                case EAST -> Orientation.UP_EAST;
                default -> Orientation.UP_NORTH;
            };
            case DOWN -> switch (horizontalDir) {
                case SOUTH -> Orientation.DOWN_SOUTH;
                case WEST -> Orientation.DOWN_WEST;
                case EAST -> Orientation.DOWN_EAST;
                default -> Orientation.DOWN_NORTH;
            };
            default -> switch (horizontalDir) {
                case SOUTH -> Orientation.SOUTH_UP;
                case WEST -> Orientation.WEST_UP;
                case EAST -> Orientation.EAST_UP;
                default -> Orientation.NORTH_UP;
            };
        };
        return defaultBlockState().setValue(ORIENTATION, orientation);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof PipeStraightBlock) {
                Direction.Axis pipeAxis = neighborState.getValue(PipeBlock.AXIS);
                if (dir.getAxis() != pipeAxis) this.convertPipeToNode(level, neighborPos, neighborState);
            } else if (neighborState.getBlock() instanceof PipeCornerBlock) {
                PipeBlock.CornerEnded corner = neighborState.getValue(PipeBlock.CORNER_ENDED);
                if (!corner.containsDirection(dir.getOpposite())) this.convertPipeToNode(level, neighborPos, neighborState);
            }
        }
    }

    private void convertPipeToNode(Level level, BlockPos pos, BlockState state) {
        BlockState nodeState = ModBlocks.PIPE_NODE.get().defaultBlockState()
            .setValue(PipeBlock.WATERLOGGED, state.getValue(PipeBlock.WATERLOGGED));
        for (Direction dir : Direction.values()) {
            nodeState = nodeState.setValue(PipeBlock.getPropertyForDirection(dir),
                PipeNodeBlock.evaluateNeighbor(level, pos, dir));
        }
        level.setBlockAndUpdate(pos, nodeState);
    }

    @Override
    protected void neighborChanged(
        BlockState state, Level level, BlockPos pos,
        Block neighborBlock, @Nullable net.minecraft.world.level.redstone.Orientation redstoneOrientation, boolean movedByPiston
    ) {
        if (level.isClientSide()) return;
        boolean hasSignal = level.hasNeighborSignal(pos);
        if (hasSignal != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, hasSignal), 2);
        }
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        BlockState state = level.getBlockState(blockPos);
        level.setBlockAndUpdate(blockPos, state.setValue(ORIENTATION, state.getValue(ORIENTATION).opposite()));
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState state) {
        return ORIENTATION;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(ORIENTATION, state.getValue(ORIENTATION).rotate(rotation));
    }

    @Override
    public @Nullable PumpBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.PUMP.get().create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return BaseEntityBlock.createTickerHelper(type, ModBlockEntities.PUMP.get(), PumpBlockEntity::tick);
    }

    @Override
    public boolean checkBlockState(BlockState blockState) {
        return false;
    }
}
