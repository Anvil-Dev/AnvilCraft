package dev.dubhe.anvilcraft.recipe.multiblock;

import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@Setter
@Accessors(fluent = true, chain = true)
public class MultiblockBuilder extends AbstractRecipeBuilder<MultiblockRecipe> {

    private final MultiblockDefinition.SeriaBuilder definition = MultiblockDefinition.seriaBuilder();
    private @Nullable ItemStackTemplate result;

    public MultiblockBuilder() {
    }

    public MultiblockBuilder(ItemLike item, int count) {
        this.result = new ItemStackTemplate(item.asItem(), count);
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
        return this.symbol(symbol, MultiblockUtil.fromLegacy(predicate));
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
        return this.symbol(symbol, BuiltInRegistries.BLOCK.getValue(Identifier.parse(block)));
    }

    public MultiblockBuilder symbol(char symbol, TagKey<Block> tag) {
        return this.symbol(symbol, BlockStatePredicate.builder().of(MultiblockUtil.BLOCK_LOOKUP, tag));
    }

    @Override
    public MultiblockRecipe buildRecipe() {
        return new MultiblockRecipe(this.definition.build(), Objects.requireNonNull(this.result));
    }

    @Override
    public void validate(Identifier id) {
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
    public ItemStackTemplate getResult() {
        return Objects.requireNonNull(this.result);
    }
}
