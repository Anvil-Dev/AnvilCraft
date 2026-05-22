package dev.dubhe.anvilcraft.inventory.component;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * 成书输出槽位(只允许取出,不允许放入),支持条件可见性
 */
public class WrittenBookOnlySlot extends Slot {
    @Nullable
    private final BooleanSupplier visibilityCondition;

    public WrittenBookOnlySlot(net.minecraft.world.Container container, int slot, int x, int y,
                              @Nullable BooleanSupplier visibilityCondition) {
        super(container, slot, x, y);
        this.visibilityCondition = visibilityCondition;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // 输出槽位：禁止放入任何物品
        return false;
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
