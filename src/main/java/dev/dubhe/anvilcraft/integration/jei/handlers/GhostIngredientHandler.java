package dev.dubhe.anvilcraft.integration.jei.handlers;

import dev.dubhe.anvilcraft.client.gui.screen.IGhostIngredientScreen;
import lombok.Getter;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.neoforge.NeoForgeTypes;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.LinkedList;
import java.util.List;

public class GhostIngredientHandler<
    M extends AbstractContainerMenu,
    T extends AbstractContainerScreen<M> & IGhostIngredientScreen
    > implements IGhostIngredientHandler<T> {
    @Override
    public <X> List<Target<X>> getTargetsTyped(
        T screen,
        ITypedIngredient<X> ingredient,
        boolean doStart
    ) {
        List<Target<X>> targets = new LinkedList<>();

        if (ingredient.getType() == VanillaTypes.ITEM_STACK
            || ingredient.getType() == NeoForgeTypes.FLUID_STACK) {
            for (int slot : screen.getGhostSlots()) {
                int[] fixedArea = screen.getGhostSlotArea(slot);
                if (fixedArea == null && !screen.getMenu().slots.get(slot).isActive()) continue;
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

        public GhostTarget(T screen, int slotIndex, IGhostIngredientScreen.Vec2i size) {
            this.screen = screen;
            this.slotIndex = slotIndex;
            int[] fixed = screen.getGhostSlotArea(slotIndex);
            if (fixed != null) {
                this.area = new Rect2i(screen.getGuiLeft() + fixed[0], screen.getGuiTop() + fixed[1], fixed[2], fixed[3]);
            } else {
                Slot slot = screen.getMenu().slots.get(slotIndex);
                this.area = new Rect2i(screen.getGuiLeft() + slot.x, screen.getGuiTop() + slot.y, size.x(), size.y());
            }
        }

        @Override
        public void accept(I ingredient) {
            if (ingredient instanceof ItemStack stack) {
                screen.acceptGhost(screen.getMenu().getSlot(slotIndex), stack);
            } else if (ingredient instanceof FluidStack fluid) {
                // 流体拖入 → 直接以 FluidStack 交给屏幕（兼容无桶流体）
                screen.acceptFluidGhost(slotIndex, fluid);
            }
        }
    }
}
