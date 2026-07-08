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
import org.jspecify.annotations.NullMarked;

import java.util.LinkedList;
import java.util.List;

@NullMarked
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

        if (ingredient.getType() == VanillaTypes.ITEM_STACK) {
            for (int slot : screen.getGhostSlots()) {
                Rect2i fixedArea = screen.getGhostSlotArea(slot);
                if (fixedArea != null) {
                    targets.add(new GhostTarget<>(screen, slot, fixedArea));
                    continue;
                }
                if (slot < 0 || slot >= screen.getMenu().slots.size()) continue;
                if (!screen.getMenu().slots.get(slot).isActive()) continue;
                targets.add(new GhostTarget<>(screen, slot, screen.getSlotSize(slot)));
            }
        } else if (ingredient.getType() == NeoForgeTypes.FLUID_STACK) {
            for (int slot : screen.getGhostSlots()) {
                Rect2i fixedArea = screen.getGhostSlotArea(slot);
                if (fixedArea == null) continue;
                targets.add(new GhostTarget<>(screen, slot, fixedArea));
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
        private final boolean fixedArea;

        public GhostTarget(T screen, int slotIndex, IGhostIngredientScreen.Vec2i size) {
            this.screen = screen;
            this.slotIndex = slotIndex;
            Slot slot = screen.getMenu().slots.get(slotIndex);
            this.area = new Rect2i(screen.getLeftPos() + slot.x, screen.getTopPos() + slot.y, size.x(), size.y());
            this.fixedArea = false;
        }

        public GhostTarget(T screen, int slotIndex, Rect2i area) {
            this.screen = screen;
            this.slotIndex = slotIndex;
            this.area = new Rect2i(
                screen.getLeftPos() + area.getX(),
                screen.getTopPos() + area.getY(),
                area.getWidth(),
                area.getHeight()
            );
            this.fixedArea = true;
        }

        @Override
        public void accept(I ingredient) {
            if (ingredient instanceof ItemStack stack) {
                Slot slot = this.fixedArea ? this.screen.getMenu().getSlot(0) : this.screen.getMenu().getSlot(this.slotIndex);
                this.screen.acceptGhost(slot, stack);
            } else if (ingredient instanceof FluidStack fluid) {
                this.screen.acceptFluidGhost(this.slotIndex, fluid);
            }
        }
    }
}
