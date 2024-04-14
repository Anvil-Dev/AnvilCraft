package dev.dubhe.anvilcraft.api.depository;

import net.minecraft.world.item.ItemStack;

/**
 * 物品容器
 */
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
     *
     * @param slot  槽位
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
     * @param isServer      是否在服务端调用
     * @return 剩余的物品堆栈
     */
    ItemStack insert(int slot, ItemStack stack, boolean simulate, boolean notifyChanges, boolean isServer);

    /**
     * 向指定槽位中插入物品
     *
     * @param slot          槽位
     * @param stack         物品堆栈
     * @param simulate      是否模拟插入
     * @param notifyChanges 是否通知更新
     * @return 剩余的物品堆栈
     */
    default ItemStack insert(int slot, ItemStack stack, boolean simulate, boolean notifyChanges) {
        return this.insert(slot, stack, simulate, !simulate, true);
    }


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
     * 判断物品是否可以进入指定槽位
     * 注：该方法不建议用于客户端侧判定
     *
     * @param slot  槽位
     * @param stack 物品堆栈
     * @return 物品是否可以进入指定槽位
     */
    default boolean canPlaceItem(int slot, ItemStack stack) {
        return this.isItemValid(slot, stack);
    }

    /**
     * 在物品存储更新后触发
     */
    default void onContentsChanged() {
    }

}
