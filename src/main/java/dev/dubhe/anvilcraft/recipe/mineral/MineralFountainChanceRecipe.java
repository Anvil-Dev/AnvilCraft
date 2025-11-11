package dev.dubhe.anvilcraft.recipe.mineral;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.recipe.component.ChanceBlockState;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MineralFountainChanceRecipe implements Recipe<MineralFountainChanceRecipe.Input> {
    private final ResourceLocation dimension;
    private final BlockStatePredicate fromBlock;
    private final ChanceBlockState toBlock;

    public MineralFountainChanceRecipe(ResourceLocation dimension, BlockStatePredicate fromBlock, ChanceBlockState toBlock) {
        this.dimension = dimension;
        this.fromBlock = fromBlock;
        this.toBlock = toBlock;
    }

    public double getChance(ServerLevel level) {
        return this.toBlock.chance().getFloat(RecipeUtil.emptyLootContext(level));
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
        return this.toBlock.state().getBlock().asItem() == Items.AIR
               ? ItemStack.EMPTY
               : new ItemStack(this.fromBlock.getStatesCache().getFirst().getBlock());
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return this.toBlock.state().getBlock().asItem() == Items.AIR
               ? ItemStack.EMPTY
               : new ItemStack(this.fromBlock.getStatesCache().getFirst().getBlock());
    }

    @Override
    public boolean matches(Input input, Level level) {
        return input.dimension.equals(this.dimension) && this.fromBlock.test(level, input.fromBlock.defaultBlockState(), null);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public record Input(ResourceLocation dimension, Block fromBlock) implements RecipeInput {
        @Override
        public ItemStack getItem(int i) {
            return new ItemStack(this.fromBlock);
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
                BlockStatePredicate.CODEC
                    .fieldOf("from_block")
                    .forGetter(MineralFountainChanceRecipe::getFromBlock),
                ChanceBlockState.CODEC
                    .fieldOf("to_block")
                    .forGetter(MineralFountainChanceRecipe::getToBlock)
            )
            .apply(ins, MineralFountainChanceRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, MineralFountainChanceRecipe> STREAM_CODEC =
            StreamCodec.composite(
                ResourceLocation.STREAM_CODEC,
                MineralFountainChanceRecipe::getDimension,
                BlockStatePredicate.STREAM_CODEC,
                MineralFountainChanceRecipe::getFromBlock,
                ChanceBlockState.STREAM_CODEC,
                MineralFountainChanceRecipe::getToBlock,
                MineralFountainChanceRecipe::new
            );

        @Override
        public MapCodec<MineralFountainChanceRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MineralFountainChanceRecipe> streamCodec() {
            return Serializer.STREAM_CODEC;
        }
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<MineralFountainChanceRecipe> {
        private ResourceLocation dimension;
        private BlockStatePredicate fromBlock;
        private ChanceBlockState toBlock;

        public Builder fromBlock(Block fromBlock) {
            this.fromBlock = BlockStatePredicate.builder().of(fromBlock).build();
            return this;
        }

        public Builder fromBlock(TagKey<Block> fromBlock) {
            this.fromBlock = BlockStatePredicate.builder().of(fromBlock).build();
            return this;
        }

        /**
         * 添加结果方块
         *
         * @param result 结果方块
         * @return 构建器实例
         */
        public Builder toBlock(ChanceBlockState result) {
            this.toBlock = result;
            return this;
        }

        /**
         * 添加结果方块（指定概率）
         *
         * @param result 结果方块
         * @param chance 概率
         * @return 构建器实例
         */
        public Builder toBlock(Block result, float chance) {
            return this.toBlock(new ChanceBlockState(result.defaultBlockState(), chance));
        }

        /**
         * 添加结果方块（默认概率为1.0f）
         *
         * @param result 结果方块
         * @return 构建器实例
         */
        public Builder toBlock(Block result) {
            return this.toBlock(result, 1.0f);
        }

        @Override
        public MineralFountainChanceRecipe buildRecipe() {
            return new MineralFountainChanceRecipe(this.dimension, this.fromBlock, this.toBlock);
        }

        @Override
        public void save(RecipeOutput recipeOutput) {
            save(
                recipeOutput,
                AnvilCraft.of(BuiltInRegistries.ITEM.getKey(getResult()).getPath())
                    .withPrefix(getType() + "/")
                    .withSuffix("_from_" + this.dimension.getPath())
            );
        }

        @Override
        public void validate(ResourceLocation pId) {
            if (this.dimension == null) {
                throw new IllegalArgumentException("Dimension must be not null, RecipeId: " + pId);
            }
            if (this.fromBlock == null) {
                throw new IllegalArgumentException("FromBlock must be not null, RecipeId: " + pId);
            }
            if (this.toBlock == null) {
                throw new IllegalArgumentException("ToBlock must be not null, RecipeId: " + pId);
            }
        }

        @Override
        public String getType() {
            return "mineral_fountain_chance";
        }

        @Override
        public Item getResult() {
            return this.toBlock.state().getBlock().asItem();
        }
    }
}
