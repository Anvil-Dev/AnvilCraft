package dev.dubhe.anvilcraft.recipe.anvil.util;

import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * 物品堆栈谓词接口
 * <p>
 * 定义物品堆栈匹配的通用接口，支持物品类型、组件和子谓词的匹配
 * </p>
 */
public interface IItemStackPredicate extends Predicate<ItemStack> {
    /**
     * 获取物品集合
     *
     * @return 物品集合
     */
    Optional<HolderSet<Item>> items();

    /**
     * 获取数据组件谓词
     *
     * @return 数据组件谓词
     */
    DataComponentPredicate components();

    /**
     * 获取子谓词映射
     *
     * @return 子谓词映射
     */
    Map<ItemSubPredicate.Type<?>, ItemSubPredicate> subPredicates();

    /**
     * 测试数量是否匹配
     *
     * @param count 数量
     * @return 是否匹配
     */
    boolean testCount(int count);

    /**
     * 测试物品堆栈是否匹配（忽略数量）
     *
     * @param itemStack 物品堆栈
     * @return 是否匹配
     */
    default boolean testIgnoreCount(ItemStack itemStack) {
        if (this.items().isPresent() && !itemStack.is(this.items().get())) {
            return false;
        } else if (!this.components().test(itemStack)) {
            return false;
        } else {
            for (ItemSubPredicate itemsubpredicate : this.subPredicates().values()) {
                if (!itemsubpredicate.matches(itemStack)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * 获取忽略数量的谓词
     *
     * @return 谓词
     */
    default Predicate<ItemStack> testIgnoreCount() {
        return new TestIgnoreCountPredicate(this);
    }

    /**
     * 测试忽略数量谓词的实现类
     */
    class TestIgnoreCountPredicate implements Predicate<ItemStack> {
        private final IItemStackPredicate self;

        /**
         * 构造一个测试忽略数量谓词
         *
         * @param self 物品堆栈谓词
         */
        public TestIgnoreCountPredicate(IItemStackPredicate self) {
            this.self = self;
        }

        @Override
        public boolean test(ItemStack stack) {
            return this.self.testIgnoreCount(stack);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || (obj instanceof TestIgnoreCountPredicate predicate && this.self.equals(predicate.self));
        }

        @Override
        public int hashCode() {
            return this.self.hashCode();
        }
    }
}