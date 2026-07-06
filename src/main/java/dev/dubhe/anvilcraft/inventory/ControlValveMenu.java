package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class ControlValveMenu extends AbstractContainerMenu {
    private final ControlValveBlockEntity be;

    public ControlValveMenu(MenuType<?> type, int containerId, Inventory inventory) {
        super(type, containerId);
        this.be = null;
    }

    public ControlValveMenu(int containerId, Inventory inventory, ControlValveBlockEntity be) {
        super(null, containerId);
        this.be = be;
    }

    public ControlValveBlockEntity getBlockEntity() {
        return this.be;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
