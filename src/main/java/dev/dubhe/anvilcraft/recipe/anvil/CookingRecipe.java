package dev.dubhe.anvilcraft.recipe.anvil;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractItemProcessBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.input.ItemProcessInput;
import dev.dubhe.anvilcraft.util.CodecUtil;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CookingRecipe extends AbstractItemProcessRecipe {
    public CookingRecipe(NonNullList<Ingredient> ingredients, List<ChanceItemStack> results) {
        super(ingredients, results);
    }

    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    @Override
    public boolean matches(ItemProcessInput pInput, Level pLevel) {
        return super.matches(pInput, pLevel);
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.COOKING_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.COOKING_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<CookingRecipe> {
        private static final MapCodec<CookingRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                CodecUtil.createIngredientListCodec("ingredients", 9, "cooking")
                    .forGetter(CookingRecipe::getIngredients),
                ChanceItemStack.CODEC.listOf().fieldOf("results").forGetter(CookingRecipe::getResults))
            .apply(ins, CookingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CookingRecipe> STREAM_CODEC =
            StreamCodec.of(Serializer::encode, Serializer::decode);

        @Override
        public MapCodec<CookingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CookingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static CookingRecipe decode(RegistryFriendlyByteBuf buf) {
            List<ChanceItemStack> results = new ArrayList<>();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) {
                results.add(ChanceItemStack.STREAM_CODEC.decode(buf));
            }
            size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            return new CookingRecipe(ingredients, results);
        }

        private static void encode(RegistryFriendlyByteBuf buf, CookingRecipe recipe) {
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

    public static class Builder extends AbstractItemProcessBuilder<CookingRecipe> {
        @Override
        public CookingRecipe buildRecipe() {
            return new CookingRecipe(ingredients, results);
        }

        @Override
        public String getType() {
            return "cooking";
        }
    }
}
