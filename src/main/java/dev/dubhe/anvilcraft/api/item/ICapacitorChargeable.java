package dev.dubhe.anvilcraft.api.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.Objects;

/**
 * 可被电容器充能的物品
 */
public interface ICapacitorChargeable {
    /**
     * 根据栈获取能量存储
     *
     * @param stack 物品栈
     * @return 能量存储
     */
    default IEnergyStorage getEnergyStorage(ItemStack stack) {
        return Objects.requireNonNull(
            stack.getCapability(Capabilities.EnergyStorage.ITEM),
            () -> "Cannot get the energy storage of %s. ".formatted(stack.getItemHolder().getRegisteredName())
                  + "Consider to register its capability or "
                  + "override dev.dubhe.anvilcraft.api.item.ICapacitorChargeable#getEnergyStorage("
                  + "Lnet/minecraft/world/item/ItemStack;"
                  + ")Lnet/neoforged/neoforge/energy/IEnergyStorage; method"
        );
    }

    /**
     * 尝试使用电容器充能
     *
     * @param stack 物品栈
     * @param capacitor 电容器
     * @param capacitorStack 电容器物品栈
     * @return 是否充能成功，也即是否消耗电容器
     * @implNote 若充能后能量值超出了最大容量，则不应充能
     */
    default boolean charge(ItemStack stack, IFullCapacitor capacitor, ItemStack capacitorStack) {
        if (!this.canAccept(stack, capacitor, capacitorStack, false)) {
            return false;
        }
        IEnergyStorage storage = this.getEnergyStorage(stack);
        if (!this.canBeCharged(stack, storage, capacitor, capacitorStack)) {
            return false;
        }

        int newStored = capacitor.getEnergyStored(capacitorStack);
        if (newStored <= 0 || storage.getEnergyStored() + newStored > storage.getMaxEnergyStored()) {
            return false;
        }

        if (storage.receiveEnergy(newStored, true) != newStored) {
            return false;
        }
        storage.receiveEnergy(newStored, false);
        return true;
    }

    /**
     * 强制使用电容器充能
     *
     * @param stack 物品栈
     * @param capacitor 电容器
     * @param capacitorStack 电容器物品栈
     * @return 是否充能成功，也即是否消耗电容器
     * @implNote 即便充能前电量未满，也应充能
     */
    default boolean chargeForce(ItemStack stack, IFullCapacitor capacitor, ItemStack capacitorStack) {
        if (!this.canAccept(stack, capacitor, capacitorStack, true)) {
            return false;
        }
        IEnergyStorage storage = this.getEnergyStorage(stack);
        if (!this.canBeCharged(stack, storage, capacitor, capacitorStack)) {
            return false;
        }
        int newStored = Math.min(capacitor.getEnergyStored(capacitorStack), storage.getMaxEnergyStored() - storage.getEnergyStored());
        if (newStored <= 0 || storage.receiveEnergy(newStored, true) != newStored) {
            return false;
        }
        storage.receiveEnergy(newStored, false);
        return true;
    }

    /**
     * 成功被电容器充能后调用
     *
     * @param stack 物品栈
     * @param capacitor 电容器
     * @param capacitorStack 电容器物品栈
     */
    default void onCharged(ItemStack stack, IFullCapacitor capacitor, ItemStack capacitorStack) {
    }

    /**
     * 判断电容器是否允许用于充能
     *
     * @param stack 物品栈
     * @param capacitor 电容器
     * @param capacitorStack 电容器物品栈
     * @param force 是否为强制充能
     * @return 该电容器是否允许用于充能
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean canAccept(ItemStack stack, IFullCapacitor capacitor, ItemStack capacitorStack, boolean force) {
        return true;
    }

    /**
     * 判断是否允许被充能
     *
     * @param stack 物品栈
     * @param storage 能量存储
     * @param capacitor 电容器
     * @param capacitorStack 电容器物品栈
     * @return 是否充能成功，也即是否消耗电容器
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean canBeCharged(ItemStack stack, IEnergyStorage storage, IFullCapacitor capacitor, ItemStack capacitorStack) {
        return storage.canReceive();
    }
}
