package dev.dubhe.anvilcraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.builder.AbstractRecipeBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

@MethodsReturnNonnullByDefault
@Getter
public class CrushRecipe implements Recipe<CrushRecipe.Input> {

    public final Block input;
    public final Block result;

    public CrushRecipe(Block input, Block result) {
        this.input = input;
        this.result = result;
    }

    public CrushRecipe(String input, String result) {
        this(
                BuiltInRegistries.BLOCK.get(ResourceLocation.parse(input)),
                BuiltInRegistries.BLOCK.get(ResourceLocation.parse(result))
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.CRUSH_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.CRUSH_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider pRegistries) {
        return this.result.asItem().getDefaultInstance();
    }

    @Override
    public boolean matches(Input pInput, Level pLevel) {
        return this.input == pInput.input;
    }

    @Override
    public ItemStack assemble(Input pInput, HolderLookup.Provider pRegistries) {
        return ItemStack.EMPTY;
    }

    public record Input(Block input) implements RecipeInput {

        @Override
        public ItemStack getItem(int pIndex) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return input == null;
        }
    }

    public static class Serializer implements RecipeSerializer<CrushRecipe> {
        private static final MapCodec<CrushRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                Codec.STRING.fieldOf("input").forGetter(recipe -> BuiltInRegistries.BLOCK.getKey(recipe.input).toString()),
                Codec.STRING.fieldOf("result").forGetter(recipe -> BuiltInRegistries.BLOCK.getKey(recipe.result).toString())
        ).apply(ins, CrushRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CrushRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, recipe -> BuiltInRegistries.BLOCK.getKey(recipe.getInput()).toString(),
                ByteBufCodecs.STRING_UTF8, recipe -> BuiltInRegistries.BLOCK.getKey(recipe.getResult()).toString(),
                CrushRecipe::new
        );

        @Override
        public MapCodec<CrushRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrushRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<CrushRecipe> {
        private Block input;
        private Block result;

        @Override
        public Item getResult() {
            return result.asItem();
        }

        @Override
        public CrushRecipe buildRecipe() {
            return new CrushRecipe(input, result);
        }

        @Override
        public void validate(ResourceLocation recipeId) {
            if (input == null) {
                throw new IllegalArgumentException("Recipe has no input, RecipeId:" + recipeId);
            }
            if (result == null) {
                throw new IllegalArgumentException("Recipe has no result, RecipeId:" + recipeId);
            }
        }
    }
}
