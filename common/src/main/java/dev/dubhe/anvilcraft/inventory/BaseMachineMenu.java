package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.BaseMachineBlockEntity;
import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public abstract class BaseMachineMenu extends AbstractContainerMenu {
    protected final Container machine;

    protected BaseMachineMenu(@Nullable MenuType<?> menuType, int containerId, @NotNull Container machine) {
        super(menuType, containerId);
        this.machine = machine;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        if (index >= this.slots.size()) return itemStack;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return itemStack;
        ItemStack itemStack2 = slot.getItem();
        itemStack = itemStack2.copy();
        if (quickMoveFilter(index,itemStack2)) {
            return ItemStack.EMPTY;
        }
        if (itemStack2.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (itemStack2.getCount() == itemStack.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, itemStack2);
        return itemStack;
    }

    private boolean quickMoveFilter(int index,ItemStack itemStack){
        if(index < this.machine.getContainerSize()){
            return !this.moveItemStackTo(itemStack, this.machine.getContainerSize(), this.machine.getContainerSize() + 36, true);
        }else{
            boolean bl = false;
            if (this.getMachine() instanceof IFilterBlockEntity entity) {
                for (int i = 0; i < this.machine.getContainerSize(); i++) {
                    ItemStack filter = entity.getFilter().get(i);
                    NonNullList<Boolean> disableds = entity.getDisabled();
                    if (filter.is(itemStack.getItem()) || !disableds.get(i)) {
                        bl = !this.moveItemStackTo(itemStack, i, i + 1, false);
                    }
                }
            }
            return bl;
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.machine.stillValid(player);
    }

    public void setDirection(Direction direction) {
        if (this.machine instanceof BaseMachineBlockEntity entity) entity.setDirection(direction);
    }
}
