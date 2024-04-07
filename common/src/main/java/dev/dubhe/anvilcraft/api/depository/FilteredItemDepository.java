package dev.dubhe.anvilcraft.api.depository;

import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;


@Getter
public class FilteredItemDepository extends ItemDepository {

    private boolean filterEnabled = false;
    private final NonNullList<Item> filteredItems;

    public FilteredItemDepository(int size) {
        super(size);
        filteredItems = NonNullList.withSize(size, Items.AIR);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (!filterEnabled) return true;
        return filteredItems.get(slot) == stack.getItem();
    }

    public boolean isDisabled(int slot) {
        return filteredItems.get(slot) == Items.AIR;
    }

    @Override
    public @NotNull CompoundTag serializeNBT() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putBoolean("filterEnabled", filterEnabled);
        ListTag listTag = new ListTag();
        int slots = this.getSlots();
        compoundTag.putInt("Size", slots);
        for (int i = 0; i < slots; i++) {
            ItemStack stack = this.getStack(i);
            ResourceLocation filterItemId = BuiltInRegistries.ITEM.getKey(filteredItems.get(i));
            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("Slot", i);
            stack.save(itemTag);
            itemTag.putString("filtered", filterItemId.toString());
            listTag.add(itemTag);
        }
        if (!listTag.isEmpty()) compoundTag.put("Items", listTag);
        return compoundTag;
    }

    @Override
    public void deserializeNBT(@NotNull CompoundTag tag) {
        if (!tag.contains("Items")) return;
        filterEnabled = tag.getBoolean("filterEnabled");
        ListTag listTag = tag.getList("Items", Tag.TAG_COMPOUND);
        int slots = this.getSlots();
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag itemTag = listTag.getCompound(i);
            int slot = itemTag.getInt("Slot");
            if (slot < 0 || slot >= slots) continue;
            this.getStacks().set(slot, ItemStack.of(itemTag));
            ResourceLocation filterItemId = new ResourceLocation(itemTag.getString("filtered"));
            this.filteredItems.set(i, BuiltInRegistries.ITEM.get(filterItemId));
        }
    }

    public static class Pollable extends FilteredItemDepository {

        public Pollable(int size) {
            super(size);
        }

        private int getEmptyOrSmallerSlot(ItemStack stack) {
            int slotCount = this.getSlots();
            int slot = -1;
            int countInSlot = Integer.MAX_VALUE;
            for (int i = 0; i < slotCount; i++) {
                Item filterItem = this.getFilteredItems().get(i);
                ItemStack stackInSlot = this.getStack(i);
                if (stackInSlot.isEmpty() && (!isFilterEnabled() || filterItem == stack.getItem())) return i;
                if (!ItemStack.isSameItemSameTags(stackInSlot, stack)) continue;
                int stackInSlotCount = stackInSlot.getCount();
                if (stackInSlotCount < countInSlot && stackInSlotCount < this.getSlotLimit(i)) {
                    slot = i;
                    countInSlot = stackInSlotCount;
                }

            }
            return slot;
        }
    }
}
