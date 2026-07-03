package dev.dubhe.anvilcraft.api.itemhandler;

import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

public class OverLimitItemHandler extends StacksResourceHandler<UnlimitedItemStack, ItemResource> {
    private final int baseLimit;

    public OverLimitItemHandler(int baseLimit) {
        this(baseLimit, 1);
    }

    public OverLimitItemHandler(int baseLimit, int size) {
        super(NonNullList.withSize(size, UnlimitedItemStack.EMPTY), UnlimitedItemStack.EMPTY, UnlimitedItemStack.CODEC);
        this.baseLimit = baseLimit;
    }

    public OverLimitItemHandler(int baseLimit, NonNullList<ItemStack> stacks) {
        super(OverLimitItemHandler.transform(stacks), UnlimitedItemStack.EMPTY, UnlimitedItemStack.CODEC);
        this.baseLimit = baseLimit;
    }

    public OverLimitItemHandler(NonNullList<UnlimitedItemStack> stacks, int baseLimit) {
        super(stacks, UnlimitedItemStack.EMPTY, UnlimitedItemStack.CODEC);
        this.baseLimit = baseLimit;
    }

    private static NonNullList<UnlimitedItemStack> transform(NonNullList<ItemStack> stacks) {
        NonNullList<UnlimitedItemStack> result = NonNullList.createWithCapacity(stacks.size());
        for (int i = 0, stacksSize = stacks.size(); i < stacksSize; i++) {
            result.add(i, new UnlimitedItemStack(stacks.get(i)));
        }
        return result;
    }

    @Override
    protected ItemResource getResourceFrom(UnlimitedItemStack stack) {
        return ItemResource.of(stack.getItem(), stack.getComponentsPatch());
    }

    @Override
    protected int getAmountFrom(UnlimitedItemStack stack) {
        return stack.getCount();
    }

    @Override
    protected UnlimitedItemStack getStackFrom(ItemResource resource, int amount) {
        return new UnlimitedItemStack(resource.toStack(), amount);
    }

    @Override
    protected UnlimitedItemStack copyOf(UnlimitedItemStack stack) {
        return stack.copy();
    }

    @Override
    protected int getCapacity(int index, ItemResource resource) {
        return !resource.isEmpty()
               ? this.getResource(index).getMaxStackSize() * this.baseLimit
               : Item.ABSOLUTE_MAX_STACK_SIZE * this.baseLimit;
    }

    public UnlimitedItemStack peek(int index) {
        ItemResource resource = this.getResource(index);
        return new UnlimitedItemStack(resource.typeHolder(), this.getAmountAsInt(index), resource.getComponentsPatch());
    }
}
