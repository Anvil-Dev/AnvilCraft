package dev.dubhe.anvilcraft.api.itemhandler;

import dev.dubhe.anvilcraft.util.CodecUtil;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@Getter
@SuppressWarnings("unused")
public class FilteredItemStackHandler extends ItemStackHandler {

    public static final Codec<FilteredItemStackHandler> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                    Codec.BOOL.fieldOf("filterEnabled").forGetter(o -> o.filterEnabled),
                    CodecUtil.createOptionalCodec(ItemStack.CODEC)
                            .listOf()
                            .fieldOf("filteredItems")
                            .forGetter(o -> o.filteredItems.stream()
                                    .map(it -> it.isEmpty() ? Optional.<ItemStack>empty() : Optional.of(it))
                                    .toList()),
                    Codec.BOOL.listOf().fieldOf("disabled").forGetter(o -> o.disabled))
            .apply(ins, FilteredItemStackHandler::new));

    private boolean filterEnabled = false;
    private NonNullList<ItemStack> filteredItems;
    private NonNullList<Boolean> disabled;

    public NonNullList<ItemStack> getStacks() {
        return stacks;
    }

    /**
     *
     */
    public FilteredItemStackHandler(
            boolean filterEnabled, List<Optional<ItemStack>> filteredItems, List<Boolean> disabled) {
        super(filteredItems.size());
        this.filteredItems = NonNullList.create();
        this.filteredItems.addAll(
                filteredItems.stream().map(it -> it.orElse(ItemStack.EMPTY)).toList());
        this.disabled = NonNullList.create();
        this.disabled.addAll(disabled);
    }

    /**
     * 有过滤的容器
     *
     * @param size 大小
     */
    public FilteredItemStackHandler(int size) {
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
                ItemStack stack = this.getStackInSlot(i);
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
    public void setStackInSlot(int slot, ItemStack stack) {
        if (filterEnabled) {
            this.setFilter(slot, stack);
        } else if (!stack.isEmpty()) {
            this.setSlotDisabled(slot, false);
        }
        super.setStackInSlot(slot, stack);
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
                    || (getStackInSlot(slot).isEmpty()
                            && this.filteredItems.get(slot).isEmpty());
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
    public boolean setFilter(int slot, @NotNull ItemStack stack) {
        if (slot < 0 || slot >= this.filteredItems.size()) return false;
        if (stack.isEmpty()) return false;
        this.setSlotDisabled(slot, false);
        this.filteredItems.set(slot, new ItemStack(stack.getItem(), 1));
        return true;
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

    public boolean isEnabled(int slot) {
        return this.disabled.get(slot);
    }

    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putBoolean("FilterEnabled", this.filterEnabled);
        ListTag inventory = new ListTag();
        int slots = this.getSlots();
        compoundTag.putInt("Size", slots);
        for (int slot = 0; slot < slots; slot++) {
            CompoundTag inventoryEntry = new CompoundTag();
            inventoryEntry.putInt("Slot", slot);
            ItemStack stack = this.getStackInSlot(slot);
            inventoryEntry.putBoolean("IsEmptySlot", stack.isEmpty());
            if (!stack.isEmpty()) {
                Tag itemTag = stack.save(provider);
                inventoryEntry.put("SlotItem", itemTag);
            }

            ItemStack filtering = this.getFilter(slot);

            inventoryEntry.putBoolean("SlotFilterEnabled", !filtering.isEmpty());
            if (!filtering.isEmpty()) {
                Tag filterItemTag = filtering.save(provider);
                inventoryEntry.put("SlotFilterItem", filterItemTag);
            }

            inventoryEntry.putBoolean("Disabled", this.disabled.get(slot));

            inventory.add(inventoryEntry);
        }
        compoundTag.put("Inventory", inventory);
        return compoundTag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, @NotNull CompoundTag tag) {
        if (!tag.contains("Inventory")) return;
        this.filterEnabled = tag.getBoolean("FilterEnabled");
        ListTag inventory = (ListTag) tag.get("Inventory");
        int size = tag.getInt("Size");
        for (Tag entry : inventory) {
            CompoundTag inventoryEntry = (CompoundTag) entry;
            int slot = inventoryEntry.getInt("Slot");
            boolean isEmptySlot = inventoryEntry.getBoolean("IsEmptySlot");
            if (!isEmptySlot) {
                CompoundTag itemTag = inventoryEntry.getCompound("SlotItem");
                this.stacks.set(slot, ItemStack.parseOptional(provider, itemTag));
            }
            boolean slotFilterEnabled = inventoryEntry.getBoolean("SlotFilterEnabled");
            if (slotFilterEnabled) {
                CompoundTag filterItemTag = inventoryEntry.getCompound("SlotFilterItem");
                this.filteredItems.set(slot, ItemStack.parseOptional(provider, filterItemTag));
            }
            this.disabled.set(slot, inventoryEntry.getBoolean("Disabled"));
        }
    }

    /**
     *
     */
    public CompoundTag serializeFiltering() {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
    }

    /**
     *
     */
    public void deserializeFiltering(@NotNull CompoundTag tag) {
        FilteredItemStackHandler handler =
                CODEC.decode(NbtOps.INSTANCE, tag).getOrThrow().getFirst();
        if (this.getSlots() != handler.getSlots()) throw new IllegalArgumentException("Depository size mismatch");
        this.filterEnabled = tag.getBoolean("filterEnabled");
        int size = handler.filteredItems.size();
        this.filteredItems = NonNullList.of(ItemStack.EMPTY, handler.filteredItems.toArray(new ItemStack[size]));
        this.disabled = handler.disabled;
    }
}
