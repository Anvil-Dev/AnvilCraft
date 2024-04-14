package dev.dubhe.anvilcraft.api.power;

import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 电网
 */
@SuppressWarnings("unused")
public class PowerGrid {
    public static final List<PowerGrid> GRID_LIST = new ArrayList<>();
    public static final int GRID_TICK = 40;
    public static int cooldown = 0;
    @Getter
    private int generate = 0; // 发电功率
    @Getter
    private int consume = 0;  // 耗电功率
    private final List<IPowerProducer> producers = new ArrayList<>(); // 发电机
    private final List<IPowerConsumer> consumers = new ArrayList<>(); // 用电器
    private final List<IPowerStorage> storages = new ArrayList<>();   // 储电
    private final List<IPowerTransmitter> transmitters = new ArrayList<>();    // 中继

    /**
     * 总电力刻
     */
    public static void tickGrid() {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        for (PowerGrid grid : PowerGrid.GRID_LIST) {
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
            List<IPowerStorage> storages = new ArrayList<>();
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
        }
        this.flush();
    }

    /**
     * 移除电力元件
     *
     * @param components 电力元件
     */
    public void remove(IPowerComponent @NotNull ... components) {
        List<IPowerComponent> list = new ArrayList<>();
        list.addAll(this.storages);
        list.addAll(this.producers);
        list.addAll(this.consumers);
        list.addAll(this.transmitters);
        for (IPowerComponent component : components) {
            list.remove(component);
        }
        PowerGrid.GRID_LIST.remove(this);
        PowerGrid.addComponent(list.toArray(IPowerComponent[]::new));
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
        PowerGrid.GRID_LIST.remove(grid);
    }

    /**
     * @param component 元件
     * @return 元件是否在电网范围内
     */
    public boolean isInRange(@NotNull IPowerComponent component) {
        int range;
        BlockPos pos = component.getPos();
        if (component instanceof IPowerTransmitter) {
            range = AnvilCraft.config.powerTransmitterRange;
        } else range = AnvilCraft.config.powerComponentRange;
        for (IPowerTransmitter transmitter : this.transmitters) {
            pos = pos.subtract(transmitter.getPos());
            if (pos.getX() > range || pos.getY() > range || pos.getZ() > range) continue;
            return true;
        }
        return false;
    }

    /**
     * 增加电力元件
     *
     * @param components 元件
     */
    public static void addComponent(IPowerComponent @NotNull ... components) {
        for (IPowerComponent component : components) {
            PowerGrid grid = null;
            for (PowerGrid grid1 : PowerGrid.GRID_LIST) {
                if (!grid1.isInRange(component)) continue;
                if (grid == null) grid = grid1;
                else grid.merge(grid1);
            }
            if (grid == null) grid = new PowerGrid();
            grid.add(component);
            PowerGrid.GRID_LIST.add(grid);
        }
    }
}
