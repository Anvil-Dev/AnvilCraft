package dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftKubeRecipe;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ChanceItemStackComponent;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ItemIngredientPredicateComponent;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ComponentRole;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public interface ItemProcessRecipeSchema {
    @SuppressWarnings({"unused"})
    class ItemProcessKubeRecipe extends AnvilCraftKubeRecipe {
        public ItemProcessKubeRecipe requires(@NotNull TagKey<Item> ingredient, int count) {
            this.computeIfAbsent(INGREDIENTS, ArrayList::new)
                .add(ItemIngredientPredicate.Builder.item().of(ingredient).withCount(count).build());
            this.save();
            return this;
        }

        public ItemProcessKubeRecipe requires(@NotNull TagKey<Item> ingredient) {
            return this.requires(ingredient, 1);
        }

        public ItemProcessKubeRecipe requires(@NotNull ItemStack ingredient) {
            this.computeIfAbsent(INGREDIENTS, ArrayList::new)
                .add(ItemIngredientPredicate.Builder.item().of(ingredient).build());
            this.save();
            return this;
        }

        public ItemProcessKubeRecipe requires(@NotNull ItemLike ingredient, int count) {
            this.computeIfAbsent(INGREDIENTS, ArrayList::new)
                .add(ItemIngredientPredicate.Builder.item().of(ingredient).withCount(count).build());
            this.save();
            return this;
        }

        public ItemProcessKubeRecipe requires(@NotNull ItemLike ingredient) {
            return this.requires(ingredient, 1);
        }

        public ItemProcessKubeRecipe result(@NotNull ItemStack result, NumberProvider count) {
            this.computeIfAbsent(RESULTS, ArrayList::new)
                .add(ChanceItemStack.of(result, count));
            this.save();
            return this;
        }

        public ItemProcessKubeRecipe result(@NotNull ItemStack result, float chance) {
            return this.result(result, BinomialDistributionGenerator.binomial(result.getCount(), chance));
        }

        public ItemProcessKubeRecipe result(@NotNull ItemStack result) {
            return this.result(result, ConstantValue.exactly(result.getCount()));
        }

        public ItemProcessKubeRecipe result(@NotNull ItemLike result, NumberProvider count) {
            this.computeIfAbsent(RESULTS, ArrayList::new)
                .add(ChanceItemStack.of(result, count));
            this.save();
            return this;
        }

        public ItemProcessKubeRecipe result(@NotNull ItemLike result, int count, float chance) {
            return this.result(result, BinomialDistributionGenerator.binomial(count, chance));
        }

        public ItemProcessKubeRecipe result(@NotNull ItemLike result, int count) {
            return this.result(result, ConstantValue.exactly(count));
        }

        public ItemProcessKubeRecipe result(@NotNull ItemLike result, float chance) {
            return this.result(result, 1, chance);
        }

        public ItemProcessKubeRecipe result(@NotNull ItemLike result) {
            return this.result(result, ConstantValue.exactly(1.0f));
        }

        @Override
        protected void validate() {
            if (computeIfAbsent(INGREDIENTS, ArrayList::new).isEmpty()) {
                throw new KubeRuntimeException("Ingredients is Empty!").source(sourceLine);
            }
            if (computeIfAbsent(RESULTS, ArrayList::new).isEmpty()) {
                throw new KubeRuntimeException("Ingredients is Empty!").source(sourceLine);
            }
        }
    }

    RecipeKey<List<ItemIngredientPredicate>> INGREDIENTS = ItemIngredientPredicateComponent.INSTANCE
        .asList()
        .key("ingredients", ComponentRole.INPUT)
        .defaultOptional();
    RecipeKey<List<ChanceItemStack>> RESULTS = ChanceItemStackComponent.INSTANCE
        .asList()
        .key("results", ComponentRole.OUTPUT)
        .defaultOptional();

    RecipeSchema SCHEMA = new RecipeSchema(INGREDIENTS, RESULTS)
        .factory(new KubeRecipeFactory(AnvilCraft.of("item_process"), ItemProcessKubeRecipe.class, ItemProcessKubeRecipe::new))
        .constructor(INGREDIENTS, RESULTS)
        .constructor(new IDRecipeConstructor())
        .constructor();
}
