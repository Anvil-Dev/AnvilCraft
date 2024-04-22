package dev.dubhe.anvilcraft.api.power;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.network.PowerGridRemovePack;
import dev.dubhe.anvilcraft.network.PowerGridSyncPack;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 电网
 */
@SuppressWarnings("unused")
public class PowerGrid {
    public static boolean isServerClosing = false;
    public static final Map<Level, Set<PowerGrid>> GRID_MAP = new HashMap<>();
    public static final int GRID_TICK = 20;
    public static int cooldown = 0; // 电网刷新冷却
    @Getter
    public boolean remove = false;
    @Getter
    private int generate = 0; // 发电功率
    @Getter
    private int consume = 0;  // 耗电功率
    private final Set<IPowerProducer> producers = new HashSet<>(); // 发电机
    private final Set<IPowerConsumer> consumers = new HashSet<>(); // 用电器
    private final Set<IPowerStorage> storages = new HashSet<>();   // 储电
    private final Set<IPowerTransmitter> transmitters = new HashSet<>();    // 中继
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
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        for (Set<PowerGrid> grids : PowerGrid.GRID_MAP.values()) {
            Iterator<PowerGrid> iterator = grids.iterator();
            while (iterator.hasNext()) {
                PowerGrid grid = iterator.next();
                if (grid.isEmpty()) iterator.remove();
                grid.tick();
            }
        }
        cooldown = GRID_TICK;
    }

    /**
     * 电力刻
     */
    protected void tick() {
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
        this.update();
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
        PowerGrid.getGridSet(this.level).remove(this);
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
            PowerGrid grid = null;
            Set<PowerGrid> grids = PowerGrid.getGridSet(component.getCurrentLevel());
            Iterator<PowerGrid> iterator = grids.iterator();
            while (iterator.hasNext()) {
                PowerGrid grid1 = iterator.next();
                if (!grid1.isInRange(component)) continue;
                if (grid == null) grid = grid1;
                else {
                    grid.merge(grid1);
                    iterator.remove();
                    new PowerGridRemovePack(grid1).broadcast();
                }
            }
            if (grid == null) grid = new PowerGrid(component.getCurrentLevel());
            grid.add(component);
            grids.add(grid);
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
            Set<PowerGrid> grids = new HashSet<>();
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

    @Getter
    public static class SimplePowerGrid {
        private final int hash;
        private final String level;
        private final BlockPos pos;
        private final Map<BlockPos, Integer> ranges = new HashMap<>();

        /**
         * @param buf 缓冲区
         */
        public void encode(@NotNull FriendlyByteBuf buf) {
            buf.writeInt(this.hash);
            buf.writeUtf(this.level);
            buf.writeBlockPos(this.pos);
            Set<Map.Entry<BlockPos, Integer>> set = this.ranges.entrySet();
            buf.writeVarInt(set.size());
            for (Map.Entry<BlockPos, Integer> entry : set) {
                buf.writeBlockPos(entry.getKey());
                buf.writeInt(entry.getValue());
            }
        }

        /**
         * @param buf 缓冲区
         */
        public SimplePowerGrid(@NotNull FriendlyByteBuf buf) {
            this.hash = buf.readInt();
            this.level = buf.readUtf();
            this.pos = buf.readBlockPos();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) {
                this.ranges.put(buf.readBlockPos(), buf.readInt());
            }
        }

        /**
         * @param grid 电网
         */
        public SimplePowerGrid(@NotNull PowerGrid grid) {
            this.hash = grid.hashCode();
            this.level = grid.getLevel().dimension().location().toString();
            this.pos = grid.getPos();
            grid.storages.forEach(this::addRange);
            grid.producers.forEach(this::addRange);
            grid.consumers.forEach(this::addRange);
            grid.transmitters.forEach(this::addRange);
        }

        public void addRange(@NotNull IPowerComponent component) {
            this.ranges.put(component.getPos(), component.getRange());
        }

        /**
         * @return 获取范围
         */
        public VoxelShape getShape() {
            return this.ranges.entrySet().stream().map(entry -> Shapes
                .box(
                    -entry.getValue(), -entry.getValue(), -entry.getValue(),
                    entry.getValue() + 1, entry.getValue() + 1, entry.getValue() + 1
                )
                .move(
                    this.offset(entry.getKey()).getX(),
                    this.offset(entry.getKey()).getY(),
                    this.offset(entry.getKey()).getZ()
                )
            ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).orElse(Shapes.block());
        }

        private @NotNull BlockPos offset(@NotNull BlockPos pos) {
            return pos.subtract(this.pos);
        }
    }
}
