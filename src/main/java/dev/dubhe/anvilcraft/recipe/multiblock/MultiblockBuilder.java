package dev.dubhe.anvilcraft.recipe.multiblock;

import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.ParametersAreNonnullByDefault;

@Setter
@Accessors(fluent = true, chain = true)
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultiblockBuilder extends AbstractRecipeBuilder<MultiblockRecipe> {

    private BlockPattern pattern = BlockPattern.create();
    private ItemStack result;

    public MultiblockBuilder() {
    }

    public MultiblockBuilder(ItemLike item, int count) {
        this.result = new ItemStack(item, count);
    }

    public MultiblockBuilder layer(String... layers) {
        pattern.layer(layers);
        return this;
    }

    public MultiblockBuilder symbol(char symbol, BlockPredicateWithState predicate) {
        pattern.symbol(symbol, predicate);
        return this;
    }

    public MultiblockBuilder symbol(char symbol, Block block) {
        return symbol(symbol, BlockPredicateWithState.of(block));
    }

    public MultiblockBuilder symbol(char symbol, Holder<Block> block) {
        return symbol(symbol, block.value());
    }

    public MultiblockBuilder symbol(char symbol, String block) {
        return symbol(symbol, BlockPredicateWithState.of(block));
    }

    @Override
    public MultiblockRecipe buildRecipe() {
        return new MultiblockRecipe(pattern, result);
    }

    @Override
    public void validate(ResourceLocation pId) {
        if (result == null) {
            throw new IllegalArgumentException("Recipe result must not be null, Recipe: " + pId);
        }
        if (!pattern.checkSymbols()) {
            throw new IllegalArgumentException("Recipe pattern must contain all valid symbols: " + pId);
        }
    }

    @Override
    public String getType() {
        return "multiblock";
    }

    @Override
    public Item getResult() {
        return result.getItem();
    }
}
