package dev.dubhe.anvilcraft.api.itemhandler;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.item.FilterItem;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import java.util.List;
import java.util.Optional;

@Getter
@SuppressWarnings("unused")
public class FilteredItemStackHandler extends ItemStacksResourceHandler {
    public static final MapCodec<FilteredItemStackHandler> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.BOOL.fieldOf("filterEnabled").forGetter(FilteredItemStackHandler::isFilterEnabled),
        CodecUtil.createOptionalCodec(ItemStack.CODEC)
            .listOf()
            .fieldOf("filteredItems")
            .forGetter(o -> o.filteredItems.stream()
                .map(it -> Optional.of(it).filter(ItemStack::isEmpty))
                .toList()),
        Codec.BOOL.listOf().fieldOf("disabled").forGetter(FilteredItemStackHandler::getDisabled),
        Codec.INT.listOf().fieldOf("slotLimits").forGetter(FilteredItemStackHandler::getSlotLimits)
    ).apply(ins, FilteredItemStackHandler::new));

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
            for (int i = 0; i < this.size(); i++) {
                ItemStack stack = this.getStackFrom(this.getResource(i), this.getAmountAsInt(i));
                if (stack.isEmpty()) continue;
                this.setFilter(i, stack);
            }
        }
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        if (!this.filterEnabled) return !this.isSlotDisabled(index);
        return !this.isSlotDisabled(index) && this.isFiltered(index, resource.toStack());
    }

    @Override
    public void set(int index, ItemResource resource, int amount) {
        if (!filterEnabled && !resource.isEmpty()) {
            this.setSlotDisabled(index, false);
        }
        super.set(index, resource, amount);
    }

    /**
     * 判断指定槽位是否被禁用
     *
     * @param slot 槽位
     * @return 指定槽位是否被禁用
     */
    public boolean isSlotDisabled(int slot) {
        if (!this.filterEnabled) return this.disabled.get(slot);
        return this.disabled.get(slot)
            || (this.getResource(slot).isEmpty() && this.filteredItems.get(slot).isEmpty());
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
    public void serialize(ValueOutput output) {
        output.putBoolean("FilterEnabled", this.filterEnabled);
        int slots = this.size();
        output.putInt("Size", slots);
        ValueOutput.ValueOutputList inventory = output.childrenList("");
        for (int slot = 0; slot < slots; slot++) {
            ValueOutput inventoryEntry = inventory.addChild();

            inventoryEntry.putInt("Slot", slot);

            ItemStack stack = this.getStackFrom(this.getResource(slot), this.getAmountAsInt(slot));
            inventoryEntry.putBoolean("IsEmptySlot", stack.isEmpty());
            if (!stack.isEmpty()) inventoryEntry.store("SlotItem", ItemStack.OPTIONAL_CODEC, stack);

            ItemStack filtering = this.getFilter(slot);
            inventoryEntry.putBoolean("SlotFilterEnabled", !filtering.isEmpty());
            if (!filtering.isEmpty()) {
                inventoryEntry.store("SlotFilterItem", ItemStack.OPTIONAL_CODEC, stack);
            }

            inventoryEntry.putBoolean("Disabled", this.disabled.get(slot));

            inventoryEntry.putInt("SlotLimit", this.getSlotLimit(slot));
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        Optional<ValueInput.ValueInputList> inventoryOp = input.childrenList("Inventory");
        if (inventoryOp.isEmpty()) return;
        this.filterEnabled = input.getBooleanOr("FilterEnabled", false);
        ValueInput.ValueInputList inventory = inventoryOp.get();
        int size = input.getIntOr("Size", -1);
        if (size < 0) return;
        for (ValueInput entry : inventory) {
            int slot = entry.getIntOr("Slot", -1);
            if (slot < 0) continue;

            boolean isEmptySlot = entry.getBooleanOr("IsEmptySlot", true);
            if (!isEmptySlot) {
                entry.read("SlotItem", ItemStack.OPTIONAL_CODEC).ifPresent(stack -> this.stacks.set(slot, stack));
            }

            boolean slotFilterEnabled = entry.getBooleanOr("SlotFilterEnabled", false);
            if (slotFilterEnabled) {
                entry.read("SlotFilterItem", ItemStack.OPTIONAL_CODEC).ifPresent(stack -> this.filteredItems.set(slot, stack));
            }

            this.disabled.set(slot, entry.getBooleanOr("Disabled", false));

            this.slotLimits.set(slot, entry.getIntOr("SlotLimit", IFilterBlockEntity.DEFAULT_SLOT_LIMIT));
        }
    }

    public void serializeFiltering(ValueOutput output) {
        output.store((CompoundTag) CODEC.codec().encodeStart(NbtOps.INSTANCE, this).getOrThrow());
    }

    public void deserializeFiltering(ValueInput input) {
        @SuppressWarnings("deprecation")
        Optional<FilteredItemStackHandler> handlerOp = input.read(CODEC);
        if (handlerOp.isEmpty()) return;
        FilteredItemStackHandler handler = handlerOp.get();
        if (this.size() != handler.size()) throw new IllegalArgumentException("Depository size mismatch");
        this.filterEnabled = input.getBooleanOr("filterEnabled", false);
        int size = handler.filteredItems.size();
        this.filteredItems = NonNullList.of(ItemStack.EMPTY, handler.filteredItems.toArray(new ItemStack[size]));
        this.disabled = handler.disabled;
        this.slotLimits = handler.slotLimits;
    }
}
