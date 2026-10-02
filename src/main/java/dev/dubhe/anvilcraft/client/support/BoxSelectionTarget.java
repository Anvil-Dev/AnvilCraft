package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.network.BoxSelectionSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public record BoxSelectionTarget(int menuId, int slotIndex) {
    public static final BoxSelectionTarget NONE = new BoxSelectionTarget(-1, -1);

    public static BoxSelectionTarget of(AbstractContainerScreen<?> screen, Slot slot) {
        if (screen instanceof CreativeModeInventoryScreen || !slot.isActive()) return NONE;
        int index = screen.getMenu().slots.indexOf(slot);
        return index < 0 ? NONE : new BoxSelectionTarget(screen.getMenu().containerId, index);
    }

    public void send(ItemStack expected, int selection) {
        var client = Minecraft.getInstance();
        if (this.slotIndex < 0 || client.getConnection() == null
            || !(client.screen instanceof AbstractContainerScreen<?> screen) || screen instanceof CreativeModeInventoryScreen) return;
        var menu = screen.getMenu();
        if (menu.containerId != this.menuId || this.slotIndex >= menu.slots.size()
            || menu.getSlot(this.slotIndex).getItem() != expected) return;
        ClientPacketDistributor.sendToServer(new BoxSelectionSyncPacket(this.menuId, this.slotIndex, selection));
    }
}
