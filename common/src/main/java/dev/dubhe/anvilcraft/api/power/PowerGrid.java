package dev.dubhe.anvilcraft.api.power;

import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 电网
 */
@SuppressWarnings("unused")
public class PowerGrid {
    public static boolean isServerClosing = false;
    @Getter
    private static Set<PowerGrid> gridSetClient = Collections.synchronizedSet(new HashSet<>());
    public static final Set<PowerGrid> GRID_SET = new HashSet<>();
    public static final int GRID_TICK = 40;
    public static int cooldown = 0;
    @Getter
    private int generate = 0; // 发电功率
    @Getter
    private int consume = 0;  // 耗电功率
    private final Set<IPowerProducer> producers = new HashSet<>(); // 发电机
    private final Set<IPowerConsumer> consumers = new HashSet<>(); // 用电器
    private final Set<IPowerStorage> storages = new HashSet<>();   // 储电
    private final Set<IPowerTransmitter> transmitters = new HashSet<>();    // 中继
    @Getter
    private VoxelShape range = null;
    @Getter
    private BlockPos pos = null;

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
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        Iterator<PowerGrid> iterator = PowerGrid.GRID_SET.iterator();
        while (iterator.hasNext()) {
            PowerGrid grid = iterator.next();
            if (grid.isEmpty()) iterator.remove();
            grid.tick();
        }
        cooldown = GRID_TICK;
        gridSetClient = Set.copyOf(GRID_SET);
    }

    /**
     * 电力刻
     */
    protected void tick() {
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
            Set<IPowerStorage> storages = new HashSet<>();
            for (IPowerStorage storage : this.storages) {
                need -= storage.getOutputPower();
                storages.add(storage);
                if (need <= 0) break;
            }
            if (need > 0) return;
            for (IPowerStorage storage : storages) {
                this.generate += storage.extract(this.consume - this.generate);
            }
        }
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
        if (this.range == null) {
            this.range = component.getRange();
            this.pos = component.getPos();
            return;
        }
        BlockPos center = this.pos;
        BlockPos vec3 = component.getPos();
        VoxelShape range = component.getRange().move(
            vec3.getX() - center.getX(),
            vec3.getY() - center.getY(),
            vec3.getZ() - center.getZ()
        );
        this.range = Shapes.join(this.range, range, BooleanOp.OR);
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
        PowerGrid.GRID_SET.remove(this);
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
            this.range,
            component.getRange().move(vec3.getX(), vec3.getY(), vec3.getZ()),
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
            PowerGrid grid = null;
            Iterator<PowerGrid> iterator = PowerGrid.GRID_SET.iterator();
            while (iterator.hasNext()) {
                PowerGrid grid1 = iterator.next();
                if (!grid1.isInRange(component)) continue;
                if (grid == null) grid = grid1;
                else {
                    grid.merge(grid1);
                    iterator.remove();
                }
            }
            if (grid == null) grid = new PowerGrid();
            grid.add(component);
            PowerGrid.GRID_SET.add(grid);
        }
    }

    /**
     * 清空电网
     */
    public static void clear() {
        PowerGrid.GRID_SET.clear();
    }
}
