package dev.dubhe.anvilcraft.inventory;

public interface HammerOpenedAnvilMenu {
    int anvilcraft$getOpenedHammerSlot();

    default boolean anvilcraft$isOpenedByHammer() {
        return this.anvilcraft$getOpenedHammerSlot() != HammerOpenedAnvilMenuHelper.NO_HAMMER_SLOT;
    }
}
