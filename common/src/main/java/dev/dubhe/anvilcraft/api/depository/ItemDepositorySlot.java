package dev.dubhe.anvilcraft.api.depository;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ItemDepositorySlot extends Slot {
    private static final Container emptyContainer = new SimpleContainer(0);
    private final IItemDepository depository;
    private final int slot;


    /**
     * 物品容器槽位
     *
     * @param depository 物品容器
     * @param slot       槽位
     * @param x          坐标 x
     * @param y          坐标 y
     */
    public ItemDepositorySlot(IItemDepository depository, int slot, int x, int y) {
        super(emptyContainer, slot, x, y);
        this.depository = depository;
        this.slot = slot;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return false;
        return depository.canPlaceItem(slot, stack);
    }

    @Override
    public @NotNull ItemStack getItem() {
        return depository.getStack(slot);
    }

    @Override
    public void set(@NotNull ItemStack stack) {
        depository.setStack(slot, stack);
    }

    @Override
    public void setChanged() {
        depository.onContentsChanged();
    }

    @Override
    public void onQuickCraft(@NotNull ItemStack oldStack, @NotNull ItemStack newStack) {

    }

    @Override
    public int getMaxStackSize() {
        return depository.getSlotLimit(slot);
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        ItemStack maxAdd = stack.copy();
        int maxInput = stack.getMaxStackSize();
        maxAdd.setCount(maxInput);

        ItemStack currentStack = depository.getStack(slot);

        ItemStack remainder = depository.insert(slot, maxAdd, true, false, false);
        int current = currentStack.getCount();
        int added = maxInput - remainder.getCount();
        return current + added;
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return !depository.extract(slot, 1, true).isEmpty();
    }

    @Override
    public @NotNull ItemStack remove(int amount) {
        return depository.extract(slot, amount, false);
    }

    /**
     * 判断槽位是否支持过滤
     *
     * @return 是否支持过滤
     */
    public boolean isFilter() {
        return this.depository instanceof FilteredItemDepository;
    }
}
