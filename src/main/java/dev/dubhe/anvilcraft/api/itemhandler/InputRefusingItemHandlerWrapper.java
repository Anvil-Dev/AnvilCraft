package dev.dubhe.anvilcraft.api.itemhandler;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class InputRefusingItemHandlerWrapper implements IItemHandler {

    private final IItemHandler delegate;

    public InputRefusingItemHandlerWrapper(IItemHandler delegate) {
        this.delegate = delegate;
    }

    public static IItemHandler wrap(IItemHandler ih) {
        return new InputRefusingItemHandlerWrapper(ih);
    }

    @Override
    public int getSlots() {
        return delegate.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return delegate.getStackInSlot(i);
    }

    @Override
    public ItemStack insertItem(int i, ItemStack itemStack, boolean b) {
        return itemStack;
    }

    @Override
    public ItemStack extractItem(int i, int i1, boolean b) {
        return delegate.extractItem(i, i1, b);
    }

    @Override
    public int getSlotLimit(int i) {
        return delegate.getSlotLimit(i);
    }

    @Override
    public boolean isItemValid(int i, ItemStack itemStack) {
        return false;
    }
}
