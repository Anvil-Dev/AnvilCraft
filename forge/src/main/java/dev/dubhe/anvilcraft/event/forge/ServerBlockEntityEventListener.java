package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.event.forge.BlockEntityEvent;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.world.load.LevelLoadManager;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.OverseerBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ServerBlockEntityEventListener {
    /**
     * @param event 服务端方块实体加载事件
     */
    @SubscribeEvent
    public static void onLoad(@NotNull BlockEntityEvent.ServerLoad event) {
        if (event.getEntity() instanceof IPowerComponent component) {
            PowerGrid.addComponent(component);
        }
        if (event.getEntity() instanceof ChargeCollectorBlockEntity chargeCollector) {
            ChargeCollectorManager.getInstance(event.getLevel()).addChargeCollector(chargeCollector);
        }
    }

    /**
     * @param event 服务端方块实体卸载事件
     */
    @SubscribeEvent
    public static void onUnload(@NotNull BlockEntityEvent.ServerUnload event) {
        if (event.getEntity() instanceof IPowerComponent component) {
            PowerGrid.removeComponent(component);
        }
        if (event.getEntity() instanceof ChargeCollectorBlockEntity chargeCollector) {
            ChargeCollectorManager.getInstance(event.getLevel()).removeChargeCollector(chargeCollector);
        }
        if (event.getEntity() instanceof OverseerBlockEntity overseerBlockEntity) {
            LevelLoadManager.unregister(overseerBlockEntity.getBlockPos(), (ServerLevel) event.getLevel());
        }
    }
}
