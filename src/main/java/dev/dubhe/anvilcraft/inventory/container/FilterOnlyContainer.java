package dev.dubhe.anvilcraft.inventory.container;

import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

public class FilterOnlyContainer implements Container {

    private int size;
    @Getter
    private NonNullList<ItemStack> filterList;
    @Getter
    @Nullable
    private final BlockEntity blockEntity;

    public FilterOnlyContainer(@Nullable BlockEntity blockEntity, int size) {
        this.blockEntity = blockEntity;
        this.size = size;
        this.filterList = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public int getContainerSize() {
        return this.size;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.filterList.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.filterList.set(slot, stack);
    }

    @Override
    public void setChanged() {
        if (this.blockEntity != null) {
            this.blockEntity.setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {

    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        ListTag nbtTagList = new ListTag();
        for (int i = 0; i < this.filterList.size(); i++) {
            if (!this.filterList.get(i).isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                nbtTagList.add(this.filterList.get(i).save(provider, itemTag));
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", nbtTagList);
        nbt.putInt("Size", this.filterList.size());
        return nbt;
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        this.size = nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : this.filterList.size();
        this.filterList = NonNullList.withSize(this.size, ItemStack.EMPTY);
        ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTags = tagList.getCompound(i);
            int slot = itemTags.getInt("Slot");
            if (slot >= 0 && slot < this.filterList.size()) {
                ItemStack.parse(provider, itemTags).ifPresent(stack -> this.filterList.set(slot, stack));
            }
        }
    }
}
