package dev.dubhe.anvilcraft.recipe;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.builder.AbstractRecipeBuilder;

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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@Getter
public class BlockCompressRecipe implements Recipe<BlockCompressRecipe.Input> {
    public final List<Block> inputs;
    public final Block result;

    public BlockCompressRecipe(List<Block> inputs, Block result) {
        this.inputs = inputs;
        this.result = result;
    }

    public BlockCompressRecipe(List<String> inputs, String result) {
        this(
                inputs.stream()
                        .map(s -> BuiltInRegistries.BLOCK.get(ResourceLocation.parse(s)))
                        .toList(),
                BuiltInRegistries.BLOCK.get(ResourceLocation.parse(result)));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.BLOCK_COMPRESS_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.BLOCK_COMPRESS_SERIALIZER.get();
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

    public static class Serializer implements RecipeSerializer<BlockCompressRecipe> {
        private static final MapCodec<BlockCompressRecipe> CODEC =
                RecordCodecBuilder.mapCodec(ins -> ins.group(
                                Codec.STRING
                                        .listOf(1, 9)
                                        .fieldOf("inputs")
                                        .forGetter(recipe -> recipe.inputs.stream()
                                                .map(b -> BuiltInRegistries.BLOCK.getKey(b).toString())
                                                .toList()),
                                Codec.STRING.fieldOf("result").forGetter(recipe -> BuiltInRegistries.BLOCK
                                        .getKey(recipe.result)
                                        .toString()))
                        .apply(ins, BlockCompressRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BlockCompressRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(9)),
                        recipe -> recipe.inputs.stream()
                                .map(b -> BuiltInRegistries.BLOCK.getKey(b).toString())
                                .toList(),
                        ByteBufCodecs.STRING_UTF8,
                        recipe -> BuiltInRegistries.BLOCK.getKey(recipe.result).toString(),
                        BlockCompressRecipe::new);

        @Override
        public MapCodec<BlockCompressRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BlockCompressRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<BlockCompressRecipe> {

        private List<Block> inputs = new ArrayList<>();
        private Block result;

        public Builder input(Block block) {
            this.inputs.add(block);
            return this;
        }

        @Override
        public BlockCompressRecipe buildRecipe() {
            return new BlockCompressRecipe(inputs, result);
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
