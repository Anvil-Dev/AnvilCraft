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

import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@Getter
public class CompressRecipe implements Recipe<CompressRecipe.Input> {
    public final List<Block> inputs;
    public final Block result;

    public CompressRecipe(List<Block> inputs, Block result) {
        this.inputs = inputs;
        this.result = result;
    }

    public CompressRecipe(List<String> inputs, String result) {
        this(
                inputs.stream().map(s -> BuiltInRegistries.BLOCK.get(ResourceLocation.parse(s))).toList(),
                BuiltInRegistries.BLOCK.get(ResourceLocation.parse(result))
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.COMPRESS_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.COMPRESS_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider pRegistries) {
        return result.asItem().getDefaultInstance();
    }

    @Override
    public boolean matches(Input pInput, Level pLevel) {
        if (pInput.inputs.size() < inputs.size()) {
            return false;
        }
        for (int i = 0; i < inputs.size(); i++) {
            if (!inputs.get(i).equals(pInput.inputs.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(Input pInput, HolderLookup.Provider pRegistries) {
        return ItemStack.EMPTY;
    }

    public record Input(List<Block> inputs) implements RecipeInput {

        @Override
        public ItemStack getItem(int pIndex) {
            return inputs.get(pIndex).asItem().getDefaultInstance();
        }

        @Override
        public int size() {
            return inputs.size();
        }

        @Override
        public boolean isEmpty() {
            for (Block block : inputs) {
                if (block == null) return true;
            }
            return false;
        }
    }

    public static class Serializer implements RecipeSerializer<CompressRecipe> {
        private static final MapCodec<CompressRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                Codec.STRING.listOf(1, 9).fieldOf("inputs").forGetter(recipe -> recipe.inputs.stream()
                        .map(b -> BuiltInRegistries.BLOCK.getKey(b).toString())
                        .toList()),
                Codec.STRING.fieldOf("result").forGetter(recipe -> BuiltInRegistries.BLOCK.getKey(recipe.result).toString())
        ).apply(ins, CompressRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CompressRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(9)),
                recipe -> recipe.inputs.stream()
                        .map(b -> BuiltInRegistries.BLOCK.getKey(b).toString())
                        .toList(),
                ByteBufCodecs.STRING_UTF8, recipe -> BuiltInRegistries.BLOCK.getKey(recipe.result).toString(),
                CompressRecipe::new
        );

        @Override
        public MapCodec<CompressRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CompressRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<CompressRecipe> {

        private List<Block> inputs = new ArrayList<>();
        private Block result;

        public Builder input(Block block) {
            this.inputs.add(block);
            return this;
        }

        @Override
        public CompressRecipe buildRecipe() {
            return new CompressRecipe(inputs, result);
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (inputs.isEmpty() || inputs.size() > 9) {
                throw new IllegalArgumentException("Recipe input list size must in 0-9, RecipeId: " + pId);
            }
            if (result == null) {
                throw new IllegalArgumentException("Recipe has no result, RecipeId:" + pId);
            }
        }

        @Override
        public Item getResult() {
            return result.asItem();
        }
    }
}
