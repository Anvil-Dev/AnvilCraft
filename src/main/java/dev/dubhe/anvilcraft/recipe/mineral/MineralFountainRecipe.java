package dev.dubhe.anvilcraft.recipe.mineral;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.util.CodecUtil;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MineralFountainRecipe implements Recipe<MineralFountainRecipe.Input> {
    private final Block needBlock;
    private final Block fromBlock;
    private final Block toBlock;

    public MineralFountainRecipe(Block needBlock, Block fromBlock, Block toBlock) {
        this.needBlock = needBlock;
        this.fromBlock = fromBlock;
        this.toBlock = toBlock;
    }

    @Contract(" -> new")
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MINERAL_FOUNTAIN.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.MINERAL_FOUNTAIN_SERIALIZER.get();
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider provider) {
        return toBlock.asItem() == Items.AIR ? ItemStack.EMPTY : new ItemStack(needBlock);
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return toBlock.asItem() == Items.AIR ? ItemStack.EMPTY : new ItemStack(needBlock);
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (input.needBlock != needBlock) return false;
        return input.fromBlock == fromBlock;
    }

    public record Input(Block needBlock, Block fromBlock) implements RecipeInput {

        @Override
        public ItemStack getItem(int i) {
            return new ItemStack(needBlock);
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }

    public static class Serializer implements RecipeSerializer<MineralFountainRecipe> {

        private static final MapCodec<MineralFountainRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                CodecUtil.BLOCK_CODEC.fieldOf("need_block").forGetter(MineralFountainRecipe::getNeedBlock),
                CodecUtil.BLOCK_CODEC.fieldOf("from_block").forGetter(MineralFountainRecipe::getFromBlock),
                CodecUtil.BLOCK_CODEC.fieldOf("to_block").forGetter(MineralFountainRecipe::getToBlock))
            .apply(ins, MineralFountainRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, MineralFountainRecipe> STREAM_CODEC =
            StreamCodec.composite(
                CodecUtil.BLOCK_STREAM_CODEC,
                MineralFountainRecipe::getNeedBlock,
                CodecUtil.BLOCK_STREAM_CODEC,
                MineralFountainRecipe::getFromBlock,
                CodecUtil.BLOCK_STREAM_CODEC,
                MineralFountainRecipe::getToBlock,
                MineralFountainRecipe::new);

        @Override
        public MapCodec<MineralFountainRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MineralFountainRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<MineralFountainRecipe> {
        private Block needBlock;
        private Block fromBlock;
        private Block toBlock;

        @Override
        public MineralFountainRecipe buildRecipe() {
            return new MineralFountainRecipe(needBlock, fromBlock, toBlock);
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (needBlock == null) {
                throw new IllegalArgumentException("needBlock must not be null, RecipeId: " + pId);
            }
            if (fromBlock == null) {
                throw new IllegalArgumentException("fromBlock must not be null, RecipeId: " + pId);
            }
            if (toBlock == null) {
                throw new IllegalArgumentException("toBlock must not be null, RecipeId: " + pId);
            }
        }

        @Override
        public String getType() {
            return "mineral_fountain";
        }

        @Override
        public Item getResult() {
            return toBlock.asItem();
        }
    }
}
