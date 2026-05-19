package dev.dubhe.anvilcraft.recipe.mineral;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public record MineralFountainChanceRecipe(Identifier dimension, BlockStatePredicate fromBlock, ChanceBlockState toBlock) implements
    Recipe<MineralFountainChanceRecipe.Input> {
    private static final MapCodec<MineralFountainChanceRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Identifier.CODEC
            .fieldOf("dimension")
            .forGetter(MineralFountainChanceRecipe::dimension),
        BlockStatePredicate.CODEC
            .fieldOf("from_block")
            .forGetter(MineralFountainChanceRecipe::fromBlock),
        ChanceBlockState.CODEC
            .fieldOf("to_block")
            .forGetter(MineralFountainChanceRecipe::toBlock)
    ).apply(ins, MineralFountainChanceRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, MineralFountainChanceRecipe> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC,
        MineralFountainChanceRecipe::dimension,
        BlockStatePredicate.STREAM_CODEC,
        MineralFountainChanceRecipe::fromBlock,
        ChanceBlockState.STREAM_CODEC,
        MineralFountainChanceRecipe::toBlock,
        MineralFountainChanceRecipe::new
    );
    public static final RecipeSerializer<MineralFountainChanceRecipe> SERIALIZER = new RecipeSerializer<>(
        MineralFountainChanceRecipe.CODEC,
        MineralFountainChanceRecipe.STREAM_CODEC
    );

    public double getChance(ServerLevel level) {
        return this.toBlock.chance().getFloat(RecipeUtil.emptyLootContext(level));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<MineralFountainChanceRecipe> getType() {
        return ModRecipeTypes.MINERAL_FOUNTAIN_CHANCE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    @Override
    public RecipeSerializer<MineralFountainChanceRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public ItemStack assemble(Input input) {
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

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "mineral_fountain_chance";
    }

    public record Input(Identifier dimension, Block fromBlock) implements RecipeInput {
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

    @Setter
    @Accessors(fluent = true, chain = true)
    public static class Builder extends AbstractRecipeBuilder<MineralFountainChanceRecipe> {
        private Identifier dimension;
        private BlockStatePredicate fromBlock;
        private ChanceBlockState toBlock;

        public Builder fromBlock(Block fromBlock) {
            this.fromBlock = BlockStatePredicate.builder().of(fromBlock).build();
            return this;
        }

        public Builder fromBlock(HolderGetter<Block> blocks, TagKey<Block> fromBlock) {
            this.fromBlock = BlockStatePredicate.builder().of(blocks, fromBlock).build();
            return this;
        }

        /**
         * 添加结果方块
         *
         * @param result 结果方块
         *
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
         *
         * @return 构建器实例
         */
        public Builder toBlock(Block result, float chance) {
            return this.toBlock(new ChanceBlockState(result.defaultBlockState(), chance));
        }

        /**
         * 添加结果方块（默认概率为1.0F）
         *
         * @param result 结果方块
         *
         * @return 构建器实例
         */
        public Builder toBlock(Block result) {
            return this.toBlock(result, 1.0F);
        }

        @Override
        public MineralFountainChanceRecipe buildRecipe() {
            return new MineralFountainChanceRecipe(this.dimension, this.fromBlock, this.toBlock);
        }

        @Override
        public void save(RecipeOutput recipeOutput) {
            save(
                recipeOutput,
                AnvilCraft.of(this.getResult().typeHolder().getKey().identifier().getPath())
                    .withPrefix(this.getType() + "/")
                    .withSuffix("_from_" + this.dimension.getPath())
            );
        }

        @Override
        public void validate(Identifier id) {
            if (this.dimension == null) {
                throw new IllegalArgumentException("Dimension must be not null, RecipeId: " + id);
            }
            if (this.fromBlock == null) {
                throw new IllegalArgumentException("FromBlock must be not null, RecipeId: " + id);
            }
            if (this.toBlock == null) {
                throw new IllegalArgumentException("ToBlock must be not null, RecipeId: " + id);
            }
        }

        @Override
        public String getType() {
            return "mineral_fountain_chance";
        }

        @Override
        public ItemStackTemplate getResult() {
            return new ItemStackTemplate(this.toBlock.state().getBlock().asItem());
        }
    }
}
