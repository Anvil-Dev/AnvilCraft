package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.item.weapon.EnergyWeaponItem;
import dev.dubhe.anvilcraft.item.weapon.SpectralWeaponLauncherItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

final class CapacitorInventoryCharge {
    private CapacitorInventoryCharge() {
    }

    static boolean tryChargeTarget(
        ItemStack capacitor,
        Slot slot,
        ClickAction clickAction,
        Player player,
        int energy,
        ItemStack emptyCapacitor
    ) {
        if (clickAction != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        ItemStack target = slot.getItem();
        if (!isChargeTarget(target)) return false;

        IEnergyStorage storage = target.getCapability(Capabilities.EnergyStorage.ITEM);
        if (storage == null || !storage.canReceive()) return false;

        int accepted = storage.receiveEnergy(energy, false);
        if (accepted <= 0) return false;

        capacitor.shrink(1);
        player.getInventory().placeItemBackInInventory(emptyCapacitor.copy());
        updateChargedModel(target);
        slot.setChanged();
        return true;
    }

    private static boolean isChargeTarget(ItemStack stack) {
        return stack.getItem() instanceof IonoCraftBackpackItem
            || stack.getItem() instanceof EnergyWeaponItem
            || stack.getItem() instanceof SpectralWeaponLauncherItem;
    }

    private static void updateChargedModel(ItemStack stack) {
        if (stack.getItem() instanceof EnergyWeaponItem) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.DEFAULT);
        }
    }
}
