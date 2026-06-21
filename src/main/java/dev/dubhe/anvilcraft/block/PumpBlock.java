package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.PumpBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeNodeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeStraightBlock;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
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
 * 泵（Pump），管道系统的主动流体输送设备。
 * 消耗 32kW 电力，输入端等效高度 +10，输出端 -10。
 * 12 向放置（{@link Orientation}），铁砧锤右键反转方向，红石可关闭。
 */
public class PumpBlock extends BetterBaseEntityBlock implements IHammerRemovable, IHammerChangeable {

    public static final EnumProperty<Orientation> ORIENTATION = EnumProperty.create("orientation", Orientation.class);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty OVERLOAD = IPowerComponent.OVERLOAD;

    /** 主体碰撞箱 [5,5,5]→[11,11,11]（6x6x6 中心体） */
    private static final VoxelShape SHAPE = box(5, 5, 5, 11, 11, 11);

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

    /** 仅主体部分的碰撞箱 */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected MapCodec<PumpBlock> codec() {
        return simpleCodec(PumpBlock::new);
    }

    /** 放置时根据玩家视线和 Shift 计算朝向。默认输入端朝向目标方块，Shift 反向 */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction lookDir = context.getNearestLookingDirection();
        Direction horizontalDir = context.getHorizontalDirection();
        Player player = context.getPlayer();
        boolean shiftDown = player != null && player.isShiftKeyDown();

        // 按住 Shift 反向放置（输出端朝向目标方块）
        if (shiftDown) horizontalDir = horizontalDir.getOpposite();

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

    /** 放置后将侧面连接的直管转为三通节点 */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof PipeStraightBlock) {
                Direction.Axis pipeAxis = neighborState.getValue(PipeBlock.AXIS);
                // 泵贴在直管侧面 → 将直管转为三通节点
                if (dir.getAxis() != pipeAxis) {
                    convertPipeToNode(level, neighborPos, neighborState, dir.getOpposite());
                }
            }
        }
    }

    /** 将直管转为三通节点，保留原有两端的连接并添加新方向 */
    private void convertPipeToNode(Level level, BlockPos pos, BlockState state, Direction newDirection) {
        Direction.Axis axis = state.getValue(PipeBlock.AXIS);
        Direction startDir = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
        Direction endDir = Direction.get(Direction.AxisDirection.POSITIVE, axis);

        BlockState nodeState = ModBlocks.PIPE_NODE.get().defaultBlockState()
            .setValue(PipeBlock.WATERLOGGED, state.getValue(PipeBlock.WATERLOGGED));
        nodeState = nodeState.setValue(
            PipeBlock.getPropertyForDirection(startDir),
            PipeNodeBlock.evaluateNeighbor(level, pos, startDir));
        nodeState = nodeState.setValue(
            PipeBlock.getPropertyForDirection(endDir),
            PipeNodeBlock.evaluateNeighbor(level, pos, endDir));
        nodeState = nodeState.setValue(
            PipeBlock.getPropertyForDirection(newDirection),
            PipeBlock.NodePipe.PIPE);
        level.setBlockAndUpdate(pos, nodeState);
    }

    /** 红石信号更新 */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;
        boolean hasSignal = level.hasNeighborSignal(pos);
        if (hasSignal != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, hasSignal), 2);
        }
    }

    /** 铁砧锤反转方向 */
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
}
