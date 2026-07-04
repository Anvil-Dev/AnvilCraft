package dev.dubhe.anvilcraft.api.fluid.network;

import dev.dubhe.anvilcraft.util.TriggerUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
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
    /** 泵位置 → 输出方向。泵是二极管：流体只能从输入侧穿到输出侧，反向不通（无关高度差）。 */
    private final Map<BlockPos, Direction> pumps;
    private final List<FluidEndpoint> endpoints;
    /** 端点按等效高度<b>降序</b>预排序（作为源的遍历顺序），构建时排一次，避免每 tick 重排。 */
    private final List<FluidEndpoint> sourcesByHeightDesc;

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
        Map<BlockPos, Direction> pumps,
        List<FluidEndpoint> endpoints
    ) {
        this.level = level;
        this.parts = parts;
        this.adjacency = adjacency;
        this.valves = valves;
        this.pumps = pumps;
        this.endpoints = endpoints;
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
        // 每 tick 分配前重置各阀门预算（实时读取阀门当前流速设置）
        if (!valves.isEmpty()) {
            for (ValveState valve : valves.values()) {
                valve.resetBudget();
            }
        }
        // 源已按等效高度降序预排序：高处先流，一 tick 内可级联下泄
        for (FluidEndpoint source : sourcesByHeightDesc) {
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
        if (endpoints.isEmpty()) {
            return;
        }
        distributeFromSource(new FluidEndpoint(srcPos, entryPipePos, null, srcHandler, sourceEffectiveHeight));
    }

    /** 从单个源端点向所有更低的端点分配其持有的流体。 */
    private void distributeFromSource(FluidEndpoint source) {
        IFluidHandler srcHandler = source.handler();
        // 逐个 tank 处理源中的流体（多数容器仅一个 tank）
        for (int tankIdx = 0; tankIdx < srcHandler.getTanks(); tankIdx++) {
            FluidStack stored = srcHandler.getFluidInTank(tankIdx);
            if (stored.isEmpty()) {
                continue;
            }
            // 从源出发做方向感知可达 BFS：泵只能正向穿过（二极管）、阀门按过滤放行；
            // 得到 可达接管口 → 路径上的阀门列表。有泵或有阀门时都需要此判定。
            boolean needReachability = !valves.isEmpty() || !pumps.isEmpty();
            Map<BlockPos, List<ValveState>> pathValves = needReachability
                ? computeReachable(source.fromPipePos(), stored)
                : Map.of();
            // 按等效高度升序分组，仅收集严格更低、接受该流体、且可达的目标
            TreeMap<Integer, List<FluidEndpoint>> byHeight = new TreeMap<>();
            for (FluidEndpoint target : endpoints) {
                if (target == source) {
                    continue;
                }
                if (target.effectiveHeight() >= source.effectiveHeight()) {
                    continue;
                }
                if (target.handler().equals(srcHandler)) {
                    continue; // 同一容器（如巨型储罐多 part）不自我搬运
                }
                if (target.handler().fill(stored.copyWithAmount(1), IFluidHandler.FluidAction.SIMULATE) <= 0) {
                    continue; // 该目标无法接受此流体（类型不符或已满）
                }
                if (needReachability && !pathValves.containsKey(target.fromPipePos())) {
                    continue; // 不可达（逆泵方向 或 被阀门过滤阻断）
                }
                byHeight.computeIfAbsent(target.effectiveHeight(), k -> new ArrayList<>()).add(target);
            }
            if (byHeight.isEmpty()) {
                continue;
            }
            // 从最低组开始填，本组填满才溢流到更高组
            for (var entry : byHeight.entrySet()) {
                int groupHeight = entry.getKey();
                List<FluidEndpoint> group = entry.getValue();
                int heightDiff = source.effectiveHeight() - groupHeight;
                int groupSpeed = speedForHeightDiff(heightDiff);
                boolean groupFull = fillGroup(source, tankIdx, group, groupSpeed, pathValves);
                // 源已流尽 → 停止
                if (srcHandler.getFluidInTank(tankIdx).isEmpty()) {
                    break;
                }
                // 本组未被填满（受流速/预算限制，或本就有余量）→ 不向更高组溢流，
                // 未流出的部分留在源中等待下 1 tick
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
     * @return 本组是否已被<b>按容量填满</b>（与阀门限流无关）——用于决定是否向更高组溢流
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private boolean fillGroup(
        FluidEndpoint source, int tankIdx, List<FluidEndpoint> group, int groupSpeed,
        Map<BlockPos, List<ValveState>> pathValves
    ) {
        // 组内按到源的"就近"排序：|Σxz差| 升序，再比 |x差|、|z差|
        BlockPos src = source.containerPos();
        group.sort(Comparator
            .comparingInt((FluidEndpoint e) -> Math.abs(sumXZ(e.containerPos()) - sumXZ(src)))
            .thenComparingInt(e -> Math.abs(e.containerPos().getX() - src.getX()))
            .thenComparingInt(e -> Math.abs(e.containerPos().getZ() - src.getZ())));

        IFluidHandler srcHandler = source.handler();
        int budget = groupSpeed;

        while (budget > 0) {
            FluidStack stored = srcHandler.getFluidInTank(tankIdx);
            if (stored.isEmpty()) {
                break; // 源已空
            }
            // 重算活跃目标：仍能按容量接受、且路径阀门有剩余预算
            List<FluidEndpoint> active = new ArrayList<>();
            for (FluidEndpoint target : group) {
                if (minValveRemaining(pathValves.get(target.fromPipePos())) <= 0) {
                    continue;
                }
                if (target.handler().fill(stored.copyWithAmount(1), IFluidHandler.FluidAction.SIMULATE) > 0) {
                    active.add(target);
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
            active.sort(Comparator.comparingInt(FluidPipeNetwork::currentAmount));
            boolean progressed = false;

            for (int k = 0; k < n && budget > 0; k++) {
                // active 已按当前存量升序：前 remainder 个（存量最少者）多给 1mB → 自纠偏至精确均分
                FluidEndpoint target = active.get(k);
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
                    progressed = true;
                    this.transferredThisTick = true;
                    if (!level.isClientSide()) {
                        TriggerUtil.pipeConnectContainers(level, source.containerPos());
                    }
                }
            }
            if (!progressed) {
                break;
            }
        }
        return isGroupCapacityFull(group);
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

    /**
     * 从源接管口出发做<b>方向感知</b>可达 BFS，返回 {@code 可达接管口 → 路径上的阀门列表}。
     * <ul>
     *   <li><b>泵（二极管）</b>：只能从输入侧穿到输出侧，反向穿越被禁止（无关高度差）。</li>
     *   <li><b>阀门</b>：不放行当前流体则该分支剪枝。</li>
     * </ul>
     * 记录 {@code cameFrom} 以对泵做"入-出"方向判定；首达路径即取到的阀门约束路径。
     */
    private Map<BlockPos, List<ValveState>> computeReachable(BlockPos start, FluidStack fluid) {
        Map<BlockPos, List<ValveState>> result = new HashMap<>();
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        List<ValveState> startPath = valvesAt(start, fluid, List.of());
        if (startPath == null) {
            return result; // start 处阀门不放行
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
                // 泵二极管：若 cur 是泵，只能沿"输入侧→输出侧"离开
                if (!canLeavePump(cur, cameFrom.get(cur), next)) {
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
        return result;
    }

    /**
     * 若 {@code cur} 是泵，判断从 {@code from} 进入、向 {@code to} 离开是否符合泵的流体方向。
     *
     * <p>方向语义：{@code getDirection()}（{@code outputDir}）侧势场 +10（上游/进泵侧），
     * 反侧 -10（下游/出泵侧）；流体从高势流向低势，即 <b>getDirection() 侧 → 反侧</b>
     * （如朝下的泵把下方水抽到上方）。故只允许 {@code from = getDirection() 侧}、
     * {@code to = 反侧}。非泵则恒允许。
     *
     * @param from 进入 {@code cur} 的来源部件（{@code null} 表示 {@code cur} 是 BFS 起点）
     */
    private boolean canLeavePump(BlockPos cur, BlockPos from, BlockPos to) {
        Direction outputDir = pumps.get(cur);
        if (outputDir == null) {
            return true; // 非泵
        }
        BlockPos highSide = cur.relative(outputDir);              // getDirection() 侧，+10，上游（进泵）
        BlockPos lowSide = cur.relative(outputDir.getOpposite()); // 反侧，-10，下游（出泵）
        // 只允许 从上游(高势侧)进入、向下游(低势侧)离开；起点恰为泵时（from==null）也只能朝下游走
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
