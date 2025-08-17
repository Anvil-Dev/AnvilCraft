package dev.dubhe.anvilcraft.recipe.anvil;

import dev.dubhe.anvilcraft.init.ModRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 配方谓词接口，用于判断配方是否满足执行条件
 * 实现该接口的类表示配方执行前需要满足的一种条件
 *
 * @param <P> 配方谓词类型
 */
public interface IRecipePredicate<P extends IRecipePredicate<P>> extends Predicate<InWorldRecipeContext>, Consumer<InWorldRecipeContext>, IPrioritized {
    /**
     * 接受配方上下文，默认为空实现
     *
     * @param context 配方上下文
     */
    @Override
    default void accept(InWorldRecipeContext context) {
    }

    /**
     * 对配方上下文进行快照
     *
     * @param context 配方上下文
     */
    default void snapshot(InWorldRecipeContext context) {
    }

    /**
     * 回滚配方上下文
     *
     * @param context 配方上下文
     */
    default void rollback(InWorldRecipeContext context) {
    }

    /**
     * 获取配方谓词的类型
     *
     * @return 配方谓词类型
     */
    Type<P> getType();

    /**
     * 配方谓词类型接口，继承自序列化接口
     *
     * @param <P> 配方谓词类型
     */
    interface Type<P extends IRecipePredicate<P>> extends ISerializer<P> {
        /**
         * 获取配方谓词类型的ID
         *
         * @return ID
         */
        default ResourceLocation getId() {
            return ModRegistries.PREDICATE_TYPE_REGISTRY.getKey(this);
        }

        /**
         * 判断是否存在冲突，默认为false
         *
         * @return 是否存在冲突
         */
        default boolean conflict() {
            return false;
        }
    }
}