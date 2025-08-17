package dev.dubhe.anvilcraft.recipe.anvil;

import dev.dubhe.anvilcraft.init.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * 配方结果接口，定义了世界内配方的结果行为
 * 实现该接口的类表示配方执行后会产生的一种结果
 *
 * @param <O> 配方结果类型
 */
public interface IRecipeOutcome<O extends IRecipeOutcome<O>> extends Consumer<InWorldRecipeContext>, IPrioritized {
    /**
     * 获取配方结果的类型
     *
     * @return 配方结果类型
     */
    Type<O> getType();

    /**
     * 获取配方结果的触发概率，默认为1.0（100%）
     *
     * @return 触发概率
     */
    default NumberProvider getChance() {
        return ConstantValue.exactly(1.0f);
    }

    /**
     * 根据概率判断是否执行配方结果
     *
     * @param context 配方上下文
     */
    default void acceptWithChance(@NotNull InWorldRecipeContext context) {
        ServerLevel level = context.getLevel();
        if (level.getRandom().nextDouble() > context.getFloat(this.getChance())) return;
        this.accept(context);
    }

    /**
     * 配方结果类型接口，继承自序列化接口
     *
     * @param <O> 配方结果类型
     */
    interface Type<O extends IRecipeOutcome<O>> extends ISerializer<O> {
        /**
         * 获取配方结果类型的ID
         *
         * @return ID
         */
        default ResourceLocation getId() {
            return ModRegistries.OUTCOME_TYPE_REGISTRY.getKey(this);
        }
    }
}