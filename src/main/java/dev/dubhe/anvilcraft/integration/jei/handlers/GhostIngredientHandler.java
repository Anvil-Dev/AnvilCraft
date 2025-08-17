package dev.dubhe.anvilcraft.integration.jei.handlers;

import dev.dubhe.anvilcraft.client.gui.screen.IGhostIngredientScreen;
import lombok.Getter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class GhostIngredientHandler<
    M extends AbstractContainerMenu,
    T extends AbstractContainerScreen<M> & IGhostIngredientScreen
    > implements IGhostIngredientHandler<T> {
    @Override
    public <X> @NotNull List<Target<X>> getTargetsTyped(
        @NotNull T screen,
        @NotNull ITypedIngredient<X> ingredient,
        boolean doStart
    ) {
        List<Target<X>> targets = new LinkedList<>();

        if (ingredient.getType() == VanillaTypes.ITEM_STACK) {
            for (int slot : screen.getGhostSlots()) {
                if (!screen.getMenu().slots.get(slot).isActive()) continue;
                targets.add(new GhostTarget<>(screen, slot, screen.getSlotSize(slot)));
            }
        }

        return targets;
    }

    @Override
    public void onComplete() {

    }

    public static class GhostTarget<
        I,
        M extends AbstractContainerMenu,
        T extends AbstractContainerScreen<M> & IGhostIngredientScreen
        > implements Target<I> {
        @Getter
        private final Rect2i area;
        private final T screen;
        private final int slotIndex;

        public GhostTarget(@NotNull T screen, int slotIndex, IGhostIngredientScreen.@NotNull Vec2i size) {
            this.screen = screen;
            this.slotIndex = slotIndex;
            Slot slot = screen.getMenu().slots.get(slotIndex);
            this.area = new Rect2i(screen.getGuiLeft() + slot.x, screen.getGuiTop() + slot.y, size.x(), size.y());
        }

        @Override
        public void accept(@NotNull I ingredient) {
            if (!(ingredient instanceof ItemStack stack)) return;
            screen.acceptGhost(screen.getMenu().getSlot(slotIndex), stack);
        }
    }
}
