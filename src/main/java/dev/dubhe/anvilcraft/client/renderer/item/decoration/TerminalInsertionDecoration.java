package dev.dubhe.anvilcraft.client.renderer.item.decoration;

import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.rpc.TerminalReachabilityCache;
import dev.dubhe.anvilcraft.item.TerminalItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

public final class TerminalInsertionDecoration implements IItemDecorator {
    @Override
    public boolean render(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        var client = Minecraft.getInstance();
        if (client.player == null || !(client.screen instanceof AbstractContainerScreen<?> screen)
            || screen instanceof StorageScreen || !(stack.getItem() instanceof TerminalItem terminal)) return false;
        ItemStack carried = screen.getMenu().getCarried();
        if (carried.isEmpty() || ItemStack.isSameItemSameComponents(stack, carried)) return false;
        var target = terminal.targetId(client.player, stack);
        if (target == null) return false;
        boolean inMenuSlot = false;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.hasItem() && slot.getItem() == stack) {
                inMenuSlot = true;
                break;
            }
        }
        if (!inMenuSlot) return false;
        if (terminal.kind() != TerminalItem.Kind.HYPERDIMENSION) {
            TerminalReachabilityCache.ensure(target);
            if (!Boolean.TRUE.equals(TerminalReachabilityCache.getReachability(target))) return false;
        }
        graphics.text(font, "+", x + 10, y + 1, 0xFFFFFFFF, true);
        return true;
    }
}
