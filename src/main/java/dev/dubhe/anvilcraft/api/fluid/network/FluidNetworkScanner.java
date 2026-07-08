package dev.dubhe.anvilcraft.api.fluid.network;

import dev.dubhe.anvilcraft.block.entity.fluid.AbstractPipeBlockEntity;
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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 从一个种子管道位置出发，flood-fill 出整张流体管道网络：
 * <ul>
 *   <li>遍历所有连通的管道部件（直管/弯管/节点/工作中的泵/控制阀/止逆阀），记录势场与邻接图；</li>
 *   <li>泵改变势场（跨泵输出侧 +10 / 输入侧 -10），控制阀是<b>门控透传</b>节点（不改势场），
 *       止逆阀是<b>被动二极管</b>（不改势场，仅允许单向通过，红石反向）；</li>
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

    /**
     * 判断一个方块是否为管道部件（直管/弯管/节点/泵/控制阀）。
     */
    public static boolean isPipePart(BlockState state) {
        return state.getBlock() instanceof PipeBlock
               || state.getBlock() instanceof PumpBlock
               || state.getBlock() instanceof ControlValveBlock;
    }

    /**
     * 判断某位置是否为流体容器（提供 ResourceHandler<FluidResource> 且非管道部件）。供管理器剔除失效容器用。
     */
    public static boolean isContainer(Level level, BlockPos pos) {
        return level.getCapability(Capabilities.Fluid.BLOCK, pos, null) != null && !isPipePart(level.getBlockState(pos));
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

    /**
     * 泵始终作为可连接的二极管接入网络（无论是否通电）；连接与否只看连接面朝向。
     * 是否提供扬程势场另由 {@link #pumpHalfLift} 决定。
     */
    private static boolean isConnectablePump(BlockState state, Direction faceToPump) {
        return state.getBlock() instanceof PumpBlock && PumpBlock.isConnectableFace(state, faceToPump);
    }

    /**
     * 泵当前提供的单侧扬程势场：通电工作时为 {@value #PUMP_HALF_LIFT}，断电/过载/红石关闭时为 0
     * （此时泵仍是二极管单向连通，只是不主动提供扬程）。
     */
    private static int pumpHalfLift(Level level, BlockPos pumpPos) {
        return level.getBlockEntity(pumpPos) instanceof PumpBlockEntity pbe && pbe.canPump() ? PUMP_HALF_LIFT : 0;
    }

    private static ResourceHandler<FluidResource> containerHandler(Level level, BlockPos pos, Direction sideToPipe) {
        if (isPipePart(level.getBlockState(pos))) {
            return null;
        }
        return level.getCapability(Capabilities.Fluid.BLOCK, pos, sideToPipe);
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
        Map<BlockPos, Direction> diodes = new HashMap<>();
        Map<BlockPos, Map<Direction, Direction>> faceFlow = new HashMap<>();
        List<FluidEndpoint> endpoints = new ArrayList<>();
        Map<ResourceHandler<FluidResource>, Boolean> seenHandlers = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        potential.put(seed, 0);
        queue.add(seed);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int phi = potential.get(pos);
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof ControlValveBlock && level.getBlockEntity(pos) instanceof ControlValveBlockEntity valveBe) {
                valves.putIfAbsent(pos, new ValveState(valveBe));
                expandAxial(
                    level,
                    pos,
                    state.getValue(ControlValveBlock.AXIS),
                    phi,
                    false,
                    potential,
                    adjacency,
                    queue,
                    endpoints,
                    seenHandlers
                );
            } else if (state.getBlock() instanceof PumpBlock) {
                // 记录泵的进液侧（二极管：仅允许 进液侧→另一侧 通过流体）
                diodes.put(pos.immutable(), state.getValue(PumpBlock.ORIENTATION).getDirection());
                expandPump(level, pos, state, phi, potential, adjacency, queue, endpoints, seenHandlers);
            } else if (state.getBlock() instanceof PipeBlock) {
                // 管道面止逆阀（HAS_CHECK_VALVE 属性需后续添加到 PipeBlock）
                // TODO: re-enable when PipeBlock gets HAS_CHECK_VALVE property
                if (state.getValue(PipeBlock.HAS_CHECK_VALVE)
                    && level.getBlockEntity(pos) instanceof AbstractPipeBlockEntity pipe) {
                    Map<Direction, Direction> flows = pipe.effectiveFlows();
                    if (!flows.isEmpty()) {
                        faceFlow.put(pos.immutable(), new EnumMap<>(flows));
                    }
                }
                expandPipe(level, pos, state, phi, potential, adjacency, queue, endpoints, seenHandlers);
            }
        }

        return new FluidPipeNetwork(level, potential.keySet(), adjacency, valves, diodes, faceFlow, endpoints);
    }

    /**
     * 记录一条 part↔part 无向邻接边。
     */
    private static void link(Map<BlockPos, List<BlockPos>> adjacency, BlockPos a, BlockPos b) {
        adjacency.computeIfAbsent(a, _ -> new ArrayList<>()).add(b.immutable());
        adjacency.computeIfAbsent(b, _ -> new ArrayList<>()).add(a.immutable());
    }

    /**
     * 展开直管/弯管/节点的所有连接方向。
     */
    private static void expandPipe(
        Level level,
        BlockPos pos,
        BlockState state,
        int phi,
        Map<BlockPos, Integer> potential,
        Map<BlockPos, List<BlockPos>> adjacency,
        Deque<BlockPos> queue,
        List<FluidEndpoint> endpoints,
        Map<ResourceHandler<FluidResource>, Boolean> seenHandlers
    ) {
        for (Direction dir : Direction.values()) {
            if (!hasAnyConnectionToward(state, dir)) {
                continue;
            }
            visitNeighbor(level, pos, dir, phi, potential, adjacency, queue, endpoints, seenHandlers);
        }
    }

    /**
     * 展开泵的两个轴端（通电时带 ±扬程势场，断电时 ±0 但仍连通）。
     */
    private static void expandPump(
        Level level,
        BlockPos pos,
        BlockState state,
        int phi,
        Map<BlockPos, Integer> potential,
        Map<BlockPos, List<BlockPos>> adjacency,
        Deque<BlockPos> queue,
        List<FluidEndpoint> endpoints,
        Map<ResourceHandler<FluidResource>, Boolean> seenHandlers
    ) {
        Direction outputDir = state.getValue(PumpBlock.ORIENTATION).getDirection();
        int lift = pumpHalfLift(level, pos);
        for (Direction side : new Direction[]{
            outputDir,
            outputDir.getOpposite()
        }) {
            int neighborPhi = phi + (side == outputDir ? lift : -lift);
            visitNeighborWithPhi(level, pos, side, neighborPhi, potential, adjacency, queue, endpoints, seenHandlers);
        }
    }

    /**
     * 展开控制阀（或任意沿单轴连接的部件）的两个轴端；不改势场。
     */
    private static void expandAxial(
        Level level,
        BlockPos pos,
        Direction.Axis axis,
        int phi,
        boolean unusedFlag,
        Map<BlockPos, Integer> potential,
        Map<BlockPos, List<BlockPos>> adjacency,
        Deque<BlockPos> queue,
        List<FluidEndpoint> endpoints,
        Map<ResourceHandler<FluidResource>, Boolean> seenHandlers
    ) {
        for (Direction side : new Direction[]{
            Direction.get(Direction.AxisDirection.POSITIVE, axis),
            Direction.get(Direction.AxisDirection.NEGATIVE, axis)
        }) {
            visitNeighbor(level, pos, side, phi, potential, adjacency, queue, endpoints, seenHandlers);
        }
    }

    /**
     * 邻居访问（同势场）。
     */
    private static void visitNeighbor(
        Level level,
        BlockPos pos,
        Direction dir,
        int phi,
        Map<BlockPos, Integer> potential,
        Map<BlockPos, List<BlockPos>> adjacency,
        Deque<BlockPos> queue,
        List<FluidEndpoint> endpoints,
        Map<ResourceHandler<FluidResource>, Boolean> seenHandlers
    ) {
        visitNeighborWithPhi(level, pos, dir, phi, potential, adjacency, queue, endpoints, seenHandlers);
    }

    /**
     * 邻居访问（邻居势场由调用方给出，用于泵）。
     */
    private static void visitNeighborWithPhi(
        Level level,
        BlockPos pos,
        Direction dir,
        int neighborPhi,
        Map<BlockPos, Integer> potential,
        Map<BlockPos, List<BlockPos>> adjacency,
        Deque<BlockPos> queue,
        List<FluidEndpoint> endpoints,
        Map<ResourceHandler<FluidResource>, Boolean> seenHandlers
    ) {
        BlockPos neighborPos = pos.relative(dir);
        if (!level.isLoaded(neighborPos)) {
            return;
        }
        BlockState neighborState = level.getBlockState(neighborPos);
        Direction faceBack = dir.getOpposite();

        // 可连接泵（无论通/断电）→ 穿过（二极管；扬程由 enqueuePump 按通电状态决定）
        if (isConnectablePump(neighborState, faceBack)) {
            link(adjacency, pos, neighborPos);
            enqueuePump(level, neighborPos, neighborState, pos, potential, queue);
            return;
        }
        // 控制阀：连接面正对本部件 → 门控透传
        if (neighborState.getBlock() instanceof ControlValveBlock && ControlValveBlock.isConnectableFace(neighborState, faceBack)) {
            link(adjacency, pos, neighborPos);
            enqueuePart(neighborPos, neighborPhi, potential, queue);
            return;
        }
        // 对准本部件的另一管道 → 同势场（用 hasAnyConnectionToward：节点认 PIPE+END，
        // 否则节点朝泵/容器的 END 方向会被漏读，导致泵紧邻节点时抽不到液体）
        if (neighborState.getBlock() instanceof PipeBlock && hasAnyConnectionToward(neighborState, faceBack)) {
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
        Level level,
        BlockPos pumpPos,
        BlockState pumpState,
        BlockPos fromPos,
        Map<BlockPos, Integer> potential,
        Deque<BlockPos> queue
    ) {
        if (potential.containsKey(pumpPos)) {
            return;
        }
        Direction outputDir = pumpState.getValue(PumpBlock.ORIENTATION).getDirection();
        int fromPhi = potential.get(fromPos);
        int lift = pumpHalfLift(level, pumpPos);
        int pumpPhi;
        if (fromPos.equals(pumpPos.relative(outputDir))) {
            pumpPhi = fromPhi - lift;      // fromPos 在输出侧：fromPhi = pumpPhi + lift
        } else if (fromPos.equals(pumpPos.relative(outputDir.getOpposite()))) {
            pumpPhi = fromPhi + lift;      // fromPos 在输入侧：fromPhi = pumpPhi - lift
        } else {
            return;
        }
        potential.put(pumpPos.immutable(), pumpPhi);
        queue.add(pumpPos.immutable());
    }

    private static void addEndpointIfContainer(
        Level level,
        BlockPos containerPos,
        Direction sideToPipe,
        int phi,
        BlockPos attachPipePos,
        List<FluidEndpoint> endpoints,
        Map<ResourceHandler<FluidResource>, Boolean> seenHandlers
    ) {
        if (!level.isLoaded(containerPos)) {
            return;
        }
        ResourceHandler<FluidResource> handler = containerHandler(level, containerPos, sideToPipe);
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
