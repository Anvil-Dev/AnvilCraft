package dev.dubhe.anvilcraft.recipe.multiple;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.dubhe.anvilcraft.api.item.IMultipleToOneSmithingRecipeResult;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import net.minecraft.MethodsReturnNonnullByDefault;
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
public class EightToOneSmithingRecipe<T extends Item & IMultipleToOneSmithingRecipeResult>
    extends BaseMultipleToOneSmithingRecipe<T> {

    final Ingredient input0;
    final Ingredient input1;
    final Ingredient input2;
    final Ingredient input3;
    final Ingredient input4;
    final Ingredient input5;
    final Ingredient input6;
    final Ingredient input7;

    protected EightToOneSmithingRecipe(
        Ingredient template, Ingredient material,
        Ingredient input0, Ingredient input1, Ingredient input2, Ingredient input3,
        Ingredient input4, Ingredient input5, Ingredient input6, Ingredient input7,
        T result, int recipeId
    ) {
        super(template, material, result, recipeId);
        this.input0 = input0;
        this.input1 = input1;
        this.input2 = input2;
        this.input3 = input3;
        this.input4 = input4;
        this.input5 = input5;
        this.input6 = input6;
        this.input7 = input7;
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
        return 8;
    }

    @Override
    public boolean matchesInput(MultipleToOneSmithingRecipeInput input, Level level) {
        return this.input0.test(input.getInputItem(0)) && this.input1.test(input.getInputItem(1))
            && this.input2.test(input.getInputItem(2)) && this.input3.test(input.getInputItem(3))
            && this.input4.test(input.getInputItem(4)) && this.input5.test(input.getInputItem(5))
            && this.input6.test(input.getInputItem(6)) && this.input7.test(input.getInputItem(7));
    }

    @Override
    public boolean isInputIngredient(int id, ItemStack input) {
        return switch (id) {
            case 0 -> this.input0.test(input);
            case 1 -> this.input1.test(input);
            case 2 -> this.input2.test(input);
            case 3 -> this.input3.test(input);
            case 4 -> this.input4.test(input);
            case 5 -> this.input5.test(input);
            case 6 -> this.input6.test(input);
            case 7 -> this.input7.test(input);
            default -> throw new IllegalStateException("Recipe does not contain input " + id);
        };
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.EIGHT_TO_ONE_SMITHING_SERIALIZER.get();
    }

    public static class Serializer<T extends Item & IMultipleToOneSmithingRecipeResult>
        implements RecipeSerializer<EightToOneSmithingRecipe<T>> {

        @Override
        public MapCodec<EightToOneSmithingRecipe<T>> codec() {
            return RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC.fieldOf("template").forGetter(recipe -> recipe.template),
                Ingredient.CODEC.fieldOf("material").forGetter(recipe -> recipe.material),
                Ingredient.CODEC.fieldOf("input0").forGetter(recipe -> recipe.input0),
                Ingredient.CODEC.fieldOf("input1").forGetter(recipe -> recipe.input1),
                Ingredient.CODEC.fieldOf("input2").forGetter(recipe -> recipe.input2),
                Ingredient.CODEC.fieldOf("input3").forGetter(recipe -> recipe.input3),
                Ingredient.CODEC.fieldOf("input4").forGetter(recipe -> recipe.input4),
                Ingredient.CODEC.fieldOf("input5").forGetter(recipe -> recipe.input5),
                Ingredient.CODEC.fieldOf("input6").forGetter(recipe -> recipe.input6),
                Ingredient.CODEC.fieldOf("input7").forGetter(recipe -> recipe.input7),
                BaseMultipleToOneSmithingRecipe.<T>RESULT_CODEC().fieldOf("result").forGetter(recipe -> recipe.result),
                Codec.INT.fieldOf("recipeId").forGetter(recipe -> recipe.recipeId)
            ).apply(inst, EightToOneSmithingRecipe::new));
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, EightToOneSmithingRecipe<T>> streamCodec() {
            return StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);
        }

        private static <T extends Item & IMultipleToOneSmithingRecipeResult> EightToOneSmithingRecipe<T> fromNetwork(
            RegistryFriendlyByteBuf buf
        ) {
            Data<T> supeR = BaseMultipleToOneSmithingRecipe.fromNetwork(buf);
            Ingredient input0 = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient input1 = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient input2 = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient input3 = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient input4 = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient input5 = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient input6 = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            Ingredient input7 = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            return new EightToOneSmithingRecipe<>(
                supeR.template(), supeR.material(),
                input0, input1, input2, input3,
                input4, input5, input6, input7,
                supeR.result(), supeR.recipeId());
        }

        private static <T extends Item & IMultipleToOneSmithingRecipeResult> void toNetwork(
            RegistryFriendlyByteBuf buf, EightToOneSmithingRecipe<T> recipe
        ) {
            BaseMultipleToOneSmithingRecipe.toNetwork(buf, recipe);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input0);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input1);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input2);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input3);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input4);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input5);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input6);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input7);
        }
    }

    public static class Builder<T extends Item & IMultipleToOneSmithingRecipeResult>
        extends BaseMultipleToOneSmithingRecipe.Builder<T, EightToOneSmithingRecipe<T>> {

        private Ingredient input0;
        private Ingredient input1;
        private Ingredient input2;
        private Ingredient input3;
        private Ingredient input4;
        private Ingredient input5;
        private Ingredient input6;
        private Ingredient input7;

        Builder(NonNullSupplier<T> resultGetter, int recipeId) {
            super(resultGetter, recipeId);
        }

        Builder(T result, int recipeId) {
            super(result, recipeId);
        }

        @Override
        protected Ingredient template() {
            return Ingredient.of(ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE);
        }

        @Override
        protected int inputSize() {
            return 8;
        }

        @Override
        public Builder<T> input(int id, Ingredient material) {
            switch (id) {
                case 0 -> this.input0 = material;
                case 1 -> this.input1 = material;
                case 2 -> this.input2 = material;
                case 3 -> this.input3 = material;
                case 4 -> this.input4 = material;
                case 5 -> this.input5 = material;
                case 6 -> this.input6 = material;
                case 7 -> this.input7 = material;
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
            if (this.input4.isEmpty()) {
                throw new IllegalArgumentException("The fifth input of two to one smithing recipe must not be empty, RecipeId: " + pId);
            }
            if (this.input5.isEmpty()) {
                throw new IllegalArgumentException("The sixth input of two to one smithing recipe must not be empty, RecipeId: " + pId);
            }
            if (this.input6.isEmpty()) {
                throw new IllegalArgumentException("The seventh input of two to one smithing recipe must not be empty, RecipeId: " + pId);
            }
            if (this.input7.isEmpty()) {
                throw new IllegalArgumentException("The eighth input of two to one smithing recipe must not be empty, RecipeId: " + pId);
            }
        }

        @Override
        public EightToOneSmithingRecipe<T> buildRecipe() {
            return new EightToOneSmithingRecipe<>(
                this.template, this.material,
                this.input0, this.input1, this.input2, this.input3,
                this.input4, this.input5, this.input6, this.input7,
                this.result, this.recipeId);
        }

        @Override
        public String getType() {
            return "eight_to_one_smithing";
        }
    }
}
