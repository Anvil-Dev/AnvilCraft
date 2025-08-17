package dev.dubhe.anvilcraft.recipe.transform;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TransformWithItemRecipeBuilder {
    private final List<ItemIngredientPredicate> itemIngredients;
    private final TransformResult specialResult;
    private final ItemStack itemResult;
    private int chancePercentPerItem = 5;
    protected final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    private final EntityType<?> inputType;
    private final List<NumericTagValuePredicate> predicates = new ArrayList<>();
    private final List<TagModification> tagModifications = new ArrayList<>();
    private final List<TransformOptions> options = new ArrayList<>();

    public TransformWithItemRecipeBuilder(
        EntityType<?> inputType,
        List<ItemIngredientPredicate> itemIngredients,
        EntityType<?> specialResult,
        ItemStack itemResult) {
        this.inputType = inputType;
        this.itemIngredients = itemIngredients;
        this.specialResult = new TransformResult(specialResult, 1d);
        this.itemResult = itemResult;
    }

    public TransformWithItemRecipeBuilder unlockedBy(String s, Criterion<?> criterion) {
        criteria.put(s, criterion);
        return this;
    }

    public MobTransformWithItemRecipe create() {
        return new MobTransformWithItemRecipe(
            inputType,
            itemIngredients,
            specialResult,
            itemResult,
            chancePercentPerItem,
            predicates, tagModifications, options);
    }

    public TransformWithItemRecipeBuilder setItemChancePercentagePerItem(int x) {
        chancePercentPerItem = x;
        return this;
    }

    /**
     *
     */
    public TransformWithItemRecipeBuilder predicate(@NotNull Consumer<NumericTagValuePredicate.Builder> predicateBuilder) {
        NumericTagValuePredicate.Builder builder = NumericTagValuePredicate.builder();
        predicateBuilder.accept(builder);
        predicates.add(builder.build());
        return this;
    }

    /**
     * 修改生物nbt
     */
    public TransformWithItemRecipeBuilder tagModification(@NotNull Consumer<TagModification.Builder> predicateBuilder) {
        TagModification.Builder builder = TagModification.builder();
        predicateBuilder.accept(builder);
        tagModifications.add(builder.build());
        return this;
    }

    /**
     * 生物转化额外选项
     */
    public TransformWithItemRecipeBuilder option(TransformOptions option) {
        options.add(option);
        return this;
    }

    public void save(RecipeOutput recipeOutput) {
        save(
            recipeOutput,
            AnvilCraft.of(
                    BuiltInRegistries.ENTITY_TYPE.getKey(inputType).getPath()
                        + "_to_" + BuiltInRegistries.ITEM.getKey(itemResult.getItem()).getPath())
                .withPrefix("mob_transform_with_item/"));
    }

    public void save(RecipeOutput pRecipeOutput, ResourceLocation pId) {
        Advancement.Builder advancement = pRecipeOutput
            .advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pId))
            .rewards(AdvancementRewards.Builder.recipe(pId))
            .requirements(AdvancementRequirements.Strategy.OR);
        criteria.forEach(advancement::addCriterion);
        MobTransformWithItemRecipe recipe = create();
        pRecipeOutput.accept(pId, recipe, advancement.build(pId.withPrefix("recipe/")));
    }
}
