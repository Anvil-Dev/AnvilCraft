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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

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
        return size;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return filterList.get(slot);
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
        filterList.set(slot, stack);
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

    public void serialize(ValueOutput output) {
        int slots = this.filterList.size();
        output.putInt("Size", slots);
        ValueOutput.ValueOutputList items = output.childrenList("Items");
        for (int i = 0; i < slots; i++) {
            ItemStack stack = this.filterList.get(i);
            if (stack.isEmpty()) continue;
            ValueOutput entry = items.addChild();
            entry.putInt("Slot", i);
            entry.store("Item", ItemStack.OPTIONAL_CODEC, stack);
        }
    }

    public void deserialize(ValueInput input) {
        this.size = input.getIntOr("Size", this.filterList.size());
        this.filterList = NonNullList.withSize(this.size, ItemStack.EMPTY);
        Optional<ValueInput.ValueInputList> itemsOp = input.childrenList("Items");
        if (itemsOp.isEmpty()) return;
        for (ValueInput entry : itemsOp.get()) {
            int slot = entry.getIntOr("Slot", -1);
            if (slot < 0 || slot >= this.filterList.size()) continue;
            entry.read("Item", ItemStack.OPTIONAL_CODEC).ifPresent(stack -> this.filterList.set(slot, stack));
        }
    }
}
