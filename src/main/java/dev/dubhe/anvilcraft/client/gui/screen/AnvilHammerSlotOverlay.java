package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.inventory.HammerOpenedAnvilMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class AnvilHammerSlotOverlay {
    private static final int COLOR = 0x66FFD800;

    private AnvilHammerSlotOverlay() {
    }

    public static void render(GuiGraphics guiGraphics, HammerOpenedAnvilMenu menu, Slot slot) {
        if (!menu.anvilcraft$isOpenedByHammer()) return;
        if (Minecraft.getInstance().player == null) return;
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        if (slot.container != inventory) return;
        if (slot.getContainerSlot() != menu.anvilcraft$getOpenedHammerSlot()) return;
        if (!slot.getItem().is(ModItemTags.ANVIL_HAMMER)) return;
        guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, COLOR);
    }
}
