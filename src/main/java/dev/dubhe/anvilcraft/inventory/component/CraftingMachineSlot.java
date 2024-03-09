package dev.dubhe.anvilcraft.inventory.component;

import dev.dubhe.anvilcraft.inventory.CraftingMachineMenu;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CraftingMachineSlot extends Slot {
    CraftingMachineMenu menu;
    public CraftingMachineSlot(Container container, int slot, int x, int y,CraftingMachineMenu menu) {
        super(container, slot, x, y);
        this.menu = menu;
    }

    public boolean mayPlace(ItemStack itemStack) {
        return !this.menu.isSlotDisabled(this.index) && super.mayPlace(itemStack);
    }

    public void setChanged() {
        super.setChanged();
        this.menu.slotsChanged(this.container);
    }
}
