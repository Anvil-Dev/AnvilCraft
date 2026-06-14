package dev.dubhe.anvilcraft.api.energy;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * 基于 DataComponent 的 FE 能量存储实现
 * 使用 {@link ModComponents#STORED_ENERGY} 作为后端存储
 */
public class ItemFEStorage implements EnergyHandler {
    private final ItemStack stack;
    private final int capacity;

    public ItemFEStorage(ItemStack stack, int capacity) {
        this.stack = stack;
        this.capacity = capacity;
    }

    @Override
    public long getAmountAsLong() {
        return stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
    }

    @Override
    public long getCapacityAsLong() {
        return capacity;
    }

    @Override
    public int insert(int maxInsert, TransactionContext transaction) {
        int energy = stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
        int accepted = Math.min(maxInsert, capacity - energy);
        if (accepted > 0) {
            stack.set(ModComponents.STORED_ENERGY, new StoredEnergy(energy + accepted));
        }
        return accepted;
    }

    @Override
    public int extract(int maxExtract, TransactionContext transaction) {
        int energy = stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
        int extracted = Math.min(energy, maxExtract);
        if (extracted > 0) {
            stack.set(ModComponents.STORED_ENERGY, new StoredEnergy(energy - extracted));
        }
        return extracted;
    }
}
