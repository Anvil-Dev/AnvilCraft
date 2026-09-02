package dev.dubhe.anvilcraft.api.itemhandler.unlimited;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.item.ItemStack;

/**
 * 可切换溢出销毁模式的物品处理器：内容上限与普通板条箱一致（按空间计 2048 单位），
 * 当 {@link #dispose} 为 true 时，超过上限的输入不会返回剩余物品，而是被无提示销毁。
 *
 * <p>未超上限或 dispose=false 时行为与 {@link SpaceSizeItemStacksResourceHandler}
 * 一致。dispose=true 时模拟插入（simulate=true）同样按「可存放多少、销毁多少」
 * 回答，因此管道 / 仓储 GUI 的容量判断会认为所有输入都能被接收。</p>
 */
public class OverflowDisposalItemStacksResourceHandler extends SpaceSizeItemStacksResourceHandler {

    @Getter
    @Setter
    private boolean dispose;

    public OverflowDisposalItemStacksResourceHandler(int spaceSize) {
        super(spaceSize);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!this.dispose) {
            return super.insertItem(slot, stack, simulate);
        }
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (slot >= this.stacks.size() && slot >= this.getSlots()) {
            // 无效增长槽：与基类约定一致原样返回，避免 ItemHandlerHelper 类遍历无限增长
            return stack;
        }
        ItemStack leftover = super.insertItem(slot, stack, simulate);
        if (leftover.getCount() == stack.getCount() && slot < this.stacks.size()
            && !this.stacks.get(slot).isEmpty()
            && !this.stacks.get(slot).isSameItemSameComponents(stack)) {
            // 槽内已被其它类型占用：基类原样拒绝，但这不是溢出——板条箱为稀疏存储
            // （每类型一槽），该类型仍可存入其它槽或作为新类型追加。改走无槽插入
            // 跨槽合并 / 新增类型，剩余部分才按 dispose 语义销毁。
            // 基类无槽遍历只对空槽 / 同类型槽调用槽位版，不会再次进入本分支，无递归。
            return this.insertItem(stack, simulate);
        }
        // 有实际插入（剩余即空间不足的溢出部分）或箱子空间已满完全拒绝：
        // 均为 dispose 语义下的溢出，剩余部分直接销毁
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        if (this.dispose) {
            // 无论放得下多少，剩余部分都视为已销毁，调用方始终收到空手
            super.insertItem(stack, simulate);
            return ItemStack.EMPTY;
        }
        return super.insertItem(stack, simulate);
    }
}
