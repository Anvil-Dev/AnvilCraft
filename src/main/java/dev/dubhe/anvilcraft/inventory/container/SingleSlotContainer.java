package dev.dubhe.anvilcraft.inventory.container;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SingleSlotContainer implements Container {
    private ItemStack item;

    public SingleSlotContainer(ItemStack item) {
        this.item = item;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return item.isEmpty();
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return slot == 0 ? item : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        int itemCount = item.getCount();
        if (amount >= itemCount) {
            item = ItemStack.EMPTY;
            return item.copy();
        }
        item.setCount(itemCount - amount);
        return item.copyWithCount(amount);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        ItemStack i = item.copy();
        item = ItemStack.EMPTY;
        return i;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (slot == 0) item = stack.copy();
    }

    @Override
    public void setChanged() {

    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        item = ItemStack.EMPTY;
    }
}
