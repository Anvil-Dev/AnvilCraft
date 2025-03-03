package dev.dubhe.anvilcraft.api.power;

import dev.dubhe.anvilcraft.network.PowerGridRemovePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class PowerGridManager {
    private final Map<Level, Set<PowerGrid>> gridMap = Collections.synchronizedMap(new HashMap<>());
    private final LinkedBlockingQueue<Map.Entry<Level, IPowerComponent>> addQueue = new LinkedBlockingQueue<>();

    public PowerGridManager() {
    }

    public synchronized void addComponent(@NotNull IPowerComponent component) {
        try {
            addQueue.offer(Map.entry(component.getCurrentLevel(), component), 500, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void remove(PowerGrid powerGrid) {
        powerGrid.markedRemoval = true;
    }

    public synchronized void removeAll(Collection<PowerGrid> powerGrids) {
        powerGrids.forEach(this::remove);
    }

    public void clear() {
        gridMap.clear();
    }

    public synchronized void tick() {
        while (!addQueue.isEmpty()) {
            Map.Entry<Level, IPowerComponent> entry = addQueue.poll();
            if (entry == null) continue;
            IPowerComponent component = entry.getValue();
            if (component.getComponentType() == PowerComponentType.INVALID) continue;
            AtomicReference<PowerGrid> grid = new AtomicReference<>(null);
            Set<PowerGrid> grids = getGridSet(entry.getKey());
            Set<PowerGrid> remove = Collections.synchronizedSet(new HashSet<>());
            grids.forEach(powerGrid -> {
                if (powerGrid.isMarkedRemoval() || !powerGrid.isInRange(component)) return;
                if (grid.get() == null) {
                    grid.set(powerGrid);
                } else {
                    grid.get().merge(powerGrid);
                    remove.add(powerGrid);
                    PacketDistributor.sendToAllPlayers(new PowerGridRemovePacket(powerGrid));
                }
            });
            grids.removeAll(remove);
            if (grid.get() == null) {
                grid.set(new PowerGrid(component.getCurrentLevel()));
            }
            grid.get().add(component);
            grids.add(grid.get());
        }
        for (Set<PowerGrid> grids : gridMap.values()) {
            Set<PowerGrid> remove = Collections.synchronizedSet(new HashSet<>());
            grids.forEach(powerGrid -> {
                if (powerGrid.isEmpty() || powerGrid.isMarkedRemoval()) {
                    remove.add(powerGrid);
                }
                powerGrid.tick();
            });
            grids.removeAll(remove);
        }
    }

    public Set<PowerGrid> getGridSet(Level level) {
        if (gridMap.containsKey(level)) {
            return gridMap.get(level);
        } else {
            Set<PowerGrid> grids = Collections.synchronizedSet(new HashSet<>());
            gridMap.put(level, grids);
            return grids;
        }
    }

    public void onPlayerJoined(Level level, ServerPlayer player) {
        Set<PowerGrid> grids = this.gridMap.get(level);
        if (grids == null) return;
        grids.forEach(it -> it.syncToPlayer(player));
    }
}
