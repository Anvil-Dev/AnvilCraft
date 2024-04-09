package dev.dubhe.anvilcraft.api.depository;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

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
     * @param source 物品源
     * @param maxAmount 导出的最大数量
     * @param predicate 能够导出的数量
     * @param level 当前 Level
     * @param pos 导出目标的位置
     * @param direction 导出目标的方向
     */
    @ExpectPlatform
    public static void exportToTarget(IItemDepository source, int maxAmount, Predicate<ItemStack> predicate, Level level, BlockPos pos, Direction direction) {
        throw new AssertionError();
    }

    /**
     * 从指定位置导入物品
     * @param target 物品目标
     * @param maxAmount 导入的最大数量
     * @param predicate 能够导入的数量
     * @param level 当前 Level
     * @param pos 导入物品源的位置
     * @param direction 导入物品源的方向
     */
    @ExpectPlatform
    public static void importToTarget(IItemDepository target, int maxAmount, Predicate<ItemStack> predicate, Level level, BlockPos pos, Direction direction) {
        throw new AssertionError();
    }

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
