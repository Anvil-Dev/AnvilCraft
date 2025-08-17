package dev.dubhe.anvilcraft.integration.kubejs.recipe.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.AnvilCraftKubeRecipe;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.IDRecipeConstructor;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.BlockStatePredicateComponent;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ChanceBlockStateComponent;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ChanceItemStackComponent;
import dev.dubhe.anvilcraft.integration.kubejs.recipe.components.ItemIngredientPredicateComponent;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceBlockState;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public interface ItemInjectRecipeSchema {
    @SuppressWarnings({"unused"})
    class ItemInjectKubeRecipe extends AnvilCraftKubeRecipe {
        public ItemInjectKubeRecipe requires(@NotNull TagKey<Item> ingredient, int count) {
            this.computeIfAbsent(INGREDIENTS, ArrayList::new)
                .add(ItemIngredientPredicate.Builder.item().of(ingredient).withCount(count).build());
            this.save();
            return this;
        }

        public ItemInjectKubeRecipe requires(@NotNull TagKey<Item> ingredient) {
            return this.requires(ingredient, 1);
        }

        public ItemInjectKubeRecipe requires(@NotNull ItemStack ingredient) {
            this.computeIfAbsent(INGREDIENTS, ArrayList::new)
                .add(ItemIngredientPredicate.Builder.item().of(ingredient).build());
            this.save();
            return this;
        }

        public ItemInjectKubeRecipe requires(@NotNull ItemLike ingredient, int count) {
            this.computeIfAbsent(INGREDIENTS, ArrayList::new)
                .add(ItemIngredientPredicate.Builder.item().of(ingredient).withCount(count).build());
            this.save();
            return this;
        }

        public ItemInjectKubeRecipe requires(@NotNull ItemLike ingredient) {
            return this.requires(ingredient, 1);
        }

        public ItemInjectKubeRecipe result(@NotNull ItemStack result, NumberProvider count) {
            this.computeIfAbsent(RESULTS, ArrayList::new)
                .add(ChanceItemStack.of(result, count));
            this.save();
            return this;
        }

        public ItemInjectKubeRecipe result(@NotNull ItemStack result, float chance) {
            return this.result(result, BinomialDistributionGenerator.binomial(result.getCount(), chance));
        }

        public ItemInjectKubeRecipe result(@NotNull ItemStack result) {
            return this.result(result, ConstantValue.exactly(result.getCount()));
        }

        public ItemInjectKubeRecipe result(@NotNull ItemLike result, NumberProvider count) {
            this.computeIfAbsent(RESULTS, ArrayList::new)
                .add(ChanceItemStack.of(result, count));
            this.save();
            return this;
        }

        public ItemInjectKubeRecipe result(@NotNull ItemLike result, int count, float chance) {
            return this.result(result, BinomialDistributionGenerator.binomial(count, chance));
        }

        public ItemInjectKubeRecipe result(@NotNull ItemLike result, int count) {
            return this.result(result, ConstantValue.exactly(count));
        }

        public ItemInjectKubeRecipe result(@NotNull ItemLike result, float chance) {
            return this.result(result, 1, chance);
        }

        public ItemInjectKubeRecipe result(@NotNull ItemLike result) {
            return this.result(result, ConstantValue.exactly(1.0f));
        }

        public ItemInjectKubeRecipe inputBlock(Block... block) {
            this.setValue(INPUT_BLOCK, BlockStatePredicate.builder().of(block).build());
            this.save();
            return this;
        }

        public final ItemInjectKubeRecipe inputBlockTag(TagKey<Block> tag) {
            this.setValue(INPUT_BLOCK, BlockStatePredicate.builder().of(tag).build());
            this.save();
            return this;
        }

        public ItemInjectKubeRecipe resultBlock(@NotNull Block block) {
            this.setValue(RESULT_BLOCK, new ChanceBlockState(block.defaultBlockState(), 1.0f));
            this.save();
            return this;
        }

        @Override
        protected void validate() {
            if (computeIfAbsent(INGREDIENTS, ArrayList::new).isEmpty()) {
                throw new KubeRuntimeException("Ingredients is Empty!").source(sourceLine);
            }
            if (getValue(INPUT_BLOCK) == null) {
                throw new KubeRuntimeException("input_block is Empty!").source(sourceLine);
            }
            if (getValue(RESULT_BLOCK) == null) {
                throw new KubeRuntimeException("output_block is Empty!").source(sourceLine);
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
    RecipeKey<BlockStatePredicate> INPUT_BLOCK = BlockStatePredicateComponent.INSTANCE
        .inputKey("input_block")
        .defaultOptional();
    RecipeKey<ChanceBlockState> RESULT_BLOCK = ChanceBlockStateComponent.INSTANCE
        .outputKey("result_block")
        .defaultOptional();

    RecipeSchema SCHEMA = new RecipeSchema(INGREDIENTS, RESULTS, INPUT_BLOCK, RESULT_BLOCK)
        .factory(new KubeRecipeFactory(AnvilCraft.of("item_inject"), ItemInjectKubeRecipe.class, ItemInjectKubeRecipe::new))
        .constructor(INGREDIENTS, RESULTS, INPUT_BLOCK, RESULT_BLOCK)
        .constructor(INGREDIENTS, INPUT_BLOCK, RESULT_BLOCK)
        .constructor(new IDRecipeConstructor())
        .constructor();
}
