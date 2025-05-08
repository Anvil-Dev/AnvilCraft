package dev.dubhe.anvilcraft.api.power;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.network.PowerGridRemovePacket;
import dev.dubhe.anvilcraft.network.PowerGridSyncPacket;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
    final Set<IPowerComponent> components = Collections.synchronizedSet(new HashSet<>());
    final Set<IPowerProducer> producers = Collections.synchronizedSet(new HashSet<>()); // 发电机
    final Set<IPowerConsumer> consumers = Collections.synchronizedSet(new HashSet<>()); // 用电器
    final Set<IPowerStorage> storages = Collections.synchronizedSet(new HashSet<>()); // 储电
    final Set<IPowerTransmitter> transmitters = Collections.synchronizedSet(new HashSet<>()); // 中继

    final Set<DynamicPowerComponent> dynamicComponents = Collections.synchronizedSet(new HashSet<>());

    @Getter
    private FastShape shape = null;

    @Getter
    private BlockPos pos = null;

    @Getter
    private final Level level;

    public PowerGrid(Level level) {
        this.level = level;
    }

    /**
     *
     */
    public void update(boolean forced) {
        if (forced || changed) {
            PacketDistributor.sendToPlayersTrackingChunk(
                (ServerLevel) level,
                this.level.getChunkAt(this.getPos()).getPos(),
                new PowerGridSyncPacket(this)
            );
        }
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
        int oldGenerate = this.generate;
        int oldConsume = this.consume;
        this.generate = 0;
        this.consume = 0;
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

        if (this.consume != oldConsume || this.generate != oldGenerate) {
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
            this.components.add(component);
            this.addRange(component);
        }
        this.flush();
        this.changed = true;
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
        this.markedRemoval = true;
        for (IPowerComponent component : this.components) {
            component.setGrid(null);
        }
        Set<IPowerComponent> set = new HashSet<>(this.components);
        this.transmitters.clear();
        this.storages.clear();
        this.producers.clear();
        this.consumers.clear();
        this.components.clear();
        for (IPowerComponent component : components) {
            set.remove(component);
        }
        PacketDistributor.sendToAllPlayers(new PowerGridRemovePacket(this));
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
        changed = true;
    }

    /**
     * @param component 元件
     * @return 元件是否在电网范围内
     */
    public boolean isInRange(@NotNull IPowerComponent component) {
        return collideFast(component.getShape());
    }

    /**
     * 增加电力元件
     *
     * @param components 元件
     */
    public static void addComponent(IPowerComponent @NotNull ... components) {
        for (IPowerComponent component : components) {
            MANAGER.addComponent(component);
        }
    }

    void syncToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new PowerGridSyncPacket(this));
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
