package dev.dubhe.anvilcraft.recipe;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.builder.AbstractItemProcessBuilder;
import dev.dubhe.anvilcraft.util.CodecUtil;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BoilingRecipe extends AbstractItemProcessRecipe {
    public BoilingRecipe(NonNullList<Ingredient> ingredients, ItemStack result) {
        super(ingredients, result);
    }

    @Contract(" -> new")
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.BOILING_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.BOILING_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<BoilingRecipe> {
        private static final MapCodec<BoilingRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                        CodecUtil.createIngredientListCodec("ingredients", 9, "boiling")
                                .forGetter(BoilingRecipe::getIngredients),
                        ItemStack.CODEC.fieldOf("result").forGetter(BoilingRecipe::getResult))
                .apply(ins, BoilingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BoilingRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::encode, Serializer::decode);

        @Override
        public MapCodec<BoilingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BoilingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static BoilingRecipe decode(RegistryFriendlyByteBuf buf) {
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            int size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            return new BoilingRecipe(ingredients, result);
        }

        private static void encode(RegistryFriendlyByteBuf buf, BoilingRecipe recipe) {
            ItemStack.STREAM_CODEC.encode(buf, recipe.result);
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
        }
    }

    public static class Builder extends AbstractItemProcessBuilder<BoilingRecipe> {
        @Override
        public BoilingRecipe buildRecipe() {
            return new BoilingRecipe(ingredients, result);
        }

        @Override
        public String getType() {
            return "boiling";
        }
    }
}
