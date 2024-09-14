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
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ItemCompressRecipe extends AbstractItemProcessRecipe {
    public ItemCompressRecipe(NonNullList<Ingredient> ingredients, ItemStack result) {
        super(ingredients, result);
    }

    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.ITEM_COMPRESS_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.ITEM_COMPRESS_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<ItemCompressRecipe> {
        private static final MapCodec<ItemCompressRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                        CodecUtil.createIngredientListCodec("ingredients", 9, "item_compress")
                                .forGetter(ItemCompressRecipe::getIngredients),
                        ItemStack.CODEC.fieldOf("result").forGetter(ItemCompressRecipe::getResult))
                .apply(ins, ItemCompressRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ItemCompressRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::encode, Serializer::decode);

        @Override
        public MapCodec<ItemCompressRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ItemCompressRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static ItemCompressRecipe decode(RegistryFriendlyByteBuf buf) {
            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
            int size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            return new ItemCompressRecipe(ingredients, result);
        }

        private static void encode(RegistryFriendlyByteBuf buf, ItemCompressRecipe recipe) {
            ItemStack.STREAM_CODEC.encode(buf, recipe.result);
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
        }
    }

    public static class Builder extends AbstractItemProcessBuilder<ItemCompressRecipe> {
        @Override
        public ItemCompressRecipe buildRecipe() {
            return new ItemCompressRecipe(this.ingredients, this.result);
        }

        @Override
        public String getType() {
            return "item_compress";
        }
    }
}
