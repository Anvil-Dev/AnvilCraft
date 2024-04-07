package dev.dubhe.anvilcraft.api.depository;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public interface IItemDepository {
    /**
     * 获取该物品存储有多少槽位
     *
     * @return 该物品存储有多少槽位
     */
    int getSlots();

    /**
     * 获取指定槽位中的物品堆栈
     *
     * @param slot 槽位
     * @return 指定槽位中的物品堆栈
     */
    ItemStack getStack(int slot);

    /**
     * 向指定槽位放入物品堆栈
     * @param slot 槽位
     * @param stack 要放入的物品堆栈
     */
    void setStack(int slot, ItemStack stack);

    /**
     * 向指定槽位中插入物品
     *
     * @param slot          槽位
     * @param stack         物品堆栈
     * @param simulate      是否模拟插入
     * @param notifyChanges 是否通知更新
     * @return 剩余的物品堆栈
     */
    ItemStack insert(int slot, ItemStack stack, boolean simulate, boolean notifyChanges);


    /**
     * 向指定槽位中插入物品
     *
     * @param slot     槽位
     * @param stack    物品堆栈
     * @param simulate 是否模拟插入（模拟插入则不通知更新）
     * @return 剩余的物品堆栈
     */
    default ItemStack insert(int slot, ItemStack stack, boolean simulate) {
        return this.insert(slot, stack, simulate, !simulate);
    }

    /**
     * 从指定槽位中提取物品堆栈
     *
     * @param slot          槽位
     * @param amount        想要的物品数量
     * @param simulate      是否模拟提取
     * @param notifyChanges 是否通知更新
     * @return 实际提取到的物品堆栈
     */
    ItemStack extract(int slot, int amount, boolean simulate, boolean notifyChanges);


    /**
     * 从指定槽位中提取物品堆栈
     *
     * @param slot     槽位
     * @param amount   想要的物品数量
     * @param simulate 是否模拟提取（模拟提取则不通知更新）
     * @return 实际提取到的物品堆栈
     */
    default ItemStack extract(int slot, int amount, boolean simulate) {
        return this.extract(slot, amount, simulate, !simulate);
    }

    /**
     * 获取该物品存储指定槽位的最大存储量
     *
     * @param slot 槽位
     * @return 该物品存储指定槽位的最大存储量
     */
    int getSlotLimit(int slot);

    /**
     * 判断物品是否可以存放至指定槽位
     *
     * @param slot  槽位
     * @param stack 物品堆栈
     * @return 物品是否可以存放至指定槽位
     */
    boolean isItemValid(int slot, ItemStack stack);

    /**
     * 在物品存储更新后触发
     */
    default void onContentsChanged() {
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
    static @Nullable IItemDepository getItemDepository(Level level, BlockPos pos, Direction direction) {
        return null;
        // throw new AssertionError();
    }
}
