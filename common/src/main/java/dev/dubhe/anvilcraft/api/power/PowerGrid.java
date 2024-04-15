package dev.dubhe.anvilcraft.api.power;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 电网
 */
@SuppressWarnings("unused")
public class PowerGrid {
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
     * 总电力刻
     */
    public static void tickGrid() {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        for (PowerGrid grid : PowerGrid.GRID_SET) {
            grid.tick();
        }
        cooldown = GRID_TICK;
    }

    /**
     * 电力刻
     */
    protected void tick() {
        this.flush();
        if (this.isWork()) {
            int remainder = this.generate - this.consume;
            for (IPowerStorage storage : storages) {
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

    private void flush() {
        this.generate = 0;
        this.consume = 0;
        for (IPowerProducer producer : this.producers) {
            this.generate += producer.getOutputPower();
        }
        for (IPowerConsumer consumer : this.consumers) {
            this.consume += consumer.getInputPower();
        }
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
            } else if (component instanceof IPowerProducer producer) {
                this.producers.add(producer);
            } else if (component instanceof IPowerConsumer consumer) {
                this.consumers.add(consumer);
            } else if (component instanceof IPowerTransmitter transmitter) {
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
     * 移除电力元件
     *
     * @param components 电力元件
     */
    public void remove(IPowerComponent @NotNull ... components) {
        Set<IPowerComponent> set = new HashSet<>();
        this.storages.stream().filter(this::clearGrid).forEach(set::add);
        this.storages.clear();
        this.producers.stream().filter(this::clearGrid).forEach(set::add);
        this.producers.clear();
        this.consumers.stream().filter(this::clearGrid).forEach(set::add);
        this.consumers.clear();
        this.transmitters.stream().filter(this::clearGrid).forEach(set::add);
        this.transmitters.clear();
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
        this.producers.addAll(grid.producers);
        this.consumers.addAll(grid.consumers);
        this.storages.addAll(grid.storages);
        this.transmitters.addAll(grid.transmitters);
    }

    /**
     * @param component 元件
     * @return 元件是否在电网范围内
     */
    public boolean isInRange(@NotNull IPowerComponent component) {
        BlockPos vec3 = component.getPos();
        BlockPos center = this.getPos();
        VoxelShape range = Shapes.join(
            this.range.move(center.getX(), center.getY(), center.getZ()),
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
