package dev.dubhe.anvilcraft.api.depository;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

public class ItemDepositoryHelper {
    private ItemDepositoryHelper() {}

    /**
     * 从指定位置获取物品存储
     *
     * @param level     维度
     * @param pos       坐标
     * @param direction 输入方向
     * @return 物品存储
     */
    public static @Nullable IItemDepository getItemDepository(
            Level level, BlockPos pos, Direction direction) {
        IItemDepository depository = ItemDepositoryHelper.getItemDepositoryFromHolder(level, pos);
        if (depository != null) return depository;
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            IItemHandler handler =
                    level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction.getOpposite());
            if (handler != null) {
                return toItemDepository(handler);
            }
        }
        List<IItemHandler> itemHandlers = level.getEntitiesOfClass(Entity.class, new AABB(pos)).stream()
                .filter(entity -> entity instanceof ContainerEntity)
                .map(entity -> entity.getCapability(Capabilities.ItemHandler.ENTITY, null))
                .filter(Objects::nonNull)
                .toList();
        if (itemHandlers.isEmpty()) return null;
        IItemHandler handler = itemHandlers.get(level.getRandom().nextInt(0, itemHandlers.size()));
        return toItemDepository(handler);
    }

    /**
     * 尝试从 {@link DepositoryHolder} 获得物品存储
     */
    @ApiStatus.Internal
    public static @Nullable IItemDepository getItemDepositoryFromHolder(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof DepositoryHolder holder) {
            return holder.getDepository();
        }
        return null;
    }

    /**
     * 向指定位置导出物品
     *
     * @param source    物品源
     * @param maxAmount 导出的最大数量
     * @param predicate 能够导出的物品
     * @param target    目标
     * @return 是否导出了物品
     */
    @SuppressWarnings("DuplicatedCode")
    public static boolean exportToTarget(
            @NotNull IItemDepository source,
            int maxAmount,
            Predicate<ItemStack> predicate,
            IItemDepository target) {
        boolean hasDone = false;
        for (int srcIndex = 0; srcIndex < source.getSlots(); srcIndex++) {
            ItemStack sourceStack = source.extract(srcIndex, Integer.MAX_VALUE, true);
            if (sourceStack.isEmpty() || !predicate.test(sourceStack)) {
                continue;
            }
            if (canInsert(sourceStack, target)) {
                // 下面这一段代码对游戏卡顿极大
                ItemStack remainder = insertItem(target, sourceStack, true);
                int amountToInsert = sourceStack.getCount() - remainder.getCount();
                if (amountToInsert > 0) {
                    sourceStack = source.extract(srcIndex, Math.min(maxAmount, amountToInsert), false);
                    int sourceStackCount = sourceStack.getCount();
                    remainder = insertItem(target, sourceStack, false);
                    hasDone = remainder.getCount() < sourceStackCount;
                    maxAmount -= Math.min(maxAmount, amountToInsert);
                    source.insert(srcIndex, remainder, false);
                }
                if (maxAmount <= 0) {
                    return hasDone;
                }
            }
        }
        return hasDone;
    }

    /**
     * 是否可以向容器中插入物品
     *
     * @param sourceStack 要插入的物品
     * @param target      目标容器
     * @return 是否可以插入物品
     */
    private static boolean canInsert(ItemStack sourceStack, IItemDepository target) {
        for (int i = 0; i < target.getSlots(); i++) {
            ItemStack targetStack = target.getStackNoClone(i);
            if (targetStack.isEmpty()) {
                return true;
            }
            if (targetStack.getCount() >= targetStack.getMaxStackSize()) {
                continue;
            }
            if (ItemStack.isSameItemSameComponents(sourceStack, targetStack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从指定位置导入物品
     *
     * @param target    物品目标
     * @param maxAmount 导入的最大数量
     * @param predicate 能够导入的物品
     * @param source    物品源
     * @return 是否导入了物品
     */
    @SuppressWarnings("DuplicatedCode")
    public static boolean importToTarget(
            IItemDepository target,
            int maxAmount,
            Predicate<ItemStack> predicate,
            @NotNull IItemDepository source) {
        boolean hasDone = false;
        for (int srcIndex = 0; srcIndex < source.getSlots(); srcIndex++) {
            ItemStack sourceStack = source.extract(srcIndex, Integer.MAX_VALUE, true);
            if (sourceStack.isEmpty() || !predicate.test(sourceStack)) {
                continue;
            }
            ItemStack remainder = insertItem(target, sourceStack, true);
            int amountToInsert = sourceStack.getCount() - remainder.getCount();
            if (amountToInsert > 0) {
                sourceStack = source.extract(srcIndex, Math.min(maxAmount, amountToInsert), false);
                int sourceStackCount = sourceStack.getCount();
                ItemStack hasNotTransferred = insertItem(target, sourceStack, false);
                hasDone = hasNotTransferred.getCount() < sourceStackCount;
                maxAmount -= Math.min(maxAmount, amountToInsert);
            }
            if (maxAmount <= 0) return hasDone;
        }
        return hasDone;
    }

    /**
     * 向 Handler 插入物品，优先尝试一样的槽位，再往空槽位插入
     *
     * @param depository 插入的地方
     * @param stack      物品堆栈
     * @param simulate   是否模拟
     * @return 未插入的物品
     */
    public static ItemStack insertItem(
            IItemDepository depository, ItemStack stack, boolean simulate) {
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
            if (canItemStackMerge(stack, slotStack)) {
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
    public static ItemStack insertToEmpty(
            IItemDepository depository, ItemStack stack, boolean simulate) {
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

    /**
     * @param a 物品堆栈 A
     * @param b 物品堆栈 B
     * @return AB能否合并
     */
    public static boolean canItemStackMerge(@Nonnull ItemStack a, @Nonnull ItemStack b) {
        if (a.isEmpty() || b.isEmpty() || a.getItem() != b.getItem()) return false;

        if (!a.isStackable()) return false;

        if (a.getComponents().isEmpty() != b.getComponents().isEmpty()) return false;

        return (!a.getComponents().isEmpty() || a.getComponents().equals(b.getComponents()));
    }

    /**
     * 复制物品堆栈
     *
     * @param stack 物品堆栈
     * @param size  数量
     * @return 物品堆栈
     */
    public static ItemStack copyStackWithSize(ItemStack stack, int size) {
        if (size == 0) {
            return ItemStack.EMPTY;
        } else {
            ItemStack copy = stack.copy();
            copy.setCount(size);
            return copy;
        }
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
            public ItemStack insert(
                    int slot, ItemStack stack, boolean simulate, boolean notifyChanges, boolean isServer) {
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

    /**
     * @param depository 物品容器
     * @return 物品容器
     */
    public static @NotNull IItemHandler toItemHandler(IItemDepository depository) {
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
