package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.item.CrabClawItem;
import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Ferocious;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import dev.dubhe.anvilcraft.item.property.component.MultiphaseRef;
import dev.dubhe.anvilcraft.item.weapon.SpectralWeaponLauncherItem;
import dev.dubhe.anvilcraft.util.PlayerUtil;
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
            IonoCraftBackpackItem.playerTick(serverPlayer);
            SpectralWeaponLauncherItem.playerTick(serverPlayer);
            Merciless.tick(serverPlayer);
            Ferocious.tick(serverPlayer);
            Eternal.tick(serverPlayer);
        } else if (PlayerUtil.isClient(event.getEntity())) {
            MultiphaseRef.tick(event.getEntity());
        }
    }

    private static void applyPowerGrid(ServerPlayer player) {
        if (player instanceof IDynamicPowerComponentHolder holder) {
            PowerGrid powerGrid = PowerGrid.findPowerGridContains(
                player.level(),
                holder.anvilcraft$getPowerSupplyingBoundingBox()
            ).orElse(null);
            holder.anvilcraft$getPowerComponent().switchTo(powerGrid);
        }
    }
}
