package dev.dubhe.anvilcraft.recipe.mineral;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.util.CodecUtil;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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

import com.mojang.serialization.Codec;
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
public class MineralFountainChanceRecipe implements Recipe<MineralFountainChanceRecipe.Input> {
    private final ResourceLocation dimension;
    private final Block fromBlock;
    private final Block toBlock;
    private final double chance;

    public MineralFountainChanceRecipe(ResourceLocation dimension, Block fromBlock, Block toBlock, double chance) {
        this.dimension = dimension;
        this.fromBlock = fromBlock;
        this.toBlock = toBlock;
        this.chance = chance;
    }

    @Contract(" -> new")
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.MINERAL_FOUNTAIN_CHANCE.get();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.MINERAL_FOUNTAIN_CHANCE_SERIALIZER.get();
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider provider) {
        return toBlock.asItem() == Items.AIR ? ItemStack.EMPTY : new ItemStack(fromBlock);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return toBlock.asItem() == Items.AIR ? ItemStack.EMPTY : new ItemStack(fromBlock);
    }

    @Override
    public boolean matches(Input input, Level level) {
        return input.dimension.equals(dimension) && input.fromBlock == fromBlock;
    }

    public record Input(ResourceLocation dimension, Block fromBlock) implements RecipeInput {

        @Override
        public ItemStack getItem(int i) {
            return new ItemStack(fromBlock);
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

    public static class Serializer implements RecipeSerializer<MineralFountainChanceRecipe> {
        private static final MapCodec<MineralFountainChanceRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
                ResourceLocation.CODEC
                    .fieldOf("dimension")
                    .forGetter(MineralFountainChanceRecipe::getDimension),
                CodecUtil.BLOCK_CODEC
                    .fieldOf("from_block")
                    .forGetter(MineralFountainChanceRecipe::getFromBlock),
                CodecUtil.BLOCK_CODEC.fieldOf("to_block").forGetter(MineralFountainChanceRecipe::getToBlock),
                Codec.DOUBLE.fieldOf("chance").forGetter(MineralFountainChanceRecipe::getChance))
            .apply(ins, MineralFountainChanceRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, MineralFountainChanceRecipe> STREAM_CODEC =
            StreamCodec.composite(
                ResourceLocation.STREAM_CODEC,
                MineralFountainChanceRecipe::getDimension,
                CodecUtil.BLOCK_STREAM_CODEC,
                MineralFountainChanceRecipe::getFromBlock,
                CodecUtil.BLOCK_STREAM_CODEC,
                MineralFountainChanceRecipe::getToBlock,
                ByteBufCodecs.DOUBLE,
                MineralFountainChanceRecipe::getChance,
                MineralFountainChanceRecipe::new);

        @Override
        public MapCodec<MineralFountainChanceRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MineralFountainChanceRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<MineralFountainChanceRecipe> {

        private ResourceLocation dimension;
        private Block fromBlock;
        private Block toBlock;
        private double chance;

        @Override
        public MineralFountainChanceRecipe buildRecipe() {
            return new MineralFountainChanceRecipe(dimension, fromBlock, toBlock, chance);
        }

        @Override
        public void save(RecipeOutput recipeOutput) {
            save(
                recipeOutput,
                AnvilCraft.of(BuiltInRegistries.ITEM.getKey(getResult()).getPath())
                    .withPrefix(getType() + "/")
                    .withSuffix("_from_" + dimension.getPath()));
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (dimension == null) {
                throw new IllegalArgumentException("Dimension must be not null, RecipeId: " + pId);
            }
            if (fromBlock == null) {
                throw new IllegalArgumentException("FromBlock must be not null, RecipeId: " + pId);
            }
            if (toBlock == null) {
                throw new IllegalArgumentException("ToBlock must be not null, RecipeId: " + pId);
            }
            if (chance <= 0 || chance > 1) {
                throw new IllegalArgumentException("Chance must be between 0 and 1, RecipeId: " + pId);
            }
        }

        @Override
        public String getType() {
            return "mineral_fountain_chance";
        }

        @Override
        public Item getResult() {
            return toBlock.asItem();
        }
    }
}
