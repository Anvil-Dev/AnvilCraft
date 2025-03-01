package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.item.CrabClawItem;
import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class PlayerTickEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        CrabClawItem.holdingCrabClawIncreasesRange(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            applyPowerGrid(serverPlayer);
            IonoCraftBackpackItem.flightTick(serverPlayer);
        }
    }

    private static void applyPowerGrid(ServerPlayer player) {
        if (player instanceof IDynamicPowerComponentHolder holder) {
            PowerGrid powerGrid = PowerGrid.findPowerGridContains(
                player.level(),
                holder.anvilCraft$getPowerSupplyingBoundingBox()
            ).orElse(null);
            holder.anvilCraft$getPowerComponent().switchTo(powerGrid);
        }
    }
}
