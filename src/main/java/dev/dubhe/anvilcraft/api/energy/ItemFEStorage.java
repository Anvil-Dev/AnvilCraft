package dev.dubhe.anvilcraft.api.energy;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * 基于 DataComponent 的 FE 能量存储实现
 * 使用 {@link ModComponents#STORED_ENERGY} 作为后端存储
 */
public class ItemFEStorage implements IEnergyStorage {
    private final ItemStack stack;
    private final int capacity;

    public ItemFEStorage(ItemStack stack, int capacity) {
        this.stack = stack;
        this.capacity = capacity;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int energy = stack.getOrDefault(ModComponents.STORED_ENERGY, 0);
        int accepted = Math.min(maxReceive, capacity - energy);
        if (!simulate && accepted > 0) {
            stack.set(ModComponents.STORED_ENERGY, energy + accepted);
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int energy = stack.getOrDefault(ModComponents.STORED_ENERGY, 0);
        int extracted = Math.min(energy, maxExtract);
        if (!simulate && extracted > 0) {
            stack.set(ModComponents.STORED_ENERGY, energy - extracted);
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return stack.getOrDefault(ModComponents.STORED_ENERGY, 0);
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return getEnergyStored() > 0;
    }

    @Override
    public boolean canReceive() {
        return getEnergyStored() < capacity;
    }
}
