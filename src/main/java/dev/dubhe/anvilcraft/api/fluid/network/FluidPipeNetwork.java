package dev.dubhe.anvilcraft.api.fluid.network;

import dev.dubhe.anvilcraft.util.TriggerUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
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
 */
public class FluidPipeNetwork {
    public static final int HEIGHT_RATE = 50;
    public static final int MAX_SPEED = 2000;
    public static final int FULL_SPEED_HEIGHT = FluidPipeNetwork.MAX_SPEED / FluidPipeNetwork.HEIGHT_RATE;

    public static int speedForHeightDiff(int heightDiff) {
        if (heightDiff <= 0) return 0;
        return Math.min(heightDiff * FluidPipeNetwork.HEIGHT_RATE, FluidPipeNetwork.MAX_SPEED);
    }

    private final Level level;
    @Getter
    private final Set<BlockPos> parts;
    private final Map<BlockPos, List<BlockPos>> adjacency;
    private final Map<BlockPos, ValveState> valves;
    private final Map<BlockPos, Direction> diodes;
    private final Map<BlockPos, Map<Direction, Direction>> faceFlow;
    private final List<FluidEndpoint> endpoints;
    private final List<FluidEndpoint> cauldronEndpoints;
    private final List<FluidEndpoint> entityEndpoints;
    private final Set<FluidEndpoint> disconnectedEntityEndpoints = new HashSet<>();
    private final boolean directionalConstraints;
    private final List<FluidEndpoint> sourcesByHeightDesc;
    private final Map<BlockPos, Map<FluidResource, Reachability>> reachabilityCache = new HashMap<>();
    private final Set<BlockPos> triggeredSources = new HashSet<>();
    private long triggerGameTime = Long.MIN_VALUE;

    private boolean transferredThisTick;
    @Getter
    private int idleTicks;

    public void updateIdle() {
        if (this.transferredThisTick) {
            this.idleTicks = 0;
        } else {
            this.idleTicks++;
        }
    }

    public FluidPipeNetwork(
        Level level,
        Set<BlockPos> parts,
        Map<BlockPos, List<BlockPos>> adjacency,
        Map<BlockPos, ValveState> valves,
        Map<BlockPos, Direction> diodes,
        Map<BlockPos, Map<Direction, Direction>> faceFlow,
        List<FluidEndpoint> endpoints
    ) {
        this.level = level;
        this.parts = parts;
        this.adjacency = adjacency;
        this.valves = valves;
        this.diodes = diodes;
        this.faceFlow = faceFlow;
        this.endpoints = endpoints;
        this.cauldronEndpoints = endpoints.stream().filter(FluidEndpoint::cauldron).toList();
        this.entityEndpoints = endpoints.stream().filter(endpoint -> endpoint.entity() != null).toList();
        this.directionalConstraints = !valves.isEmpty() || !diodes.isEmpty() || !faceFlow.isEmpty();
        this.sourcesByHeightDesc = new ArrayList<>(endpoints);
        this.sourcesByHeightDesc.sort(Comparator.comparingInt(FluidEndpoint::effectiveHeight).reversed());
    }

    public void tick() {
        this.transferredThisTick = false;
        if (this.endpoints.size() < 2) return;
        if (!this.canTickEndpoints()) return;
        this.reachabilityCache.clear();
        if (!this.valves.isEmpty()) {
            for (ValveState valve : this.valves.values()) {
                valve.resetBudget();
            }
        }
        for (FluidEndpoint source : this.sourcesByHeightDesc) {
            if (!this.isEndpointConnected(source)) continue;
            this.distributeFromSource(source);
        }
    }

    public void pushFromExternalSource(
        ResourceHandler<FluidResource> srcHandler,
        BlockPos srcPos,
        BlockPos entryPipePos,
        int sourceEffectiveHeight
    ) {
        if (this.endpoints.isEmpty() || !this.canTickEndpoints()) return;
        this.reachabilityCache.clear();
        this.distributeFromSource(new FluidEndpoint(
            srcPos,
            entryPipePos,
            null,
            srcHandler,
            sourceEffectiveHeight,
            false
        ));
    }

    /**
     * 从单个源端点向所有更低的端点分配其持有的流体。
     */
    private void distributeFromSource(FluidEndpoint source) {
        if (source.sideToPipe() != null) {
            Direction faceToContainer = source.sideToPipe().getOpposite();
            Map<Direction, Direction> faces = this.faceFlow.get(source.fromPipePos());
            if (faces != null) {
                Direction allowed = faces.get(faceToContainer);
                if (allowed != null && allowed == faceToContainer) {
                    return;
                }
            }
        }
        ResourceHandler<FluidResource> srcHandler = source.handler();
        for (int tankIdx = 0; tankIdx < srcHandler.size(); tankIdx++) {
            FluidResource stored = srcHandler.getResource(tankIdx);
            int storedAmount = srcHandler.getAmountAsInt(tankIdx);
            if (stored.isEmpty() || storedAmount <= 0) continue;

            Reachability reach = this.directionalConstraints
                                 ? this.computeReachableCached(source.fromPipePos(), stored)
                                 : null;
            Map<BlockPos, List<ValveState>> pathValves = reach == null ? Map.of() : reach.pathValves();

            TreeMap<Integer, List<FluidEndpoint>> byHeight = this.collectTargetsByHeight(
                source,
                tankIdx,
                stored,
                reach
            );
            if (byHeight.isEmpty()) continue;
            if (this.hasHigherPrioritySource(source, stored, byHeight)) continue;

            for (var entry : byHeight.entrySet()) {
                int groupHeight = entry.getKey();
                List<FluidEndpoint> group = entry.getValue();
                int heightDiff = source.effectiveHeight() - groupHeight;
                int groupSpeed = FluidPipeNetwork.speedForHeightDiff(heightDiff);
                boolean groupFull = this.fillGroup(source, tankIdx, stored, group, groupSpeed, pathValves);
                if (srcHandler.getAmountAsInt(tankIdx) <= 0) break;
                if (!groupFull) break;
            }
        }
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private boolean fillGroup(
        FluidEndpoint source,
        int tankIdx,
        FluidResource fluidType,
        List<FluidEndpoint> group,
        int groupSpeed,
        Map<BlockPos, List<ValveState>> pathValves
    ) {
        BlockPos src = source.containerPos();
        group.sort(Comparator.comparingInt((FluidEndpoint e) -> Math.abs(
                FluidPipeNetwork.sumXZ(e.containerPos()) - FluidPipeNetwork.sumXZ(src)))
            .thenComparingInt(e -> Math.abs(e.containerPos().getX() - src.getX()))
            .thenComparingInt(e -> Math.abs(e.containerPos().getZ() - src.getZ())));

        List<FluidEndpoint> allTargets = group;
        ResourceHandler<FluidResource> srcHandler = source.handler();
        if (source.cauldron()) {
            this.fillFromFullCauldron(source, tankIdx, group, pathValves);
            return FluidPipeNetwork.isGroupCapacityFull(group);
        }
        if (this.fillFirstWholeCauldronTarget(source, tankIdx, group, pathValves)) {
            return FluidPipeNetwork.isGroupCapacityFull(group);
        }
        group = group.stream().filter(target -> !target.cauldron()).toList();
        if (group.isEmpty()) return false;
        int budget = groupSpeed;

        while (budget > 0) {
            int currentStored = srcHandler.getAmountAsInt(tankIdx);
            if (currentStored <= 0) break;

            List<ActiveTarget> active = new ArrayList<>();
            for (FluidEndpoint target : group) {
                if (FluidPipeNetwork.minValveRemaining(pathValves.get(target.fromPipePos())) <= 0) continue;
                if (FluidPipeNetwork.canInsert(target.handler(), fluidType)) {
                    active.add(new ActiveTarget(target, FluidPipeNetwork.currentAmount(target)));
                }
            }
            if (active.isEmpty()) break;

            int n = active.size();
            int roundBudget = Math.min(budget, currentStored);
            int base = roundBudget / n;
            int remainder = roundBudget % n;
            active.sort(Comparator.comparingInt(ActiveTarget::amount));
            boolean progressed = false;

            for (int k = 0; k < n && budget > 0; k++) {
                FluidEndpoint target = active.get(k).endpoint();
                int want = base + (k < remainder ? 1 : 0);
                if (want <= 0) continue;

                int srcAmount = srcHandler.getAmountAsInt(tankIdx);
                if (srcAmount <= 0) break;

                List<ValveState> valvePath = pathValves.get(target.fromPipePos());
                int valveLimit = FluidPipeNetwork.minValveRemaining(valvePath);
                want = Math.min(want, Math.min(budget, Math.min(valveLimit, srcAmount)));
                if (want <= 0) continue;

                // Execute transfer in a single transaction
                try (Transaction tx = Transaction.openRoot()) {
                    int extracted = srcHandler.extract(tankIdx, fluidType, want, tx);
                    if (extracted <= 0) continue;
                    int inserted = target.handler().insert(fluidType, extracted, tx);
                    if (inserted <= 0) continue;
                    if (inserted < extracted) {
                        // Return excess to source
                        srcHandler.insert(fluidType, extracted - inserted, tx);
                    }
                    tx.commit();
                    budget -= inserted;
                    FluidPipeNetwork.deductValves(valvePath, inserted);
                    progressed = true;
                    this.onTransferred(source);
                }
            }
            if (!progressed) break;
        }
        return FluidPipeNetwork.isGroupCapacityFull(allTargets);
    }

    private TreeMap<Integer, List<FluidEndpoint>> collectTargetsByHeight(
        FluidEndpoint source,
        int tankIdx,
        FluidResource stored,
        Reachability reach
    ) {
        TreeMap<Integer, List<FluidEndpoint>> byHeight = new TreeMap<>();
        for (FluidEndpoint target : this.endpoints) {
            if (!this.canTarget(source, tankIdx, target, stored, reach)) continue;
            byHeight.computeIfAbsent(target.effectiveHeight(), _ -> new ArrayList<>()).add(target);
        }
        return byHeight;
    }

    private boolean canTarget(
        FluidEndpoint source,
        int tankIdx,
        FluidEndpoint target,
        FluidResource stored,
        Reachability reach
    ) {
        if (!this.isEndpointConnected(source) || !this.isEndpointConnected(target)) return false;
        if (target == source || target.effectiveHeight() >= source.effectiveHeight()) return false;
        if (target.handler().equals(source.handler())) return false;
        if (source.cauldron() || target.cauldron()) {
            if (this.wholeCauldronTransferAmount(source, tankIdx, target, stored) <= 0) return false;
        } else if (!FluidPipeNetwork.canInsert(target.handler(), stored)) {
            return false;
        }
        return reach == null || this.isEndpointReachable(reach, target);
    }

    private boolean hasHigherPrioritySource(
        FluidEndpoint source,
        FluidResource stored,
        TreeMap<Integer, List<FluidEndpoint>> targetsByHeight
    ) {
        for (FluidEndpoint higher : this.sourcesByHeightDesc) {
            if (!this.isEndpointConnected(higher)) continue;
            if (higher.effectiveHeight() <= source.effectiveHeight()) return false;
            if (higher.handler().equals(source.handler())) continue;
            for (int i = 0; i < higher.handler().size(); i++) {
                FluidResource higherStored = higher.handler().getResource(i);
                if (higherStored.isEmpty() || !higherStored.equals(stored)) continue;
                Reachability higherReach = this.directionalConstraints
                                           ? this.computeReachableCached(higher.fromPipePos(), higherStored)
                                           : null;
                if (this.canTarget(higher, i, source, higherStored, higherReach)) return true;
                for (List<FluidEndpoint> targets : targetsByHeight.values()) {
                    for (FluidEndpoint target : targets) {
                        if (this.canTarget(higher, i, target, higherStored, higherReach)) return true;
                    }
                }
            }
        }
        return false;
    }

    private void fillFromFullCauldron(
        FluidEndpoint source,
        int tankIdx,
        List<FluidEndpoint> group,
        Map<BlockPos, List<ValveState>> pathValves
    ) {
        FluidResource stored = source.handler().getResource(tankIdx);
        for (FluidEndpoint target : group) {
            int amount = this.wholeCauldronTransferAmount(source, tankIdx, target, stored);
            List<ValveState> valvePath = pathValves.get(target.fromPipePos());
            if (amount <= 0 || FluidPipeNetwork.minValveRemaining(valvePath) < amount) continue;
            if (this.moveWholeCauldron(source, tankIdx, target, stored, amount) == amount) {
                FluidPipeNetwork.deductValves(valvePath, amount);
                this.onTransferred(source);
                return;
            }
        }
    }

    private boolean fillFirstWholeCauldronTarget(
        FluidEndpoint source,
        int tankIdx,
        List<FluidEndpoint> group,
        Map<BlockPos, List<ValveState>> pathValves
    ) {
        for (FluidEndpoint target : group) {
            if (!target.cauldron()) continue;
            FluidResource stored = source.handler().getResource(tankIdx);
            int amount = this.wholeCauldronTransferAmount(source, tankIdx, target, stored);
            List<ValveState> valvePath = pathValves.get(target.fromPipePos());
            if (amount <= 0 || FluidPipeNetwork.minValveRemaining(valvePath) < amount) continue;
            if (this.moveWholeCauldron(source, tankIdx, target, stored, amount) != amount) continue;
            FluidPipeNetwork.deductValves(valvePath, amount);
            this.onTransferred(source);
            return true;
        }
        return false;
    }

    private int wholeCauldronTransferAmount(
        FluidEndpoint source,
        int tankIdx,
        FluidEndpoint target,
        FluidResource stored
    ) {
        if ((!source.cauldron() && !target.cauldron()) || stored.isEmpty()) return 0;
        int amount;
        if (source.cauldron()) {
            amount = source.handler().getCapacityAsInt(tankIdx, stored);
            if (amount <= 0 || source.handler().getAmountAsInt(tankIdx) != amount) return 0;
        } else {
            amount = FluidPipeNetwork.capacityFor(target.handler(), stored);
            if (amount <= 0 || source.handler().getAmountAsInt(tankIdx) < amount) return 0;
        }
        if (target.cauldron() && FluidPipeNetwork.currentAmount(target) != 0) return 0;
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = source.handler().extract(tankIdx, stored, amount, transaction);
            if (extracted != amount) return 0;
            return target.handler().insert(stored, amount, transaction) == amount ? amount : 0;
        }
    }

    private int moveWholeCauldron(
        FluidEndpoint source,
        int tankIdx,
        FluidEndpoint target,
        FluidResource stored,
        int amount
    ) {
        if (amount <= 0) return 0;
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = source.handler().extract(tankIdx, stored, amount, transaction);
            if (extracted != amount) return 0;
            int inserted = target.handler().insert(stored, amount, transaction);
            if (inserted != amount) return 0;
            transaction.commit();
            return amount;
        }
    }

    private boolean canTickEndpoints() {
        this.disconnectedEntityEndpoints.clear();
        for (FluidEndpoint endpoint : this.entityEndpoints) {
            Entity entity = endpoint.entity();
            if (entity == null || !FluidContainerLookup.isEntityConnectedToPipe(
                    this.level,
                    endpoint.containerPos(),
                    endpoint.sideToPipe(),
                    entity
                )) {
                this.disconnectedEntityEndpoints.add(endpoint);
            }
        }
        for (FluidEndpoint endpoint : this.cauldronEndpoints) {
            // 实体炼药锅由上面的接触判定负责，不看方块状态
            if (endpoint.entity() != null) continue;
            if (!this.level.isLoaded(endpoint.containerPos())) return false;
            FluidContainerLookup.Result container = FluidContainerLookup.find(
                this.level,
                endpoint.containerPos(),
                endpoint.sideToPipe()
            );
            if (container == null || !container.cauldron()) {
                FluidNetworkManager.INSTANCE.markDirty(this.level);
                return false;
            }
        }
        return true;
    }

    /// 实体端点在本 tick 是否仍与管道接触；方块端点恒为 true。
    private boolean isEndpointConnected(FluidEndpoint endpoint) {
        return !this.disconnectedEntityEndpoints.contains(endpoint);
    }

    private void onTransferred(FluidEndpoint source) {
        this.transferredThisTick = true;
        if (this.level.isClientSide()) return;
        long gameTime = this.level.getGameTime();
        if (this.triggerGameTime != gameTime) {
            this.triggerGameTime = gameTime;
            this.triggeredSources.clear();
        }
        if (this.triggeredSources.add(source.containerPos())) {
            TriggerUtil.connectFluidContainers(this.level, source.containerPos());
        }
    }

    private static int capacityFor(ResourceHandler<FluidResource> handler, FluidResource resource) {
        int total = 0;
        for (int i = 0; i < handler.size(); i++) {
            total += handler.getCapacityAsInt(i, resource);
        }
        return total;
    }

    /**
     * Check if a handler can accept the given fluid (simulate insert of 1 unit).
     */
    private static boolean canInsert(ResourceHandler<FluidResource> handler, FluidResource resource) {
        try (Transaction tx = Transaction.openRoot()) {
            return handler.insert(resource, 1, tx) > 0;
        }
    }

    private static boolean isGroupCapacityFull(List<FluidEndpoint> group) {
        for (FluidEndpoint target : group) {
            ResourceHandler<FluidResource> h = target.handler();
            for (int i = 0; i < h.size(); i++) {
                FluidResource res = h.getResource(i);
                if (res.isEmpty()) return false; // empty slot = not full
                if (h.getAmountAsInt(i) < h.getCapacityAsInt(i, res)) return false;
            }
        }
        return true;
    }

    private static int minValveRemaining(List<ValveState> valvePath) {
        if (valvePath == null || valvePath.isEmpty()) return FluidPipeNetwork.MAX_SPEED;
        int min = FluidPipeNetwork.MAX_SPEED;
        for (ValveState v : valvePath) {
            min = Math.min(min, v.remaining());
        }
        return min;
    }

    private static void deductValves(List<ValveState> valvePath, int amount) {
        if (valvePath == null) return;
        for (ValveState v : valvePath) {
            v.consume(amount);
        }
    }

    private record ActiveTarget(FluidEndpoint endpoint, int amount) {
    }

    private record Reachability(Map<BlockPos, List<ValveState>> pathValves, Map<BlockPos, BlockPos> cameFrom) {
    }

    private boolean isEndpointReachable(Reachability reach, FluidEndpoint target) {
        BlockPos pipe = target.fromPipePos();
        if (!reach.pathValves().containsKey(pipe)) return false;
        if (!this.canLeaveDiode(pipe, reach.cameFrom().get(pipe), target.containerPos())) return false;
        if (target.sideToPipe() != null) {
            Direction toContainer = target.sideToPipe().getOpposite();
            Map<Direction, Direction> faces = this.faceFlow.get(pipe);
            if (faces != null) {
                Direction allowed = faces.get(toContainer);
                return allowed == null || allowed == toContainer;
            }
        }
        return true;
    }

    private Reachability computeReachable(BlockPos start, FluidResource fluid) {
        Map<BlockPos, List<ValveState>> result = new HashMap<>();
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        List<ValveState> startPath = this.valvesAt(start, fluid, List.of());
        if (startPath == null) return new Reachability(result, cameFrom);
        result.put(start, startPath);
        cameFrom.put(start, null);

        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            List<ValveState> curPath = result.get(cur);
            for (BlockPos next : this.adjacency.getOrDefault(cur, List.of())) {
                if (result.containsKey(next)) continue;
                if (!this.canLeaveDiode(cur, cameFrom.get(cur), next)) continue;
                if (!this.canPassFaceValve(cur, next)) continue;
                List<ValveState> nextPath = this.valvesAt(next, fluid, curPath);
                if (nextPath == null) continue;
                result.put(next, nextPath);
                cameFrom.put(next, cur);
                queue.add(next);
            }
        }
        return new Reachability(result, cameFrom);
    }

    private Reachability computeReachableCached(BlockPos start, FluidResource fluid) {
        Map<FluidResource, Reachability> cached = this.reachabilityCache.computeIfAbsent(
            start.immutable(),
            _ -> new HashMap<>()
        );
        return cached.computeIfAbsent(fluid, _ -> this.computeReachable(start, fluid));
    }

    private boolean canPassFaceValve(BlockPos cur, BlockPos next) {
        Direction d = null;
        int dx = next.getX() - cur.getX();
        int dy = next.getY() - cur.getY();
        int dz = next.getZ() - cur.getZ();
        for (Direction dir : Direction.values()) {
            if (dir.getStepX() == dx && dir.getStepY() == dy && dir.getStepZ() == dz) {
                d = dir;
                break;
            }
        }
        if (d == null) return true;
        Map<Direction, Direction> fc = this.faceFlow.get(cur);
        if (fc != null) {
            Direction allowed = fc.get(d);
            if (allowed != null && allowed != d) return false;
        }
        Map<Direction, Direction> fn = this.faceFlow.get(next);
        if (fn != null) {
            Direction allowed = fn.get(d.getOpposite());
            return allowed == null || allowed == d;
        }
        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean canLeaveDiode(BlockPos cur, BlockPos from, BlockPos to) {
        Direction inflowDir = this.diodes.get(cur);
        if (inflowDir == null) return true;
        BlockPos lowSide = cur.relative(inflowDir.getOpposite());
        if (!to.equals(lowSide)) return false;
        return from == null || from.equals(cur.relative(inflowDir));
    }

    private List<ValveState> valvesAt(BlockPos pos, FluidResource fluid, List<ValveState> base) {
        ValveState valve = this.valves.get(pos);
        if (valve == null) return base;
        if (!valve.allows(fluid)) return null;
        List<ValveState> extended = new ArrayList<>(base);
        extended.add(valve);
        return extended;
    }

    private static int sumXZ(BlockPos pos) {
        return pos.getX() + pos.getZ();
    }

    private static int currentAmount(FluidEndpoint endpoint) {
        ResourceHandler<FluidResource> handler = endpoint.handler();
        int total = 0;
        for (int i = 0; i < handler.size(); i++) {
            total += handler.getAmountAsInt(i);
        }
        return total;
    }
}
