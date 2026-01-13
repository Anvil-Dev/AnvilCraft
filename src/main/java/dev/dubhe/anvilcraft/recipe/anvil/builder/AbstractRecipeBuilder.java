package dev.dubhe.anvilcraft.recipe.anvil.builder;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 抽象配方构建器类，用于构建各种类型的配方
 * 该类实现了 RecipeBuilder 接口，提供了构建配方的基本功能
 *
 * @param <T> 配方类型
 */
public abstract class AbstractRecipeBuilder<T extends Recipe<?>> implements RecipeBuilder {
    /**
     * 存储配方条件的映射表
     */
    protected final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    /**
     * 添加解锁条件
     *
     * @param name      条件名称
     * @param criterion 条件
     * @return 配方构建器实例
     */
    @Override
    public RecipeBuilder unlockedBy(String name, Criterion<?> criterion) {
        criteria.put(name, criterion);
        return this;
    }

    /**
     * 设置配方组
     *
     * @param groupName 配方组名称
     * @return 配方构建器实例
     */
    @Override
    public RecipeBuilder group(@Nullable String groupName) {
        return this;
    }

    /**
     * 保存配方到指定位置
     *
     * @param output 配方输出
     * @param id 配方ID
     */
    @Override
    public void save(RecipeOutput output, ResourceLocation id) {
        validate(id);
        Advancement.Builder advancement = output
            .advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .rewards(AdvancementRewards.Builder.recipe(id))
            .requirements(AdvancementRequirements.Strategy.OR);
        criteria.forEach(advancement::addCriterion);
        T recipe = buildRecipe();
        output.accept(id, recipe, advancement.build(id.withPrefix("recipes/")));
    }

    /**
     * 保存配方到指定位置
     *
     * @param output 配方输出
     * @param id 配方ID的字符串形式
     */
    @Override
    public void save(RecipeOutput output, String id) {
        save(output, AnvilCraft.of(id).withPrefix(getType() + "/"));
    }

    /**
     * 保存配方
     *
     * @param recipeOutput 配方输出
     */
    @Override
    public void save(RecipeOutput recipeOutput) {
        save(recipeOutput, BuiltInRegistries.ITEM.getKey(getResult()).getPath());
    }

    /**
     * 构建配方
     *
     * @return 配方实例
     */
    public abstract T buildRecipe();

    /**
     * 验证配方参数
     *
     * @param id 配方ID
     */
    public abstract void validate(ResourceLocation id);

    /**
     * 获取配方类型
     *
     * @return 配方类型
     */
    public abstract String getType();

    /**
     * 获取配方结果物品
     *
     * @return 配方结果物品
     */
    public abstract Item getResult();
}