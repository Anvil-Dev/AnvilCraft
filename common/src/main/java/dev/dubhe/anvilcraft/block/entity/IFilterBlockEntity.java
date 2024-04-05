package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public interface IFilterBlockEntity extends Container {
    boolean isRecord();

    void setRecord(boolean record);

    default void safeSetRecord(boolean record) {
        this.setRecord(record);
        if (!this.isRecord()) {
            this.getFilter().clear();
        }
    }

    NonNullList<Boolean> getDisabled();

    NonNullList<@Unmodifiable ItemStack> getFilter();

    default NonNullList<Boolean> getNewDisabled() {
        return NonNullList.withSize(this.getContainerSize(), false);
    }

    default NonNullList<@Unmodifiable ItemStack> getNewFilter() {
        return NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
    }

    default void loadTag(@NotNull CompoundTag tag) {
        this.setRecord(tag.getBoolean("record"));
        ListTag tags = tag.getList("disabled", Tag.TAG_BYTE);
        for (int i = 0; i < tags.size(); i++) {
            this.getDisabled().set(i, ((ByteTag) tags.get(i)).getAsByte() != 0);
        }
        CompoundTag filter = tag.getCompound("filter");
        ContainerHelper.loadAllItems(filter, this.getFilter());
    }

    default void saveTag(@NotNull CompoundTag tag) {
        tag.put("record", ByteTag.valueOf(this.isRecord()));
        ListTag tags = new ListTag();
        for (Boolean b : this.getDisabled()) {
            tags.add(ByteTag.valueOf(b));
        }
        tag.put("disabled", tags);
        CompoundTag filter = new CompoundTag();
        ContainerHelper.saveAllItems(filter, this.getFilter());
        tag.put("filter", filter);
    }

    default boolean canPlaceItem(int index, @NotNull ItemStack insertingStack) {
        if (this.getDisabled().get(index)) return false;
        ItemStack storedStack = this.getItems().get(index);
        ItemStack filterStack = this.getFilter().get(index);
        if (isRecord() && filterStack.isEmpty()) return insertingStack.isEmpty();
        int count = storedStack.getCount();
        if (count >= storedStack.getMaxStackSize()) {
            return false;
        }
        if (storedStack.isEmpty()) {
            return filterStack.isEmpty() || ItemStack.isSameItemSameTags(insertingStack, filterStack);
        }
        return !this.smallerStackExist(count, storedStack, index);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean smallerStackExist(int count, ItemStack itemStack, int index);

    NonNullList<ItemStack> getItems();
}
