package dev.dubhe.anvilcraft.inventory.component.sc;

import dev.dubhe.anvilcraft.saved.sc.ContainerStorage;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Setter
public class ShulkerContainerSlot extends Slot {
    private static final Container EMPTY = new SimpleContainer(0);
    private ContainerStorage storage;
    private int index;
    @Getter
    private boolean folded;

    public ShulkerContainerSlot(ContainerStorage storage, int row, int column, int leftPos, int topPos, int slotSize) {
        super(ShulkerContainerSlot.EMPTY, 0, leftPos + column * slotSize, topPos + row * slotSize);
        this.storage = storage;
        this.index = row * 9 + column;
    }

    @Override
    public void onQuickCraft(ItemStack oldStack, ItemStack newStack) {
    }

    @Override
    public ItemStack getItem() {
        if (this.storage == null) return ItemStack.EMPTY;
        return this.storage.getItem(this.index).copy().toStack();
    }

    public UnlimitedItemStack getUnlimitedItem() {
        return this.storage.getItem(this.index).copy();
    }

    @Override
    public void set(ItemStack stack) {
    }

    @Override
    public void setByPlayer(ItemStack stack) {
        if (stack.isEmpty()) return;
        int result = this.storage.addItem(stack);
        if (result != stack.getCount()) stack.setCount(result);
    }

    @Override
    public ItemStack safeInsert(ItemStack stack, int increment) {
        if (stack.isEmpty() || !this.mayPlace(stack)) return stack;

        int result = this.storage.addItem(stack);
        if (result != stack.getCount()) stack.setCount(result);

        return stack;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public int getMaxStackSize() {
        return this.storage.getMaxStackSize();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return this.storage.getMaxStackSize(stack);
    }

    @Override
    public ItemStack remove(int amount) {
        UnlimitedItemStack stack = this.storage.getItem(this.index);
        ItemStack result = this.storage.split(this.index, amount);
        if (stack.getCount() == result.getCount() && this.folded) {
            this.index = this.storage.getEntries().getFirstIndexForItem(result.getItemHolder());
        }
        return result;
    }

    @Override
    public boolean isSameInventory(Slot other) {
        return false;
    }

    @Override
    public boolean isActive() {
        return this.storage != null;
    }
}
