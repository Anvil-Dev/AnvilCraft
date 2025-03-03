package dev.dubhe.anvilcraft.recipe.anvil;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.util.CodecUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BlockCompressRecipe implements Recipe<BlockCompressRecipe.Input> {
    public final List<Either<TagKey<Block>, Block>> inputs;
    public final Block result;

    public BlockCompressRecipe(List<Either<TagKey<Block>, Block>> inputs, Block result) {
        this.inputs = inputs;
        this.result = result;
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
            Either<TagKey<Block>, Block> either = inputs.get(i);
            boolean[] result = new boolean[]{true};
            int finalI = i;
            either.ifLeft(tag -> {
                    if (!pInput.inputs.get(finalI).defaultBlockState().is(tag)) {
                        result[0] = false;
                    }
                })
                .ifRight(block -> {
                    if (!block.equals(pInput.inputs.get(finalI))) {
                        result[0] = false;
                    }
                });
            if (!result[0]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(Input pInput, HolderLookup.Provider pRegistries) {
        return ItemStack.EMPTY;
    }

    public BlockCompressRecipe self() {
        return this;
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
        private static final MapCodec<BlockCompressRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                Codec.xor(TagKey.hashedCodec(Registries.BLOCK), CodecUtil.BLOCK_CODEC)
                    .listOf(1, 9)
                    .fieldOf("inputs")
                    .forGetter(BlockCompressRecipe::getInputs),
                CodecUtil.BLOCK_CODEC.fieldOf("result").forGetter(BlockCompressRecipe::getResult))
            .apply(ins, BlockCompressRecipe::new));

        public static final Codec<BlockCompressRecipe> CODEC_NOT_MAP = CODEC.codec();

        private static final StreamCodec<RegistryFriendlyByteBuf, BlockCompressRecipe> STREAM_CODEC =
            StreamCodec.composite(
                CodecUtil.nbtWrapped(CODEC_NOT_MAP),
                BlockCompressRecipe::self,
                tag -> tag
            );

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

        private List<Either<TagKey<Block>, Block>> inputs = new ArrayList<>();
        private Block result;

        public Builder input(Block block) {
            this.inputs.add(Either.right(block));
            return this;
        }

        public Builder input(TagKey<Block> block) {
            this.inputs.add(Either.left(block));
            return this;
        }

        @Override
        public BlockCompressRecipe buildRecipe() {
            return new BlockCompressRecipe(inputs, result);
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (inputs.isEmpty() || inputs.size() > 2) {
                throw new IllegalArgumentException("Recipe input list size must in 1-2, RecipeId: " + pId);
            }
            if (result == null) {
                throw new IllegalArgumentException("Recipe has no result, RecipeId:" + pId);
            }
        }

        @Override
        public Item getResult() {
            return result.asItem();
        }

        @Override
        public String getType() {
            return "block_compress";
        }
    }
}
