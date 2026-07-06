package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.inventory.ControlValveMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;

public class ControlValveScreen extends AbstractContainerScreen<ControlValveMenu> {
    private int value;
    private FluidStack filter = FluidStack.EMPTY;

    public ControlValveScreen(ControlValveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setFilter(int index, FluidStack fluid) {
        this.filter = fluid;
    }
}
