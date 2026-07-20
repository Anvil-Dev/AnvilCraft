package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Ferocious;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.item.tool.DragonRodItem;
import dev.dubhe.anvilcraft.item.weapon.LaserGunItem;
import dev.dubhe.anvilcraft.item.weapon.SpectralWeaponLauncherItem;
import dev.dubhe.anvilcraft.network.PlayerSettingsSyncPacket;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

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
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            IonoCraftBackpackItem.onPlayerLoggedOut(serverPlayer.getUUID());
            LaserGunItem.clearState(serverPlayer.getUUID());
            StorageServerStub.remove(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            applyPowerGrid(serverPlayer);
            IonoCraftBackpackItem.playerTick(serverPlayer);
            handleCapacitorCharging(serverPlayer);
            SpectralWeaponLauncherItem.playerTick(serverPlayer);
            Merciless.tick(serverPlayer);
            Ferocious.tick(serverPlayer);
            Eternal.tick(serverPlayer);
            DragonRodItem.tickContinuousDevour(serverPlayer);
        }
    }

    private static void handleCapacitorCharging(ServerPlayer player) {
        Inventory inventory = player.getInventory();

        // 先找电容器，再找超级电容器
        int capacitorSlot = inventory.findSlotMatchingItem(ModItems.CAPACITOR.asStack());
        boolean hasCapacitor = capacitorSlot >= 0;
        if (!hasCapacitor) {
            capacitorSlot = inventory.findSlotMatchingItem(ModItems.SUPER_CAPACITOR.asStack());
        }
        if (capacitorSlot < 0) return;

        int energyAmount = hasCapacitor ? 8_000_000 : 160_000_000;
        ItemStack emptyCapacitor = hasCapacitor
            ? ModItems.CAPACITOR_EMPTY.asStack()
            : ModItems.SUPER_CAPACITOR_EMPTY.asStack();

        // 操作 STORED_ENERGY 组件，为物品充能
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item.isEmpty()) continue;
            if (!item.has(ModComponents.STORED_ENERGY)) continue;

            // 获取物品的最大容量
            @SuppressWarnings("UnstableApiUsage")
            EnergyHandler storage = Capabilities.Energy.ITEM.getCapability(item, ItemAccess.forStack(item));
            if (storage == null) continue;

            int current = item.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
            int maxEnergy = storage.getCapacityAsInt();
            int added = Math.min(energyAmount, maxEnergy - current);
            if (added <= 0) continue;

            item.set(ModComponents.STORED_ENERGY, new StoredEnergy(current + added));
            inventory.removeItem(capacitorSlot, 1);
            inventory.placeItemBackInInventory(emptyCapacitor);
            return;
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
