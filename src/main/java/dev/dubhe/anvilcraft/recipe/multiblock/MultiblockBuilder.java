package dev.dubhe.anvilcraft.recipe.multiblock;

import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

@Setter
@Accessors(fluent = true, chain = true)
public class MultiblockBuilder extends AbstractRecipeBuilder<MultiblockRecipe> {

    private BlockPattern pattern = BlockPattern.create();
    private ItemStackTemplate result;

    public MultiblockBuilder() {
    }

    public MultiblockBuilder(ItemLike item, int count) {
        this.result = new ItemStackTemplate(item.asItem(), count);
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
        return new MultiblockRecipe(this.pattern, this.result);
    }

    @Override
    public void validate(Identifier id) {
        if (result == null) {
            throw new IllegalArgumentException("Recipe result must not be null, Recipe: " + id);
        }
        if (!pattern.checkSymbols()) {
            throw new IllegalArgumentException("Recipe pattern must contain all valid symbols: " + id);
        }
    }

    @Override
    public String getType() {
        return "multiblock";
    }

    @Override
    public ItemStackTemplate getResult() {
        return this.result;
    }
}
