package dev.dubhe.anvilcraft.api.itemhandler;

import lombok.Getter;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemStackResourceHandler;

@Getter
public class SingleStackResourceHandler extends ItemStackResourceHandler {
    protected ItemStack stack = ItemStack.EMPTY;

    @Override
    public void setStack(ItemStack stack) {
        this.stack = stack;
        this.onContentChanged(stack);
    }

    protected void onContentChanged(ItemStack stack) {
    }
}
