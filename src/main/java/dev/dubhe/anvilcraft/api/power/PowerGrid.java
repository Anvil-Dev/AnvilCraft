package dev.dubhe.anvilcraft.api.power;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.network.PowerGridRemovePacket;
import dev.dubhe.anvilcraft.network.PowerGridSyncChunkPacket;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 电网
 */
@SuppressWarnings("unused")
public class PowerGrid {
    public static boolean isServerClosing = false;
    public static final PowerGridManager MANAGER = new PowerGridManager();
    public static final int GRID_TICK = 20;

    @Getter
    public boolean markedRemoval = false;

    private boolean changed = true;

    @Getter
    private int generate = 0; // 发电功率

    @Getter
    private int consume = 0; // 耗电功率

    @Getter
    private boolean hasInfinitePower = false; // 是否有无限电力（如戴森球加速期间）
    @Getter
    final Set<IPowerComponent> components = Collections.synchronizedSet(new HashSet<>());
    final Set<IPowerProducer> producers = Collections.synchronizedSet(new HashSet<>()); // 发电机
    final Set<IPowerConsumer> consumers = Collections.synchronizedSet(new HashSet<>()); // 用电器
    final Set<IPowerStorage> storages = Collections.synchronizedSet(new HashSet<>()); // 储电
    final Set<IPowerTransmitter> transmitters = Collections.synchronizedSet(new HashSet<>()); // 中继
    @Getter
    final Set<DynamicPowerComponent> dynamicComponents = Collections.synchronizedSet(new HashSet<>());
    private final Map<IPowerComponent, Set<IPowerComponent>> connections = new HashMap<>();

    @Getter
    private FastShape shape = null;

    @Getter
    private BlockPos pos = null;

    @Getter
    private final Level level;

    public PowerGrid(Level level) {
        this.level = level;
    }

    static {
        ConnectivityChecker.register(new FastCollisionConnectivityChecker());
    }

    public void update(boolean forced) {
        if (forced || changed) {
            PowerGridSyncChunkPacket.send(this);
        }
    }

    public int getComponentCount() {
        return this.components.size();
    }

    public boolean isEmpty() {
        return this.getComponentCount() <= 0;
    }

    /**
     * 获取电网中的剩余电量
     *
     * @return 剩余电量，可为负值
     */
    public int getRemaining() {
        return this.generate - this.consume;
    }

    public void markChanged() {
        this.changed = true;
    }

    /**
     * 总电力刻
     */
    public static void tickGrid() {
        MANAGER.tick();
    }

    /**
     * 电力刻
     */
    protected void tick() {
        if (this.level.getGameTime() % GRID_TICK != 0) return;
        if (this.isMarkedRemoval()) return;
        if (this.flush()) return;
        if (this.isWorking()) {
            int remainder = this.generate - this.consume;
            for (IPowerStorage storage : storages) {
                if (checkRemove(storage)) return;
                remainder = storage.insert(remainder);
                if (remainder <= 0) break;
            }
        } else {
            int need = this.consume - this.generate;
            Set<IPowerStorage> storages = Collections.synchronizedSet(new HashSet<>());
            for (IPowerStorage storage : this.storages) {
                need -= storage.getOutputPower();
                storages.add(storage);
                if (need <= 0) {
                    for (IPowerStorage selectStorage : storages) {
                        this.generate += selectStorage.extract(this.consume - this.generate);
                    }
                    break;
                }
            }
        }
        this.gridTick();
        this.update(false);
        changed = false;
    }

    private void gridTick() {
        components.forEach(IPowerComponent::gridTick);
        dynamicComponents.forEach(DynamicPowerComponent::gridTick);
    }

    private boolean checkRemove(IPowerComponent component) {
        if (component instanceof BlockEntity entity && entity.isRemoved()) {
            PowerGrid.removeComponent(component);
            return true;
        }
        return false;
    }

    public boolean flush() {
        final int oldGenerate = this.generate;
        final int oldConsume = this.consume;
        final boolean oldInfinitePower = this.hasInfinitePower;
        this.generate = 0;
        this.consume = 0;
        this.hasInfinitePower = false;
        for (IPowerTransmitter transmitter : transmitters) {
            if (checkRemove(transmitter)) {
                return true;
            }
        }
        for (IPowerProducer producer : this.producers) {
            if (checkRemove(producer)) {
                return true;
            }
            this.generate += producer.getOutputPower();
            if (producer.isInfinitePower()) {
                this.hasInfinitePower = true;
            }
        }
        for (IPowerConsumer consumer : this.consumers) {
            if (checkRemove(consumer)) {
                return true;
            }
            this.consume += consumer.getInputPower();
        }

        for (DynamicPowerComponent dynamicComponent : new ArrayList<>(this.dynamicComponents)) {
            Entity owner = dynamicComponent.getOwner();
            if (owner.level() != this.level || !this.collideFast(dynamicComponent.boundingBox())) {
                notifyLeaving(dynamicComponent);
                continue;
            }
            int power = dynamicComponent.getPowerConsumption();
            if (power > 0) {
                this.consume += power;
            } else {
                this.generate += power;
            }
        }

        if (this.consume != oldConsume || this.generate != oldGenerate || this.hasInfinitePower != oldInfinitePower) {
            this.changed = true;
        }
        return false;
    }

    public boolean inRangeFast(Vec3 pos) {
        return shape.inRange(pos);
    }

    public boolean collideFast(AABB box) {
        return shape.intersects(box);
    }

    /**
     * 是否正常工作（未过载）
     */
    public boolean isWorking() {
        return this.generate >= this.consume;
    }

    /**
     * 增加电力元件
     *
     * @param components 元件
     */
    public void add(IPowerComponent... components) {
        for (IPowerComponent component : components) {
            if (component.getComponentType() == PowerComponentType.INVALID) continue;
            if (this.components.contains(component)) continue;
            this.connect(component);
            if (component instanceof IPowerStorage storage) {
                this.storages.add(storage);
            } else {
                if (component instanceof IPowerProducer producer) {
                    this.producers.add(producer);
                }
                if (component instanceof IPowerConsumer consumer) {
                    this.consumers.add(consumer);
                }
                if (component instanceof IPowerTransmitter transmitter) {
                    this.transmitters.add(transmitter);
                }
            }
            component.setGrid(this);
            this.components.add(component);
            this.addRange(component);
        }
        this.flush();
        this.changed = true;
    }

    private void connect(IPowerComponent component) {
        Set<IPowerComponent> neighbors = new HashSet<>();
        AABB shape = component.getShape();
        for (IPowerComponent other : this.components) {
            if (!shape.intersects(other.getShape())) continue;
            neighbors.add(other);
            this.connections.computeIfAbsent(other, ignored -> new HashSet<>()).add(component);
        }
        this.connections.put(component, neighbors);
    }

    private void addRange(IPowerComponent component) {
        if (this.shape == null) {
            this.shape = new FastShape(List.of(component.getShape()));
            this.pos = component.getPos();
            return;
        }
        this.shape.add(component.getShape());
    }

    public void notifyLeaving(DynamicPowerComponent component) {
        this.dynamicComponents.remove(component);
    }

    public void notifyEntering(DynamicPowerComponent component) {
        this.dynamicComponents.add(component);
    }

    /**
     * 移除电网元件
     *
     * @param components 元件
     */
    public static void removeComponent(IPowerComponent... components) {
        try {
            if (PowerGrid.isServerClosing) return;
            for (IPowerComponent component : components) {
                PowerGrid grid = component.getGrid();
                if (grid == null) return;
                grid.remove(component);
            }
        } catch (Exception e) {
            AnvilCraft.LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * 移除电力元件
     *
     * @param components 电力元件
     */
    public void remove(IPowerComponent... components) {
        Set<IPowerComponent> removed = new HashSet<>();
        Set<IPowerComponent> boundary = new HashSet<>();
        for (IPowerComponent component : components) {
            if (component.getGrid() != this) continue;
            boundary.addAll(this.detach(component));
            removed.add(component);
        }
        if (removed.isEmpty()) return;
        if (this.components.isEmpty()) {
            for (DynamicPowerComponent dynamicComponent : new ArrayList<>(this.dynamicComponents)) {
                dynamicComponent.switchTo(null);
            }
            this.markedRemoval = true;
            PacketDistributor.sendToAllPlayers(new PowerGridRemovePacket(this));
            return;
        }

        boundary.removeAll(removed);
        List<PowerGrid> affectedGrids = new ArrayList<>();
        affectedGrids.add(this);
        if (!this.isBoundaryConnected(boundary)) {
            List<Set<IPowerComponent>> groups = this.findConnectedComponents();
            groups.sort(Comparator.comparingInt(Set<IPowerComponent>::size).reversed());
            groups.removeFirst();
            for (Set<IPowerComponent> group : groups) {
                for (IPowerComponent component : group) {
                    this.detach(component);
                }
                PowerGrid powerGrid = new PowerGrid(this.level);
                powerGrid.add(group.toArray(IPowerComponent[]::new));
                MANAGER.addGrid(powerGrid);
                affectedGrids.add(powerGrid);
            }
        }
        this.pos = this.components.iterator().next().getPos();
        this.reassignDynamicComponents(affectedGrids);
        for (PowerGrid powerGrid : affectedGrids) {
            powerGrid.flush();
            powerGrid.changed = false;
            PowerGridSyncChunkPacket.sendToAllPlayers(powerGrid);
        }
    }

    private Set<IPowerComponent> detach(IPowerComponent component) {
        Set<IPowerComponent> neighbors = this.connections.remove(component);
        if (neighbors != null) {
            for (IPowerComponent neighbor : neighbors) {
                Set<IPowerComponent> connections = this.connections.get(neighbor);
                if (connections != null) {
                    connections.remove(component);
                }
            }
        }
        Objects.requireNonNull(this.shape).remove(component.getShape());
        if (component instanceof IPowerStorage storage) {
            this.storages.remove(storage);
        } else {
            if (component instanceof IPowerProducer producer) {
                this.producers.remove(producer);
            }
            if (component instanceof IPowerConsumer consumer) {
                this.consumers.remove(consumer);
            }
            if (component instanceof IPowerTransmitter transmitter) {
                this.transmitters.remove(transmitter);
            }
        }
        this.components.remove(component);
        component.setGrid(null);
        return neighbors == null ? Set.of() : neighbors;
    }

    private boolean isBoundaryConnected(Set<IPowerComponent> boundary) {
        if (boundary.size() <= 1) return true;
        Set<IPowerComponent> targets = new HashSet<>(boundary);
        Set<IPowerComponent> visited = new HashSet<>();
        ArrayDeque<IPowerComponent> queue = new ArrayDeque<>();
        IPowerComponent start = targets.iterator().next();
        targets.remove(start);
        visited.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            IPowerComponent component = queue.removeFirst();
            for (IPowerComponent neighbor : this.connections.getOrDefault(component, Set.of())) {
                if (!visited.add(neighbor)) continue;
                if (targets.remove(neighbor) && targets.isEmpty()) {
                    return true;
                }
                queue.addLast(neighbor);
            }
        }
        return false;
    }

    private List<Set<IPowerComponent>> findConnectedComponents() {
        Set<IPowerComponent> remaining = new HashSet<>(this.components);
        List<Set<IPowerComponent>> groups = new ArrayList<>();
        while (!remaining.isEmpty()) {
            IPowerComponent start = remaining.iterator().next();
            Set<IPowerComponent> group = new HashSet<>();
            ArrayDeque<IPowerComponent> queue = new ArrayDeque<>();
            queue.add(start);
            remaining.remove(start);
            while (!queue.isEmpty()) {
                IPowerComponent component = queue.removeFirst();
                group.add(component);
                for (IPowerComponent neighbor : this.connections.getOrDefault(component, Set.of())) {
                    if (remaining.remove(neighbor)) {
                        queue.addLast(neighbor);
                    }
                }
            }
            groups.add(group);
        }
        return groups;
    }

    private void reassignDynamicComponents(List<PowerGrid> grids) {
        for (DynamicPowerComponent component : new ArrayList<>(this.dynamicComponents)) {
            PowerGrid target = null;
            for (PowerGrid grid : grids) {
                if (grid.collideFast(component.boundingBox())) {
                    target = grid;
                    break;
                }
            }
            component.switchTo(target);
        }
    }

    /**
     * 将另一个电网合并至当前电网
     *
     * @param grid 电网
     */
    public void merge(PowerGrid grid) {
        this.add(grid.components.toArray(IPowerComponent[]::new));
        for (DynamicPowerComponent component : new ArrayList<>(grid.dynamicComponents)) {
            component.switchTo(this);
        }
        changed = true;
    }

    /**
     * 判断元件是否在电网范围内
     *
     * @param component 元件
     * @return 元件是否在电网范围内
     */
    public boolean isInRange(IPowerComponent component) {
        return ConnectivityChecker.check(this, component);
    }

    /**
     * 增加电力元件
     *
     * @param components 元件
     */
    public static void addComponent(IPowerComponent... components) {
        for (IPowerComponent component : components) {
            MANAGER.addComponent(component);
        }
    }

    void syncToPlayer(ServerPlayer player) {
        PowerGridSyncChunkPacket.sendToPlayer(this, player);
    }

    public static Optional<PowerGrid> findPowerGridContains(Level level, Vec3 vec3) {
        Optional<PowerGrid> powerGrid = Optional.empty();
        for (PowerGrid it : MANAGER.getGridSet(level)) {
            if (it.inRangeFast(vec3)) {
                return Optional.of(it);
            }
        }
        return Optional.empty();
    }

    public static Optional<PowerGrid> findPowerGridContains(Level level, AABB vec3) {
        Optional<PowerGrid> powerGrid = Optional.empty();
        for (PowerGrid it : MANAGER.getGridSet(level)) {
            if (it.collideFast(vec3)) {
                return Optional.of(it);
            }
        }
        return Optional.empty();
    }

    /**
     * 清空电网
     */
    public static void clear() {
        MANAGER.clear();
    }

}
