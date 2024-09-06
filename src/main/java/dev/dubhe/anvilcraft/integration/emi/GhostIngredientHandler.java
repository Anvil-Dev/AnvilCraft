package dev.dubhe.anvilcraft.integration.emi;

import dev.dubhe.anvilcraft.client.gui.screen.inventory.IFilterScreen;
import dev.dubhe.anvilcraft.network.SlotFilterChangePack;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GhostIngredientHandler<T extends Screen & IFilterScreen<?>> implements EmiDragDropHandler<T> {
    @Override
    public boolean dropStack(
        @NotNull T screen, @NotNull EmiIngredient ingredient, int x, int y
    ) {
        if (!screen.isFilterEnabled()) return false;
        List<EmiStack> stacks = ingredient.getEmiStacks();
        if (stacks.size() != 1) return false;
        ItemStack stack = stacks.get(0).getItemStack();
        if (stack.isEmpty()) return false;
        for (Slot slot : screen.getFilterMenu().slots) {
            Rect2i rect2i = new Rect2i(screen.getOffsetX() + slot.x, screen.getOffsetY() + slot.y, 16, 16);
            if (rect2i.contains(x, y)) {
                if (screen.setFilter(screen.getFilterMenu().getFilterSlotIndex(slot), stack)) {
                    new SlotFilterChangePack(screen.getFilterMenu().getFilterSlotIndex(slot), stack).send();
                    return true;
                }
            }
        }
        return false;
    }
}
