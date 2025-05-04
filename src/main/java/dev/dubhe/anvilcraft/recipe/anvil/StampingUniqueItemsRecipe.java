package dev.dubhe.anvilcraft.recipe.anvil;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractItemProcessBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.input.ItemProcessInput;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class StampingUniqueItemsRecipe extends StampingRecipe {
    public StampingUniqueItemsRecipe(
        NonNullList<Ingredient> ingredients,
        List<ChanceItemStack> results
    ) {
        super(ingredients, results);
    }

    public static Builder builderUnique() {
        return new Builder();
    }

    @Override
    public boolean matches(ItemProcessInput pInput, Level pLevel) {
        if (pInput.items().size() != Set.copyOf(pInput.items()).size()) return false;
        if (pInput.items().size() != this.ingredients.size()) return false;
        if (!Util.allMatch(pInput.items(), itemStack -> itemStack.getCount() == 1)) return false;

        for (int i = 0; i < pInput.size(); i++) {
            if (!this.ingredients.get(i).test(pInput.getItem(i))) return false;
        }

        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.STAMPING_UNIQUE_ITEMS_SERIALIZER.get();
    }

    @Override
    public int getMaxCraftTime(ItemProcessInput pInput) {
        return 1;
    }

    public static class Serializer implements RecipeSerializer<StampingUniqueItemsRecipe> {
        private static final MapCodec<StampingUniqueItemsRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                CodecUtil.createIngredientListCodec("ingredients", 9, "stamping_unique_items")
                    .forGetter(StampingUniqueItemsRecipe::getIngredients),
                ChanceItemStack.CODEC.listOf().fieldOf("results").forGetter(StampingUniqueItemsRecipe::getResults))
            .apply(ins, StampingUniqueItemsRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, StampingUniqueItemsRecipe> STREAM_CODEC =
            StreamCodec.of(Serializer::encode, Serializer::decode);

        @Override
        public MapCodec<StampingUniqueItemsRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StampingUniqueItemsRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static StampingUniqueItemsRecipe decode(RegistryFriendlyByteBuf buf) {
            List<ChanceItemStack> results = new ArrayList<>();
            int size = buf.readVarInt();
            for (int i = 0; i < size; i++) {
                results.add(ChanceItemStack.STREAM_CODEC.decode(buf));
            }
            size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            return new StampingUniqueItemsRecipe(ingredients, results);
        }

        private static void encode(RegistryFriendlyByteBuf buf, StampingUniqueItemsRecipe recipe) {
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

    public static class Builder extends AbstractItemProcessBuilder<StampingUniqueItemsRecipe> {
        @Override
        public StampingUniqueItemsRecipe buildRecipe() {
            return new StampingUniqueItemsRecipe(ingredients, results);
        }

        @Override
        public String getType() {
            return "stamping_unique_items";
        }
    }
}
