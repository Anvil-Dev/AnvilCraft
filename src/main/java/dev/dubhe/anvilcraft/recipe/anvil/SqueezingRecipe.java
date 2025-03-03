package dev.dubhe.anvilcraft.recipe.anvil;


import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.CodecUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SqueezingRecipe implements Recipe<SqueezingRecipe.Input> {
    public final Block inputBlock;
    public final Block resultBlock;
    public final Block cauldron;

    public SqueezingRecipe(Block inputBlock, Block resultBlock, Block cauldron) {
        this.inputBlock = inputBlock;
        this.resultBlock = resultBlock;
        this.cauldron = cauldron;
    }

    @Contract(" -> new")
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.SQUEEZING_TYPE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.SQUEEZING_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return resultBlock.asItem().getDefaultInstance();
    }

    @Override
    public boolean matches(Input input, Level level) {
        return input.inputBlock == inputBlock && CauldronUtil.compatibleForFill(input.cauldronState, this.cauldron, 1);
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider provider) {
        return ItemStack.EMPTY;
    }

    public record Input(Block inputBlock, BlockState cauldronState) implements RecipeInput {
        @Override
        public ItemStack getItem(int i) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return inputBlock == null;
        }
    }

    public static class Serializer implements RecipeSerializer<SqueezingRecipe> {
        private static final MapCodec<SqueezingRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                CodecUtil.BLOCK_CODEC.fieldOf("input_block").forGetter(SqueezingRecipe::getInputBlock),
                CodecUtil.BLOCK_CODEC.fieldOf("result_block").forGetter(SqueezingRecipe::getResultBlock),
                CodecUtil.BLOCK_CODEC.fieldOf("cauldron").forGetter(SqueezingRecipe::getCauldron))
            .apply(ins, SqueezingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, SqueezingRecipe> STREAM_CODEC = StreamCodec.composite(
            CodecUtil.BLOCK_STREAM_CODEC,
            SqueezingRecipe::getInputBlock,
            CodecUtil.BLOCK_STREAM_CODEC,
            SqueezingRecipe::getResultBlock,
            CodecUtil.BLOCK_STREAM_CODEC,
            SqueezingRecipe::getCauldron,
            SqueezingRecipe::new);

        @Override
        public MapCodec<SqueezingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SqueezingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<SqueezingRecipe> {
        private Block inputBlock;
        private Block resultBlock;
        private Block cauldron;

        @Override
        public SqueezingRecipe buildRecipe() {
            return new SqueezingRecipe(inputBlock, resultBlock, cauldron);
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (inputBlock == null) {
                throw new IllegalStateException("Recipe inputBlock must not be null, Recipe: " + pId);
            }
            if (resultBlock == null) {
                throw new IllegalStateException("Recipe resultBlock must not be null, Recipe: " + pId);
            }
            if (cauldron == null || !(cauldron instanceof AbstractCauldronBlock)) {
                throw new IllegalStateException("Recipe cauldron must be cauldron, Recipe: " + pId);
            }
        }

        @Override
        public String getType() {
            return "squeezing";
        }

        @Override
        public Item getResult() {
            return resultBlock.asItem();
        }
    }
}
