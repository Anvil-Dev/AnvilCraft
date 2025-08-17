package dev.dubhe.anvilcraft.recipe.anvil;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

/**
 * 世界内配方数据记录类，用于存储配方执行过程中的数据
 * 该类是一个记录类（record），包含数据的ID信息和数据提供器函数
 *
 * @param <T> 数据类型
 */
public record InWorldRecipeData<T>(
    ResourceLocation location, // 数据的ID
    BiFunction<InWorldRecipeContext, InWorldRecipeData<T>, T> supplier // 数据提供器函数，用于生成或获取数据
) {
    /**
     * 创建一个具有默认值的世界内配方数据
     *
     * @param location     ID
     * @param defaultValue 默认值
     * @param <T>          数据类型
     * @return 世界内配方数据实例
     */
    public static <T> @NotNull InWorldRecipeData<T> of(ResourceLocation location, T defaultValue) {
        return new InWorldRecipeData<>(location, (ctx, self) -> defaultValue);
    }

    /**
     * 创建一个具有提供器函数的世界内配方数据
     *
     * @param location ID
     * @param supplier 数据提供器函数
     * @param <T>      数据类型
     * @return 世界内配方数据实例
     */
    public static <T> @NotNull InWorldRecipeData<T> of(
        ResourceLocation location,
        BiFunction<InWorldRecipeContext, InWorldRecipeData<T>, T> supplier
    ) {
        return new InWorldRecipeData<>(location, supplier);
    }

    /**
     * 创建一个仅包含ID的世界内配方数据
     *
     * @param location ID
     * @param <T>      数据类型
     * @return 世界内配方数据实例
     */
    public static <T> @NotNull InWorldRecipeData<T> of(ResourceLocation location) {
        return new InWorldRecipeData<>(location, null);
    }
}