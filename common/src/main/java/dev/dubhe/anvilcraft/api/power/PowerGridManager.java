package dev.dubhe.anvilcraft.api.power;

import dev.dubhe.anvilcraft.network.PowerGridRemovePack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PowerGridManager {
    private final Map<Level, Set<PowerGrid>> gridMap = Collections.synchronizedMap(new HashMap<>());
    private final LinkedBlockingQueue<Map.Entry<Level, IPowerComponent>> addQueue = new LinkedBlockingQueue<>();

    public PowerGridManager() {
    }

    /**
     * 添加电力组件
     */
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

    /**
     * 清除电网
     */
    public void clear() {
        gridMap.clear();
    }

    /**
     * tick
     */
    public synchronized void tick() {
        for (int i = 0; i < addQueue.size(); i++) {
            try {
                Map.Entry<Level, IPowerComponent> entry = addQueue.poll(500, TimeUnit.MICROSECONDS);
                if (entry == null) continue;
                IPowerComponent component = entry.getValue();
                if (component.getComponentType() == PowerComponentType.INVALID) continue;
                final PowerGrid[] grid = {null};
                Set<PowerGrid> grids = getGridSet(entry.getKey());
                Set<PowerGrid> remove = Collections.synchronizedSet(new HashSet<>());
                grids.forEach(powerGrid -> {
                    if (powerGrid.isMarkedRemoval() || !powerGrid.isInRange(component)) return;
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
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        for (Set<PowerGrid> grids : gridMap.values()) {
            Set<PowerGrid> remove = Collections.synchronizedSet(new HashSet<>());
            grids.forEach(powerGrid -> {
                if (powerGrid.isEmpty() || powerGrid.isMarkedRemoval()) remove.add(powerGrid);
                powerGrid.tick();
            });
            grids.removeAll(remove);
        }
    }

    private Set<PowerGrid> getGridSet(Level level) {
        if (gridMap.containsKey(level)) {
            return gridMap.get(level);
        } else {
            Set<PowerGrid> grids = Collections.synchronizedSet(new HashSet<>());
            gridMap.put(level, grids);
            return grids;
        }
    }

    /**
     * 玩家加入时同步电网至客户端
     */
    public void onPlayerJoined(Level level, ServerPlayer player) {
        Set<PowerGrid> grids = this.gridMap.get(level);
        if (grids == null) return;
        grids.forEach(it -> it.syncToPlayer(player));
    }
}
