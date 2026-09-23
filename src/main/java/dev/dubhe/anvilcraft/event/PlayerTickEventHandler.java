package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Ferocious;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import dev.dubhe.anvilcraft.item.tool.DragonRodItem;
import dev.dubhe.anvilcraft.item.weapon.LaserGunItem;
import dev.dubhe.anvilcraft.network.PlayerSettingsSyncPacket;
import dev.dubhe.anvilcraft.rpc.BundleLikeServerStub;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.util.dummy.DummyCat;
import dev.dubhe.anvilcraft.util.dummy.DummyWolf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber
public class PlayerTickEventHandler {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(
                serverPlayer,
                new PlayerSettingsSyncPacket(PlayerSettings.getSetting(serverPlayer.registryAccess(), serverPlayer.nameAndId()))
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        DummyCat.clear(player);
        DummyWolf.clear(player);
        if (player instanceof ServerPlayer serverPlayer) {
            IonoCraftBackpackItem.onPlayerLoggedOut(serverPlayer.getUUID());
            LaserGunItem.clearState(serverPlayer.getUUID());
            StorageServerStub.remove(serverPlayer.getUUID());
            BundleLikeServerStub.clear(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        IonoCraftBackpackItem.applySlowFalling(event.getEntity());
        BuildingRodItem.updateReach(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerTickEventHandler.applyPowerGrid(serverPlayer);
            IonoCraftBackpackItem.playerTick(serverPlayer);
            Merciless.tick(serverPlayer);
            Ferocious.tick(serverPlayer);
            Eternal.tick(serverPlayer);
            DragonRodItem.tickContinuousDevour(serverPlayer);
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
