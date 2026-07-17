package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
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
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FluidMixingRecipe implements Recipe<FluidMixingRecipe.Input> {
    public static final int MAX_INGREDIENTS = 4;
    public static final int MAX_ITEM_RESULTS = 3;
    public static final int MAX_FLUID_RESULTS = 3;
    public static final int MAX_RESULTS = MAX_ITEM_RESULTS + MAX_FLUID_RESULTS;

    private final List<SizedFluidIngredient> ingredients;
    @Getter
    private final List<ItemStack> itemResults;
    @Getter
    private final List<FluidStack> fluidResults;

    public FluidMixingRecipe(
        List<SizedFluidIngredient> ingredients,
        List<ItemStack> itemResults,
        List<FluidStack> fluidResults
    ) {
        this.ingredients = List.copyOf(ingredients);
        this.itemResults = itemResults.stream().map(ItemStack::copy).toList();
        this.fluidResults = fluidResults.stream().map(FluidStack::copy).toList();
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<SizedFluidIngredient> getFluidIngredients() {
        return this.ingredients;
    }

    public int getResultCount() {
        return this.itemResults.size() + this.fluidResults.size();
    }

    /**
     * Returns the fluid state after consumption, or an empty optional when the ingredients cannot all be matched.
     */
    public Optional<List<FluidStack>> consume(List<FluidStack> storedFluids) {
        List<FluidStack> remaining = copyFluids(storedFluids);
        return this.consumeIngredient(0, remaining) ? Optional.of(remaining) : Optional.empty();
    }

    private boolean consumeIngredient(int ingredientIndex, List<FluidStack> remaining) {
        if (ingredientIndex >= this.ingredients.size()) return true;
        SizedFluidIngredient ingredient = this.ingredients.get(ingredientIndex);
        for (int tank = 0; tank < remaining.size(); tank++) {
            FluidStack stored = remaining.get(tank);
            if (!ingredient.test(stored)) continue;
            int amount = stored.getAmount() - ingredient.amount();
            remaining.set(tank, amount == 0 ? FluidStack.EMPTY : stored.copyWithAmount(amount));
            if (this.consumeIngredient(ingredientIndex + 1, remaining)) return true;
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
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.itemResults.isEmpty() ? ItemStack.EMPTY : this.itemResults.getFirst().copy();
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
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.FLUID_MIXING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.FLUID_MIXING_TYPE.get();
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

    public static class Serializer implements RecipeSerializer<FluidMixingRecipe> {
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
        private static final MapCodec<FluidMixingRecipe> CODEC =
            RecordCodecBuilder.<FluidMixingRecipe>mapCodec(instance -> instance.group(
                INGREDIENTS_CODEC.fieldOf("ingredients").forGetter(FluidMixingRecipe::getFluidIngredients),
                ITEM_RESULTS_CODEC.optionalFieldOf("results", List.<ItemStack>of())
                    .forGetter(FluidMixingRecipe::getItemResults),
                FLUID_RESULTS_CODEC.optionalFieldOf("fluid_results", List.<FluidStack>of())
                    .forGetter(FluidMixingRecipe::getFluidResults)
            ).apply(instance, FluidMixingRecipe::new)).validate(Serializer::validateResults);
        private static final StreamCodec<RegistryFriendlyByteBuf, FluidMixingRecipe> STREAM_CODEC =
            StreamCodec.composite(
                SizedFluidIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()),
                FluidMixingRecipe::getFluidIngredients,
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                FluidMixingRecipe::getItemResults,
                FluidStack.STREAM_CODEC.apply(ByteBufCodecs.list()),
                FluidMixingRecipe::getFluidResults,
                FluidMixingRecipe::new
            );

        private static DataResult<FluidMixingRecipe> validateResults(FluidMixingRecipe recipe) {
            int resultCount = recipe.getResultCount();
            if (resultCount < 1 || resultCount > MAX_RESULTS) {
                return DataResult.error(() ->
                    "Fluid mixing recipes must contain between 1 and " + MAX_RESULTS + " total results");
            }
            return DataResult.success(recipe);
        }

        private static <T> DataResult<List<T>> validateSize(List<T> values, int min, int max, String name) {
            if (values.size() < min || values.size() > max) {
                return DataResult.error(() -> name + " must contain between " + min + " and " + max + " entries");
            }
            return DataResult.success(values);
        }

        @Override
        public MapCodec<FluidMixingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FluidMixingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static class Builder extends AbstractRecipeBuilder<FluidMixingRecipe> {
        private final List<SizedFluidIngredient> ingredients = new ArrayList<>();
        private final List<ItemStack> itemResults = new ArrayList<>();
        private final List<FluidStack> fluidResults = new ArrayList<>();

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

        @Override
        public FluidMixingRecipe buildRecipe() {
            return new FluidMixingRecipe(this.ingredients, this.itemResults, this.fluidResults);
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
            return "fluid_mixing";
        }

        @Override
        public Item getResult() {
            return this.itemResults.isEmpty() ? Items.AIR : this.itemResults.getFirst().getItem();
        }
    }
}
