package dev.dubhe.anvilcraft.api.depository.forge;

import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ItemDepositoryHelperImpl {
    private ItemDepositoryHelperImpl() {
    }

    @Nullable
    public static IItemDepository getItemDepository(@NotNull Level level, BlockPos pos, Direction direction) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            LazyOptional<IItemHandler> capability = be.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite());
            if (capability.isPresent() && capability.resolve().isPresent()) {
                return toItemDepository(capability.resolve().get());
            }
        }
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, new AABB(pos));
        for (Entity entity : entities) {
            LazyOptional<IItemHandler> capability = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite());
            if (capability.isPresent() && capability.resolve().isPresent()) {
                return toItemDepository(capability.resolve().get());
            }
        }
        return null;
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

    /**
     * 向 Handler 插入物品，优先尝试一样的槽位，再往空槽位插入
     *
     * @param handler  插入的地方
     * @param stack    物品堆栈
     * @param simulate 是否模拟
     * @return 未插入的物品
     */
    public static ItemStack insertItem(IItemHandler handler, ItemStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        if (!stack.isStackable()) {
            return insertToEmpty(handler, stack, simulate);
        }

        IntList emptySlots = new IntArrayList();
        int slots = handler.getSlots();

        for (int i = 0; i < slots; i++) {
            ItemStack slotStack = handler.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                emptySlots.add(i);
            }
            if (ItemHandlerHelper.canItemStacksStackRelaxed(stack, slotStack)) {
                stack = handler.insertItem(i, stack, simulate);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }

        for (int slot : emptySlots) {
            stack = handler.insertItem(slot, stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    /**
     * 向 Handler 插入物品，只向空槽位插入物品，适合不可堆叠物品
     *
     * @param handler  插入的地方
     * @param stack    物品堆栈
     * @param simulate 是否模拟
     * @return 未插入的物品
     */
    public static ItemStack insertToEmpty(IItemHandler handler, ItemStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack slotStack = handler.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                stack = handler.insertItem(i, stack, simulate);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }

    public static void exportToTarget(IItemDepository source, int maxAmount, Predicate<ItemStack> predicate, Level level, BlockPos pos, Direction direction) {
        if (level.getBlockState(pos).hasBlockEntity()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                Optional<IItemHandler> cap = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction).resolve();
                if (cap.isPresent()) {
                    IItemHandler target = cap.get();
                    for (int srcIndex = 0; srcIndex < source.getSlots(); srcIndex++) {
                        ItemStack sourceStack = source.extract(srcIndex, Integer.MAX_VALUE, true);
                        if (sourceStack.isEmpty() || !predicate.test(sourceStack)) {
                            continue;
                        }
                        ItemStack remainder = insertItem(target, sourceStack, true);
                        int amountToInsert = sourceStack.getCount() - remainder.getCount();
                        if (amountToInsert > 0) {
                            sourceStack = source.extract(srcIndex, Math.min(maxAmount, amountToInsert), false);
                            insertItem(target, sourceStack, false);
                            maxAmount -= Math.min(maxAmount, amountToInsert);
                        }
                        if (maxAmount <= 0) return;
                    }
                }
            }
        }
    }

    /**
     * 向 Handler 插入物品，优先尝试一样的槽位，再往空槽位插入
     *
     * @param depository 插入的地方
     * @param stack      物品堆栈
     * @param simulate   是否模拟
     * @return 未插入的物品
     */
    public static ItemStack insertItem(IItemDepository depository, ItemStack stack, boolean simulate) {
        if (depository == null || stack.isEmpty()) {
            return stack;
        }
        if (!stack.isStackable()) {
            return insertToEmpty(depository, stack, simulate);
        }

        IntList emptySlots = new IntArrayList();
        int slots = depository.getSlots();

        for (int i = 0; i < slots; i++) {
            ItemStack slotStack = depository.getStack(i);
            if (slotStack.isEmpty()) {
                emptySlots.add(i);
            }
            if (ItemHandlerHelper.canItemStacksStackRelaxed(stack, slotStack)) {
                stack = depository.insert(i, stack, simulate);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }

        for (int slot : emptySlots) {
            stack = depository.insert(slot, stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    /**
     * 向 Handler 插入物品，只向空槽位插入物品，适合不可堆叠物品
     *
     * @param depository 插入的地方
     * @param stack      物品堆栈
     * @param simulate   是否模拟
     * @return 未插入的物品
     */
    public static ItemStack insertToEmpty(IItemDepository depository, ItemStack stack, boolean simulate) {
        if (depository == null || stack.isEmpty()) {
            return stack;
        }
        int slots = depository.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack slotStack = depository.getStack(i);
            if (slotStack.isEmpty()) {
                stack = depository.insert(i, stack, simulate);
                if (stack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }

    public static void importToTarget(IItemDepository target, int maxAmount, Predicate<ItemStack> predicate, Level level, BlockPos pos, Direction direction) {
        if (level.getBlockState(pos).hasBlockEntity()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                Optional<IItemHandler> cap = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction).resolve();
                if (cap.isPresent()) {
                    IItemHandler source = cap.get();
                    for (int srcIndex = 0; srcIndex < source.getSlots(); srcIndex++) {
                        ItemStack sourceStack = source.extractItem(srcIndex, Integer.MAX_VALUE, true);
                        if (sourceStack.isEmpty() || !predicate.test(sourceStack)) {
                            continue;
                        }
                        ItemStack remainder = insertItem(target, sourceStack, true);
                        int amountToInsert = sourceStack.getCount() - remainder.getCount();
                        if (amountToInsert > 0) {
                            sourceStack = source.extractItem(srcIndex, Math.min(maxAmount, amountToInsert), false);
                            insertItem(target, sourceStack, false);
                            maxAmount -= Math.min(maxAmount, amountToInsert);
                        }
                        if (maxAmount <= 0) return;
                    }
                }
            }
        }
    }


}
