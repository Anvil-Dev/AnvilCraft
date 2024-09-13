package dev.dubhe.anvilcraft.recipe;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.builder.AbstractItemProcessBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class StampingRecipe extends AbstractItemProcessRecipe {
    public StampingRecipe(NonNullList<Ingredient> ingredients, ItemStack result) {
        super(ingredients, result);
    }

    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.STAMPING_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.STAMPING_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<StampingRecipe> {
        private static final MapCodec<StampingRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                        Ingredient.CODEC_NONEMPTY
                                .listOf(1, 9)
                                .fieldOf("ingredients")
                                .flatXmap(
                                        i -> {
                                            Ingredient[] ingredients = i.toArray(Ingredient[]::new);
                                            if (ingredients.length == 0) {
                                                return DataResult.error(() -> "No ingredients for item_crush recipe");
                                            } else {
                                                return ingredients.length > 9
                                                        ? DataResult.error(
                                                                () ->
                                                                        "Too many ingredients for item_crush recipe. The maximum is: 9")
                                                        : DataResult.success(
                                                                NonNullList.of(Ingredient.EMPTY, ingredients));
                                            }
                                        },
                                        DataResult::success)
                                .forGetter(StampingRecipe::getIngredients),
                        ItemStack.CODEC.fieldOf("result").forGetter(StampingRecipe::getResult))
                .apply(ins, StampingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, StampingRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::encode, Serializer::decode);

        @Override
        public MapCodec<StampingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StampingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static StampingRecipe decode(RegistryFriendlyByteBuf buf) {
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            int size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            return new StampingRecipe(ingredients, result);
        }

        private static void encode(RegistryFriendlyByteBuf buf, StampingRecipe recipe) {
            ItemStack.STREAM_CODEC.encode(buf, recipe.result);
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
        }
    }

    public static class Builder extends AbstractItemProcessBuilder<StampingRecipe> {
        @Override
        public StampingRecipe buildRecipe() {
            return new StampingRecipe(ingredients, result);
        }

        @Override
        public String getType() {
            return "stamping";
        }
    }
}
