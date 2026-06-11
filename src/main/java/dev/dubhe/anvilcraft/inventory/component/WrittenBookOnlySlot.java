package dev.dubhe.anvilcraft.inventory.component;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * 只允许放入成书的槽位，且此槽位只能取出不能放入
 */
public class WrittenBookOnlySlot extends Slot {
    @Nullable
    private final BooleanSupplier isActiveCondition;

    public WrittenBookOnlySlot(Container container, int slot, int x, int y,
                               @Nullable BooleanSupplier isActiveCondition) {
        super(container, slot, x, y);
        this.isActiveCondition = isActiveCondition;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return true;
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
