package dev.dubhe.anvilcraft.integration.rei.client;

import dev.dubhe.anvilcraft.client.gui.screen.inventory.IFilterScreen;
import dev.dubhe.anvilcraft.network.SlotFilterChangePack;
import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GhostIngredientHandler<T extends Screen & IFilterScreen<?>> implements DraggableStackVisitor<T> {
    @Override
    public <R extends Screen> boolean isHandingScreen(R screen) {
        return screen instanceof IFilterScreen<?>;
    }

    @Override
    public DraggedAcceptorResult acceptDraggedStack(
        @NotNull DraggingContext<T> context, @NotNull DraggableStack stack
    ) {
        Point cursor = context.getCurrentPosition();
        if (cursor != null) {
            int x = cursor.getX();
            int y = cursor.getY();
            Object held = stack.getStack().getValue();
            if (held instanceof ItemStack item) {
                T screen = context.getScreen();
                for (Slot slot : screen.getFilterMenu().slots) {
                    Rect2i rect2i = new Rect2i(screen.getOffsetX() + slot.x, screen.getOffsetY() + slot.y, 16, 16);
                    if (rect2i.contains(x, y)) {
                        if (screen.setFilter(screen.getFilterMenu().getFilterSlotIndex(slot), item)) {
                            new SlotFilterChangePack(screen.getFilterMenu().getFilterSlotIndex(slot), item).send();
                            return DraggedAcceptorResult.CONSUMED;
                        }
                    }
                }
                return DraggedAcceptorResult.CONSUMED;
            }
        }
        return DraggableStackVisitor.super.acceptDraggedStack(context, stack);
    }
}
