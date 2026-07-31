package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeSerializers;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import lombok.Getter;
import net.minecraft.core.HolderGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FluidMixingRecipe implements Recipe<FluidMixingRecipe.Input> {
    public static final int MAX_INGREDIENTS = 4;
    public static final int MAX_ITEM_RESULTS = 3;
    public static final int MAX_FLUID_RESULTS = 3;
    public static final int MAX_RESULTS = FluidMixingRecipe.MAX_ITEM_RESULTS + FluidMixingRecipe.MAX_FLUID_RESULTS;
    public static final RecipeSerializer<FluidMixingRecipe> SERIALIZER = new RecipeSerializer<>(
        Serializer.CODEC,
        Serializer.STREAM_CODEC
    );

    private final List<SizedFluidIngredient> ingredients;
    private final List<ItemStackTemplate> itemResults;
    @Getter
    private final List<FluidStack> fluidResults;
    private final boolean consumeMaximum;

    public FluidMixingRecipe(
        List<SizedFluidIngredient> ingredients,
        List<ItemStackTemplate> itemResults,
        List<FluidStack> fluidResults,
        boolean consumeMaximum
    ) {
        this.ingredients = List.copyOf(ingredients);
        this.itemResults = List.copyOf(itemResults);
        this.fluidResults = fluidResults.stream().map(FluidStack::copy).toList();
        this.consumeMaximum = consumeMaximum;
    }

    public static Builder builder(HolderGetter<Fluid> fluids) {
        return new Builder(fluids);
    }

    public List<SizedFluidIngredient> getFluidIngredients() {
        return this.ingredients;
    }

    public List<ItemStack> getItemResults() {
        return this.itemResults.stream().map(ItemStackTemplate::create).toList();
    }

    private List<ItemStackTemplate> getItemResultTemplates() {
        return this.itemResults;
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
        List<FluidStack> remaining = FluidMixingRecipe.copyFluids(storedFluids);
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

    @Override
    public boolean matches(Input input, Level level) {
        return this.consume(input.fluids()).isPresent();
    }

    @Override
    public ItemStack assemble(Input input) {
        return this.itemResults.isEmpty() ? ItemStack.EMPTY : this.itemResults.getFirst().create();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<FluidMixingRecipe> getSerializer() {
        return ModRecipeSerializers.FLUID_MIXING.get();
    }

    @Override
    public RecipeType<FluidMixingRecipe> getType() {
        return ModRecipeTypes.FLUID_MIXING.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "fluid_mixing";
    }

    public record Input(List<FluidStack> fluids) implements RecipeInput {
        public Input {
            fluids = FluidMixingRecipe.copyFluids(fluids);
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

    private static class Serializer {
        private static final Codec<List<SizedFluidIngredient>> INGREDIENTS_CODEC =
            SizedFluidIngredient.CODEC.listOf().validate(ingredients -> Serializer.validateSize(
                ingredients,
                2,
                FluidMixingRecipe.MAX_INGREDIENTS,
                "Fluid mixing ingredients"
            ));
        private static final Codec<List<ItemStackTemplate>> ITEM_RESULTS_CODEC =
            ItemStackTemplate.CODEC.listOf().validate(results ->
                                                          Serializer.validateSize(
                                                              results, 0, FluidMixingRecipe.MAX_ITEM_RESULTS, "Fluid mixing item results"));
        private static final Codec<List<FluidStack>> FLUID_RESULTS_CODEC =
            FluidStack.CODEC.listOf().validate(results ->
                                                   Serializer.validateSize(
                                                       results, 0, FluidMixingRecipe.MAX_FLUID_RESULTS, "Fluid mixing fluid results"));
        private static final MapCodec<FluidMixingRecipe> CODEC =
            RecordCodecBuilder.<FluidMixingRecipe>mapCodec(instance -> instance.group(
                Serializer.INGREDIENTS_CODEC.fieldOf("ingredients").forGetter(FluidMixingRecipe::getFluidIngredients),
                Serializer.ITEM_RESULTS_CODEC.optionalFieldOf("results", List.of())
                    .forGetter(FluidMixingRecipe::getItemResultTemplates),
                Serializer.FLUID_RESULTS_CODEC.optionalFieldOf("fluid_results", List.of())
                    .forGetter(FluidMixingRecipe::getFluidResults),
                Codec.BOOL.optionalFieldOf("consume_maximum", false)
                    .forGetter(FluidMixingRecipe::consumesMaximum)
            ).apply(instance, FluidMixingRecipe::new)).validate(Serializer::validateResults);
        private static final StreamCodec<RegistryFriendlyByteBuf, FluidMixingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                SizedFluidIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()),
                FluidMixingRecipe::getFluidIngredients,
                ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()),
                FluidMixingRecipe::getItemResultTemplates,
                FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                FluidMixingRecipe::getFluidResults,
                ByteBufCodecs.BOOL,
                FluidMixingRecipe::consumesMaximum,
                FluidMixingRecipe::new
            );

        private static DataResult<FluidMixingRecipe> validateResults(FluidMixingRecipe recipe) {
            int resultCount = recipe.getResultCount();
            if (resultCount < 1 || resultCount > FluidMixingRecipe.MAX_RESULTS) {
                return DataResult.error(() ->
                                            "Fluid mixing recipes must contain between 1 and " + FluidMixingRecipe.MAX_RESULTS
                                            + " total results");
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

    }

    public static class Builder extends AbstractRecipeBuilder<FluidMixingRecipe> {
        private final HolderGetter<Fluid> fluids;
        private final List<SizedFluidIngredient> ingredients = new ArrayList<>();
        private final List<ItemStackTemplate> itemResults = new ArrayList<>();
        private final List<FluidStack> fluidResults = new ArrayList<>();
        private boolean consumeMaximum;

        private Builder(HolderGetter<Fluid> fluids) {
            this.fluids = fluids;
        }

        public Builder requires(Fluid fluid, int amount) {
            this.ingredients.add(SizedFluidIngredient.of(fluid, amount));
            return this;
        }

        public Builder requires(TagKey<Fluid> fluid, int amount) {
            this.ingredients.add(new SizedFluidIngredient(
                FluidIngredient.of(this.fluids.getOrThrow(fluid)),
                amount
            ));
            return this;
        }

        public Builder result(ItemLike item, int count) {
            this.itemResults.add(new ItemStackTemplate(item.asItem(), count));
            return this;
        }

        public Builder result(ItemStack item) {
            this.itemResults.add(ItemStackTemplate.fromNonEmptyStack(item));
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
        public void validate(Identifier id) {
            Builder.validateCount(this.ingredients.size(), 2, FluidMixingRecipe.MAX_INGREDIENTS, "ingredients", id);
            Builder.validateCount(this.itemResults.size(), 0, FluidMixingRecipe.MAX_ITEM_RESULTS, "item results", id);
            Builder.validateCount(this.fluidResults.size(), 0, FluidMixingRecipe.MAX_FLUID_RESULTS, "fluid results", id);
            Builder.validateCount(
                this.itemResults.size() + this.fluidResults.size(), 1, FluidMixingRecipe.MAX_RESULTS, "total results", id);
            if (this.itemResults.stream().anyMatch(result ->
                                                       result.item().value() == Items.AIR || result.count() <= 0)) {
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

        private static void validateCount(int count, int min, int max, String name, Identifier id) {
            if (count < min || count > max) {
                throw new IllegalArgumentException(
                    "Fluid mixing recipe must have between " + min + " and " + max + " " + name + ", RecipeId: " + id
                );
            }
        }

        @Override
        public String getType() {
            return "fluid_mixing";
        }

        @Override
        public ItemStackTemplate getResult() {
            return this.itemResults.isEmpty()
                ? new ItemStackTemplate(Items.AIR)
                : this.itemResults.getFirst();
        }
    }
}
