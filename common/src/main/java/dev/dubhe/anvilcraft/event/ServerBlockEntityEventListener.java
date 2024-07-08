package dev.dubhe.anvilcraft.event;

import dev.anvilcraft.lib.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.chargecollector.HeatedBlockRecorder;
import dev.dubhe.anvilcraft.api.event.server.block.ServerBlockEntityLoadEvent;
import dev.dubhe.anvilcraft.api.event.server.block.ServerBlockEntityUnloadEvent;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.world.load.LevelLoadManager;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import dev.dubhe.anvilcraft.block.entity.OverseerBlockEntity;

public class ServerBlockEntityEventListener {

    /**
     * @param event 服务端方块实体加载事件
     */
    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onLoad(ServerBlockEntityLoadEvent event) {
        if (event.getBlockEntity() instanceof IPowerComponent component) {
            PowerGrid.addComponent(component);
        }
        if (event.getBlockEntity() instanceof ChargeCollectorBlockEntity chargeCollector) {
            ChargeCollectorManager.getInstance(event.getServerLevel()).addChargeCollector(chargeCollector);
        }
    }

    /**
     * @param event 服务端方块实体卸载事件
     */
    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onUnload(ServerBlockEntityUnloadEvent event) {
        if (event.getBlockEntity() instanceof IPowerComponent component) {
            PowerGrid.removeComponent(component);
        }
        if (event.getBlockEntity() instanceof ChargeCollectorBlockEntity chargeCollector) {
            ChargeCollectorManager.getInstance(event.getServerLevel()).removeChargeCollector(chargeCollector);
            return;
        }
        if (event.getBlockEntity() instanceof OverseerBlockEntity overseerBlockEntity) {
            LevelLoadManager.unregister(overseerBlockEntity.getBlockPos(), event.getServerLevel());
            return;
        }
        if (event.getBlockEntity() instanceof HeliostatsBlockEntity heliostatsBlockEntity) {
            HeatedBlockRecorder.getInstance(event.getServerLevel()).remove(
                    heliostatsBlockEntity.getIrritatePos(),
                    heliostatsBlockEntity
            );
        }
    }
}
