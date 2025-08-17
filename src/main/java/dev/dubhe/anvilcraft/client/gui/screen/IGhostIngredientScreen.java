package dev.dubhe.anvilcraft.client.gui.screen;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

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

    default void acceptGhost(Slot slot, ItemStack ingredient) {
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
