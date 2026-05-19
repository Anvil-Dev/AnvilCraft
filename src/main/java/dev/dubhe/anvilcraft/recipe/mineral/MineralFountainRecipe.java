package dev.dubhe.anvilcraft.recipe.mineral;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
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

public record MineralFountainRecipe(BlockStatePredicate needBlock, BlockStatePredicate fromBlock, ChanceBlockState toBlock) implements
    Recipe<MineralFountainRecipe.Input> {
    private static final MapCodec<MineralFountainRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        BlockStatePredicate.CODEC
            .fieldOf("need_block")
            .forGetter(MineralFountainRecipe::needBlock),
        BlockStatePredicate.CODEC
            .fieldOf("from_block")
            .forGetter(MineralFountainRecipe::fromBlock),
        ChanceBlockState.CODEC
            .fieldOf("to_block")
            .forGetter(MineralFountainRecipe::toBlock)
    ).apply(ins, MineralFountainRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, MineralFountainRecipe> STREAM_CODEC = StreamCodec.composite(
        BlockStatePredicate.STREAM_CODEC,
        MineralFountainRecipe::needBlock,
        BlockStatePredicate.STREAM_CODEC,
        MineralFountainRecipe::fromBlock,
        ChanceBlockState.STREAM_CODEC,
        MineralFountainRecipe::toBlock,
        MineralFountainRecipe::new
    );
    public static final RecipeSerializer<MineralFountainRecipe> SERIALIZER = new RecipeSerializer<>(
        MineralFountainRecipe.CODEC,
        MineralFountainRecipe.STREAM_CODEC
    );

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeType<MineralFountainRecipe> getType() {
        return ModRecipeTypes.MINERAL_FOUNTAIN.get();
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
    public RecipeSerializer<MineralFountainRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public ItemStack assemble(Input input) {
        return this.toBlock.state().getBlock().asItem() == Items.AIR
               ? ItemStack.EMPTY
               : new ItemStack(this.needBlock.getStatesCache().getFirst().getBlock());
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (!this.needBlock.test(level, input.needBlock.defaultBlockState(), null)) {
            return false;
        }
        return this.fromBlock.test(level, input.fromBlock.defaultBlockState(), null);
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
        return "mineral_fountain";
    }

    public record Input(Block needBlock, Block fromBlock) implements RecipeInput {
        @Override
        public ItemStack getItem(int i) {
            return new ItemStack(this.needBlock);
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
    public static class Builder extends AbstractRecipeBuilder<MineralFountainRecipe> {
        private BlockStatePredicate needBlock;
        private BlockStatePredicate fromBlock;
        private ChanceBlockState toBlock;

        public Builder needBlock(Block needBlock) {
            this.needBlock = BlockStatePredicate.builder().of(needBlock).build();
            return this;
        }

        public Builder needBlock(HolderGetter<Block> blocks, TagKey<Block> needBlock) {
            this.needBlock = BlockStatePredicate.builder().of(blocks, needBlock).build();
            return this;
        }

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
         * 添加结果方块（默认概率为1.0F）
         *
         * @param result 结果方块
         *
         * @return 构建器实例
         */
        public Builder toBlock(Block result) {
            return this.toBlock(new ChanceBlockState(result.defaultBlockState(), 1.0F));
        }

        @Override
        public MineralFountainRecipe buildRecipe() {
            return new MineralFountainRecipe(this.needBlock, this.fromBlock, this.toBlock);
        }

        @Override
        public void validate(Identifier id) {
            if (this.needBlock == null) {
                throw new IllegalArgumentException("needBlock must not be null, RecipeId: " + id);
            }
            if (this.fromBlock == null) {
                throw new IllegalArgumentException("fromBlock must not be null, RecipeId: " + id);
            }
            if (this.toBlock == null) {
                throw new IllegalArgumentException("toBlock must not be null, RecipeId: " + id);
            }
        }

        @Override
        public String getType() {
            return "mineral_fountain";
        }

        @Override
        public ItemStackTemplate getResult() {
            return new ItemStackTemplate(this.toBlock.state().getBlock().asItem());
        }
    }
}
