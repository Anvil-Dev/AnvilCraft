package dev.dubhe.anvilcraft.api.depository.fabric;

import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import dev.dubhe.anvilcraft.api.depository.util.fabric.ItemDepositoryHelper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class IItemDepositoryImpl implements IItemDepository {
    private final Storage<ItemVariant> storage;
    private final List<StorageView<ItemVariant>> views;

    public IItemDepositoryImpl(Storage<ItemVariant> storage) {
        this.storage = storage;
        this.views = new ArrayList<>();
        for (StorageView<ItemVariant> view : this.storage) {
            this.views.add(view);
        }
    }

    @Override
    public int getSlots() {
        return this.views.size();
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.views.get(slot).getResource().toStack((int) this.views.get(slot).getAmount());
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        // TODO
    }

    @Override
    public ItemStack insert(int slot, @NotNull ItemStack stack, boolean simulate, boolean notifyChanges, boolean isServer) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copied = stack.copy();
        Storage<ItemVariant> handler = this.storage;
        if (views.get(slot) instanceof SingleItemStorage itemStorage) handler = itemStorage;
        else if (slot != 0) return stack;
        try (Transaction transaction = Transaction.openOuter()) {
            int filled = (int) handler.insert(ItemVariant.of(stack), stack.getCount(), transaction);
            copied.shrink(filled);
            if (!simulate && filled > 0) transaction.commit();
        }
        return copied;
    }

    @Override
    public ItemStack extract(int slot, int amount, boolean simulate, boolean notifyChanges) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack stack = this.getStack(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copied = stack.copy();
        try (Transaction transaction = Transaction.openOuter()) {
            int extracted = (int) this.views.get(slot).extract(ItemVariant.of(stack), amount, transaction);
            copied.setCount(extracted);
            if (!simulate && extracted > 0) transaction.commit();
        }
        return copied;
    }

    @Override
    public int getSlotLimit(int slot) {
        return (int) this.views.get(slot).getCapacity();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot < views.size();
    }

    @Nullable
    public static IItemDepository getItemDepository(@NotNull Level level, BlockPos pos, Direction direction) {
        Storage<ItemVariant> target = ItemStorage.SIDED.find(level, pos, direction);
        if (target == null) return null;
        return ItemDepositoryHelper.toItemDepository(target);
    }
}
