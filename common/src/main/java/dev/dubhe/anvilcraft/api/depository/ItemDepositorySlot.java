package dev.dubhe.anvilcraft.api.depository;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ItemDepositorySlot extends Slot {
    private static final Container emptyContainer = new SimpleContainer(0);
    private final IItemDepository depository;
    private final int slot;
    public ItemDepositorySlot(IItemDepository depository, int slot, int x, int y) {
        super(emptyContainer, slot, x, y);
        this.depository = depository;
        this.slot = slot;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return depository.isItemValid(slot, stack);
    }

    @Override
    public ItemStack getItem() {
        return depository.getStack(slot);
    }

    @Override
    public void onQuickCraft(ItemStack oldStack, ItemStack newStack) {

    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        ItemStack maxAdd = stack.copy();
        int maxInput = stack.getMaxStackSize();
        maxAdd.setCount(maxInput);

        ItemStack currentStack = depository.getStack(slot);

        ItemStack remainder = depository.insert(slot, maxAdd, true);
        int current = currentStack.getCount();
        int added = maxInput - remainder.getCount();
        return current + added;
    }

    @Override
    public boolean mayPickup(Player player) {
        return !depository.extract(slot, 1, true).isEmpty();
    }

    @Override
    public ItemStack remove(int amount) {
        return depository.extract(slot, amount, false);
    }
}
