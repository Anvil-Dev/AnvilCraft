package dev.dubhe.anvilcraft.api.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Objects;

public interface ICapacitorChargeable {
    default EnergyHandler getEnergyStorage(ItemStack stack) {
        return Objects.requireNonNull(ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM),
            () -> "Missing energy capability for " + stack.getItem());
    }

    default boolean charge(ItemStack stack, IFullCapacitor capacitor, ItemStack capacitorStack) {
        if (!this.canAccept(stack, capacitor, capacitorStack, false)) return false;
        EnergyHandler storage = this.getEnergyStorage(stack);
        if (!this.canBeCharged(stack, storage, capacitor, capacitorStack)) return false;
        int energy = capacitor.getEnergyStored(capacitorStack);
        if (energy <= 0 || energy > storage.getCapacityAsLong() - storage.getAmountAsLong()) return false;
        try (var transaction = Transaction.openRoot()) {
            if (storage.insert(energy, transaction) != energy) return false;
            transaction.commit();
            return true;
        }
    }

    default boolean chargeForce(ItemStack stack, IFullCapacitor capacitor, ItemStack capacitorStack) {
        if (!this.canAccept(stack, capacitor, capacitorStack, true)) return false;
        EnergyHandler storage = this.getEnergyStorage(stack);
        if (!this.canBeCharged(stack, storage, capacitor, capacitorStack)) return false;
        long room = Math.max(0L, storage.getCapacityAsLong() - storage.getAmountAsLong());
        int energy = (int) Math.min(capacitor.getEnergyStored(capacitorStack), room);
        if (energy <= 0) return false;
        try (var transaction = Transaction.openRoot()) {
            if (storage.insert(energy, transaction) != energy) return false;
            transaction.commit();
            return true;
        }
    }

    default void onCharged(ItemStack stack, IFullCapacitor capacitor, ItemStack capacitorStack) {
    }

    default boolean canAccept(ItemStack stack, IFullCapacitor capacitor, ItemStack capacitorStack, boolean force) {
        return true;
    }

    default boolean canBeCharged(ItemStack stack, EnergyHandler storage, IFullCapacitor capacitor, ItemStack capacitorStack) {
        try (var transaction = Transaction.openRoot()) {
            return storage.insert(Math.max(0, capacitor.getEnergyStored(capacitorStack)), transaction) > 0;
        }
    }
}
