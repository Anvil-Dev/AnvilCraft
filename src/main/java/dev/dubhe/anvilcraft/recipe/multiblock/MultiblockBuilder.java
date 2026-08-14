package dev.dubhe.anvilcraft.recipe.multiblock;

import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import java.util.List;

@Setter
@Accessors(fluent = true, chain = true)
public class MultiblockBuilder extends AbstractRecipeBuilder<MultiblockRecipe> {

    private final MultiblockDefinition.SeriaBuilder definition = MultiblockDefinition.seriaBuilder();
    private ItemStack result;

    public MultiblockBuilder() {
    }

    public MultiblockBuilder(ItemLike item, int count) {
        this.result = new ItemStack(item, count);
    }

    public MultiblockBuilder layer(String... layers) {
        this.definition.layer(layers);
        return this;
    }

    public MultiblockBuilder layer(List<String> layers) {
        this.definition.layer(layers.toArray(String[]::new));
        return this;
    }

    public MultiblockBuilder symbol(char symbol, BlockPredicateWithState predicate) {
        this.definition.map(symbol, MultiblockUtil.toBlockStatePredicateBuilder(predicate));
        return this;
    }

    public MultiblockBuilder symbol(char symbol, BlockStatePredicate.Builder predicate) {
        this.definition.map(symbol, predicate);
        return this;
    }

    public MultiblockBuilder symbol(char symbol, Block block) {
        this.definition.map(symbol, block);
        return this;
    }

    public MultiblockBuilder symbol(char symbol, Holder<Block> block) {
        return this.symbol(symbol, block.value());
    }

    public MultiblockBuilder symbol(char symbol, String block) {
        return this.symbol(symbol, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(block)));
    }

    public MultiblockBuilder symbol(char symbol, TagKey<Block> tag) {
        return this.symbol(symbol, BlockStatePredicate.builder().of(tag));
    }

    @Override
    public MultiblockRecipe buildRecipe() {
        return new MultiblockRecipe(this.definition.build(), this.result);
    }

    @Override
    public void validate(ResourceLocation id) {
        if (this.result == null) {
            throw new IllegalArgumentException("Recipe result must not be null, Recipe: " + id);
        }
        if (this.definition.build().definition().isEmpty()) {
            throw new IllegalArgumentException("Recipe definition must not be empty: " + id);
        }
    }

    @Override
    public String getType() {
        return "multiblock";
    }

    @Override
    public Item getResult() {
        return this.result.getItem();
    }
}
