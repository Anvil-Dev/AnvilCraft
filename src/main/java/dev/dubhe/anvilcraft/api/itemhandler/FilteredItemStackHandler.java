package dev.dubhe.anvilcraft.api.itemhandler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.recipe.util.CodecUtil;
import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.item.FilterItem;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

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
            Codec.BOOL.listOf().fieldOf("disabled").forGetter(o -> o.disabled),
            Codec.INT.listOf().fieldOf("slotLimits").forGetter(o -> o.slotLimits))
        .apply(ins, FilteredItemStackHandler::new));

    private boolean filterEnabled = false;
    private NonNullList<ItemStack> filteredItems;
    private NonNullList<Boolean> disabled;
    private NonNullList<Integer> slotLimits;

    public NonNullList<ItemStack> getStacks() {
        return stacks;
    }

    public FilteredItemStackHandler(
        boolean filterEnabled, List<Optional<ItemStack>> filteredItems, List<Boolean> disabled, List<Integer> slotLimits) {
        super(filteredItems.size());
        this.filteredItems = NonNullList.create();
        this.filteredItems.addAll(filteredItems.stream()
            .map(it -> it.orElse(ItemStack.EMPTY)).toList()
        );
        this.disabled = NonNullList.create();
        this.disabled.addAll(disabled);
        this.slotLimits = NonNullList.create();
        this.slotLimits.addAll(slotLimits);
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
        this.slotLimits = NonNullList.withSize(size, IFilterBlockEntity.DEFAULT_SLOT_LIMIT);
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
        if (!filterEnabled && !stack.isEmpty()) {
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
        if (!this.filterEnabled) {
            return this.disabled.get(slot);
        } else {
            return this.disabled.get(slot)
                || (getStackInSlot(slot).isEmpty()
                && this.filteredItems.get(slot).isEmpty()
            );
        }
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
     * 判断指定槽位是否允许放入指定物品堆叠
     *
     * @param slot  槽位
     * @param stack 物品堆叠
     * @return 指定槽位是否允许放入指定物品堆叠
     */
    public boolean isFiltered(int slot, ItemStack stack) {
        return FilterItem.filter(this.filteredItems.get(slot), stack);
    }

    /**
     * 设置指定槽位的过滤
     *
     * @param slot  槽位
     * @param stack 过滤物品堆叠（不检查NBT）
     */
    public boolean setFilter(int slot, ItemStack stack) {
        if (slot < 0 || slot >= this.filteredItems.size()) return false;
        if (stack.isEmpty()) return false;
        this.setSlotDisabled(slot, false);
        this.filteredItems.set(slot, stack.copyWithCount(1));
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

    public boolean isEmpty() {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取指定槽位的物品数量上限
     *
     * @param slot 槽位
     * @return 物品数量上限
     */
    public int getSlotLimit(int slot) {
        if (slot < 0 || slot >= this.slotLimits.size()) return IFilterBlockEntity.DEFAULT_SLOT_LIMIT;
        return this.slotLimits.get(slot);
    }

    /**
     * 设置指定槽位的物品数量上限
     *
     * @param slot  槽位
     * @param limit 物品数量上限
     */
    public void setSlotLimit(int slot, int limit) {
        if (slot < 0 || slot >= this.slotLimits.size()) return;
        this.slotLimits.set(slot, limit);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
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
            inventoryEntry.putInt("SlotLimit", this.getSlotLimit(slot));

            inventory.add(inventoryEntry);
        }
        compoundTag.put("Inventory", inventory);
        return compoundTag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
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
            if (inventoryEntry.contains("SlotLimit")) {
                this.slotLimits.set(slot, inventoryEntry.getInt("SlotLimit"));
            } else {
                this.slotLimits.set(slot, IFilterBlockEntity.DEFAULT_SLOT_LIMIT);
            }
        }
    }

    public CompoundTag serializeFiltering() {
        return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
    }

    public void deserializeFiltering(CompoundTag tag) {
        FilteredItemStackHandler handler =
            CODEC.decode(NbtOps.INSTANCE, tag).getOrThrow().getFirst();
        if (this.getSlots() != handler.getSlots()) throw new IllegalArgumentException("Depository size mismatch");
        this.filterEnabled = tag.getBoolean("filterEnabled");
        int size = handler.filteredItems.size();
        this.filteredItems = NonNullList.of(ItemStack.EMPTY, handler.filteredItems.toArray(new ItemStack[size]));
        this.disabled = handler.disabled;
        this.slotLimits = handler.slotLimits;
    }
}
