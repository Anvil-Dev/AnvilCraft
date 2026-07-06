package dev.dubhe.anvilcraft.block.fluid;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.network.ControlValveInitPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * 控制阀（Control Valve），管道系统中的流体门控部件。
 *
 * <p>沿单一轴向（{@link #AXIS} X/Y/Z）连接管道，无前后方向之分。右键打开 GUI
 * 设置允许通过的流体类型（白名单过滤）和最大流速。放置方式类似泵：贴在侧面的
 * 直管/弯管会转为节点以正确吸附。
 */
public class ControlValveBlock extends BetterBaseEntityBlock
    implements IHammerRemovable, IMoveableEntityBlock {
    public static final MapCodec<ControlValveBlock> CODEC = simpleCodec(ControlValveBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    /**
     * 红石锁定：任意侧收到红石信号则锁定，流速视为 0 且 GUI 中不可调。
     */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final VoxelShape SHAPE_X = box(0, 3, 3, 16, 13, 13);
    private static final VoxelShape SHAPE_Y = box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape SHAPE_Z = box(3, 3, 0, 13, 13, 16);

    public ControlValveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, POWERED);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(AXIS)) {
            case X -> SHAPE_X;
            case Y -> SHAPE_Y;
            default -> SHAPE_Z;
        };
    }

    /**
     * 放置时沿点击面的轴向，并按周围红石初始化锁定状态。
     */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
        return defaultBlockState()
            .setValue(AXIS, context.getClickedFace().getAxis())
            .setValue(POWERED, powered);
    }

    /**
     * 判断控制阀在某面方向上是否为连接面（仅朝向轴两端）。
     */
    public static boolean isConnectableFace(BlockState state, Direction faceToNeighbor) {
        if (!(state.getBlock() instanceof ControlValveBlock)) {
            return false;
        }
        return faceToNeighbor.getAxis() == state.getValue(AXIS);
    }

    /**
     * 计算手轮应朝向的面：在垂直于阀门轴 {@code axis} 的 4 个方向中，取最朝向玩家的那一面
     * （法线与玩家视线最反向者）。
     */
    private static Direction computeHandwheelFacing(Direction.Axis axis, LivingEntity placer) {
        net.minecraft.world.phys.Vec3 look = placer.getLookAngle();
        Direction best = null;
        double bestDot = Double.NEGATIVE_INFINITY;
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() == axis) {
                continue; // 手轮不能在连接面（轴两端）
            }
            // 面法线朝玩家 = 与视线方向点积最小（最反向）；等价于 -(look·normal) 最大
            double dot = -(look.x * dir.getStepX() + look.y * dir.getStepY() + look.z * dir.getStepZ());
            if (dot > bestDot) {
                bestDot = dot;
                best = dir;
            }
        }
        return best == null ? Direction.NORTH : best;
    }

    /**
     * 放置后将轴向上连接的直管/弯管转为节点，使其能正确吸附；并记录手轮朝向（面向玩家、垂直于轴的一面）。
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) {
            return;
        }
        // 记录手轮朝向：取玩家视线反方向中、垂直于阀门轴的那一面（即玩家面对的、能看到手轮的一面）
        if (placer != null && level.getBlockEntity(pos) instanceof ControlValveBlockEntity be) {
            be.setFacing(computeHandwheelFacing(state.getValue(AXIS), placer));
        }
        for (Direction dir : Direction.values()) {
            if (!isConnectableFace(state, dir)) {
                continue;
            }
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof PipeStraightBlock) {
                if (dir.getAxis() != neighborState.getValue(PipeBlock.AXIS)) {
                    this.convertPipeToNode(level, neighborPos, neighborState);
                }
            } else if (neighborState.getBlock() instanceof PipeCornerBlock) {
                if (!neighborState.getValue(PipeBlock.CORNER_ENDED).containsDirection(dir.getOpposite())) {
                    this.convertPipeToNode(level, neighborPos, neighborState);
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

    @Override
    public InteractionResult use(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
            && level.getBlockEntity(pos) instanceof ControlValveBlockEntity be) {
            serverPlayer.openMenu(be, pos);
            PacketDistributor.sendToPlayer(serverPlayer, new ControlValveInitPacket(be.getMaxRate(), be.getFilter(0)));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 红石信号更新：任意侧有信号则锁定（POWERED=true），流速视为 0。
     */
    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block block,
        @Nullable Orientation orientation,
        boolean movedByPiston
    ) {
        if (level.isClientSide()) {
            return;
        }
        boolean hasSignal = level.hasNeighborSignal(pos);
        if (hasSignal != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, hasSignal), Block.UPDATE_CLIENTS);
            // 锁定状态变化会改变有效流速 → 使网络缓存失效
            FluidNetworkManager.INSTANCE.markDirty(level);
        }
    }

    /**
     * 控制阀放置 / 落地时使流体网络缓存失效（拓扑可能变化）。
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            FluidNetworkManager.INSTANCE.markDirty(level);
        }
    }

    /**
     * 控制阀被移除 / 被推走时使流体网络缓存失效。
     */
    @Override
    protected void affectNeighborsAfterRemoval(
        BlockState state,
        net.minecraft.server.level.ServerLevel level,
        BlockPos pos,
        boolean movedByPiston
    ) {
        FluidNetworkManager.INSTANCE.markDirty(level);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state;
    }

    @Override
    public @Nullable ControlValveBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CONTROL_VALVE.get().create(pos, state);
    }
}
