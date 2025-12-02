package dev.dubhe.anvilcraft.inventory.component;

import dev.dubhe.anvilcraft.inventory.container.FilterOnlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class FilterOnlySlot extends Slot {
    public FilterOnlySlot(FilterOnlyContainer container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public ItemStack safeInsert(ItemStack stack, int increment) {
        this.container.setItem(this.getSlotIndex(), stack.copyWithCount(increment));
        return stack;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return false;
    }
}
