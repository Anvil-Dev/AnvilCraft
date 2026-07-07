package dev.dubhe.anvilcraft.inventory.component;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * 只允许放入书类物品的槽位（书与笔、成书、普通书）
 */
public class BookOnlySlot extends Slot {
    @Nullable
    private final BooleanSupplier isActiveCondition;

    public BookOnlySlot(Container container, int slot, int x, int y) {
        this(container, slot, x, y, null);
    }

    public BookOnlySlot(Container container, int slot, int x, int y,
                        @Nullable BooleanSupplier isActiveCondition) {
        super(container, slot, x, y);
        this.isActiveCondition = isActiveCondition;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(Items.WRITTEN_BOOK)
            || stack.is(Items.WRITABLE_BOOK)
            || stack.is(Items.BOOK);
    }

    @Override
    public boolean isActive() {
        if (this.isActiveCondition != null) {
            return this.isActiveCondition.getAsBoolean();
        }
        return true;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}
