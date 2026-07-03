package dev.dubhe.anvilcraft.api.fluid.network;

import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.PumpBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.ControlValveBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeNodeBlock;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 从一个种子管道位置出发，flood-fill 出整张流体管道网络：
 * <ul>
 *   <li>遍历所有连通的管道部件（直管/弯管/节点/工作中的泵/控制阀），记录势场与邻接图；</li>
 *   <li>泵改变势场（跨泵输出侧 +10 / 输入侧 -10），控制阀是<b>门控透传</b>节点（不改势场）；</li>
 *   <li>发现端点容器：管道部件的 END/PIPE 连接朝向真实容器时记为端点，
 *       等效高度 = 容器 Y + 该管道部件的势场偏移。</li>
 * </ul>
 *
 * <p>不工作的泵阻断连接（视为闭合阀门），面向空气的端头不产生端点（断路）。
 */
public final class FluidNetworkScanner {
    private static final int PUMP_HALF_LIFT = PumpBlockEntity.PUMP_HEADLIFT;

    private FluidNetworkScanner() {
    }

    /** 判断一个方块是否为管道部件（直管/弯管/节点/泵/控制阀）。 */
    public static boolean isPipePart(BlockState state) {
        return state.getBlock() instanceof PipeBlock
            || state.getBlock() instanceof PumpBlock
            || state.getBlock() instanceof ControlValveBlock;
    }

    /** 判断某位置是否为流体容器（提供 IFluidHandler 且非管道部件）。供管理器剔除失效容器用。 */
    public static boolean isContainer(Level level, BlockPos pos) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null) != null
            && !isPipePart(level.getBlockState(pos));
    }

    /**
     * 管道部件在某方向上是否有<b>任何</b>连接（PIPE 或 END），用于网络遍历。
     * 节点需认 END（连容器方向），否则容器无法接入网络。
     */
    private static boolean hasAnyConnectionToward(BlockState state, Direction toward) {
        if (state.getBlock() instanceof PipeNodeBlock) {
            return state.getValue(PipeBlock.getPropertyForDirection(toward)) != PipeBlock.NodePipe.NONE;
        }
        return PipeBlock.hasConnectionToward(state, toward);
    }

    private static boolean isWorkingConnectablePump(Level level, BlockPos pos, BlockState state, Direction faceToPump) {
        if (!(state.getBlock() instanceof PumpBlock)) {
            return false;
        }
        if (!PumpBlock.isConnectableFace(state, faceToPump)) {
            return false;
        }
        return level.getBlockEntity(pos) instanceof PumpBlockEntity pbe && pbe.canPump();
    }

    private static IFluidHandler containerHandler(Level level, BlockPos pos, Direction sideToPipe) {
        if (isPipePart(level.getBlockState(pos))) {
            return null;
        }
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, sideToPipe);
    }

    /**
     * 从种子管道位置 flood-fill 出整张网络（含邻接图与阀门快照）。
     *
     * @return 网络对象；种子非管道部件时返回 {@code null}
     */
    public static FluidPipeNetwork scan(Level level, BlockPos seed) {
        if (!isPipePart(level.getBlockState(seed))) {
            return null;
        }

        Map<BlockPos, Integer> potential = new HashMap<>();
        Map<BlockPos, List<BlockPos>> adjacency = new HashMap<>();
        Map<BlockPos, ValveState> valves = new HashMap<>();
        Map<BlockPos, Direction> pumps = new HashMap<>();
        List<FluidEndpoint> endpoints = new ArrayList<>();
        Map<IFluidHandler, Boolean> seenHandlers = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        potential.put(seed, 0);
        queue.add(seed);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int phi = potential.get(pos);
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof ControlValveBlock
                && level.getBlockEntity(pos) instanceof ControlValveBlockEntity valveBe) {
                valves.putIfAbsent(pos, new ValveState(valveBe));
                expandAxial(level, pos, state.getValue(ControlValveBlock.AXIS), phi, false,
                    potential, adjacency, queue, endpoints, seenHandlers);
            } else if (state.getBlock() instanceof PumpBlock) {
                // 记录泵的输出方向（二极管：仅允许 输入侧→输出侧 通过流体）
                pumps.put(pos.immutable(), state.getValue(PumpBlock.ORIENTATION).getDirection());
                expandPump(level, pos, state, phi, potential, adjacency, queue, endpoints, seenHandlers);
            } else if (state.getBlock() instanceof PipeBlock) {
                expandPipe(level, pos, state, phi, potential, adjacency, queue, endpoints, seenHandlers);
            }
        }

        return new FluidPipeNetwork(level, potential.keySet(), adjacency, valves, pumps, endpoints);
    }

    /** 记录一条 part↔part 无向邻接边。 */
    private static void link(Map<BlockPos, List<BlockPos>> adjacency, BlockPos a, BlockPos b) {
        adjacency.computeIfAbsent(a, k -> new ArrayList<>()).add(b.immutable());
        adjacency.computeIfAbsent(b, k -> new ArrayList<>()).add(a.immutable());
    }

    /** 展开直管/弯管/节点的所有连接方向。 */
    private static void expandPipe(
        Level level, BlockPos pos, BlockState state, int phi,
        Map<BlockPos, Integer> potential, Map<BlockPos, List<BlockPos>> adjacency,
        Deque<BlockPos> queue, List<FluidEndpoint> endpoints, Map<IFluidHandler, Boolean> seenHandlers
    ) {
        for (Direction dir : Direction.values()) {
            if (!hasAnyConnectionToward(state, dir)) {
                continue;
            }
            visitNeighbor(level, pos, dir, phi, potential, adjacency, queue, endpoints, seenHandlers);
        }
    }

    /** 展开泵的两个轴端（带势场偏移）。 */
    private static void expandPump(
        Level level, BlockPos pos, BlockState state, int phi,
        Map<BlockPos, Integer> potential, Map<BlockPos, List<BlockPos>> adjacency,
        Deque<BlockPos> queue, List<FluidEndpoint> endpoints, Map<IFluidHandler, Boolean> seenHandlers
    ) {
        Direction outputDir = state.getValue(PumpBlock.ORIENTATION).getDirection();
        for (Direction side : new Direction[]{outputDir, outputDir.getOpposite()}) {
            int neighborPhi = phi + (side == outputDir ? PUMP_HALF_LIFT : -PUMP_HALF_LIFT);
            visitNeighborWithPhi(level, pos, side, neighborPhi, potential, adjacency, queue, endpoints, seenHandlers);
        }
    }

    /** 展开控制阀（或任意沿单轴连接的部件）的两个轴端；不改势场。 */
    private static void expandAxial(
        Level level, BlockPos pos, Direction.Axis axis, int phi, boolean unusedFlag,
        Map<BlockPos, Integer> potential, Map<BlockPos, List<BlockPos>> adjacency,
        Deque<BlockPos> queue, List<FluidEndpoint> endpoints, Map<IFluidHandler, Boolean> seenHandlers
    ) {
        for (Direction side : new Direction[]{
            Direction.get(Direction.AxisDirection.POSITIVE, axis),
            Direction.get(Direction.AxisDirection.NEGATIVE, axis)
        }) {
            visitNeighbor(level, pos, side, phi, potential, adjacency, queue, endpoints, seenHandlers);
        }
    }

    /** 邻居访问（同势场）。 */
    private static void visitNeighbor(
        Level level, BlockPos pos, Direction dir, int phi,
        Map<BlockPos, Integer> potential, Map<BlockPos, List<BlockPos>> adjacency,
        Deque<BlockPos> queue, List<FluidEndpoint> endpoints, Map<IFluidHandler, Boolean> seenHandlers
    ) {
        visitNeighborWithPhi(level, pos, dir, phi, potential, adjacency, queue, endpoints, seenHandlers);
    }

    /** 邻居访问（邻居势场由调用方给出，用于泵）。 */
    private static void visitNeighborWithPhi(
        Level level, BlockPos pos, Direction dir, int neighborPhi,
        Map<BlockPos, Integer> potential, Map<BlockPos, List<BlockPos>> adjacency,
        Deque<BlockPos> queue, List<FluidEndpoint> endpoints, Map<IFluidHandler, Boolean> seenHandlers
    ) {
        BlockPos neighborPos = pos.relative(dir);
        if (!level.isLoaded(neighborPos)) {
            return;
        }
        BlockState neighborState = level.getBlockState(neighborPos);
        Direction faceBack = dir.getOpposite();

        // 工作中的可连接泵 → 穿过
        if (isWorkingConnectablePump(level, neighborPos, neighborState, faceBack)) {
            link(adjacency, pos, neighborPos);
            enqueuePump(neighborPos, neighborState, pos, potential, queue);
            return;
        }
        // 控制阀：连接面正对本部件 → 门控透传
        if (neighborState.getBlock() instanceof ControlValveBlock
            && ControlValveBlock.isConnectableFace(neighborState, faceBack)) {
            link(adjacency, pos, neighborPos);
            enqueuePart(neighborPos, neighborPhi, potential, queue);
            return;
        }
        // 对准本部件的另一管道 → 同势场（用 hasAnyConnectionToward：节点认 PIPE+END，
        // 否则节点朝泵/容器的 END 方向会被漏读，导致泵紧邻节点时抽不到液体）
        if (neighborState.getBlock() instanceof PipeBlock
            && hasAnyConnectionToward(neighborState, faceBack)) {
            link(adjacency, pos, neighborPos);
            enqueuePart(neighborPos, neighborPhi, potential, queue);
            return;
        }
        // 容器 → 端点
        addEndpointIfContainer(level, neighborPos, faceBack, neighborPhi, pos, endpoints, seenHandlers);
    }

    private static void enqueuePart(BlockPos pos, int phi, Map<BlockPos, Integer> potential, Deque<BlockPos> queue) {
        if (potential.containsKey(pos)) {
            return;
        }
        potential.put(pos.immutable(), phi);
        queue.add(pos.immutable());
    }

    private static void enqueuePump(
        BlockPos pumpPos, BlockState pumpState, BlockPos fromPos,
        Map<BlockPos, Integer> potential, Deque<BlockPos> queue
    ) {
        if (potential.containsKey(pumpPos)) {
            return;
        }
        Direction outputDir = pumpState.getValue(PumpBlock.ORIENTATION).getDirection();
        int fromPhi = potential.get(fromPos);
        int pumpPhi;
        if (fromPos.equals(pumpPos.relative(outputDir))) {
            pumpPhi = fromPhi - PUMP_HALF_LIFT;      // fromPos 在输出侧：fromPhi = pumpPhi + 10
        } else if (fromPos.equals(pumpPos.relative(outputDir.getOpposite()))) {
            pumpPhi = fromPhi + PUMP_HALF_LIFT;      // fromPos 在输入侧：fromPhi = pumpPhi - 10
        } else {
            return;
        }
        potential.put(pumpPos.immutable(), pumpPhi);
        queue.add(pumpPos.immutable());
    }

    private static void addEndpointIfContainer(
        Level level, BlockPos containerPos, Direction sideToPipe, int phi, BlockPos attachPipePos,
        List<FluidEndpoint> endpoints, Map<IFluidHandler, Boolean> seenHandlers
    ) {
        if (!level.isLoaded(containerPos)) {
            return;
        }
        IFluidHandler handler = containerHandler(level, containerPos, sideToPipe);
        if (handler == null) {
            return;
        }
        if (seenHandlers.putIfAbsent(handler, Boolean.TRUE) != null) {
            return;
        }
        int effectiveHeight = containerPos.getY() + phi;
        endpoints.add(new FluidEndpoint(containerPos.immutable(), attachPipePos.immutable(), sideToPipe, handler, effectiveHeight));
    }
}
