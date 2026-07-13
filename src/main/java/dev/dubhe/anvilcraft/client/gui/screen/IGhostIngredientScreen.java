package dev.dubhe.anvilcraft.client.gui.screen;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("unused")
public interface IGhostIngredientScreen {
    default Collection<Integer> getGhostSlots() {
        return List.of();
    }

    default Vec2i getSlotSize(int slot) {
        return new Vec2i(16, 16);
    }

    default @Nullable Rect2i getGhostSlotArea(int slot) {
        return null;
    }

    default void acceptGhost(Slot slot, ItemStack ingredient) {
    }

    default void acceptFluidGhost(int slot, FluidStack fluid) {
    }

    record Vec2i(int x, int y) {
    }

    static Collection<Integer> range(int start, int end, int step) {
        List<Integer> list = new LinkedList<>();
        for (int i = start; i < end; i += step) {
            list.add(i);
        }
        return list;
    }
}
