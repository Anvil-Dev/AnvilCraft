package dev.dubhe.anvilcraft.block.fluid;

import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkScanner;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 止逆阀（Check Valve），管道系统中的被动单向部件。
 *
 * <p>沿 {@link #FACING} 轴向连接管道，流体仅能沿 {@link #FACING} 方向通过
 * （被动二极管：不改变势场、不耗电，见 {@link FluidNetworkScanner}）；
 * 接受红石信号时（{@link #POWERED}）流向反转。铁砧锤右键反转朝向。
 *
 * <p>放置规则（按住 Shift 时以下流向全部取反）：
 * <ul>
 *   <li>点击流体系统相关方块（管道部件或流体容器）→ 流向朝向被点击的方块；</li>
 *   <li>点击其它方块 → 流向沿玩家视线方向。</li>
 * </ul>
 */
public class CheckValveBlock extends Block implements IHammerRemovable, IHammerChangeable {
    /** 允许的流出方向（无红石信号时）。 */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    /** 红石反向：任意侧收到红石信号则流向反转。 */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final VoxelShape SHAPE_X = box(0, 3, 3, 16, 13, 13);
    private static final VoxelShape SHAPE_Y = box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape SHAPE_Z = box(3, 3, 0, 13, 13, 16);

    public CheckValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACING).getAxis()) {
            case X -> SHAPE_X;
            case Y -> SHAPE_Y;
            default -> SHAPE_Z;
        };
    }

    /**
     * 放置方向：
     * <ul>
     *   <li>点击流体系统相关方块（管道部件或流体容器）→ 流向朝向被点击的方块；</li>
     *   <li>点击其它方块 → 流向沿玩家视线方向；</li>
     *   <li>按住 Shift → 以上流向取反。</li>
     * </ul>
     * 并按周围红石初始化反向状态。
     */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos targetPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        boolean targetIsFluidRelated = FluidNetworkScanner.isPipePart(level.getBlockState(targetPos))
            || PipeBlock.isFluidHandler(level, targetPos);

        Direction facing = targetIsFluidRelated
            ? context.getClickedFace().getOpposite()
            : context.getNearestLookingDirection();
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            facing = facing.getOpposite();
        }
        return defaultBlockState()
            .setValue(FACING, facing)
            .setValue(POWERED, level.hasNeighborSignal(context.getClickedPos()));
    }

    /**
     * 判断止逆阀在某面方向上是否为连接面（仅朝向轴两端）。
     */
    public static boolean isConnectableFace(BlockState state, Direction faceToNeighbor) {
        if (!(state.getBlock() instanceof CheckValveBlock)) {
            return false;
        }
        return faceToNeighbor.getAxis() == state.getValue(FACING).getAxis();
    }

    /**
     * 当前进液侧（高势侧）方向：无红石信号时为 {@link #FACING} 反侧，红石反向时为 {@link #FACING} 侧。
     * 流体仅允许 进液侧 → 另一侧 通过。
     */
    public static Direction inflowSide(BlockState state) {
        Direction facing = state.getValue(FACING);
        return state.getValue(POWERED) ? facing : facing.getOpposite();
    }

    /** 放置后将轴向上连接的直管/弯管转为节点，使其能正确吸附。 */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (!isConnectableFace(state, dir)) {
                continue;
            }
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof PipeStraightBlock) {
                if (dir.getAxis() != neighborState.getValue(PipeBlock.AXIS)) {
                    convertPipeToNode(level, neighborPos, neighborState);
                }
            } else if (neighborState.getBlock() instanceof PipeCornerBlock) {
                if (!neighborState.getValue(PipeBlock.CORNER_ENDED).containsDirection(dir.getOpposite())) {
                    convertPipeToNode(level, neighborPos, neighborState);
                }
            }
        }
    }

    private void convertPipeToNode(Level level, BlockPos pos, BlockState state) {
        BlockState nodeState = ModBlocks.PIPE_NODE.get()
            .defaultBlockState()
            .setValue(PipeBlock.WATERLOGGED, state.getValue(PipeBlock.WATERLOGGED));
        for (Direction dir : Direction.values()) {
            nodeState = nodeState.setValue(
                PipeBlock.getPropertyForDirection(dir),
                PipeNodeBlock.evaluateNeighbor(level, pos, dir)
            );
        }
        level.setBlockAndUpdate(pos, nodeState);
    }

    /** 红石信号更新：任意侧有信号则流向反转（POWERED=true）。 */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }
        boolean hasSignal = level.hasNeighborSignal(pos);
        if (hasSignal != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, hasSignal), Block.UPDATE_ALL);
            // 流向变化 → 使网络缓存失效以重扫二极管方向
            FluidNetworkManager.INSTANCE.markDirty(level);
        }
    }

    // ---- 铁砧锤：反转流向 ----

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        BlockState state = level.getBlockState(blockPos);
        if (!(state.getBlock() instanceof CheckValveBlock)) {
            return false;
        }
        level.setBlockAndUpdate(blockPos, state.setValue(FACING, state.getValue(FACING).getOpposite()));
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING;
    }

    /** 止逆阀放置 / 落地 / 状态变化时使流体网络缓存失效（拓扑或流向可能变化）。 */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            FluidNetworkManager.INSTANCE.markDirty(level);
        }
    }

    /** 止逆阀被移除 / 被推走时使流体网络缓存失效。 */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            FluidNetworkManager.INSTANCE.markDirty(level);
        }
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
}
