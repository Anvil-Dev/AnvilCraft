package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.builder.AbstractItemProcessBuilder;
import dev.dubhe.anvilcraft.recipe.input.ItemProcessInput;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SuperHeatingRecipe extends AbstractItemProcessRecipe {

    @Nullable
    public final Block blockResult;

    public SuperHeatingRecipe(NonNullList<Ingredient> ingredients, ItemStack result) {
        super(ingredients, result);
        this.blockResult = null;
    }

    public SuperHeatingRecipe(NonNullList<Ingredient> ingredients, ItemStack result, @Nullable Block blockResult) {
        super(ingredients, result);
        this.blockResult = blockResult;
    }

    public static SuperHeatingRecipe fromCodec(NonNullList<Ingredient> ingredients, ItemStack result, String blockResult) {
        if (blockResult.isEmpty()) {
            return new SuperHeatingRecipe(ingredients, result);
        } else {
            return new SuperHeatingRecipe(ingredients, result, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockResult)));
        }
    }

    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    @Override
    public int getMaxCraftTime(ItemProcessInput pInput) {
        int times = super.getMaxCraftTime(pInput);
        if (times >= 1 && blockResult != null) {
            cacheInput = pInput;
            cacheMaxCraftTime = 1;
            return 1;
        } else {
            return times;
        }
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.SUPER_HEATING_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.SUPER_HEATING_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<SuperHeatingRecipe> {

        private static final MapCodec<SuperHeatingRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                Ingredient.CODEC_NONEMPTY
                        .listOf(1, 64)
                        .fieldOf("ingredients")
                        .flatXmap(
                                i -> {
                                    Ingredient[] ingredients = i.toArray(Ingredient[]::new);
                                    if (ingredients.length == 0) {
                                        return DataResult.error(() -> "No ingredients for super_heating recipe");
                                    } else {
                                        return ingredients.length > 64
                                                ? DataResult.error(() ->
                                                "Too many ingredients for super_heating recipe. The maximum is: 64")
                                                : DataResult.success(NonNullList.of(Ingredient.EMPTY, ingredients));
                                    }
                                },
                                DataResult::success)
                        .forGetter(SuperHeatingRecipe::getIngredients),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("result", ItemStack.EMPTY).forGetter(SuperHeatingRecipe::getResult),
                Codec.STRING.optionalFieldOf("block_result", "").forGetter(recipe -> {
                    if (recipe.blockResult != null) {
                        return BuiltInRegistries.BLOCK.getKey(recipe.blockResult).toString();
                    } else {
                        return "";
                    }
                })
        ).apply(ins, SuperHeatingRecipe::fromCodec));

        private static final StreamCodec<RegistryFriendlyByteBuf, SuperHeatingRecipe> STREAM_CODEC = StreamCodec.of(
                Serializer::encode, Serializer::decode);

        @Override
        public MapCodec<SuperHeatingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SuperHeatingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static SuperHeatingRecipe decode(RegistryFriendlyByteBuf buf) {
            ItemStack result = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            int size = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(i -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            String string = buf.readUtf();
            if (string.isEmpty()) {
                return new SuperHeatingRecipe(ingredients, result);
            } else {
                return new SuperHeatingRecipe(ingredients, result, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(string)));
            }

        }

        private static void encode(RegistryFriendlyByteBuf buf, SuperHeatingRecipe recipe) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.result);
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
            if (recipe.blockResult != null) {
                buf.writeUtf(BuiltInRegistries.BLOCK.getKey(recipe.blockResult).toString());
            } else {
                buf.writeUtf("");
            }
        }
    }


    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractItemProcessBuilder<SuperHeatingRecipe> {
        protected Block blockResult;

        @Override
        public void save(RecipeOutput recipeOutput) {
            ResourceLocation id;
            if (result == null || result.isEmpty()) {
                if (blockResult != null) {
                    id = BuiltInRegistries.BLOCK.getKey(blockResult);
                } else {
                    throw new IllegalArgumentException("Recipe either result or blockResult must not be null");
                }
            } else {
                id = BuiltInRegistries.ITEM.getKey(result.getItem());
            }
            save(recipeOutput, AnvilCraft.of(id.getPath()).withPrefix(getType() + "/"));
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (ingredients.isEmpty() || ingredients.size() > 64) {
                throw new IllegalArgumentException("Recipe ingredients size must in 0-64, RecipeId: " + pId);
            }
            if (result == null && blockResult == null) {
                throw new IllegalArgumentException(
                        "Recipe either result or blockResult must not be null, RecipeId: " + pId);
            }
        }

        @Override
        public SuperHeatingRecipe buildRecipe() {
            if (result == null) {
                result = ItemStack.EMPTY;
            }
            return new SuperHeatingRecipe(ingredients, result, blockResult);
        }

        @Override
        public String getType() {
            return "super_heating";
        }
    }
}
