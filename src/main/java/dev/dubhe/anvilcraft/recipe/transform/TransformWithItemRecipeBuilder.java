package dev.dubhe.anvilcraft.recipe.transform;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TransformWithItemRecipeBuilder {
    private final List<ItemIngredientPredicate> itemIngredients;
    private final TransformResult specialResult;
    private final ItemStackTemplate itemResult;
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
        ItemStackTemplate itemResult
    ) {
        this.inputType = inputType;
        this.itemIngredients = itemIngredients;
        this.specialResult = new TransformResult(specialResult, 1d);
        this.itemResult = itemResult;
    }

    public TransformWithItemRecipeBuilder unlockedBy(String s, Criterion<?> criterion) {
        this.criteria.put(s, criterion);
        return this;
    }

    public MobTransformWithItemRecipe create() {
        return new MobTransformWithItemRecipe(
            this.inputType,
            this.itemIngredients,
            this.specialResult,
            this.itemResult,
            this.chancePercentPerItem,
            this.predicates,
            this.tagModifications,
            this.options
        );
    }

    public TransformWithItemRecipeBuilder setItemChancePercentagePerItem(int x) {
        this.chancePercentPerItem = x;
        return this;
    }

    public TransformWithItemRecipeBuilder predicate(Consumer<NumericTagValuePredicate.Builder> predicateBuilder) {
        NumericTagValuePredicate.Builder builder = NumericTagValuePredicate.builder();
        predicateBuilder.accept(builder);
        this.predicates.add(builder.build());
        return this;
    }

    /**
     * 修改生物nbt
     */
    public TransformWithItemRecipeBuilder tagModification(Consumer<TagModification.Builder> predicateBuilder) {
        TagModification.Builder builder = TagModification.builder();
        predicateBuilder.accept(builder);
        this.tagModifications.add(builder.build());
        return this;
    }

    /**
     * 生物转化额外选项
     */
    public TransformWithItemRecipeBuilder option(TransformOptions option) {
        this.options.add(option);
        return this;
    }

    public void save(RecipeOutput recipeOutput) {
        this.save(
            recipeOutput,
            AnvilCraft.of(
                BuiltInRegistries.ENTITY_TYPE.getKey(this.inputType).getPath()
                + "_to_" + BuiltInRegistries.ITEM.getKey(this.itemResult.getItem()).getPath()
            ).withPrefix("mob_transform_with_item/")
        );
    }

    public void save(RecipeOutput output, Identifier id) {
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
        Advancement.Builder advancement = output
            .advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(key))
            .rewards(AdvancementRewards.Builder.recipe(key))
            .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancement::addCriterion);
        MobTransformWithItemRecipe recipe = this.create();
        output.accept(key, recipe, advancement.build(id.withPrefix("recipe/")));
    }
}
