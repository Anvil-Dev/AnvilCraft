package dev.dubhe.anvilcraft.recipe.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractItemProcessBuilder;
import dev.dubhe.anvilcraft.recipe.anvil.input.ItemProcessInput;
import dev.dubhe.anvilcraft.util.CodecUtil;

import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.minecraft.world.level.block.Blocks;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SuperHeatingRecipe extends AbstractItemProcessRecipe {

    public final Block blockResult;

    public SuperHeatingRecipe(NonNullList<Ingredient> ingredients, ItemStack result, Block blockResult) {
        super(ingredients, result);
        this.blockResult = blockResult;
    }

    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    @Override
    public int getMaxCraftTime(ItemProcessInput pInput) {
        int times = super.getMaxCraftTime(pInput);
        if (times >= 1 && blockResult != Blocks.AIR) {
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
                        CodecUtil.createIngredientListCodec("ingredients", 64, "super_heating")
                                .forGetter(SuperHeatingRecipe::getIngredients),
                        ItemStack.OPTIONAL_CODEC
                                .optionalFieldOf("result", ItemStack.EMPTY)
                                .forGetter(SuperHeatingRecipe::getResult),
                        CodecUtil.BLOCK_CODEC
                                .optionalFieldOf("block_result", Blocks.AIR)
                                .forGetter(SuperHeatingRecipe::getBlockResult))
                .apply(ins, SuperHeatingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, SuperHeatingRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::encode, Serializer::decode);

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
            Block blockResult = CodecUtil.BLOCK_STREAM_CODEC.decode(buf);
            return new SuperHeatingRecipe(ingredients, result, blockResult);
        }

        private static void encode(RegistryFriendlyByteBuf buf, SuperHeatingRecipe recipe) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, recipe.result);
            buf.writeVarInt(recipe.ingredients.size());
            for (Ingredient ingredient : recipe.ingredients) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
            }
            CodecUtil.BLOCK_STREAM_CODEC.encode(buf, recipe.blockResult);
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
            if (blockResult == null) {
                blockResult = Blocks.AIR;
            }
            return new SuperHeatingRecipe(ingredients, result, blockResult);
        }

        @Override
        public String getType() {
            return "super_heating";
        }
    }
}
