package dev.dubhe.anvilcraft.recipe.anvil;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractItemProcessBuilder;
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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BoilingRecipe extends AbstractItemProcessRecipe {
    public BoilingRecipe(NonNullList<Ingredient> ingredients, List<ItemStack> results) {
        super(ingredients, results);
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
                        ItemStack.CODEC.listOf().fieldOf("result").forGetter(BoilingRecipe::getResults))
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
            List<ItemStack> results = new ArrayList<>();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) {
                results.add(ItemStack.STREAM_CODEC.decode(buf));
            }
            size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            return new BoilingRecipe(ingredients, results);
        }

        private static void encode(RegistryFriendlyByteBuf buf, BoilingRecipe recipe) {
            buf.writeVarInt(recipe.results.size());
            for (ItemStack stack : recipe.results) {
                ItemStack.STREAM_CODEC.encode(buf, stack);
            }
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
        }
    }

    public static class Builder extends AbstractItemProcessBuilder<BoilingRecipe> {
        @Override
        public BoilingRecipe buildRecipe() {
            return new BoilingRecipe(ingredients, results);
        }

        @Override
        public String getType() {
            return "boiling";
        }
    }
}
