package dev.dubhe.anvilcraft.inventory.component;

import dev.dubhe.anvilcraft.api.container.ContainerStorage;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import lombok.Setter;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Setter
public class ShulkerContainerSlot extends Slot {
    private static final Container EMPTY = new SimpleContainer(0);
    private final ContainerStorage storage;
    private final int containerSlot;
    private int index;

    public ShulkerContainerSlot(ContainerStorage storage, int row, int column, int leftPos, int topPos, int slotSize) {
        super(ShulkerContainerSlot.EMPTY, 0, leftPos + column * slotSize, topPos + row * slotSize);
        this.storage = storage;
        this.containerSlot = row * 9 + column;
        this.index = row * 9 + column;
    }

    @Override
    public void onQuickCraft(ItemStack oldStack, ItemStack newStack) {
    }

    @Override
    public ItemStack getItem() {
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
        return this.storage.split(this.index, amount);
    }

    @Override
    public int getContainerSlot() {
        return this.containerSlot;
    }

    @Override
    public boolean isSameInventory(Slot other) {
        return false;
    }

    public boolean isFull() {
        return this.storage.isFull(this.storage.getItem(this.index));
    }
}
