package dev.dubhe.anvilcraft.recipe.anvil;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractItemProcessBuilder;
import dev.dubhe.anvilcraft.util.CodecUtil;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Getter
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UnpackRecipe extends AbstractItemProcessRecipe {

    public UnpackRecipe(NonNullList<Ingredient> ingredients, List<ChanceItemStack> result) {
        super(ingredients, result);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.UNPACK_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.UNPACK_SERIALIZERS.get();
    }

    public static class Serializer implements RecipeSerializer<UnpackRecipe> {
        private static final MapCodec<UnpackRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                CodecUtil.createIngredientListCodec("ingredients", 9, "unpack")
                    .forGetter(UnpackRecipe::getIngredients),
                ChanceItemStack.CODEC.listOf().fieldOf("results").forGetter(UnpackRecipe::getResults))
            .apply(ins, UnpackRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, UnpackRecipe> STREAM_CODEC =
            StreamCodec.of(Serializer::encode, Serializer::decode);

        @Override
        public MapCodec<UnpackRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, UnpackRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static UnpackRecipe decode(RegistryFriendlyByteBuf buf) {
            List<ChanceItemStack> results = new ArrayList<>();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) {
                results.add(ChanceItemStack.STREAM_CODEC.decode(buf));
            }
            size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            return new UnpackRecipe(ingredients, results);
        }

        private static void encode(RegistryFriendlyByteBuf buf, UnpackRecipe recipe) {
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

    public static class Builder extends AbstractItemProcessBuilder<UnpackRecipe> {
        @Override
        public UnpackRecipe buildRecipe() {
            return new UnpackRecipe(ingredients, results);
        }

        @Override
        public String getType() {
            return "unpack";
        }
    }
}
