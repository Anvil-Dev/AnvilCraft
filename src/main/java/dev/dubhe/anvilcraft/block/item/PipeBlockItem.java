package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeCornerBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeNodeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeStraightBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import javax.annotation.Nullable;

/**
 * 管道放置物品，负责处理所有管道放置和连接的交互逻辑。
 *
 * <h3>放置模式</h3>
 * <ol>
 *   <li><b>视线模式</b>（Shift+点击 或 点击非管道/非IFluidHandler）：
 *       沿玩家视线方向放置直管，两端均有端头</li>
 *   <li><b>连接模式</b>（点击管道或 IFluidHandler 表面）：
 *       沿被点击面轴线放置直管，朝向目标的端头根据目标类型（管/IFluidHandler）开关</li>
 *   <li><b>相邻连接模式</b>（两端都是管道或 IFluidHandler）：
 *       不放置新方块，直接修改两侧已有方块建立连接</li>
 * </ol>
 *
 * <h3>弯管交互</h3>
 * 点击弯管时会根据弯管状态和被点击面对弯管进行转换（→直管/节点/旋转）。
 */
public class PipeBlockItem extends Item {

    public PipeBlockItem(Properties properties) {
        super(properties);
    }

    /**
     * 使用物品时的入口：非 Shift 时先尝试相邻连接，失败则走普通放置。
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        Player player = context.getPlayer();
        if (player != null && !player.isShiftKeyDown()) {
            if (tryConnectAdjacent(placeContext)) {
                return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
            }
        }
        return this.place(placeContext);
    }

    /**
     * 尝试在相邻的两个实体（管道↔管道 或 管道↔IFluidHandler）之间建立连接。
     * 双方都是管道时各自转为节点互相连接；只有一方是管道时仅修改管道。
     * 只在服务端执行修改，客户端返回 null。
     *
     * @return 是否实际修改了任意方块
     */
    private boolean tryConnectAdjacent(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos placePos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockPos targetPos = placePos.relative(clickedFace.getOpposite());

        BlockState targetState = level.getBlockState(targetPos);
        BlockState placeState = level.getBlockState(placePos);

        boolean targetIsPipe = targetState.getBlock() instanceof PipeBlock;
        boolean targetIsFluid = PipeBlock.isFluidHandler(level, targetPos);
        boolean placeIsPipe = placeState.getBlock() instanceof PipeBlock;
        boolean placeIsFluid = PipeBlock.isFluidHandler(level, placePos);

        // 双方至少有一方是管道/IFluidHandler 且另一方也是
        if ((!targetIsPipe && !targetIsFluid) || (!placeIsPipe && !placeIsFluid)) {
            return false;
        }

        boolean modified = false;
        Player player = context.getPlayer();
        if (targetIsPipe) {
            BlockState newState = modifyPipeToConnect(level, targetPos, targetState, clickedFace, placeIsPipe);
            if (newState != null) {
                playPlaceSound(level, targetPos, newState, player);
                modified = true;
            }
        }
        if (placeIsPipe) {
            BlockState newState = modifyPipeToConnect(level, placePos, placeState, clickedFace.getOpposite(), targetIsPipe);
            if (newState != null) {
                playPlaceSound(level, placePos, newState, player);
                modified = true;
            }
        }
        return modified;
    }

    /**
     * 修改指定管道以在 {@code toward} 方向建立连接。
     * 如果该方向已有开放连接则跳过（返回 null）。
     * 按管型分派到 {@link #modifyStraightToConnect} / {@link #modifyCornerToConnect}。
     *
     * @param level        世界
     * @param pos          管道位置
     * @param state        管道当前状态
     * @param toward       连接方向
     * @param towardIsPipe 该方向是否为管道（否则为 IFluidHandler）
     * @return 修改后的方块状态，未修改则返回 null（客户端也返回 null）
     */
    @Nullable
    private static BlockState modifyPipeToConnect(Level level, BlockPos pos, BlockState state, Direction toward, boolean towardIsPipe) {
        if (hasOpenConnectionToward(state, toward)) {
            return null;
        }

        if (state.getBlock() instanceof PipeStraightBlock) {
            return modifyStraightToConnect(level, pos, state, toward, towardIsPipe);
        } else if (state.getBlock() instanceof PipeCornerBlock) {
            return modifyCornerToConnect(level, pos, state, toward, towardIsPipe);
        } else if (state.getBlock() instanceof PipeNodeBlock) {
            // 节点：直接设置对应方向
            PipeBlock.NodePipe value = towardIsPipe ? PipeBlock.NodePipe.PIPE : PipeBlock.NodePipe.END;
            BlockState newState = state.setValue(PipeBlock.getPropertyForDirection(toward), value);
            level.setBlockAndUpdate(pos, newState);
            return newState;
        }
        return null;
    }

    /**
     * 修改直管以建立连接。
     * <ul>
     *   <li>{@code toward} 在轴方向 → 只开关端头（{@code towardIsPipe} 决定端头状态）</li>
     *   <li>{@code toward} 在侧面 → 调用 {@link #getConnectedBlockState} 计算转换</li>
     * </ul>
     */
    private static @Nullable BlockState modifyStraightToConnect(
        Level level,
        BlockPos pos,
        BlockState state,
        Direction toward,
        boolean towardIsPipe
    ) {
        Direction.Axis axis = state.getValue(PipeBlock.AXIS);
        Direction startDir = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
        Direction endDir = Direction.get(Direction.AxisDirection.POSITIVE, axis);

        if (toward.getAxis() == axis) {
            // 轴方向：只开关端头
            return getContainsDirectionBlockState(level, pos, state, toward, towardIsPipe, startDir);
        }
        // 侧面：需要转换管型
        return getConnectedBlockState(level, pos, state, toward, towardIsPipe, startDir, endDir);
    }

    /**
     * 修改弯管以建立连接。
     * <ul>
     *   <li>{@code toward} 是弯管已有方向 → 只开关端头</li>
     *   <li>{@code toward} 非弯管方向 → 调用 {@link #getConnectedBlockState} 计算转换</li>
     * </ul>
     */
    private static @Nullable BlockState modifyCornerToConnect(
        Level level,
        BlockPos pos,
        BlockState state,
        Direction toward,
        boolean towardIsPipe
    ) {
        PipeBlock.CornerEnded corner = state.getValue(PipeBlock.CORNER_ENDED);
        Direction first = corner.getFirstDirection();
        Direction second = corner.getSecondDirection();

        if (corner.containsDirection(toward)) {
            // 弯管已有方向：只开关端头
            return getContainsDirectionBlockState(level, pos, state, toward, towardIsPipe, first);
        }
        // 新方向：需要转换管型
        return getConnectedBlockState(level, pos, state, toward, towardIsPipe, first, second);
    }

    /**
     * 在管道已有方向上开关端头（不改变管型）。
     *
     * @param first 第一端方向（用于区分 HAS_END_START / HAS_END_END）
     */
    private static @Nullable BlockState getContainsDirectionBlockState(
        Level level,
        BlockPos pos,
        BlockState state,
        Direction toward,
        boolean towardIsPipe,
        Direction first
    ) {
        BlockState newState = state;
        if (toward == first) {
            newState = newState.setValue(PipeBlock.HAS_END_START, !towardIsPipe);
        } else {
            newState = newState.setValue(PipeBlock.HAS_END_END, !towardIsPipe);
        }
        if (newState != state) {
            level.setBlockAndUpdate(pos, newState);
            return newState;
        }
        return null;
    }

    /**
     * 计算管道在新方向建立连接后的方块状态。
     * <ul>
     *   <li>两端都已占用 → 节点</li>
     *   <li>一端占用且 {@code toward} 是对向 → 直管（对向贯通）</li>
     *   <li>一端占用且 {@code toward} 非对向 → 弯管</li>
     * </ul>
     *
     * @param startDir 已有第一端方向
     * @param endDir   已有第二端方向
     */
    private static BlockState getConnectedBlockState(
        Level level,
        BlockPos pos,
        BlockState state,
        Direction toward,
        boolean towardIsPipe,
        Direction startDir,
        Direction endDir
    ) {
        boolean startOccupied = PipeBlock.isNeighborOccupied(level, pos, startDir);
        boolean endOccupied = PipeBlock.isNeighborOccupied(level, pos, endDir);

        if (startOccupied && endOccupied) {
            // 两端都忙 → 节点（3+ 连接）
            return convertToNode(level, pos, state, toward, towardIsPipe, startDir, endDir);
        } else {
            Direction occupiedEnd = startOccupied ? startDir : endDir;
            boolean occupiedEndIsPipe = PipeBlock.isNeighborPipeToward(level, pos, occupiedEnd);

            if (occupiedEnd.getOpposite() == toward) {
                // 占用端与连接方向对向 → 直管贯通
                Direction.Axis axis = occupiedEnd.getAxis();
                Direction negDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
                boolean negIsOccupied = negDir == occupiedEnd;
                BlockState straightState = ModBlocks.PIPE_STRAIGHT.get()
                    .defaultBlockState()
                    .setValue(PipeBlock.WATERLOGGED, state.getValue(PipeBlock.WATERLOGGED))
                    .setValue(PipeBlock.AXIS, axis)
                    .setValue(PipeBlock.HAS_END_START, negIsOccupied ? !occupiedEndIsPipe : !towardIsPipe)
                    .setValue(PipeBlock.HAS_END_END, negIsOccupied ? !towardIsPipe : !occupiedEndIsPipe);
                level.setBlockAndUpdate(pos, straightState);
                return straightState;
            }

            // 弯管
            PipeBlock.CornerEnded corner = PipeBlock.CornerEnded.fromDirections(occupiedEnd, toward);
            boolean firstIsOccupied = corner.getFirstDirection() == occupiedEnd;
            BlockState cornerState = ModBlocks.PIPE_CORNER.get()
                .defaultBlockState()
                .setValue(PipeBlock.WATERLOGGED, state.getValue(PipeBlock.WATERLOGGED))
                .setValue(PipeBlock.CORNER_ENDED, corner)
                .setValue(PipeBlock.HAS_END_START, firstIsOccupied ? !occupiedEndIsPipe : !towardIsPipe)
                .setValue(PipeBlock.HAS_END_END, firstIsOccupied ? !towardIsPipe : !occupiedEndIsPipe);
            level.setBlockAndUpdate(pos, cornerState);
            return cornerState;
        }
    }

    /**
     * 将管道转为节点，保留两个已有方向的连接并添加新方向。
     */
    private static BlockState convertToNode(
        Level level,
        BlockPos pos,
        BlockState state,
        Direction toward,
        boolean towardIsPipe,
        Direction dir1,
        Direction dir2
    ) {
        BlockState nodeState = ModBlocks.PIPE_NODE.get()
            .defaultBlockState()
            .setValue(PipeBlock.WATERLOGGED, state.getValue(PipeBlock.WATERLOGGED));
        nodeState = nodeState.setValue(PipeBlock.getPropertyForDirection(dir1), PipeNodeBlock.evaluateNeighbor(level, pos, dir1));
        nodeState = nodeState.setValue(PipeBlock.getPropertyForDirection(dir2), PipeNodeBlock.evaluateNeighbor(level, pos, dir2));
        nodeState = nodeState.setValue(
            PipeBlock.getPropertyForDirection(toward),
            towardIsPipe ? PipeBlock.NodePipe.PIPE : PipeBlock.NodePipe.END
        );
        level.setBlockAndUpdate(pos, nodeState);
        return nodeState;
    }

    /**
     * 普通放置流程（同 BlockItem），放置新方块并播放声音、消耗物品。
     */
    public InteractionResult place(BlockPlaceContext context) {
        if (!context.canPlace()) {
            return InteractionResult.FAIL;
        }
        BlockState blockstate = this.getPlacementState(context);
        if (blockstate == null) {
            return InteractionResult.FAIL;
        }
        if (!this.placeBlock(context, blockstate)) {
            return InteractionResult.FAIL;
        }

        BlockPos blockpos = context.getClickedPos();
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack itemstack = context.getItemInHand();
        BlockState blockstate1 = level.getBlockState(blockpos);
        if (blockstate1.is(blockstate.getBlock())) {
            blockstate1 = this.updateBlockStateFromTag(blockpos, level, itemstack, blockstate1);
            this.updateCustomBlockEntityTag(blockpos, level, player, itemstack, blockstate1);
            PipeBlockItem.updateBlockEntityComponents(level, blockpos, itemstack);
            blockstate1.getBlock().setPlacedBy(level, blockpos, blockstate1, player, itemstack);
            if (player instanceof ServerPlayer) {
                CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, blockpos, itemstack);
            }
        }
        SoundType soundtype = blockstate1.getSoundType(level, blockpos, context.getPlayer());
        level.playSound(
            player,
            blockpos,
            this.getPlaceSound(blockstate1, level, blockpos, context.getPlayer()),
            SoundSource.BLOCKS,
            (soundtype.getVolume() + 1.0F) / 2.0F,
            soundtype.getPitch() * 0.8F
        );
        level.gameEvent(GameEvent.BLOCK_PLACE, blockpos, GameEvent.Context.of(player, blockstate1));
        itemstack.consume(1, player);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * 计算放置时的新方块状态。
     *
     * <p>优先级：
     * <ol>
     *   <li>Shift / 非管道&非IFluidHandler → 视线方向直管（两端端头）</li>
     *   <li>点击弯管 → {@link #handleCornerPlacement} 特殊交互</li>
     *   <li>点击管道/IFluidHandler → 沿被点击面轴线直管，端头按邻居类型开关</li>
     * </ol>
     */
    @Nullable
    protected BlockState getPlacementState(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos placePos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        Player player = context.getPlayer();

        BlockPos targetPos = placePos.relative(clickedFace.getOpposite());
        BlockState targetState = level.getBlockState(targetPos);
        Block targetBlock = targetState.getBlock();

        boolean shiftDown = player != null && player.isShiftKeyDown();
        boolean clickedOnPipe = targetBlock instanceof PipeBlock;
        boolean clickedOnFluidHandler = PipeBlock.isFluidHandler(level, targetPos);

        // 视线模式
        if (shiftDown || (!clickedOnPipe && !clickedOnFluidHandler)) {
            Direction.Axis axis = getLookAxis(player);
            return makeStraightState(level, placePos, axis, true, true);
        }

        // 弯管交互
        if (targetBlock instanceof PipeCornerBlock) {
            return handleCornerPlacement(level, placePos, clickedFace, targetPos, targetState, player);
        }

        // 连接模式
        Direction.Axis axis = clickedFace.getAxis();
        Direction startDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
        Direction endDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.POSITIVE);
        Direction towardTarget = clickedFace.getOpposite();

        boolean startIsPipe;
        boolean endIsPipe;
        if (towardTarget == startDir) {
            startIsPipe = clickedOnPipe;
            endIsPipe = level.getBlockState(placePos.relative(endDir)).getBlock() instanceof PipeBlock;
        } else {
            endIsPipe = clickedOnPipe;
            startIsPipe = level.getBlockState(placePos.relative(startDir)).getBlock() instanceof PipeBlock;
        }

        return makeStraightState(level, placePos, axis, !startIsPipe, !endIsPipe);
    }

    /**
     * 构造直管方块状态
     */
    private BlockState makeStraightState(Level level, BlockPos pos, Direction.Axis axis, boolean hasEndStart, boolean hasEndEnd) {
        return ModBlocks.PIPE_STRAIGHT.get()
            .defaultBlockState()
            .setValue(PipeBlock.AXIS, axis)
            .setValue(PipeBlock.HAS_END_START, hasEndStart)
            .setValue(PipeBlock.HAS_END_END, hasEndEnd)
            .setValue(PipeBlock.WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
    }

    /**
     * 弯管放置交互逻辑（点击弯管表面时触发）。
     *
     * <p>根据弯管两端占用状态和点击面决定操作：
     * <ul>
     *   <li>两端都忙 → 弯管转节点（额外一侧连接）</li>
     *   <li>两端都闲 或 点击面对向忙端 → 弯管转直管</li>
     *   <li>方向不匹配 → 旋转弯管（忙端保持 + 新方向替换闲端）</li>
     *   <li>方向匹配 → 不修改弯管（neighborChanged 自会开端头）</li>
     * </ul>
     *
     * <p>弯管修改仅在服务端执行。
     *
     * @return 新管道的方块状态（始终是直管）
     */
    private BlockState handleCornerPlacement(
        Level level,
        BlockPos placePos,
        Direction clickedFace,
        BlockPos cornerPos,
        BlockState cornerState,
        @Nullable Player player
    ) {
        PipeBlock.CornerEnded corner = cornerState.getValue(PipeBlock.CORNER_ENDED);
        Direction first = corner.getFirstDirection();
        Direction second = corner.getSecondDirection();

        boolean firstOccupied = PipeBlock.isNeighborOccupied(level, cornerPos, first);
        boolean secondOccupied = PipeBlock.isNeighborOccupied(level, cornerPos, second);
        boolean bothFree = !firstOccupied && !secondOccupied;
        boolean bothOccupied = firstOccupied && secondOccupied;
        boolean directionMatches = corner.containsDirection(clickedFace);
        boolean oppositeOccupied = (firstOccupied && clickedFace == first.getOpposite())
                                   || (secondOccupied && clickedFace == second.getOpposite());

        // 弯管修改仅在服务端
        if (!level.isClientSide()) {
            if (bothOccupied) {
                // 两端都忙 → 转节点
                BlockState nodeState = ModBlocks.PIPE_NODE.get()
                    .defaultBlockState()
                    .setValue(PipeBlock.WATERLOGGED, cornerState.getValue(PipeBlock.WATERLOGGED));
                nodeState = nodeState.setValue(
                    PipeBlock.getPropertyForDirection(first),
                    PipeNodeBlock.evaluateNeighbor(level, cornerPos, first)
                );
                nodeState = nodeState.setValue(
                    PipeBlock.getPropertyForDirection(second),
                    PipeNodeBlock.evaluateNeighbor(level, cornerPos, second)
                );
                nodeState = nodeState.setValue(PipeBlock.getPropertyForDirection(clickedFace), PipeBlock.NodePipe.PIPE);
                level.setBlockAndUpdate(cornerPos, nodeState);
                playPlaceSound(level, cornerPos, nodeState, player);
            } else if (bothFree || oppositeOccupied) {
                // 都闲 或 点击面对向忙端 → 转直管
                Direction.Axis axis = clickedFace.getAxis();
                Direction startDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
                Direction endDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.POSITIVE);

                boolean startIsPipe = PipeBlock.isNeighborPipeToward(level, cornerPos, startDir);
                boolean endIsPipe = PipeBlock.isNeighborPipeToward(level, cornerPos, endDir);
                if (clickedFace == startDir) {
                    startIsPipe = true;
                } else if (clickedFace == endDir) {
                    endIsPipe = true;
                }

                BlockState straightState = ModBlocks.PIPE_STRAIGHT.get()
                    .defaultBlockState()
                    .setValue(PipeBlock.AXIS, axis)
                    .setValue(PipeBlock.HAS_END_START, !startIsPipe)
                    .setValue(PipeBlock.HAS_END_END, !endIsPipe)
                    .setValue(PipeBlock.WATERLOGGED, cornerState.getValue(PipeBlock.WATERLOGGED));
                level.setBlockAndUpdate(cornerPos, straightState);
                playPlaceSound(level, cornerPos, straightState, player);
            } else if (!directionMatches) {
                // 方向不匹配 → 旋转弯管（保留忙端，闲端改为新方向）
                Direction occupiedEnd = firstOccupied ? first : second;
                PipeBlock.CornerEnded newCorner = PipeBlock.CornerEnded.fromDirections(occupiedEnd, clickedFace);
                boolean occupiedEndIsPipe = PipeBlock.isNeighborPipeToward(level, cornerPos, occupiedEnd);
                boolean firstIsOccupied = newCorner.getFirstDirection() == occupiedEnd;

                BlockState newCornerState = ModBlocks.PIPE_CORNER.get()
                    .defaultBlockState()
                    .setValue(PipeBlock.WATERLOGGED, cornerState.getValue(PipeBlock.WATERLOGGED))
                    .setValue(PipeBlock.CORNER_ENDED, newCorner)
                    .setValue(PipeBlock.HAS_END_START, firstIsOccupied && !occupiedEndIsPipe)
                    .setValue(PipeBlock.HAS_END_END, !firstIsOccupied && !occupiedEndIsPipe);
                level.setBlockAndUpdate(cornerPos, newCornerState);
                playPlaceSound(level, cornerPos, newCornerState, player);
            }
        }

        // 计算新管道状态（两端的端头根据邻居类型决定）
        Direction.Axis axis = clickedFace.getAxis();
        Direction startDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
        Direction endDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.POSITIVE);
        Direction towardCorner = clickedFace.getOpposite();

        boolean startIsPipe = towardCorner == startDir;
        boolean endIsPipe = towardCorner == endDir;
        if (startIsPipe) {
            endIsPipe = level.getBlockState(placePos.relative(endDir)).getBlock() instanceof PipeBlock;
        } else {
            startIsPipe = level.getBlockState(placePos.relative(startDir)).getBlock() instanceof PipeBlock;
        }

        return makeStraightState(level, placePos, axis, !startIsPipe, !endIsPipe);
    }

    /**
     * 检查管道在指定方向是否已有开放连接（无端头）。
     * 与 {@link PipeBlock#hasConnectionToward} 的区别是此方法考虑端头状态。
     */
    private static boolean hasOpenConnectionToward(BlockState state, Direction toward) {
        if (state.getBlock() instanceof PipeStraightBlock) {
            Direction.Axis axis = state.getValue(PipeBlock.AXIS);
            if (toward.getAxis() != axis) {
                return false;
            }
            Direction startDir = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
            if (toward == startDir) {
                return !state.getValue(PipeBlock.HAS_END_START);
            }
            return !state.getValue(PipeBlock.HAS_END_END);
        }
        if (state.getBlock() instanceof PipeCornerBlock) {
            PipeBlock.CornerEnded corner = state.getValue(PipeBlock.CORNER_ENDED);
            if (!corner.containsDirection(toward)) {
                return false;
            }
            if (toward == corner.getFirstDirection()) {
                return !state.getValue(PipeBlock.HAS_END_START);
            }
            return !state.getValue(PipeBlock.HAS_END_END);
        }
        if (state.getBlock() instanceof PipeNodeBlock) {
            return state.getValue(PipeBlock.getPropertyForDirection(toward)) != PipeBlock.NodePipe.NONE;
        }
        return false;
    }

    /**
     * 播放方块放置音效和 GameEvent（用于修改已有方块时）
     */
    private static void playPlaceSound(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        SoundType soundtype = state.getSoundType(level, pos, player);
        level.playSound(
            player,
            pos,
            soundtype.getPlaceSound(),
            SoundSource.BLOCKS,
            (soundtype.getVolume() + 1.0F) / 2.0F,
            soundtype.getPitch() * 0.8F
        );
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
    }

    /**
     * 获取玩家视线方向对应的轴向
     */
    private static Direction.Axis getLookAxis(@Nullable Player player) {
        if (player == null) {
            return Direction.Axis.Y;
        }
        Vec3 lookVec = player.getViewVector(1.0f);
        return Direction.getNearest(lookVec.x, lookVec.y, lookVec.z).getAxis();
    }

    // ---- 标准 BlockItem 方法 ----

    public boolean canPlace(BlockPlaceContext context, BlockState state) {
        Player player = context.getPlayer();
        CollisionContext collisioncontext = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        return (!this.mustSurvive() || state.canSurvive(context.getLevel(), context.getClickedPos())) && context.getLevel()
            .isUnobstructed(state, context.getClickedPos(), collisioncontext);
    }

    protected boolean mustSurvive() {
        return true;
    }

    protected SoundEvent getPlaceSound(BlockState blockState, Level world, BlockPos pos, @Nullable Player entity) {
        return blockState.getSoundType(world, pos, entity).getPlaceSound();
    }

    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        return context.getLevel().setBlock(context.getClickedPos(), state, 11);
    }

    @SuppressWarnings({"UnusedReturnValue", "unused"})
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
        return PipeBlockItem.updateCustomBlockEntityTag(level, player, pos, stack);
    }

    public static boolean updateCustomBlockEntityTag(Level level, @Nullable Player player, BlockPos pos, ItemStack stack) {
        MinecraftServer minecraftserver = level.getServer();
        if (minecraftserver == null) {
            return false;
        }
        CustomData customdata = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (customdata.isEmpty()) {
            return false;
        }
        BlockEntity blockentity = level.getBlockEntity(pos);
        if (blockentity == null) {
            return false;
        }
        if (level.isClientSide() || !blockentity.onlyOpCanSetNbt() || player != null && player.canUseGameMasterBlocks()) {
            return customdata.loadInto(blockentity, level.registryAccess());
        }
        return false;
    }

    public BlockState updateBlockStateFromTag(BlockPos pos, Level level, ItemStack stack, BlockState state) {
        BlockItemStateProperties blockitemstateproperties = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        if (blockitemstateproperties.isEmpty()) {
            return state;
        }
        BlockState blockstate = blockitemstateproperties.apply(state);
        if (blockstate != state) {
            level.setBlock(pos, blockstate, 2);
        }
        return blockstate;
    }

    public static void updateBlockEntityComponents(Level level, BlockPos poa, ItemStack stack) {
        BlockEntity blockentity = level.getBlockEntity(poa);
        if (blockentity == null) {
            return;
        }
        blockentity.applyComponentsFromItemStack(stack);
        blockentity.setChanged();
    }
}
