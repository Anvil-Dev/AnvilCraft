package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface IFilterMenu {
    default boolean filter(int index, ItemStack stack) {
        ItemStack filter = this.getFilter(index);
        return filter.isEmpty() || ItemStack.isSameItemSameTags(filter, stack);
    }

    @Nullable IFilterBlockEntity getEntity();

    default boolean isRecord() {
        if (this.getEntity() == null) return false;
        return this.getEntity().isRecord();
    }

    default void setRecord(boolean record) {
        if (this.getEntity() != null) this.getEntity().safeSetRecord(record);
    }

    default ItemStack getFilter(int index) {
        if (this.getEntity() == null) return ItemStack.EMPTY;
        return this.getEntity().getFilter().get(index);
    }

    default void setFilter(int index, ItemStack stack) {
        if (this.getEntity() != null) this.getEntity().getFilter().set(index, stack);
    }

    default boolean isSlotDisabled(int index) {
        if (this.getEntity() == null) return false;
        return this.getEntity().getDisabled().get(index);
    }

    default void setSlotDisabled(int index, boolean state) {
        if (this.getEntity() != null) this.getEntity().getDisabled().set(index, state);
    }

    void slotsChanged(Container container);

    default void update(){}
}

