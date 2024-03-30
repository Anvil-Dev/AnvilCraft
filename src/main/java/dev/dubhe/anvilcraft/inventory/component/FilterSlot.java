package dev.dubhe.anvilcraft.inventory.component;

import dev.dubhe.anvilcraft.inventory.IFilterMenu;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FilterSlot extends Slot {
    IFilterMenu menu;

    public FilterSlot(Container container, int slot, int x, int y, IFilterMenu menu) {
        super(container, slot, x, y);
        this.menu = menu;
    }

    public boolean mayPlace(ItemStack stack) {
        return !this.menu.isSlotDisabled(this.index) && this.menu.filter(this.index, stack) && super.mayPlace(stack);
    }

    public void setChanged() {
        super.setChanged();
        this.menu.slotsChanged(this.container);
    }
}
