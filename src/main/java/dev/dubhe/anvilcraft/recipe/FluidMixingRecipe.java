package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FluidMixingRecipe extends SolidLiquidRecipe {
    public static final int MAX_INGREDIENTS = 4;
    public static final int MAX_ITEM_RESULTS = 3;
    public static final int MAX_FLUID_RESULTS = 3;
    public static final int MAX_RESULTS = MAX_ITEM_RESULTS + MAX_FLUID_RESULTS;

    private final List<SizedFluidIngredient> ingredients;
    @Getter
    private final List<ItemStack> itemResults;
    @Getter
    private final List<FluidStack> fluidResults;
    private final boolean consumeMaximum;

    public FluidMixingRecipe(
        List<SizedFluidIngredient> ingredients,
        List<ItemStack> itemResults,
        List<FluidStack> fluidResults,
        boolean consumeMaximum
    ) {
        super(List.of(), List.of(), HasCauldronSimple.empty().build(), Integer.MAX_VALUE);
        this.ingredients = List.copyOf(ingredients);
        this.itemResults = itemResults.stream().map(ItemStack::copy).toList();
        this.fluidResults = fluidResults.stream().map(FluidStack::copy).toList();
        this.consumeMaximum = consumeMaximum;
    }

    public List<SizedFluidIngredient> getFluidIngredients() {
        return this.ingredients;
    }

    public int getResultCount() {
        return this.itemResults.size() + this.fluidResults.size();
    }

    public boolean consumesMaximum() {
        return this.consumeMaximum;
    }

    /**
     * Returns the fluid state after consumption, or an empty optional when the ingredients cannot all be matched.
     */
    public Optional<List<FluidStack>> consume(List<FluidStack> storedFluids) {
        return this.consume(storedFluids, 1);
    }

    public Optional<List<FluidStack>> consume(List<FluidStack> storedFluids, int batches) {
        if (batches <= 0) return Optional.empty();
        List<FluidStack> remaining = copyFluids(storedFluids);
        return this.consumeIngredient(0, batches, remaining) ? Optional.of(remaining) : Optional.empty();
    }

    public int getMaximumBatches(List<FluidStack> storedFluids) {
        if (!this.consumeMaximum) return this.consume(storedFluids).isPresent() ? 1 : 0;
        long storedAmount = storedFluids.stream().mapToLong(FluidStack::getAmount).sum();
        long amountPerBatch = this.ingredients.stream().mapToLong(SizedFluidIngredient::amount).sum();
        int low = 0;
        int high = (int) Math.min(Integer.MAX_VALUE, storedAmount / Math.max(1L, amountPerBatch));
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            if (this.consume(storedFluids, middle).isPresent()) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    public Optional<List<FluidStack>> getFluidResults(int batches) {
        if (batches <= 0) return Optional.empty();
        List<FluidStack> results = new ArrayList<>(this.fluidResults.size());
        for (FluidStack result : this.fluidResults) {
            long amount = (long) result.getAmount() * batches;
            if (amount > Integer.MAX_VALUE) return Optional.empty();
            results.add(result.copyWithAmount((int) amount));
        }
        return Optional.of(results);
    }

    private boolean consumeIngredient(int ingredientIndex, int batches, List<FluidStack> remaining) {
        if (ingredientIndex >= this.ingredients.size()) return true;
        SizedFluidIngredient ingredient = this.ingredients.get(ingredientIndex);
        long requiredAmount = (long) ingredient.amount() * batches;
        if (requiredAmount > Integer.MAX_VALUE) return false;
        for (int tank = 0; tank < remaining.size(); tank++) {
            FluidStack stored = remaining.get(tank);
            if (!ingredient.test(stored) || stored.getAmount() < requiredAmount) continue;
            int amount = stored.getAmount() - (int) requiredAmount;
            remaining.set(tank, amount == 0 ? FluidStack.EMPTY : stored.copyWithAmount(amount));
            if (this.consumeIngredient(ingredientIndex + 1, batches, remaining)) return true;
            remaining.set(tank, stored);
        }
        return false;
    }

    private static List<FluidStack> copyFluids(List<FluidStack> fluids) {
        List<FluidStack> result = new ArrayList<>(fluids.size());
        for (FluidStack fluid : fluids) result.add(fluid.copy());
        return result;
    }

    public boolean matches(Input input, Level level) {
        return this.consume(input.fluids()).isPresent();
    }

    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.itemResults.isEmpty() ? ItemStack.EMPTY : this.itemResults.getFirst().copy();
    }

    @Override
    public boolean matches(InWorldRecipeContext context, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(InWorldRecipeContext context, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.itemResults.isEmpty() ? ItemStack.EMPTY : this.itemResults.getFirst().copy();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public record Input(List<FluidStack> fluids) implements RecipeInput {
        public Input {
            fluids = copyFluids(fluids);
        }

        @Override
        public ItemStack getItem(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return this.fluids.stream().allMatch(FluidStack::isEmpty);
        }
    }

    private static final Codec<List<SizedFluidIngredient>> INGREDIENTS_CODEC =
        SizedFluidIngredient.FLAT_CODEC.listOf().validate(ingredients -> validateSize(
            ingredients,
            2,
            MAX_INGREDIENTS,
            "Fluid mixing ingredients"
        ));
    private static final Codec<List<ItemStack>> ITEM_RESULTS_CODEC = ItemStack.CODEC.listOf().validate(results ->
        validateSize(results, 0, MAX_ITEM_RESULTS, "Fluid mixing item results"));
    private static final Codec<List<FluidStack>> FLUID_RESULTS_CODEC =
        FluidStack.CODEC.listOf().validate(results ->
            validateSize(results, 0, MAX_FLUID_RESULTS, "Fluid mixing fluid results"));
    private static final Codec<FluidMixingRecipe> MIXING_RECIPE_CODEC = RecordCodecBuilder
        .<FluidMixingRecipe>mapCodec(instance -> instance.group(
            INGREDIENTS_CODEC.fieldOf("ingredients").forGetter(FluidMixingRecipe::getFluidIngredients),
            ITEM_RESULTS_CODEC.optionalFieldOf("results", List.<ItemStack>of())
                .forGetter(FluidMixingRecipe::getItemResults),
            FLUID_RESULTS_CODEC.optionalFieldOf("fluid_results", List.<FluidStack>of())
                .forGetter(FluidMixingRecipe::getFluidResults),
            Codec.BOOL.optionalFieldOf("consume_maximum", false)
                .forGetter(FluidMixingRecipe::consumesMaximum)
        ).apply(instance, FluidMixingRecipe::new))
        .codec()
        .validate(FluidMixingRecipe::validateResults);

    public static final Codec<SolidLiquidRecipe> MIXING_CODEC = MIXING_RECIPE_CODEC
        .xmap(recipe -> recipe, recipe -> (FluidMixingRecipe) recipe);

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidMixingRecipe> MIXING_STREAM_CODEC =
        StreamCodec.composite(
            SizedFluidIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()),
            FluidMixingRecipe::getFluidIngredients,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            FluidMixingRecipe::getItemResults,
            FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
            FluidMixingRecipe::getFluidResults,
            ByteBufCodecs.BOOL,
            FluidMixingRecipe::consumesMaximum,
            FluidMixingRecipe::new
        );

    private static DataResult<FluidMixingRecipe> validateResults(FluidMixingRecipe recipe) {
        int resultCount = recipe.getResultCount();
        if (resultCount < 1 || resultCount > MAX_RESULTS) {
            return DataResult.error(() ->
                "Fluid mixing recipes must contain between 1 and " + MAX_RESULTS + " total results");
        }
        if (recipe.consumesMaximum() && (!recipe.itemResults.isEmpty() || recipe.fluidResults.isEmpty())) {
            return DataResult.error(() ->
                "Maximum-consumption fluid mixing recipes must have fluid results only");
        }
        return DataResult.success(recipe);
    }

    private static <T> DataResult<List<T>> validateSize(List<T> values, int min, int max, String name) {
        if (values.size() < min || values.size() > max) {
            return DataResult.error(() -> name + " must contain between " + min + " and " + max + " entries");
        }
        return DataResult.success(values);
    }

    public static class Builder extends AbstractRecipeBuilder<FluidMixingRecipe> {
        private final List<SizedFluidIngredient> ingredients = new ArrayList<>();
        private final List<ItemStack> itemResults = new ArrayList<>();
        private final List<FluidStack> fluidResults = new ArrayList<>();
        private boolean consumeMaximum;

        public Builder requires(Fluid fluid, int amount) {
            this.ingredients.add(SizedFluidIngredient.of(fluid, amount));
            return this;
        }

        public Builder requires(TagKey<Fluid> fluid, int amount) {
            this.ingredients.add(SizedFluidIngredient.of(fluid, amount));
            return this;
        }

        public Builder result(ItemLike item, int count) {
            return this.result(new ItemStack(item, count));
        }

        public Builder result(ItemStack item) {
            this.itemResults.add(item.copy());
            return this;
        }

        public Builder result(Fluid fluid, int amount) {
            return this.result(new FluidStack(fluid, amount));
        }

        public Builder result(FluidStack fluid) {
            this.fluidResults.add(fluid.copy());
            return this;
        }

        public Builder consumeMaximum() {
            this.consumeMaximum = true;
            return this;
        }

        @Override
        public FluidMixingRecipe buildRecipe() {
            return new FluidMixingRecipe(
                this.ingredients,
                this.itemResults,
                this.fluidResults,
                this.consumeMaximum
            );
        }

        @Override
        public void validate(ResourceLocation id) {
            validateCount(this.ingredients.size(), 2, MAX_INGREDIENTS, "ingredients", id);
            validateCount(this.itemResults.size(), 0, MAX_ITEM_RESULTS, "item results", id);
            validateCount(this.fluidResults.size(), 0, MAX_FLUID_RESULTS, "fluid results", id);
            validateCount(this.itemResults.size() + this.fluidResults.size(), 1, MAX_RESULTS, "total results", id);
            if (this.itemResults.stream().anyMatch(ItemStack::isEmpty)) {
                throw new IllegalArgumentException("Fluid mixing recipe has an empty result, RecipeId: " + id);
            }
            if (this.fluidResults.stream().anyMatch(FluidStack::isEmpty)) {
                throw new IllegalArgumentException("Fluid mixing recipe has an empty fluid result, RecipeId: " + id);
            }
            if (this.consumeMaximum && (!this.itemResults.isEmpty() || this.fluidResults.isEmpty())) {
                throw new IllegalArgumentException(
                    "Maximum-consumption fluid mixing recipe must have fluid results only, RecipeId: " + id
                );
            }
        }

        private static void validateCount(int count, int min, int max, String name, ResourceLocation id) {
            if (count < min || count > max) {
                throw new IllegalArgumentException(
                    "Fluid mixing recipe must have between " + min + " and " + max + " " + name + ", RecipeId: " + id
                );
            }
        }

        @Override
        public String getType() {
            return "solid_liquid";
        }

        @Override
        public Item getResult() {
            return this.itemResults.isEmpty() ? Items.AIR : this.itemResults.getFirst().getItem();
        }
    }
}
