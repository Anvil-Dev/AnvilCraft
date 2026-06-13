package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.api.power.IDynamicPowerComponentHolder;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.CrabClawItem;
import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Ferocious;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import dev.dubhe.anvilcraft.item.property.component.MultiphaseRef;
import dev.dubhe.anvilcraft.item.weapon.SpectralWeaponLauncherItem;
import dev.dubhe.anvilcraft.util.PlayerUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class PlayerTickEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        CrabClawItem.holdingCrabClawIncreasesRange(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            applyPowerGrid(serverPlayer);
            IonoCraftBackpackItem.playerTick(serverPlayer);
            handleCapacitorCharging(serverPlayer);
            SpectralWeaponLauncherItem.playerTick(serverPlayer);
            Merciless.tick(serverPlayer);
            Ferocious.tick(serverPlayer);
            Eternal.tick(serverPlayer);
        } else if (PlayerUtil.isClient(event.getEntity())) {
            MultiphaseRef.tick(event.getEntity());
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

        // 直接操作 STORED_ENERGY 组件，绕过充能速率限制
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item.isEmpty()) continue;
            if (!item.has(ModComponents.STORED_ENERGY)) continue;

            // 获取物品的最大容量
            IEnergyStorage storage = item.getCapability(Capabilities.EnergyStorage.ITEM);
            if (storage == null) continue;

            int current = item.getOrDefault(ModComponents.STORED_ENERGY, 0);
            int maxEnergy = storage.getMaxEnergyStored();
            int added = Math.min(energyAmount, maxEnergy - current);
            if (added <= 0) continue;

            item.set(ModComponents.STORED_ENERGY, current + added);
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
