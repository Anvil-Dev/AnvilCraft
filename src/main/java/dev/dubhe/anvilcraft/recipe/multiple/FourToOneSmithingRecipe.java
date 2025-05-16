package dev.dubhe.anvilcraft.recipe.multiple;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.dubhe.anvilcraft.api.item.IMultipleToOneSmithingRecipeResult;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FourToOneSmithingRecipe<T extends Item & IMultipleToOneSmithingRecipeResult>
    extends BaseMultipleToOneSmithingRecipe<T> {

    final Ingredient input0;
    final Ingredient input1;
    final Ingredient input2;
    final Ingredient input3;

    protected FourToOneSmithingRecipe(
        Ingredient template, Ingredient material,
        Ingredient input0, Ingredient input1, Ingredient input2, Ingredient input3,
        T result, int recipeId
    ) {
        super(template, material, result, recipeId);
        this.input0 = input0;
        this.input1 = input1;
        this.input2 = input2;
        this.input3 = input3;
    }

    public static <T extends Item & IMultipleToOneSmithingRecipeResult> Builder<T> builder(
        NonNullSupplier<T> resultGetter, int recipeId) {
        return new Builder<>(resultGetter, recipeId);
    }

    public static <T extends Item & IMultipleToOneSmithingRecipeResult> Builder<T> builder(T result, int recipeId) {
        return new Builder<>(result, recipeId);
    }

    @Override
    public int inputSize() {
        return 4;
    }

    @Override
    public boolean matchesInput(MultipleToOneSmithingRecipeInput input, Level level) {
        return this.input0.test(input.getInputItem(0)) && this.input1.test(input.getInputItem(1))
            && this.input2.test(input.getInputItem(2)) && this.input3.test(input.getInputItem(3));
    }

    @Override
    public boolean isInputIngredient(int id, ItemStack input) {
        return switch (id) {
            case 0 -> this.input0.test(input);
            case 1 -> this.input1.test(input);
            case 2 -> this.input2.test(input);
            case 3 -> this.input3.test(input);
            default -> throw new IllegalStateException("Recipe does not contain input " + id);
        };
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = super.getIngredients();
        ingredients.add(input0);
        ingredients.add(input1);
        ingredients.add(input2);
        ingredients.add(input3);
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.FOUR_TO_ONE_SMITHING_SERIALIZER.get();
    }

    public static class Serializer<T extends Item & IMultipleToOneSmithingRecipeResult>
        implements RecipeSerializer<FourToOneSmithingRecipe<T>> {

        @Override
        public MapCodec<FourToOneSmithingRecipe<T>> codec() {
            return RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC.fieldOf("template").forGetter(recipe -> recipe.template),
                Ingredient.CODEC.fieldOf("material").forGetter(recipe -> recipe.material),
                Ingredient.CODEC.fieldOf("input0").forGetter(recipe -> recipe.input0),
                Ingredient.CODEC.fieldOf("input1").forGetter(recipe -> recipe.input1),
                Ingredient.CODEC.fieldOf("input2").forGetter(recipe -> recipe.input2),
                Ingredient.CODEC.fieldOf("input3").forGetter(recipe -> recipe.input3),
                BaseMultipleToOneSmithingRecipe.<T>RESULT_CODEC().fieldOf("result").forGetter(recipe -> recipe.result),
                Codec.INT.fieldOf("recipeId").forGetter(recipe -> recipe.recipeId)
            ).apply(inst, FourToOneSmithingRecipe::new));
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FourToOneSmithingRecipe<T>> streamCodec() {
            return StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);
        }

        private static <T extends Item & IMultipleToOneSmithingRecipeResult> FourToOneSmithingRecipe<T> fromNetwork(
            RegistryFriendlyByteBuf buf
        ) {
            Data<T> supeR = BaseMultipleToOneSmithingRecipe.fromNetwork(buf);
            Ingredient input0 = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient input1 = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient input2 = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient input3 = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            return new FourToOneSmithingRecipe<>(
                supeR.template(), supeR.material(),
                input0, input1, input2, input3,
                supeR.result(), supeR.recipeId());
        }

        private static <T extends Item & IMultipleToOneSmithingRecipeResult> void toNetwork(
            RegistryFriendlyByteBuf buf, FourToOneSmithingRecipe<T> recipe
        ) {
            BaseMultipleToOneSmithingRecipe.toNetwork(buf, recipe);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input0);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input1);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input2);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input3);
        }
    }

    public static class Builder<T extends Item & IMultipleToOneSmithingRecipeResult>
        extends BaseMultipleToOneSmithingRecipe.Builder<T, FourToOneSmithingRecipe<T>> {

        private Ingredient input0;
        private Ingredient input1;
        private Ingredient input2;
        private Ingredient input3;

        Builder(NonNullSupplier<T> resultGetter, int recipeId) {
            super(resultGetter, recipeId);
        }

        Builder(T result, int recipeId) {
            super(result, recipeId);
        }

        @Override
        protected Ingredient template() {
            return Ingredient.of(ModItems.FOUR_TO_ONE_SMITHING_TEMPLATE);
        }

        @Override
        protected int inputSize() {
            return 4;
        }

        @Override
        public Builder<T> input(int id, Ingredient material) {
            switch (id) {
                case 0 -> this.input0 = material;
                case 1 -> this.input1 = material;
                case 2 -> this.input2 = material;
                case 3 -> this.input3 = material;
                default -> throw new IllegalArgumentException("Recipe does not contain input " + id);
            }
            return this;
        }

        @Override
        protected void validateInput(ResourceLocation pId) {
            if (this.input0.isEmpty()) {
                throw new IllegalArgumentException("The first input of two to one smithing recipe must not be empty, RecipeId: " + pId);
            }
            if (this.input1.isEmpty()) {
                throw new IllegalArgumentException("The second input of two to one smithing recipe must not be empty, RecipeId: " + pId);
            }
            if (this.input2.isEmpty()) {
                throw new IllegalArgumentException("The third input of two to one smithing recipe must not be empty, RecipeId: " + pId);
            }
            if (this.input3.isEmpty()) {
                throw new IllegalArgumentException("The fourth input of two to one smithing recipe must not be empty, RecipeId: " + pId);
            }
        }

        @Override
        public FourToOneSmithingRecipe<T> buildRecipe() {
            return new FourToOneSmithingRecipe<>(
                this.template, this.material,
                this.input0, this.input1, this.input2, this.input3,
                this.result, this.recipeId);
        }

        @Override
        public String getType() {
            return "four_to_one_smithing";
        }
    }
}
