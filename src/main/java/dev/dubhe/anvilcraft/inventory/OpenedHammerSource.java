package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class OpenedHammerSource {
    private final @Nullable Inventory inventory;
    private final int inventorySlot;
    private final @Nullable Slot menuSlot;
    private final int clientInventorySlot;
    private final Item openedHammerItem;

    private OpenedHammerSource(
        @Nullable Inventory inventory,
        int inventorySlot,
        @Nullable Slot menuSlot,
        int clientInventorySlot,
        Item openedHammerItem
    ) {
        this.inventory = inventory;
        this.inventorySlot = inventorySlot;
        this.menuSlot = menuSlot;
        this.clientInventorySlot = clientInventorySlot;
        this.openedHammerItem = openedHammerItem;
    }

    public static @Nullable OpenedHammerSource fromInventory(Inventory inventory, int inventorySlot) {
        if (HammerOpenedAnvilMenuHelper.isValidInventorySlot(inventory, inventorySlot)) return null;
        ItemStack stack = inventory.getItem(inventorySlot);
        if (!(stack.getItem() instanceof AnvilHammerItem)) return null;
        return new OpenedHammerSource(inventory, inventorySlot, null, inventorySlot, stack.getItem());
    }

    public static @Nullable OpenedHammerSource fromMenuSlot(Slot slot, Inventory playerInventory) {
        ItemStack stack = slot.getItem();
        if (!(stack.getItem() instanceof AnvilHammerItem)) return null;
        int clientSlot = slot.container == playerInventory
                         ? slot.getContainerSlot()
                         : HammerOpenedAnvilMenuHelper.REMOTE_HAMMER_SLOT;
        return new OpenedHammerSource(null, HammerOpenedAnvilMenuHelper.NO_HAMMER_SLOT, slot, clientSlot, stack.getItem());
    }

    public int clientInventorySlot() {
        return this.clientInventorySlot;
    }

    public Item openedHammerItem() {
        return this.openedHammerItem;
    }

    public boolean stillInPlace() {
        return this.getStack().is(this.openedHammerItem);
    }

    public void damage() {
        ItemStack hammer = this.getStack();
        if (!hammer.is(this.openedHammerItem) || !hammer.isDamageableItem()) return;
        int nextDamage = hammer.getDamageValue() + 1;
        if (nextDamage >= hammer.getMaxDamage()) {
            this.setStack(ItemStack.EMPTY);
        } else {
            hammer.setDamageValue(nextDamage);
            this.setChanged();
        }
    }

    private ItemStack getStack() {
        if (this.inventory != null) {
            return this.inventory.getItem(this.inventorySlot);
        }
        if (this.menuSlot != null) {
            return this.menuSlot.getItem();
        }
        return ItemStack.EMPTY;
    }

    private void setStack(ItemStack stack) {
        if (this.inventory != null) {
            this.inventory.setItem(this.inventorySlot, stack);
            this.inventory.setChanged();
            return;
        }
        if (this.menuSlot != null) {
            this.menuSlot.set(stack);
            this.menuSlot.setChanged();
            Container container = this.menuSlot.container;
            container.setChanged();
        }
    }

    private void setChanged() {
        if (this.inventory != null) {
            this.inventory.setChanged();
            return;
        }
        if (this.menuSlot != null) {
            this.menuSlot.setChanged();
            this.menuSlot.container.setChanged();
        }
    }
}
