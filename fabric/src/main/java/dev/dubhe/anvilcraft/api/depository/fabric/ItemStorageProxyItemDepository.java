package dev.dubhe.anvilcraft.api.depository.fabric;

import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class ItemStorageProxyItemDepository implements IItemDepository {
    private final Storage<ItemVariant> storage;
    private final List<StorageView<ItemVariant>> views;
    private static final Method METHOD_GET_STACK;

    static {
        try {
            Method method = SingleStackStorage.class.getDeclaredMethod("getStack");
            method.setAccessible(true);
            METHOD_GET_STACK = method;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 物品存储代理
     *
     * @param storage 物品存储
     */
    public ItemStorageProxyItemDepository(Storage<ItemVariant> storage) {
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
        StorageView<ItemVariant> view = this.views.get(slot);
        if (view instanceof SingleStackStorage singleStackStorage) {
            try {
                return ((ItemStack) METHOD_GET_STACK.invoke(singleStackStorage)).copy();
            } catch (Throwable ignored) {
                return view.getResource().toStack(view.getAmount() > 0x7fffffff ? 0x7fffffff : (int) view.getAmount());
            }
        }
        return view.getResource().toStack(view.getAmount() > 0x7fffffff ? 0x7fffffff : (int) view.getAmount());
    }

    @Override
    public ItemStack getStackNoClone(int slot) {
        StorageView<ItemVariant> view = this.views.get(slot);
        if (view instanceof SingleStackStorage singleStackStorage) {
            try {
                return ((ItemStack) METHOD_GET_STACK.invoke(singleStackStorage));
            } catch (Throwable ignored) {
                return view.getResource().toStack((int) view.getAmount());
            }
        }
        return view.getResource().toStack((int) view.getAmount());
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        // TODO
    }

    @Override
    public ItemStack insert(
            int slot, @NotNull ItemStack stack, boolean simulate, boolean notifyChanges, boolean isServer
    ) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copied = stack.copy();
        Storage<ItemVariant> handler = this.storage;
        if (views.get(slot) instanceof SingleItemStorage itemStorage) {
            if (slot != 0) return stack;
            handler = itemStorage;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            int filled;
            if (simulate) {
                filled =
                        (int) StorageUtil.simulateInsert(handler, ItemVariant.of(stack), stack.getCount(), transaction);
            } else {
                filled = (int) handler.insert(ItemVariant.of(stack), stack.getCount(), transaction);
            }
            copied.shrink(filled);
            if (filled > 0) transaction.commit();
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
            int extracted;
            if (simulate) {
                extracted =
                        (int) StorageUtil.simulateExtract(
                                this.views.get(slot),
                                ItemVariant.of(stack),
                                amount, transaction
                        );
            } else {
                extracted = (int) this.views.get(slot).extract(ItemVariant.of(stack), amount, transaction);
            }
            copied.setCount(extracted);
            if (extracted > 0) transaction.commit();
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

}
