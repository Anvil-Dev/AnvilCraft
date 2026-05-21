package dev.dubhe.anvilcraft.inventory.container;

import dev.dubhe.anvilcraft.inventory.JewelCraftingMenu;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class JewelSourceContainer extends SimpleContainer {
    private final JewelCraftingMenu menu;

    public JewelSourceContainer(JewelCraftingMenu menu) {
        super(1);
        this.menu = menu;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        super.setItem(index, stack);
        this.menu.slotsChanged(this);
    }
}
