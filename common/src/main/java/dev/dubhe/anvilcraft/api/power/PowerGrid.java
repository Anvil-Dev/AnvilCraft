package dev.dubhe.anvilcraft.api.power;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.network.PowerGridRemovePack;
import dev.dubhe.anvilcraft.network.PowerGridSyncPack;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 电网
 */
@SuppressWarnings("unused")
public class PowerGrid {
    public static boolean isServerClosing = false;
    private static final Map<Level, Set<PowerGrid>> GRID_MAP = Collections.synchronizedMap(new HashMap<>());
    public static final int GRID_TICK = 20;
    @Getter
    public boolean remove = false;
    @Getter
    private int generate = 0; // 发电功率
    @Getter
    private int consume = 0;  // 耗电功率
    final Set<IPowerProducer> producers = Collections.synchronizedSet(new HashSet<>()); // 发电机
    final Set<IPowerConsumer> consumers = Collections.synchronizedSet(new HashSet<>()); // 用电器
    final Set<IPowerStorage> storages = Collections.synchronizedSet(new HashSet<>());   // 储电
    final Set<IPowerTransmitter> transmitters = Collections.synchronizedSet(new HashSet<>());    // 中继
    @Getter
    private VoxelShape shape = null;
    @Getter
    private BlockPos pos = null;
    @Getter
    private final Level level;

    public PowerGrid(Level level) {
        this.level = level;
    }

    public void update() {
        new PowerGridSyncPack(this).broadcast(this.level.getChunkAt(this.getPos()));
    }

    /**
     * @return 获取电网中的元件数量
     */
    public int getComponentCount() {
        return this.transmitters.size() + this.producers.size() + this.consumers.size() + this.storages.size();
    }

    /**
     * @return 该电网是否为空电网
     */
    public boolean isEmpty() {
        return this.getComponentCount() <= 0;
    }

    /**
     * 总电力刻
     */
    public static void tickGrid() {
        for (Set<PowerGrid> grids : PowerGrid.GRID_MAP.values()) {
            Set<PowerGrid> remove = Collections.synchronizedSet(new HashSet<>());
            grids.forEach(powerGrid -> {
                if (powerGrid.isEmpty() || powerGrid.isRemove()) remove.add(powerGrid);
                powerGrid.tick();
            });
            grids.removeAll(remove);
        }
    }

    /**
     * 电力刻
     */
    protected void tick() {
        if (this.level.getGameTime() % GRID_TICK != 0) return;
        if (this.isRemove()) return;
        if (this.flush()) return;
        if (this.isWork()) {
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
                if (need <= 0) break;
            }
            if (need > 0) {
                this.update();
                return;
            }
            for (IPowerStorage storage : storages) {
                this.generate += storage.extract(this.consume - this.generate);
            }
        }
        this.gridTick();
        this.update();
    }

    private void gridTick() {
        Set<IPowerComponent> components = Collections.synchronizedSet(new HashSet<>());
        components.addAll(this.transmitters);
        components.addAll(this.consumers);
        components.addAll(this.storages);
        components.addAll(this.producers);
        components.forEach(IPowerComponent::gridTick);
    }

    private boolean checkRemove(IPowerComponent component) {
        if (component instanceof BlockEntity entity && entity.isRemoved()) {
            PowerGrid.removeComponent(component);
            return true;
        }
        return false;
    }

    private boolean flush() {
        this.generate = 0;
        this.consume = 0;
        for (IPowerTransmitter transmitter : transmitters) {
            if (checkRemove(transmitter)) return true;
        }
        for (IPowerProducer producer : this.producers) {
            if (checkRemove(producer)) return true;
            this.generate += producer.getOutputPower();
        }
        for (IPowerConsumer consumer : this.consumers) {
            if (checkRemove(consumer)) return true;
            this.consume += consumer.getInputPower();
        }
        return false;
    }

    /**
     * 是否正常工作（未过载）
     */
    public boolean isWork() {
        return this.generate >= this.consume;
    }

    /**
     * 增加电力元件
     *
     * @param components 元件
     */
    public void add(IPowerComponent @NotNull ... components) {
        for (IPowerComponent component : components) {
            if (component.getComponentType() == PowerComponentType.INVALID) continue;
            if (component instanceof IPowerStorage storage) {
                this.storages.add(storage);
                continue;
            }
            if (component instanceof IPowerProducer producer) {
                this.producers.add(producer);
            }
            if (component instanceof IPowerConsumer consumer) {
                this.consumers.add(consumer);
            }
            if (component instanceof IPowerTransmitter transmitter) {
                this.transmitters.add(transmitter);
            }
            component.setGrid(this);
            this.addRange(component);
        }
        this.flush();
    }

    private void addRange(IPowerComponent component) {
        if (this.shape == null) {
            this.shape = component.getShape();
            this.pos = component.getPos();
            return;
        }
        BlockPos center = this.pos;
        BlockPos vec3 = component.getPos();
        VoxelShape range = component.getShape().move(
            vec3.getX() - center.getX(),
            vec3.getY() - center.getY(),
            vec3.getZ() - center.getZ()
        );
        this.shape = Shapes.join(this.shape, range, BooleanOp.OR);
    }

    /**
     * 移除除电网元件
     *
     * @param components 元件
     */
    public static void removeComponent(IPowerComponent @NotNull ... components) {
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
    public void remove(IPowerComponent @NotNull ... components) {
        this.remove = true;
        Set<IPowerComponent> set = new LinkedHashSet<>();
        this.transmitters.stream().filter(this::clearGrid).forEach(set::add);
        this.transmitters.clear();
        this.storages.stream().filter(this::clearGrid).forEach(set::add);
        this.storages.clear();
        this.producers.stream().filter(this::clearGrid).forEach(set::add);
        this.producers.clear();
        this.consumers.stream().filter(this::clearGrid).forEach(set::add);
        this.consumers.clear();
        for (IPowerComponent component : components) {
            set.remove(component);
        }
        new PowerGridRemovePack(this).broadcast();
        PowerGrid.addComponent(set.toArray(IPowerComponent[]::new));
    }

    private boolean clearGrid(@NotNull IPowerComponent component) {
        component.setGrid(null);
        return true;
    }

    /**
     * 将另一个电网合并至当前电网
     *
     * @param grid 电网
     */
    public void merge(@NotNull PowerGrid grid) {
        grid.producers.forEach(this::add);
        grid.consumers.forEach(this::add);
        grid.storages.forEach(this::add);
        grid.transmitters.forEach(this::add);
    }

    /**
     * @param component 元件
     * @return 元件是否在电网范围内
     */
    public boolean isInRange(@NotNull IPowerComponent component) {
        BlockPos vec3 = component.getPos().subtract(this.getPos());
        VoxelShape range = Shapes.join(
            this.shape,
            component.getShape().move(vec3.getX(), vec3.getY(), vec3.getZ()),
            BooleanOp.AND
        );
        return !range.isEmpty();
    }

    /**
     * 增加电力元件
     *
     * @param components 元件
     */
    public static void addComponent(IPowerComponent @NotNull ... components) {

        for (IPowerComponent component : components) {
            if (component.getComponentType() == PowerComponentType.INVALID) continue;
            final PowerGrid[] grid = {null};
            Set<PowerGrid> grids = PowerGrid.getGridSet(component.getCurrentLevel());
            Set<PowerGrid> remove = Collections.synchronizedSet(new HashSet<>());
            grids.forEach(powerGrid -> {
                if (powerGrid.isRemove() || !powerGrid.isInRange(component)) return;
                if (grid[0] == null) grid[0] = powerGrid;
                else {
                    grid[0].merge(powerGrid);
                    remove.add(powerGrid);
                    new PowerGridRemovePack(powerGrid).broadcast();
                }
            });
            grids.removeAll(remove);
            if (grid[0] == null) grid[0] = new PowerGrid(component.getCurrentLevel());
            grid[0].add(component);
            grids.add(grid[0]);
        }

    }

    /**
     * 获取指定世界的电网集合
     *
     * @param level 世界
     * @return 电网集合
     */
    public static Set<PowerGrid> getGridSet(Level level) {
        if (PowerGrid.GRID_MAP.containsKey(level)) {
            return PowerGrid.GRID_MAP.get(level);
        } else {
            Set<PowerGrid> grids = Collections.synchronizedSet(new HashSet<>());
            PowerGrid.GRID_MAP.put(level, grids);
            return grids;
        }
    }

    /**
     * 清空电网
     */
    public static void clear() {
        PowerGrid.GRID_MAP.values().forEach(Collection::clear);
    }
}
