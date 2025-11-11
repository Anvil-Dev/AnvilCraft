package dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil;

import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftKubeRecipe;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ChanceItemStackComponent;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ItemIngredientPredicateComponent;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.ArrayList;
import java.util.List;

public interface UnpackRecipeSchema {
    @SuppressWarnings({"unused"})
    class UnpackKubeRecipe extends AnvilCraftKubeRecipe {
        public UnpackKubeRecipe requires(TagKey<Item> ingredient, int count) {
            this.computeIfAbsent(INGREDIENTS, ArrayList::new)
                .add(ItemIngredientPredicate.Builder.item().of(ingredient).withCount(count).build());
            this.save();
            return this;
        }

        public UnpackKubeRecipe requires(TagKey<Item> ingredient) {
            return this.requires(ingredient, 1);
        }

        public UnpackKubeRecipe requires(ItemStack ingredient) {
            this.computeIfAbsent(INGREDIENTS, ArrayList::new)
                .add(ItemIngredientPredicate.Builder.item().of(ingredient).build());
            this.save();
            return this;
        }

        public UnpackKubeRecipe requires(ItemLike ingredient, int count) {
            this.computeIfAbsent(INGREDIENTS, ArrayList::new)
                .add(ItemIngredientPredicate.Builder.item().of(ingredient).withCount(count).build());
            this.save();
            return this;
        }

        public UnpackKubeRecipe requires(ItemLike ingredient) {
            return this.requires(ingredient, 1);
        }

        public UnpackKubeRecipe result(ItemStack result, NumberProvider count) {
            this.computeIfAbsent(RESULTS, ArrayList::new)
                .add(ChanceItemStack.of(result, count));
            this.save();
            return this;
        }

        public UnpackKubeRecipe result(ItemStack result, float chance) {
            return this.result(result, BinomialDistributionGenerator.binomial(result.getCount(), chance));
        }

        public UnpackKubeRecipe result(ItemStack result) {
            return this.result(result, ConstantValue.exactly(result.getCount()));
        }

        public UnpackKubeRecipe result(ItemLike result, NumberProvider count) {
            this.computeIfAbsent(RESULTS, ArrayList::new)
                .add(ChanceItemStack.of(result, count));
            this.save();
            return this;
        }

        public UnpackKubeRecipe result(ItemLike result, int count, float chance) {
            return this.result(result, BinomialDistributionGenerator.binomial(count, chance));
        }

        public UnpackKubeRecipe result(ItemLike result, int count) {
            return this.result(result, ConstantValue.exactly(count));
        }

        public UnpackKubeRecipe result(ItemLike result, float chance) {
            return this.result(result, 1, chance);
        }

        public UnpackKubeRecipe result(ItemLike result) {
            return this.result(result, ConstantValue.exactly(1.0f));
        }

        @Override
        protected void validate() {
        }
    }

    RecipeKey<List<ItemIngredientPredicate>> INGREDIENTS = ItemIngredientPredicateComponent.INSTANCE
        .asList()
        .inputKey("ingredients")
        .defaultOptional();
    RecipeKey<List<ChanceItemStack>> RESULTS = ChanceItemStackComponent.INSTANCE
        .asList()
        .inputKey("results")
        .defaultOptional();

    RecipeSchema SCHEMA = new RecipeSchema(INGREDIENTS, RESULTS)
        .factory(new KubeRecipeFactory(AnvilCraft.of("unpack"), UnpackKubeRecipe.class, UnpackKubeRecipe::new))
        .constructor(INGREDIENTS, RESULTS)
        .constructor(new IDRecipeConstructor())
        .constructor();
}
