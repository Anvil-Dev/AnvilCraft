package dev.dubhe.anvilcraft.recipe.anvil;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractItemProcessBuilder;
import dev.dubhe.anvilcraft.util.CodecUtil;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemCrushRecipe extends AbstractItemProcessRecipe {
    public ItemCrushRecipe(NonNullList<Ingredient> ingredients, List<ChanceItemStack> result) {
        super(ingredients, result);
    }

    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.ITEM_CRUSH_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.ITEM_CRUSH_SERIALIZERS.get();
    }

    public static class Serializer implements RecipeSerializer<ItemCrushRecipe> {
        private static final MapCodec<ItemCrushRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                        CodecUtil.createIngredientListCodec("ingredients", 9, "item_crush")
                                .forGetter(ItemCrushRecipe::getIngredients),
                        ChanceItemStack.CODEC.listOf().fieldOf("results").forGetter(ItemCrushRecipe::getResults))
                .apply(ins, ItemCrushRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ItemCrushRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::encode, Serializer::decode);

        @Override
        public MapCodec<ItemCrushRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ItemCrushRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static ItemCrushRecipe decode(RegistryFriendlyByteBuf buf) {
            List<ChanceItemStack> results = new ArrayList<>();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) {
                results.add(ChanceItemStack.STREAM_CODEC.decode(buf));
            }
            size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            return new ItemCrushRecipe(ingredients, results);
        }

        private static void encode(RegistryFriendlyByteBuf buf, ItemCrushRecipe recipe) {
            buf.writeVarInt(recipe.results.size());
            for (ChanceItemStack stack : recipe.results) {
                ChanceItemStack.STREAM_CODEC.encode(buf, stack);
            }
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
        }
    }

    public static class Builder extends AbstractItemProcessBuilder<ItemCrushRecipe> {
        @Override
        public ItemCrushRecipe buildRecipe() {
            return new ItemCrushRecipe(ingredients, results);
        }

        @Override
        public String getType() {
            return "item_crush";
        }
    }

}

