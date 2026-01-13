package dev.dubhe.anvilcraft.recipe.mineral;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.recipe.component.ChanceBlockState;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
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

@Getter
public class MineralFountainRecipe implements Recipe<MineralFountainRecipe.Input> {
    private final BlockStatePredicate needBlock;
    private final BlockStatePredicate fromBlock;
    private final ChanceBlockState toBlock;

    public MineralFountainRecipe(BlockStatePredicate needBlock, BlockStatePredicate fromBlock, ChanceBlockState toBlock) {
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
        return this.toBlock.state().getBlock().asItem() == Items.AIR
               ? ItemStack.EMPTY
               : new ItemStack(this.needBlock.getStatesCache().getFirst().getBlock());
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
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

    public static class Serializer implements RecipeSerializer<MineralFountainRecipe> {

        private static final MapCodec<MineralFountainRecipe> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            BlockStatePredicate.CODEC
                .fieldOf("need_block")
                .forGetter(MineralFountainRecipe::getNeedBlock),
            BlockStatePredicate.CODEC
                .fieldOf("from_block")
                .forGetter(MineralFountainRecipe::getFromBlock),
            ChanceBlockState.CODEC
                .fieldOf("to_block")
                .forGetter(MineralFountainRecipe::getToBlock)
        ).apply(ins, MineralFountainRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, MineralFountainRecipe> STREAM_CODEC =
            StreamCodec.composite(
                BlockStatePredicate.STREAM_CODEC,
                MineralFountainRecipe::getNeedBlock,
                BlockStatePredicate.STREAM_CODEC,
                MineralFountainRecipe::getFromBlock,
                ChanceBlockState.STREAM_CODEC,
                MineralFountainRecipe::getToBlock,
                MineralFountainRecipe::new
            );

        @Override
        public MapCodec<MineralFountainRecipe> codec() {
            return Serializer.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MineralFountainRecipe> streamCodec() {
            return Serializer.STREAM_CODEC;
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

        public Builder needBlock(TagKey<Block> needBlock) {
            this.needBlock = BlockStatePredicate.builder().of(needBlock).build();
            return this;
        }

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
         * 添加结果方块（默认概率为1.0f）
         *
         * @param result 结果方块
         * @return 构建器实例
         */
        public Builder toBlock(Block result) {
            return this.toBlock(new ChanceBlockState(result.defaultBlockState(), 1.0f));
        }

        @Override
        public MineralFountainRecipe buildRecipe() {
            return new MineralFountainRecipe(this.needBlock, this.fromBlock, this.toBlock);
        }

        @Override
        public void validate(ResourceLocation id) {
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
        public Item getResult() {
            return this.toBlock.state().getBlock().asItem();
        }
    }
}
