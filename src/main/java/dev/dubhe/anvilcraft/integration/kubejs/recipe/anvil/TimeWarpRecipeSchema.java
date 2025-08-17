package dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftKubeRecipe;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftRecipeComponents;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ChanceItemStackComponent;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ItemIngredientPredicateComponent;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.WrapUtils;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.latvian.mods.kubejs.error.KubeRuntimeException;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ComponentRole;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.KubeRecipeFactory;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public interface TimeWarpRecipeSchema {
    @SuppressWarnings({"unused"})
    class TimeWarpKubeRecipe extends AnvilCraftKubeRecipe {
        public TimeWarpKubeRecipe cauldron(ResourceLocation fluid) {
            this.setValue(FLUID, fluid);
            this.save();
            return this;
        }

        public TimeWarpKubeRecipe cauldron(Block cauldron) {
            return this.cauldron(WrapUtils.cauldron2Fluid(cauldron));
        }

        public TimeWarpKubeRecipe transform(ResourceLocation transform) {
            this.setValue(TRANSFORM, transform);
            this.save();
            return this;
        }

        public TimeWarpKubeRecipe transform(Block transform) {
            return this.transform(WrapUtils.cauldron2Fluid(transform));
        }

        public TimeWarpKubeRecipe produceFluid(boolean produceFluid) {
            if (!produceFluid) return this;
            this.setValue(CONSUME, -1);
            this.save();
            return this;
        }

        public TimeWarpKubeRecipe consumeFluid(boolean consumeFluid) {
            if (!consumeFluid) return this;
            this.setValue(CONSUME, 1);
            this.save();
            return this;
        }

        public TimeWarpKubeRecipe requires(@NotNull TagKey<Item> ingredient, int count) {
            this.computeIfAbsent(INGREDIENTS, ArrayList::new)
                .add(ItemIngredientPredicate.Builder.item().of(ingredient).withCount(count).build());
            this.save();
            return this;
        }

        public TimeWarpKubeRecipe requires(@NotNull TagKey<Item> ingredient) {
            return this.requires(ingredient, 1);
        }

        public TimeWarpKubeRecipe requires(@NotNull ItemStack ingredient) {
            this.computeIfAbsent(INGREDIENTS, ArrayList::new)
                .add(ItemIngredientPredicate.Builder.item().of(ingredient).build());
            this.save();
            return this;
        }

        public TimeWarpKubeRecipe requires(@NotNull ItemLike ingredient, int count) {
            this.computeIfAbsent(INGREDIENTS, ArrayList::new)
                .add(ItemIngredientPredicate.Builder.item().of(ingredient).withCount(count).build());
            this.save();
            return this;
        }

        public TimeWarpKubeRecipe requires(@NotNull ItemLike ingredient) {
            return this.requires(ingredient, 1);
        }

        public TimeWarpKubeRecipe result(@NotNull ItemStack result, NumberProvider count) {
            this.computeIfAbsent(RESULTS, ArrayList::new)
                .add(ChanceItemStack.of(result, count));
            this.save();
            return this;
        }

        public TimeWarpKubeRecipe result(@NotNull ItemStack result, float chance) {
            return this.result(result, BinomialDistributionGenerator.binomial(result.getCount(), chance));
        }

        public TimeWarpKubeRecipe result(@NotNull ItemStack result) {
            return this.result(result, ConstantValue.exactly(result.getCount()));
        }

        public TimeWarpKubeRecipe result(@NotNull ItemLike result, NumberProvider count) {
            this.computeIfAbsent(RESULTS, ArrayList::new)
                .add(ChanceItemStack.of(result, count));
            this.save();
            return this;
        }

        public TimeWarpKubeRecipe result(@NotNull ItemLike result, int count, float chance) {
            return this.result(result, BinomialDistributionGenerator.binomial(count, chance));
        }

        public TimeWarpKubeRecipe result(@NotNull ItemLike result, int count) {
            return this.result(result, ConstantValue.exactly(count));
        }

        public TimeWarpKubeRecipe result(@NotNull ItemLike result, float chance) {
            return this.result(result, 1, chance);
        }

        public TimeWarpKubeRecipe result(@NotNull ItemLike result) {
            return this.result(result, ConstantValue.exactly(1.0f));
        }

        @Override
        protected void validate() {
            if (computeIfAbsent(INGREDIENTS, ArrayList::new).isEmpty()) {
                throw new KubeRuntimeException("Inputs is Empty!").source(sourceLine);
            }
            if (computeIfAbsent(RESULTS, ArrayList::new).isEmpty()) {
                throw new KubeRuntimeException("Result is Empty!").source(sourceLine);
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
    RecipeKey<ResourceLocation> FLUID = AnvilCraftRecipeComponents.RESOURCE_LOCATION
        .key("fluid", ComponentRole.OUTPUT)
        .optional(HasCauldron.EMPTY)
        .alwaysWrite();
    RecipeKey<Integer> CONSUME = NumberComponent.INT
        .key("consume", ComponentRole.OUTPUT)
        .optional(0)
        .alwaysWrite();
    RecipeKey<ResourceLocation> TRANSFORM = AnvilCraftRecipeComponents.RESOURCE_LOCATION
        .key("transform", ComponentRole.OUTPUT)
        .optional(HasCauldron.NULL)
        .alwaysWrite();

    RecipeSchema SCHEMA = new RecipeSchema(INGREDIENTS, RESULTS, FLUID, CONSUME, TRANSFORM)
        .factory(new KubeRecipeFactory(AnvilCraft.of("time_warp"), TimeWarpKubeRecipe.class, TimeWarpKubeRecipe::new))
        .constructor(INGREDIENTS, RESULTS, FLUID, CONSUME, TRANSFORM)
        .constructor(INGREDIENTS, RESULTS, FLUID, CONSUME)
        .constructor(INGREDIENTS, RESULTS, FLUID)
        .constructor(INGREDIENTS, RESULTS)
        .constructor(new IDRecipeConstructor())
        .constructor();
}
