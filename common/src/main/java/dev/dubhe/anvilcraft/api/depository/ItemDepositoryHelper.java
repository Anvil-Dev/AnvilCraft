package dev.dubhe.anvilcraft.api.depository;

import dev.architectury.injectables.annotations.ExpectPlatform;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

public class ItemDepositoryHelper {
    private ItemDepositoryHelper() {
    }

    /**
     * 从指定位置获取物品存储
     *
     * @param level     维度
     * @param pos       坐标
     * @param direction 输入方向
     * @return 物品存储
     */
    @ExpectPlatform
    public static @Nullable IItemDepository getItemDepository(Level level, BlockPos pos, Direction direction) {
        throw new AssertionError();
    }

    /**
     * 向指定位置导出物品
     *
     * @param source    物品源
     * @param maxAmount 导出的最大数量
     * @param predicate 能够导出的物品
     * @param level     当前 Level
     * @param pos       导出目标的位置
     * @param direction 导出目标的方向
     */
    @SuppressWarnings({"DuplicatedCode", "UnreachableCode"})
    public static void exportToTarget(
        IItemDepository source,
        int maxAmount,
        Predicate<ItemStack> predicate,
        @NotNull Level level,
        BlockPos pos,
        Direction direction
    ) {
        if (level.getBlockState(pos).hasBlockEntity()) {
            IItemDepository target = getItemDepository(level, pos, direction);
            for (int srcIndex = 0; srcIndex < source.getSlots(); srcIndex++) {
                ItemStack sourceStack = source.extract(srcIndex, Integer.MAX_VALUE, true);
                if (sourceStack.isEmpty() || !predicate.test(sourceStack)) {
                    continue;
                }
                ItemStack remainder = insertItem(target, sourceStack, true);
                int amountToInsert = sourceStack.getCount() - remainder.getCount();
                if (amountToInsert > 0) {
                    sourceStack = source.extract(srcIndex, Math.min(maxAmount, amountToInsert), false);
                    remainder = insertItem(target, sourceStack, false);
                    maxAmount -= Math.min(maxAmount, amountToInsert);
                    source.insert(srcIndex, remainder, false);
                }
                if (maxAmount <= 0) return;
            }
        }
    }

    /**
     * 从指定位置导入物品
     *
     * @param target    物品目标
     * @param maxAmount 导入的最大数量
     * @param predicate 能够导入的物品
     * @param level     当前 Level
     * @param pos       导入物品源的位置
     * @param direction 导入物品源的方向
     */
    @SuppressWarnings({"DuplicatedCode", "UnreachableCode"})
    public static void importToTarget(
        IItemDepository target,
        int maxAmount,
        Predicate<ItemStack> predicate,
        @NotNull Level level,
        BlockPos pos,
        Direction direction
    ) {
        if (level.getBlockState(pos).hasBlockEntity()) {
            IItemDepository source = getItemDepository(level, pos, direction);
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
            if (canItemStacksStackRelaxed(stack, slotStack)) {
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

    /**
     * @param a 物品堆栈 A
     * @param b 物品堆栈 B
     * @return AB能否合并
     */
    public static boolean canItemStacksStackRelaxed(@Nonnull ItemStack a, @Nonnull ItemStack b) {
        if (a.isEmpty() || b.isEmpty() || a.getItem() != b.getItem())
            return false;

        if (!a.isStackable())
            return false;

        if (a.hasTag() != b.hasTag())
            return false;

        return (!a.hasTag() || a.getOrCreateTag().equals(b.getTag()));
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
}
