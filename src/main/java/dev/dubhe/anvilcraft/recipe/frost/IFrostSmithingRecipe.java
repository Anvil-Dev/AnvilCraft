package dev.dubhe.anvilcraft.recipe.frost;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.api.recipe.result.ResultContext;
import dev.dubhe.anvilcraft.api.recipe.slot.RecipeInputSlot;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

public interface IFrostSmithingRecipe extends Recipe<FrostSmithingRecipeInput> {
    @Override
    default boolean matches(FrostSmithingRecipeInput input, Level level) {
        return this.isTemplate(input.template()) && this.isMaterial(input.material()) && this.isInput(input.input());
    }

    boolean isTemplate(ItemStack template);

    boolean isMaterial(ItemStack material);

    default boolean isInput(ItemStack input) {
        for (RecipeResult result : this.inputs()) {
            if (input.is(result.result().item())) return true;
        }
        return false;
    }

    @Unmodifiable List<RecipeResult> inputs();

    default @Unmodifiable List<RecipeResult> inputs(ItemStack input) {
        int head;
        for (head = 0; head < this.inputs().size(); head++) {
            if (input.is(this.inputs().get(head).result().item())) break;
        }

        ImmutableList.Builder<RecipeResult> results = ImmutableList.builder();
        for (int i = 1; i < this.inputs().size(); i++) {
            results.add(this.inputs().get((head + i) % this.inputs().size()));
        }
        return results.build();
    }

    @Override
    default PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    default RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Deprecated
    @Override
    default ItemStack assemble(FrostSmithingRecipeInput input) {
        return ItemStack.EMPTY;
    }

    default ItemStack assemble(int selected, FrostSmithingRecipeInput inputting, Level level) {
        RecipeResult input = this.inputs(inputting.input()).get(selected);
        ItemStack result = inputting.input().transmuteCopy(input.result().item().value());
        if (input.result().components().getPatch(DataComponents.TOOL).isPresent()) {
            result.set(DataComponents.TOOL, input.result().get(DataComponents.TOOL));
        }
        if (input.result().components().getPatch(DataComponents.ATTRIBUTE_MODIFIERS).isPresent()) {
            result.set(DataComponents.ATTRIBUTE_MODIFIERS, input.result().get(DataComponents.ATTRIBUTE_MODIFIERS));
        }
        var builder = ResultContext.builder(level.registryAccess(), level.getRandom(), result)
            .slot(RecipeInputSlot.TEMPLATE, inputting.template())
            .slot(RecipeInputSlot.MATERIAL, inputting.material())
            .input(0, inputting.input());
        return input.getResult(builder.build());
    }

    @Override
    default boolean showNotification() {
        return false;
    }

    @Override
    default boolean isSpecial() {
        return true;
    }

    abstract class BaseBuilder<B extends BaseBuilder<B, R>, R extends IFrostSmithingRecipe> extends AbstractRecipeBuilder<R> {
        private ItemIngredientPredicate template;
        private ItemIngredientPredicate material;
        private final List<RecipeResult> inputs = new ArrayList<>();
        
        protected abstract B getThis();

        public B template(ItemIngredientPredicate template) {
            this.template = template;
            return this.getThis();
        }

        public B template(ItemIngredientPredicate.Builder templateBuilder) {
            return this.template(templateBuilder.build());
        }

        public B template(int count, ItemLike... templates) {
            return this.template(ItemIngredientPredicate.of(templates).withCount(count));
        }

        public B template(ItemLike... templates) {
            return this.template(1, templates);
        }

        public B template(int count, TagKey<Item> templateTag) {
            return this.template(ItemIngredientPredicate.of(templateTag).withCount(count));
        }

        public B template(TagKey<Item> templateTag) {
            return this.template(1, templateTag);
        }

        public B material(ItemIngredientPredicate material) {
            this.material = material;
            return this.getThis();
        }

        public B material(ItemIngredientPredicate.Builder materialBuilder) {
            return this.material(materialBuilder.build());
        }

        public B material(int count, ItemLike... materials) {
            return this.material(ItemIngredientPredicate.of(materials).withCount(count));
        }

        public B material(ItemLike... materials) {
            return this.material(1, materials);
        }

        public B material(int count, TagKey<Item> materialTag) {
            return this.material(ItemIngredientPredicate.of(materialTag).withCount(count));
        }

        public B material(TagKey<Item> materialTag) {
            return this.material(1, materialTag);
        }

        public B input(RecipeResult.Builder input) {
            this.inputs.add(input.build());
            return this.getThis();
        }

        public B input(Item input) {
            return this.input(RecipeResult.simple(input));
        }

        public B input(ItemLike input) {
            return this.input(input.asItem());
        }

        public B input(ItemLike input, int count) {
            return this.input(RecipeResult.simple(input).count(count));
        }

        public B input(ItemLike input, DataComponentPatch patch) {
            return this.input(RecipeResult.simple(input).withData(input, patch));
        }

        public B input(ItemLike input, int count, DataComponentPatch patch) {
            return this.input(RecipeResult.simple(input).count(count).withData(input, patch));
        }

        public B input(ItemStack result) {
            return this.input(
                RecipeResult
                    .simple(result.getItem())
                    .count(result.getCount())
                    .withData(result.getItem(), result.getComponentsPatch())
            );
        }

        @Override
        public void validate(Identifier id) {
            if (this.material.items().isEmpty()) {
                throw new IllegalArgumentException("The material of " + this.getType() + " recipe must not be empty, RecipeId: " + id);
            }
            if (this.inputs.isEmpty()) {
                throw new IllegalArgumentException("The inputs of " + this.getType() + " recipe must not be empty, RecipeId: " + id);
            }
        }

        public abstract R build(ItemIngredientPredicate template, ItemIngredientPredicate material, List<RecipeResult> inputs);

        @Override
        public R buildRecipe() {
            return this.build(this.template, this.material, this.inputs);
        }

        @Override
        public ItemStackTemplate getResult() {
            return this.inputs.getFirst().result();
        }

        @Deprecated
        @Override
        public void save(RecipeOutput output) {
            this.save(output, this.inputs.getFirst().result().typeHolder().getKey().identifier().withPrefix(this.getType() + "/"));
        }
    }
}
