package dev.dubhe.anvilcraft.api.fluid.network;

import dev.dubhe.anvilcraft.block.entity.fluid.GlassPipeBlockEntity;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 一个流体管道网络：由一组连通的管道部件（直管/弯管/节点/泵）及其发现的
 * 端点容器组成。管道部件本身不存流体，只把端点容器连成网络；网络每 tick
 * 执行一次全局重力分配。
 *
 * <h3>重力分配规则</h3>
 * <ul>
 *   <li>按等效高度从高到低取源容器；源只向<b>严格更低</b>的容器排液，同高之间不主动分配。</li>
 *   <li>目标按等效高度<b>升序分组</b>，从最低组开始填，<b>本组填满才溢到上一组</b></li>
 *   <li>每组流速按高度差<b>线性增长</b>：每格 {@value #HEIGHT_RATE} mB/tick，
 *       {@value #FULL_SPEED_HEIGHT} 格达上限 {@value #MAX_SPEED} mB/tick（见 {@link #speedForHeightDiff}）。</li>
 *   <li>组内对活跃目标做"基础均分 + 余量轮转"，使同高容器均匀进水。</li>
 *   <li>炼药锅仅在满锅时输出、空锅时输入，并以整锅为单位按组内优先顺序转移。</li>
 * </ul>
 *
 * <p>等效高度 = 容器 Y + 沿管道路径累计的泵势场偏移（见 {@link FluidNetworkScanner}）。
 */
public class FluidPipeNetwork {
    /** 每格高度差提供的流速（mB/tick） */
    public static final int HEIGHT_RATE = 50;
    /** 高度差达到 {@value #FULL_SPEED_HEIGHT} 格时的流速上限（mB/tick） */
    public static final int MAX_SPEED = 2000;
    /** 达到流速上限所需的高度差（格）= MAX_SPEED / HEIGHT_RATE = 40 */
    public static final int FULL_SPEED_HEIGHT = MAX_SPEED / HEIGHT_RATE;

    /**
     * 按高度差计算流速（线性增长）：
     * <ul>
     *   <li>高度差 ≤ 0 → 0</li>
     *   <li>高度差 1~{@value #FULL_SPEED_HEIGHT} 格 → {@code h × }{@value #HEIGHT_RATE} mB/tick（50~2000）</li>
     *   <li>{@value #FULL_SPEED_HEIGHT} 格及以上 → 上限 {@value #MAX_SPEED} mB/tick</li>
     * </ul>
     */
    public static int speedForHeightDiff(int heightDiff) {
        if (heightDiff <= 0) {
            return 0;
        }
        return Math.min(heightDiff * HEIGHT_RATE, MAX_SPEED);
    }

    private final Level level;
    @Getter
    private final Set<BlockPos> parts;
    private final Map<BlockPos, List<BlockPos>> adjacency;
    private final Map<BlockPos, ValveState> valves;
    /** 二极管部件（泵）位置 → 进液侧方向。流体只能从进液侧穿到另一侧，反向不通（无关高度差）。 */
    private final Map<BlockPos, Direction> diodes;
    /**
     * 管道面止逆阀：管道位置 → (装阀面 → 允许流出的世界方向)。流体只能沿该方向穿过此面，
     * 反向被阻断（无关高度差）。逐面约束天然覆盖直管/弯管/节点及朝容器的端点面。
     */
    private final Map<BlockPos, Map<Direction, Direction>> faceFlow;
    private final Set<BlockPos> glassPipePositions;
    private final List<FluidEndpoint> endpoints;
    private final List<FluidEndpoint> cauldronEndpoints;
    private final List<FluidEndpoint> entityEndpoints;
    private final Set<FluidEndpoint> disconnectedEntityEndpoints = new HashSet<>();
    private final boolean directionalConstraints;
    /** 端点按等效高度<b>降序</b>预排序（作为源的遍历顺序），构建时排一次，避免每 tick 重排。 */
    private final List<FluidEndpoint> sourcesByHeightDesc;
    private final Map<BlockPos, List<CachedReachability>> reachabilityCache = new HashMap<>();
    private final Set<BlockPos> triggeredSources = new HashSet<>();
    private long triggerGameTime = Long.MIN_VALUE;

    /** 本 tick 是否发生过流体转移（供管理器判定网络是否活跃，见 #4 降频）。 */
    private boolean transferredThisTick;
    @Getter
    private int idleTicks;

    /** 供管理器在每次 {@link #tick()} 后更新空闲计数。 */
    public void updateIdle() {
        if (transferredThisTick) {
            idleTicks = 0;
        } else {
            idleTicks++;
        }
    }

    public FluidPipeNetwork(
        Level level,
        Set<BlockPos> parts,
        Map<BlockPos, List<BlockPos>> adjacency,
        Map<BlockPos, ValveState> valves,
        Map<BlockPos, Direction> diodes,
        Map<BlockPos, Map<Direction, Direction>> faceFlow,
        Set<BlockPos> glassPipePositions,
        List<FluidEndpoint> endpoints
    ) {
        this.level = level;
        this.parts = parts;
        this.adjacency = adjacency;
        this.valves = valves;
        this.diodes = diodes;
        this.faceFlow = faceFlow;
        this.glassPipePositions = glassPipePositions;
        this.endpoints = endpoints;
        this.directionalConstraints = !valves.isEmpty() || !diodes.isEmpty() || !faceFlow.isEmpty();
        this.cauldronEndpoints = new ArrayList<>();
        this.entityEndpoints = new ArrayList<>();
        for (FluidEndpoint endpoint : endpoints) {
            if (endpoint.cauldron()) {
                this.cauldronEndpoints.add(endpoint);
            }
            if (endpoint.entity() != null) {
                this.entityEndpoints.add(endpoint);
            }
        }
        // 预排序一次（缓存网络下每 tick 复用）
        this.sourcesByHeightDesc = new ArrayList<>(endpoints);
        this.sourcesByHeightDesc.sort(Comparator.comparingInt(FluidEndpoint::effectiveHeight).reversed());
    }

    /**
     * 每 tick 的全局重力分配。
     *
     * <p>对每个持有流体的源端点，收集所有等效高度严格更低、且能接受该流体的目标，
     * 按目标等效高度升序分组，从最低组开始逐组填充（本组填满才处理更高组）。
     */
    public void tick() {
        this.transferredThisTick = false;
        if (endpoints.size() < 2) {
            return;
        }
        if (!canTickEndpoints()) {
            return;
        }
        reachabilityCache.clear();
        // 每 tick 分配前重置各阀门预算（实时读取阀门当前流速设置）
        if (!valves.isEmpty()) {
            for (ValveState valve : valves.values()) {
                valve.resetBudget();
            }
        }
        // 源已按等效高度降序预排序：高处先流，一 tick 内可级联下泄
        for (FluidEndpoint source : sourcesByHeightDesc) {
            if (!isEndpointConnected(source)) {
                continue;
            }
            distributeFromSource(source);
        }
    }

    /**
     * 供外部主动泵送设备（如锻星砧流体接口）使用：把一个外部源容器当作等效高度为
     * {@code sourceEffectiveHeight} 的源，向本网络中更低的端点分配流体。
     *
     * @param srcHandler            外部源的流体处理器
     * @param srcPos                外部源的位置（用于就近排序）
     * @param entryPipePos          源接入网络的那根管道位置（可达 BFS 的起点，须在本网络内）
     * @param sourceEffectiveHeight 外部源的等效高度（通常 = 设备 Y + 扬程）
     */
    public void pushFromExternalSource(
        IFluidHandler srcHandler, BlockPos srcPos, BlockPos entryPipePos, int sourceEffectiveHeight
    ) {
        if (endpoints.isEmpty() || !canTickEndpoints()) {
            return;
        }
        reachabilityCache.clear();
        distributeFromSource(new FluidEndpoint(
            srcPos, entryPipePos, null, srcHandler, sourceEffectiveHeight, false, null));
    }

    /** 从单个源端点向所有更低的端点分配其持有的流体。 */
    private void distributeFromSource(FluidEndpoint source) {
        // 源接管口朝本源容器那一面若装止逆阀，其允许方向必须朝网络（背离容器），否则本源无法向网络排液
        if (!canDrainFromEndpoint(source)) {
            return;
        }
        IFluidHandler srcHandler = source.handler();
        // 逐个 tank 处理源中的流体（多数容器仅一个 tank）
        for (int tankIdx = 0; tankIdx < srcHandler.getTanks(); tankIdx++) {
            FluidStack stored = srcHandler.getFluidInTank(tankIdx);
            if (stored.isEmpty()) {
                continue;
            }
            // 从源出发做方向感知可达 BFS：二极管（泵）只能正向穿过、阀门按过滤放行、面止逆阀只能沿允许方向穿过；得到 可达接管口 → 路径上的阀门列表。
            Reachability reach = directionalConstraints
                ? computeReachableCached(source.fromPipePos(), stored)
                : null;
            Map<BlockPos, List<ValveState>> pathValves = reach == null ? Map.of() : reach.pathValves();
            // 按等效高度升序分组，仅收集严格更低、接受该流体、且可达的目标
            TreeMap<Integer, List<FluidEndpoint>> byHeight = collectTargetsByHeight(source, tankIdx, stored, reach);
            if (byHeight.isEmpty()) {
                continue;
            }
            if (hasHigherPrioritySource(source, stored, byHeight)) {
                continue;
            }
            // 从最低组开始填，本组填满才溢流到更高组
            for (var entry : byHeight.entrySet()) {
                int groupHeight = entry.getKey();
                List<FluidEndpoint> group = entry.getValue();
                int heightDiff = source.effectiveHeight() - groupHeight;
                int groupSpeed = speedForHeightDiff(heightDiff);
                boolean groupFull = fillGroup(source, tankIdx, group, groupSpeed, pathValves, reach);
                // 源已流尽 → 停止
                if (srcHandler.getFluidInTank(tankIdx).isEmpty()) {
                    break;
                }
                // 本组未被填满（受流速/预算限制，或本就有余量）→ 不向更高组溢流，未流出的部分留在源中等待下 1 tick
                if (!groupFull) {
                    break;
                }
                // 本组已全满 → 继续向更高组溢流
            }
        }
    }

    /**
     * 在同高度组内<b>公平均分</b>本 tick 的流速预算：不论高度差大小、预算大小，同高容器都尽量平均进水。
     *
     * <p>每一轮对当前活跃(仍能接受且阀门放行)目标做"基础均分 + 余量轮转"：
     * {@code base = 预算/活跃数}，每个目标得 {@code base}；不整除的余量按<b>轮转起点</b>
     * （{@code gameTime % 组大小}，逐 tick 转动）依次多给 1mB。预算小于目标数时 base=0，
     * 仅靠余量轮转——于是每 tick 喂到不同容器，长期仍然均摊（解决阀门限流为 1~2 时依次灌满的问题）。
     * 就近排序仅决定轮转的基准顺序。
     *
     * <p>炼药锅不参与均分：每次只对优先级最高的可用目标执行一笔整锅事务。
     *
     * @return 本组是否已被<b>按容量填满</b>（与阀门限流无关）——用于决定是否向更高组溢流
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private boolean fillGroup(
        FluidEndpoint source, int tankIdx, List<FluidEndpoint> group, int groupSpeed,
        Map<BlockPos, List<ValveState>> pathValves, @Nullable Reachability reach
    ) {
        // 组内按到源的"就近"排序：|Σxz差| 升序，再比 |x差|、|z差|
        BlockPos src = source.containerPos();
        group.sort(Comparator
            .comparingInt((FluidEndpoint e) -> Math.abs(sumXZ(e.containerPos()) - sumXZ(src)))
            .thenComparingInt(e -> Math.abs(e.containerPos().getX() - src.getX()))
            .thenComparingInt(e -> Math.abs(e.containerPos().getZ() - src.getZ())));

        List<FluidEndpoint> allTargets = group;
        IFluidHandler srcHandler = source.handler();
        if (isCauldron(source)) {
            fillFromFullCauldron(source, tankIdx, group, pathValves, reach);
            return isGroupCapacityFull(group);
        }
        if (fillFirstWholeCauldronTarget(source, tankIdx, group, pathValves, reach)) {
            return isGroupCapacityFull(group);
        }
        List<FluidEndpoint> regularTargets = group.stream().filter(target -> !isCauldron(target)).toList();
        if (regularTargets.isEmpty()) {
            return false;
        }
        group = regularTargets;
        int budget = groupSpeed;

        while (budget > 0) {
            FluidStack stored = srcHandler.getFluidInTank(tankIdx);
            if (stored.isEmpty()) {
                break; // 源已空
            }
            // 重算活跃目标：仍能按容量接受、且路径阀门有剩余预算
            List<ActiveTarget> active = new ArrayList<>();
            for (FluidEndpoint target : group) {
                if (minValveRemaining(pathValves.get(target.fromPipePos())) <= 0) {
                    continue;
                }
                if (target.handler().fill(stored.copyWithAmount(1), IFluidHandler.FluidAction.SIMULATE) > 0) {
                    active.add(new ActiveTarget(target, currentAmount(target)));
                }
            }
            if (active.isEmpty()) {
                break;
            }
            int n = active.size();
            // 本轮可分配量按预算与<b>源当前实际含量</b>双重封顶，再均分——
            // 否则源在一轮中途耗尽会把排在后面的容器饿死（造成 h=3/6... 分配不均）。
            int roundBudget = Math.min(budget, stored.getAmount());
            int base = roundBudget / n;
            int remainder = roundBudget % n;
            // 余量的 +1mB 发给<b>当前存量最少</b>的容器（自纠偏），使长期精确均分而非 ±1 抖动。
            // active 原为就近序，稳定排序后同存量仍按就近，保证确定性。
            active.sort(Comparator.comparingInt(ActiveTarget::amount));
            boolean progressed = false;

            for (int k = 0; k < n && budget > 0; k++) {
                // active 已按当前存量升序：前 remainder 个（存量最少者）多给 1mB → 自纠偏至精确均分
                FluidEndpoint target = active.get(k).endpoint();
                int want = base + (k < remainder ? 1 : 0);
                if (want <= 0) {
                    continue;
                }
                stored = srcHandler.getFluidInTank(tankIdx);
                if (stored.isEmpty()) {
                    break;
                }
                List<ValveState> valvePath = pathValves.get(target.fromPipePos());
                int valveLimit = minValveRemaining(valvePath);
                want = Math.min(want, Math.min(budget, Math.min(valveLimit, stored.getAmount())));
                if (want <= 0) {
                    continue;
                }
                int filled = target.handler().fill(stored.copyWithAmount(want), IFluidHandler.FluidAction.SIMULATE);
                if (filled <= 0) {
                    continue;
                }
                FluidStack drained = srcHandler.drain(stored.copyWithAmount(filled), IFluidHandler.FluidAction.EXECUTE);
                if (drained.isEmpty()) {
                    continue;
                }
                int actuallyFilled = target.handler().fill(drained, IFluidHandler.FluidAction.EXECUTE);
                if (actuallyFilled < drained.getAmount()) {
                    srcHandler.fill(drained.copyWithAmount(drained.getAmount() - actuallyFilled), IFluidHandler.FluidAction.EXECUTE);
                }
                budget -= actuallyFilled;
                deductValves(valvePath, actuallyFilled);
                if (actuallyFilled > 0) {
                    showFluidAlongPipePath(drained, source, target, reach);
                    progressed = true;
                    onTransferred(source);
                }
            }
            if (!progressed) {
                break;
            }
        }
        return isGroupCapacityFull(allTargets);
    }

    /**
     * 低位流体源不得抢占高位源仍可供给的目标容器。
     * 该逻辑保证整个管道网络遵循「高位容器优先输出」规则，即便单向阀会改变流体源的可达判定逻辑也不受影响。
     */
    private boolean hasHigherPrioritySource(
        FluidEndpoint source, FluidStack stored, TreeMap<Integer, List<FluidEndpoint>> targetsByHeight
    ) {
        for (FluidEndpoint higher : sourcesByHeightDesc) {
            if (!isEndpointConnected(higher)) {
                continue;
            }
            if (higher.effectiveHeight() <= source.effectiveHeight()) {
                return false;
            }
            if (higher.handler().equals(source.handler()) || !canDrainFromEndpoint(higher)) {
                continue;
            }
            IFluidHandler higherHandler = higher.handler();
            for (int i = 0; i < higherHandler.getTanks(); i++) {
                FluidStack higherStored = higherHandler.getFluidInTank(i);
                if (higherStored.isEmpty() || !FluidStack.isSameFluidSameComponents(higherStored, stored)) {
                    continue;
                }
                Reachability higherReach = directionalConstraints
                    ? computeReachableCached(higher.fromPipePos(), higherStored)
                    : null;
                if (canTarget(higher, i, source, higherStored, higherReach)) {
                    return true;
                }
                for (List<FluidEndpoint> targets : targetsByHeight.values()) {
                    for (FluidEndpoint target : targets) {
                        if (canTarget(higher, i, target, higherStored, higherReach)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private TreeMap<Integer, List<FluidEndpoint>> collectTargetsByHeight(
        FluidEndpoint source, int tankIdx, FluidStack stored, Reachability reach
    ) {
        TreeMap<Integer, List<FluidEndpoint>> byHeight = new TreeMap<>();
        for (FluidEndpoint target : endpoints) {
            if (!canTarget(source, tankIdx, target, stored, reach)) {
                continue;
            }
            byHeight.computeIfAbsent(target.effectiveHeight(), k -> new ArrayList<>()).add(target);
        }
        return byHeight;
    }

    private boolean canTarget(
        FluidEndpoint source, int tankIdx, FluidEndpoint target, FluidStack stored, Reachability reach
    ) {
        if (!isEndpointConnected(source)
            || !isEndpointConnected(target)
            || target == source
            || target.effectiveHeight() >= source.effectiveHeight()) {
            return false;
        }
        if (target.handler().equals(source.handler())) {
            return false;
        }
        if (isCauldron(source) || isCauldron(target)) {
            if (wholeCauldronTransferAmount(source, tankIdx, target, stored) <= 0) {
                return false;
            }
        } else if (target.handler().fill(
            stored.copyWithAmount(1), IFluidHandler.FluidAction.SIMULATE) <= 0) {
            return false;
        }
        return reach == null || isEndpointReachable(reach, target);
    }

    private boolean canDrainFromEndpoint(FluidEndpoint source) {
        if (source.sideToPipe() == null) {
            return true;
        }
        Direction faceToContainer = source.sideToPipe().getOpposite();
        Map<Direction, Direction> faces = faceFlow.get(source.fromPipePos());
        if (faces == null) {
            return true;
        }
        Direction allowed = faces.get(faceToContainer);
        return allowed == null || allowed != faceToContainer;
    }

    private boolean isCauldron(FluidEndpoint endpoint) {
        return endpoint.cauldron();
    }

    private void fillFromFullCauldron(
        FluidEndpoint source, int tankIdx, List<FluidEndpoint> group,
        Map<BlockPos, List<ValveState>> pathValves, @Nullable Reachability reach
    ) {
        FluidStack stored = source.handler().getFluidInTank(tankIdx);
        for (FluidEndpoint target : group) {
            int amount = wholeCauldronTransferAmount(source, tankIdx, target, stored);
            List<ValveState> valvePath = pathValves.get(target.fromPipePos());
            if (amount <= 0 || minValveRemaining(valvePath) < amount) {
                continue;
            }
            FluidStack moved = stored.copyWithAmount(amount);
            if (moveWholeCauldron(source, tankIdx, target, amount) == amount) {
                deductValves(valvePath, amount);
                showFluidAlongPipePath(moved, source, target, reach);
                onTransferred(source);
                return;
            }
        }
    }

    private boolean canTickEndpoints() {
        disconnectedEntityEndpoints.clear();
        for (FluidEndpoint endpoint : entityEndpoints) {
            if (!FluidContainerLookup.isEntityConnectedToPipe(
                level,
                endpoint.containerPos(),
                endpoint.sideToPipe(),
                endpoint.entity()
            )) {
                disconnectedEntityEndpoints.add(endpoint);
            }
        }
        for (FluidEndpoint endpoint : cauldronEndpoints) {
            if (endpoint.entity() != null) {
                continue;
            }
            if (!level.isLoaded(endpoint.containerPos())) {
                return false;
            }
            FluidContainerLookup.Result container = FluidContainerLookup.find(
                level,
                endpoint.containerPos(),
                endpoint.sideToPipe()
            );
            if (container == null || !container.cauldron()) {
                FluidNetworkManager.INSTANCE.markDirty(level);
                return false;
            }
        }
        return true;
    }

    private boolean isEndpointConnected(FluidEndpoint endpoint) {
        return !disconnectedEntityEndpoints.contains(endpoint);
    }

    private boolean fillFirstWholeCauldronTarget(
        FluidEndpoint source, int tankIdx, List<FluidEndpoint> group,
        Map<BlockPos, List<ValveState>> pathValves, @Nullable Reachability reach
    ) {
        for (FluidEndpoint target : group) {
            if (!isCauldron(target)) {
                continue;
            }
            FluidStack stored = source.handler().getFluidInTank(tankIdx);
            int amount = wholeCauldronTransferAmount(source, tankIdx, target, stored);
            if (amount <= 0) {
                continue;
            }
            List<ValveState> valvePath = pathValves.get(target.fromPipePos());
            if (minValveRemaining(valvePath) < amount) {
                continue;
            }
            FluidStack moved = stored.copyWithAmount(amount);
            if (moveWholeCauldron(source, tankIdx, target, amount) != amount) {
                continue;
            }
            deductValves(valvePath, amount);
            showFluidAlongPipePath(moved, source, target, reach);
            onTransferred(source);
            return true;
        }
        return false;
    }

    private int wholeCauldronTransferAmount(
        FluidEndpoint source, int tankIdx, FluidEndpoint target, FluidStack stored
    ) {
        boolean sourceCauldron = isCauldron(source);
        boolean targetCauldron = isCauldron(target);
        if ((!sourceCauldron && !targetCauldron) || stored.isEmpty()) {
            return 0;
        }

        int amount;
        if (sourceCauldron) {
            amount = source.handler().getTankCapacity(tankIdx);
            if (amount <= 0 || stored.getAmount() != amount) {
                return 0;
            }
        } else {
            amount = totalCapacity(target.handler());
            if (amount <= 0 || stored.getAmount() < amount) {
                return 0;
            }
        }

        if (targetCauldron
            && (currentAmount(target) != 0 || totalCapacity(target.handler()) != amount)) {
            return 0;
        }

        FluidStack toMove = stored.copyWithAmount(amount);
        if (source.handler().drain(toMove, IFluidHandler.FluidAction.SIMULATE).getAmount() != amount) {
            return 0;
        }
        return target.handler().fill(toMove, IFluidHandler.FluidAction.SIMULATE) == amount ? amount : 0;
    }

    private int moveWholeCauldron(
        FluidEndpoint source, int tankIdx, FluidEndpoint target, int amount
    ) {
        FluidStack stored = source.handler().getFluidInTank(tankIdx);
        if (stored.isEmpty() || wholeCauldronTransferAmount(source, tankIdx, target, stored) != amount) {
            return 0;
        }
        FluidStack drained = source.handler().drain(
            stored.copyWithAmount(amount), IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != amount) {
            if (!drained.isEmpty()) {
                source.handler().fill(drained, IFluidHandler.FluidAction.EXECUTE);
            }
            return 0;
        }
        int filled = target.handler().fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (filled < amount) {
            source.handler().fill(drained.copyWithAmount(amount - filled), IFluidHandler.FluidAction.EXECUTE);
        }
        return filled;
    }

    private void onTransferred(FluidEndpoint source) {
        this.transferredThisTick = true;
        if (!level.isClientSide()) {
            long gameTime = level.getGameTime();
            if (triggerGameTime != gameTime) {
                triggerGameTime = gameTime;
                triggeredSources.clear();
            }
            if (!triggeredSources.add(source.containerPos())) {
                return;
            }
            TriggerUtil.pipeConnectContainers(level, source.containerPos());
        }
    }

    private void showFluidAlongPipePath(
        FluidStack fluid, FluidEndpoint source, FluidEndpoint target, @Nullable Reachability reach
    ) {
        if (level.isClientSide() || glassPipePositions.isEmpty()) {
            return;
        }
        List<BlockPos> path = reach == null
            ? undirectedPipePath(source.fromPipePos(), target.fromPipePos())
            : directionalPipePath(source.fromPipePos(), target.fromPipePos(), reach);
        List<BlockPos> glassPath = new ArrayList<>();
        for (BlockPos pos : path) {
            if (glassPipePositions.contains(pos)) {
                glassPath.add(pos);
            }
        }
        if (glassPath.isEmpty()) {
            return;
        }
        Map<BlockPos, EnumSet<Direction>> displayDirections = displayDirectionsByPipe(path, source, target);
        for (BlockPos pos : glassPath) {
            if (level.getBlockEntity(pos) instanceof GlassPipeBlockEntity pipe) {
                pipe.showFluid(fluid, displayDirections.getOrDefault(pos, EnumSet.noneOf(Direction.class)));
            }
        }
    }

    private Map<BlockPos, EnumSet<Direction>> displayDirectionsByPipe(
        List<BlockPos> path, FluidEndpoint source, FluidEndpoint target
    ) {
        Map<BlockPos, EnumSet<Direction>> directions = new HashMap<>();
        for (BlockPos pos : path) {
            directions.put(pos, EnumSet.noneOf(Direction.class));
        }
        for (int i = 1; i < path.size(); i++) {
            addPipePathDirections(directions, path.get(i - 1), path.get(i));
        }
        addEndpointDirection(directions, source);
        addEndpointDirection(directions, target);
        return directions;
    }

    private static void addPipePathDirections(
        Map<BlockPos, EnumSet<Direction>> directions, BlockPos first, BlockPos second
    ) {
        Direction direction = directionBetween(first, second);
        if (direction == null) {
            return;
        }
        directions.computeIfAbsent(first, key -> EnumSet.noneOf(Direction.class)).add(direction);
        directions.computeIfAbsent(second, key -> EnumSet.noneOf(Direction.class)).add(direction.getOpposite());
    }

    private static void addEndpointDirection(
        Map<BlockPos, EnumSet<Direction>> directions, FluidEndpoint endpoint
    ) {
        if (endpoint.sideToPipe() == null) {
            return;
        }
        EnumSet<Direction> pipeDirections = directions.get(endpoint.fromPipePos());
        if (pipeDirections != null) {
            pipeDirections.add(endpoint.sideToPipe().getOpposite());
        }
    }

    @Nullable
    private static Direction directionBetween(BlockPos from, BlockPos to) {
        return Direction.fromDelta(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
    }

    private List<BlockPos> directionalPipePath(BlockPos sourcePipe, BlockPos targetPipe, Reachability reach) {
        if (!reach.pathValves().containsKey(targetPipe)) {
            return List.of();
        }
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> path = new ArrayDeque<>();
        BlockPos current = targetPipe;
        while (current != null && seen.add(current)) {
            path.addFirst(current);
            if (current.equals(sourcePipe)) {
                return new ArrayList<>(path);
            }
            current = reach.cameFrom().get(current);
        }
        return List.of();
    }

    private List<BlockPos> undirectedPipePath(BlockPos start, BlockPos target) {
        final Set<BlockPos> visited = new HashSet<>();
        final Deque<List<BlockPos>> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(List.of(start));
        while (!queue.isEmpty()) {
            List<BlockPos> path = queue.poll();
            BlockPos current = path.get(path.size() - 1);
            if (current.equals(target)) {
                return path;
            }
            for (BlockPos next : adjacency.getOrDefault(current, List.of())) {
                if (!visited.add(next)) {
                    continue;
                }
                List<BlockPos> nextPath = new ArrayList<>(path);
                nextPath.add(next);
                queue.add(nextPath);
            }
        }
        return start.equals(target) ? List.of(start) : List.of(start, target);
    }

    private static int totalCapacity(IFluidHandler handler) {
        int total = 0;
        for (int i = 0; i < handler.getTanks(); i++) {
            total += handler.getTankCapacity(i);
        }
        return total;
    }

    /** 本组是否所有目标都按<b>容量</b>装满（与阀门限流无关，决定是否溢流到更高组）。 */
    private static boolean isGroupCapacityFull(List<FluidEndpoint> group) {
        for (FluidEndpoint target : group) {
            for (int i = 0; i < target.handler().getTanks(); i++) {
                if (target.handler().getFluidInTank(i).getAmount() < target.handler().getTankCapacity(i)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 路径上所有阀门的剩余预算取最小；无阀门则不限（返回 {@link #MAX_SPEED}）。 */
    private static int minValveRemaining(List<ValveState> valvePath) {
        if (valvePath == null || valvePath.isEmpty()) {
            return MAX_SPEED;
        }
        int min = MAX_SPEED;
        for (ValveState v : valvePath) {
            min = Math.min(min, v.remaining());
        }
        return min;
    }

    /** 扣减路径上所有阀门的通过预算。 */
    private static void deductValves(List<ValveState> valvePath, int amount) {
        if (valvePath == null) {
            return;
        }
        for (ValveState v : valvePath) {
            v.consume(amount);
        }
    }

    private record ActiveTarget(FluidEndpoint endpoint, int amount) {
    }

    private record CachedReachability(FluidStack fluid, Reachability reachability) {
    }

    /**
     * 方向感知可达 BFS 的结果：可达接管口 → 路径阀门列表，以及每个接管口的来源
     * （{@code cameFrom}，用于对"端点挂在二极管上"的情形做最后一步方向校验）。
     */
    private record Reachability(Map<BlockPos, List<ValveState>> pathValves, Map<BlockPos, BlockPos> cameFrom) {
    }

    /**
     * 判断目标端点是否真正可达：接管口在 BFS 结果中，接管口若是二极管（泵）能朝容器合法离开，
     * 且接管口朝容器那一面若装有止逆阀，流向必须允许朝容器（否则"逆着止逆阀方向最后一格进罐"）。
     */
    private boolean isEndpointReachable(Reachability reach, FluidEndpoint target) {
        BlockPos pipe = target.fromPipePos();
        if (!reach.pathValves().containsKey(pipe)) {
            return false;
        }
        if (!canLeaveDiode(pipe, reach.cameFrom().get(pipe), target.containerPos())) {
            return false;
        }
        // 接管口朝容器那一面的止逆阀方向校验：容器方向 = sideToPipe 的反侧
        if (target.sideToPipe() != null) {
            Direction toContainer = target.sideToPipe().getOpposite();
            Map<Direction, Direction> faces = faceFlow.get(pipe);
            if (faces != null) {
                Direction allowed = faces.get(toContainer);
                return allowed == null || allowed == toContainer;
            }
        }
        return true;
    }

    /**
     * 从源接管口出发做<b>方向感知</b>可达 BFS，返回 {@code 可达接管口 → 路径上的阀门列表}。
     * <ul>
     *   <li><b>二极管（泵）</b>：只能从进液侧穿到另一侧，反向穿越被禁止（无关高度差）。</li>
     *   <li><b>面止逆阀</b>：流体只能沿该面允许的方向穿过，反向剪枝。</li>
     *   <li><b>阀门</b>：不放行当前流体则该分支剪枝。</li>
     * </ul>
     * 记录 {@code cameFrom} 以对二极管做"入-出"方向判定；首达路径即取到的阀门约束路径。
     */
    private Reachability computeReachable(BlockPos start, FluidStack fluid) {
        Map<BlockPos, List<ValveState>> result = new HashMap<>();
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        List<ValveState> startPath = valvesAt(start, fluid, List.of());
        if (startPath == null) {
            return new Reachability(result, cameFrom); // start 处阀门不放行
        }
        result.put(start, startPath);
        cameFrom.put(start, null);

        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            List<ValveState> curPath = result.get(cur);
            for (BlockPos next : adjacency.getOrDefault(cur, List.of())) {
                if (result.containsKey(next)) {
                    continue;
                }
                // 二极管：若 cur 是泵，只能沿"进液侧→另一侧"离开
                if (!canLeaveDiode(cur, cameFrom.get(cur), next)) {
                    continue;
                }
                // 面止逆阀：cur→next 这条边两端的止逆阀方向校验
                if (!canPassFaceValve(cur, next)) {
                    continue;
                }
                List<ValveState> nextPath = valvesAt(next, fluid, curPath);
                if (nextPath == null) {
                    continue; // 阀门不放行 → 剪枝
                }
                result.put(next, nextPath);
                cameFrom.put(next, cur);
                queue.add(next);
            }
        }
        return new Reachability(result, cameFrom);
    }

    private Reachability computeReachableCached(BlockPos start, FluidStack fluid) {
        List<CachedReachability> cached = reachabilityCache.computeIfAbsent(
            start.immutable(), key -> new ArrayList<>());
        for (CachedReachability entry : cached) {
            if (FluidStack.isSameFluidSameComponents(entry.fluid(), fluid)) {
                return entry.reachability();
            }
        }
        Reachability reachability = computeReachable(start, fluid);
        cached.add(new CachedReachability(fluid.copyWithAmount(1), reachability));
        return reachability;
    }

    /**
     * 沿边 {@code cur → next}（世界方向 {@code d}）判断两端管道的面止逆阀是否放行：
     * <ul>
     *   <li>{@code cur} 朝 {@code d} 的面装阀：允许流出方向须为 {@code d}；</li>
     *   <li>{@code next} 朝 {@code -d} 的面（正对 cur）装阀：允许流出方向须为 {@code d}（即允许流体流入 next）。</li>
     * </ul>
     */
    private boolean canPassFaceValve(BlockPos cur, BlockPos next) {
        Direction d = Direction.fromDelta(
            next.getX() - cur.getX(), next.getY() - cur.getY(), next.getZ() - cur.getZ());
        if (d == null) {
            return true;
        }
        Map<Direction, Direction> fc = faceFlow.get(cur);
        if (fc != null) {
            Direction allowed = fc.get(d);
            if (allowed != null && allowed != d) {
                return false;
            }
        }
        Map<Direction, Direction> fn = faceFlow.get(next);
        if (fn != null) {
            Direction allowed = fn.get(d.getOpposite());
            return allowed == null || allowed == d;
        }
        return true;
    }

    /**
     * 若 {@code cur} 是二极管（泵），判断从 {@code from} 进入、向 {@code to} 离开
     * 是否符合其流体方向。
     *
     * <p>方向语义：{@code diodes} 存进液侧方向（泵为 {@code getDirection()} 侧、势场 +10）；
     * 流体只允许 <b>进液侧 → 另一侧</b> 通过（如朝下的泵把下方水抽到上方）。非二极管则恒允许。
     *
     * @param from 进入 {@code cur} 的来源部件（{@code null} 表示 {@code cur} 是 BFS 起点）
     */
    private boolean canLeaveDiode(BlockPos cur, BlockPos from, BlockPos to) {
        Direction inflowDir = diodes.get(cur);
        if (inflowDir == null) {
            return true; // 非二极管
        }
        BlockPos highSide = cur.relative(inflowDir);              // 进液侧，上游
        BlockPos lowSide = cur.relative(inflowDir.getOpposite()); // 另一侧，下游
        // 只允许 从上游(进液侧)进入、向下游离开；起点恰为二极管时（from==null）也只能朝下游走
        if (!to.equals(lowSide)) {
            return false;
        }
        return from == null || from.equals(highSide);
    }

    /**
     * 若 {@code pos} 是阀门：放行该流体则返回 {@code base + 本阀门}，否则返回 {@code null}（阻断）。
     * 非阀门则原样返回 {@code base}。
     */
    private List<ValveState> valvesAt(BlockPos pos, FluidStack fluid, List<ValveState> base) {
        ValveState valve = valves.get(pos);
        if (valve == null) {
            return base;
        }
        if (!valve.allows(fluid)) {
            return null;
        }
        List<ValveState> extended = new ArrayList<>(base);
        extended.add(valve);
        return extended;
    }

    private static int sumXZ(BlockPos pos) {
        return pos.getX() + pos.getZ();
    }

    /** 目标容器当前存量（所有 tank 之和），用于"余量给存量最少者"的自纠偏均分。 */
    private static int currentAmount(FluidEndpoint endpoint) {
        IFluidHandler handler = endpoint.handler();
        int total = 0;
        for (int i = 0; i < handler.getTanks(); i++) {
            total += handler.getFluidInTank(i).getAmount();
        }
        return total;
    }
}
