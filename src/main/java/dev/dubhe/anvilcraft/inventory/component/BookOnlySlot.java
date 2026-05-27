package dev.dubhe.anvilcraft.inventory.component;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * 只允许放入书物品的槽位(输入),支持条件可见性
 */
public class BookOnlySlot extends Slot {
    @Nullable
    private final BooleanSupplier visibilityCondition;
    
    public BookOnlySlot(net.minecraft.world.Container container, int slot, int x, int y) {
        this(container, slot, x, y, null);
    }
    
    public BookOnlySlot(net.minecraft.world.Container container, int slot, int x, int y, 
                       @Nullable BooleanSupplier visibilityCondition) {
        super(container, slot, x, y);
        this.visibilityCondition = visibilityCondition;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // 只允许放入成书、书与笔、可读的书
        return stack.is(Items.WRITTEN_BOOK) 
            || stack.is(Items.WRITABLE_BOOK)
            || stack.is(Items.BOOK);
    }
    
    @Override
    public boolean isActive() {
        // 如果有可见性条件,检查条件;否则默认激活
        return visibilityCondition == null || visibilityCondition.getAsBoolean();
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
