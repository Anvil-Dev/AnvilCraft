package dev.dubhe.anvilcraft.api.itemhandler;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 只进不出的虚空物品容器。
 *
 * <p>接受任意物品的输入并直接销毁（相当于创造板条箱的只输入版本），
 * 永不存储、永不输出。用于虚空物质块：通过漏斗、溜槽等方块输入的物品
 * 会被无限吸收并消失。</p>
 *
 * <p>带永恒词条的物品免疫虚空，会被原样退回而不销毁。</p>
 */
public final class VoidItemHandler implements IItemHandler {
    public static final VoidItemHandler INSTANCE = new VoidItemHandler();

    private VoidItemHandler() {
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        // 永恒物品免疫虚空（与物品实体掉出世界的虚空击杀保护一致）：原样退回，不销毁
        if (VoidItemHandler.isEternal(stack)) {
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return !VoidItemHandler.isEternal(stack);
    }

    /** 是否带有永恒词条。 */
    public static boolean isEternal(ItemStack stack) {
        return stack.has(ModComponents.ETERNAL);
    }
}
