package dev.dubhe.anvilcraft.api.depository.util.forge;

import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

public class ItemDepositoryHelper {
    private ItemDepositoryHelper() {
    }

    /**
     * 将 {@link IItemHandler} 转换为 {@link IItemDepository}
     *
     * @param handler 要转换的 ItemHandler
     * @return 转换为的 ItemDepository
     */
    public static @NotNull IItemDepository toItemDepository(IItemHandler handler) {
        return new IItemDepository() {
            @Override
            public int getSlots() {
                return handler.getSlots();
            }

            @Override
            public ItemStack getStack(int slot) {
                return handler.getStackInSlot(slot);
            }

            @Override
            public void setStack(int slot, ItemStack stack) {
                ((IItemHandlerModifiable) handler).setStackInSlot(slot, stack);
            }

            @Override
            public ItemStack insert(int slot, ItemStack stack, boolean simulate, boolean notifyChanges, boolean isServer) {
                return handler.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extract(int slot, int amount, boolean simulate, boolean notifyChanges) {
                return handler.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return handler.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return handler.isItemValid(slot, stack);
            }
        };
    }

    public static IItemHandler toItemHandler(IItemDepository depository) {
        return new IItemHandler() {

            @Override
            public int getSlots() {
                return depository.getSlots();
            }

            @Override
            public @NotNull ItemStack getStackInSlot(int i) {
                return depository.getStack(i);
            }

            @Override
            public @NotNull ItemStack insertItem(int i, @NotNull ItemStack arg, boolean bl) {
                return depository.insert(i, arg, bl);
            }

            @Override
            public @NotNull ItemStack extractItem(int i, int j, boolean bl) {
                return depository.extract(i, j, bl);
            }

            @Override
            public int getSlotLimit(int i) {
                return depository.getSlotLimit(i);
            }

            @Override
            public boolean isItemValid(int i, @NotNull ItemStack arg) {
                return depository.isItemValid(i, arg);
            }
        };
    }
}
