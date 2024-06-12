package dev.dubhe.anvilcraft.api.depository;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
@SuppressWarnings("unused")
public class FilteredItemDepository extends ItemDepository {

    public static final Codec<FilteredItemDepository> CODEC =
            RecordCodecBuilder.create(ins -> ins.group(
                            Codec.BOOL.fieldOf("filterEnabled").forGetter(o -> o.filterEnabled),
                            ItemStack.CODEC.listOf().fieldOf("filteredItems").forGetter(o -> o.filteredItems),
                            Codec.BOOL.listOf().fieldOf("disabled").forGetter(o -> o.disabled))
                    .apply(ins, FilteredItemDepository::new));

    private boolean filterEnabled = false;
    private NonNullList<ItemStack> filteredItems;
    private NonNullList<Boolean> disabled;

    /**
     *
     */
    public FilteredItemDepository(
            boolean filterEnabled, List<ItemStack> filteredItems, List<Boolean> disabled) {
        super(filteredItems.size());
        this.filteredItems = NonNullList.create();
        this.filteredItems.addAll(filteredItems);
        this.disabled = NonNullList.create();
        this.disabled.addAll(disabled);
    }

    /**
     * 有过滤的容器
     *
     * @param size 大小
     */
    public FilteredItemDepository(int size) {
        super(size);
        this.filteredItems = NonNullList.withSize(size, ItemStack.EMPTY);
        this.disabled = NonNullList.withSize(size, false);
    }

    /**
     * 设置是否启用过滤
     *
     * @param filterEnabled 是否启用过滤
     */
    public void setFilterEnabled(boolean filterEnabled) {
        this.filteredItems.clear();
        this.filterEnabled = filterEnabled;
        if (this.filterEnabled) {
            for (int i = 0; i < this.getSlots(); i++) {
                ItemStack stack = this.getStack(i);
                if (stack.isEmpty()) continue;
                this.setFilter(i, stack);
            }
        }
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (!this.filterEnabled) return !this.isSlotDisabled(slot);
        return !this.isSlotDisabled(slot) && this.isFiltered(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return super.isItemValid(slot, stack);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (filterEnabled) {
            this.setFilter(slot, stack);
        } else if (!stack.isEmpty()) {
            this.setSlotDisabled(slot, false);
        }
        super.setStack(slot, stack);
    }

    /**
     * 判断指定槽位是否被禁用
     *
     * @param slot 槽位
     * @return 指定槽位是否被禁用
     */
    public boolean isSlotDisabled(int slot) {
        if (!this.filterEnabled) return this.disabled.get(slot);
        else
            return this.disabled.get(slot)
                    || (getStack(slot).isEmpty() && this.filteredItems.get(slot).isEmpty());
    }

    /**
     * 为指定槽位设定禁用情况
     *
     * @param slot    槽位
     * @param disable 禁用情况
     */
    public void setSlotDisabled(int slot, boolean disable) {
        this.filteredItems.set(slot, ItemStack.EMPTY);
        this.disabled.set(slot, disable);
    }

    /**
     * 使指定槽位禁用情况翻转
     *
     * @param slot 槽位
     * @return 指定槽位的禁用情况
     */
    public boolean cycleDisabled(int slot) {
        boolean disable = !this.disabled.get(slot);
        this.setSlotDisabled(slot, disable);
        return disable;
    }

    /**
     * 判断指定槽位是否允许放入指定物品堆栈
     *
     * @param slot  槽位
     * @param stack 物品堆栈
     * @return 指定槽位是否允许放入指定物品堆栈
     */
    public boolean isFiltered(int slot, ItemStack stack) {
        ItemStack filter = this.filteredItems.get(slot);
        return filter.isEmpty() || ItemStack.isSameItem(filter, stack);
    }

    /**
     * 设置指定槽位的过滤
     *
     * @param slot  槽位
     * @param stack 过滤物品堆栈（不检查NBT）
     */
    public void setFilter(int slot, @NotNull ItemStack stack) {
        if (stack.isEmpty()) return;
        this.setSlotDisabled(slot, false);
        this.filteredItems.set(slot, new ItemStack(stack.getItem(), 1));
    }

    /**
     * 获取指定槽位上的过滤
     *
     * @param slot 槽位
     * @return 指定槽位上的过滤
     */
    public ItemStack getFilter(int slot) {
        return this.filteredItems.get(slot);
    }

    @Override
    public @NotNull CompoundTag serializeNbt() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putBoolean("filterEnabled", this.filterEnabled);
        ListTag listTag = new ListTag();
        int slots = this.getSlots();
        compoundTag.putInt("Size", slots);
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = this.getStack(slot);
            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("Slot", slot);
            stack.save(itemTag);
            ItemStack filter = this.filteredItems.get(slot);
            if (!filter.isEmpty()) {
                CompoundTag filtered = new CompoundTag();
                filter.save(filtered);
                itemTag.put("filtered", filtered);
            }
            itemTag.putBoolean("disabled", this.disabled.get(slot));
            listTag.add(itemTag);
        }
        if (!listTag.isEmpty()) compoundTag.put("Items", listTag);
        return compoundTag;
    }

    @Override
    public void deserializeNbt(@NotNull CompoundTag tag) {
        if (!tag.contains("Items")) return;
        this.filterEnabled = tag.getBoolean("filterEnabled");
        ListTag listTag = tag.getList("Items", Tag.TAG_COMPOUND);
        int slots = this.getSlots();
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag itemTag = listTag.getCompound(i);
            int slot = itemTag.getInt("Slot");
            if (slot < 0 || slot >= slots) continue;
            this.getStacks().set(slot, ItemStack.of(itemTag));
            if (itemTag.contains("filtered"))
                this.filteredItems.set(slot, ItemStack.of(itemTag.getCompound("filtered")));
            this.disabled.set(slot, itemTag.getBoolean("disabled"));
        }
    }

    /**
     *
     */
    public CompoundTag serializeFiltering() {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow(false, e -> {});
    }

    /**
     *
     */
    public void deserializeFiltering(@NotNull CompoundTag tag) {
        FilteredItemDepository depository =
                CODEC.decode(NbtOps.INSTANCE, tag).getOrThrow(false, s -> {}).getFirst();
        if (this.getSize() != depository.getSize())
            throw new IllegalArgumentException("Depository size mismatch");
        this.filterEnabled = tag.getBoolean("filterEnabled");
        int size = depository.filteredItems.size();
        this.filteredItems =
                NonNullList.of(ItemStack.EMPTY, depository.filteredItems.toArray(new ItemStack[size]));
        this.disabled = depository.disabled;
    }

    public static class Pollable extends FilteredItemDepository {

        public Pollable(int size) {
            super(size);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return getEmptyOrSmallerSlot(stack) == slot && super.isItemValid(slot, stack);
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return super.canPlaceItem(slot, stack);
        }

        private int getEmptyOrSmallerSlot(ItemStack stack) {
            int slotCount = this.getSlots();
            int slot = -1;
            int countInSlot = Integer.MAX_VALUE;
            for (int index = slotCount - 1; index >= 0; index--) {
                if (this.isSlotDisabled(index)) continue;
                ItemStack stackInSlot = this.getStack(index);
                if (this.isSlotDisabled(index)) continue;
                if (!this.isFiltered(index, stack)) continue;
                if (stackInSlot.isEmpty()) {
                    slot = index;
                    countInSlot = 0;
                    continue;
                } else if (!ItemStack.isSameItemSameTags(stackInSlot, stack)) continue;
                int stackInSlotCount = stackInSlot.getCount();
                if (stackInSlotCount <= countInSlot && stackInSlotCount < this.getSlotLimit(index)) {
                    slot = index;
                    countInSlot = stackInSlotCount;
                }
            }
            return slot;
        }
    }
}
