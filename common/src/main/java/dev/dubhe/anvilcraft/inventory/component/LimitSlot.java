package dev.dubhe.anvilcraft.inventory.component;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class LimitSlot extends Slot {
    private final Predicate<ItemStack> limit;

    public LimitSlot(Container container, int slot, int x, int y, Predicate<ItemStack> limit) {
        super(container, slot, x, y);
        this.limit = limit;
    }

    public LimitSlot(Container container, int slot, int x, int y) {
        this(container, slot, x, y, i -> false);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.limit.test(stack);
    }
}
