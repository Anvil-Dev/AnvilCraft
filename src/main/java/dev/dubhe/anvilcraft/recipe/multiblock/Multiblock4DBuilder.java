package dev.dubhe.anvilcraft.recipe.multiblock;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractRecipeBuilder;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import javax.annotation.Nullable;

@Setter
@Accessors(fluent = true, chain = true)
public class Multiblock4DBuilder extends AbstractRecipeBuilder<Multiblock4DRecipe> {
    private final ImmutableList.Builder<MultiblockDefinition> definitions = ImmutableList.builder();
    private MultiblockDefinition.SeriaBuilder definition = MultiblockDefinition.seriaBuilder();
    private @Nullable ItemStack result;

    public Multiblock4DBuilder() {
    }

    public Multiblock4DBuilder(ItemLike item, int count) {
        this.result = new ItemStack(item, count);
    }

    public Multiblock4DBuilder layer(String... layers) {
        this.definition.layer(layers);
        return this;
    }

    public Multiblock4DBuilder symbol(char symbol, BlockStatePredicate.Builder predicate) {
        this.definition.map(symbol, predicate);
        return this;
    }

    public Multiblock4DBuilder symbol(char symbol, Block block) {
        this.definition.map(symbol, block);
        return this;
    }

    public Multiblock4DBuilder symbol(char symbol, Holder<Block> block) {
        return this.symbol(symbol, block.value());
    }

    public Multiblock4DBuilder symbol(char symbol, String block) {
        return this.symbol(symbol, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(block)));
    }

    public Multiblock4DBuilder symbol(char symbol, TagKey<Block> tag) {
        return this.symbol(symbol, BlockStatePredicate.builder().of(tag));
    }

    public Multiblock4DBuilder symbol(char symbol, Block... blocks) {
        return this.symbol(symbol, BlockStatePredicate.builder().of(blocks));
    }

    public Multiblock4DBuilder symbol(char symbol, BlockState block) {
        this.definition.map(symbol, block);
        return this;
    }

    public Multiblock4DBuilder symbol(char symbol, Block block, BlockState state) {
        this.definition.map(symbol, block, state);
        return this;
    }

    public Multiblock4DBuilder symbol(char symbol, Holder<Block> block, BlockState state) {
        return this.symbol(symbol, block.value(), state);
    }

    public Multiblock4DBuilder symbol(char symbol, String block, BlockState state) {
        return this.symbol(symbol, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(block)), state);
    }

    public Multiblock4DBuilder symbol(char symbol, TagKey<Block> tag, BlockState state) {
        return this.symbol(symbol, BlockStatePredicate.builder().of(tag).with(state));
    }

    public Multiblock4DBuilder symbol(char symbol, BlockState state, Block... blocks) {
        return this.symbol(symbol, BlockStatePredicate.builder().of(blocks).with(state));
    }

    public Multiblock4DBuilder symbol(char symbol, CompoundTag nbt) {
        this.definition.map(symbol, nbt);
        return this;
    }

    public Multiblock4DBuilder symbol(char symbol, Block block, CompoundTag nbt) {
        this.definition.map(symbol, block, nbt);
        return this;
    }

    public Multiblock4DBuilder symbol(char symbol, Holder<Block> block, CompoundTag nbt) {
        return this.symbol(symbol, block.value(), nbt);
    }

    public Multiblock4DBuilder symbol(char symbol, String block, CompoundTag nbt) {
        return this.symbol(symbol, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(block)), nbt);
    }

    public Multiblock4DBuilder symbol(char symbol, TagKey<Block> tag, CompoundTag nbt) {
        return this.symbol(symbol, BlockStatePredicate.builder().of(tag).nbt(nbt));
    }

    public Multiblock4DBuilder symbol(char symbol, CompoundTag nbt, Block... blocks) {
        return this.symbol(symbol, BlockStatePredicate.builder().of(blocks).nbt(nbt));
    }

    public Multiblock4DBuilder symbol(char symbol, BlockState block, CompoundTag nbt) {
        this.definition.map(symbol, block, nbt);
        return this;
    }

    public Multiblock4DBuilder symbol(char symbol, Block block, BlockState state, CompoundTag nbt) {
        this.definition.map(symbol, block, state, nbt);
        return this;
    }

    public Multiblock4DBuilder symbol(char symbol, Holder<Block> block, BlockState state, CompoundTag nbt) {
        return this.symbol(symbol, block.value(), state, nbt);
    }

    public Multiblock4DBuilder symbol(char symbol, String block, BlockState state, CompoundTag nbt) {
        return this.symbol(symbol, BuiltInRegistries.BLOCK.get(ResourceLocation.parse(block)), state, nbt);
    }

    public Multiblock4DBuilder symbol(char symbol, TagKey<Block> tag, BlockState state, CompoundTag nbt) {
        return this.symbol(symbol, BlockStatePredicate.builder().of(tag).with(state).nbt(nbt));
    }

    public Multiblock4DBuilder symbol(char symbol, BlockState state, CompoundTag nbt, Block... blocks) {
        return this.symbol(symbol, BlockStatePredicate.builder().of(blocks).with(state).nbt(nbt));
    }

    public Multiblock4DBuilder next() {
        this.definitions.add(this.definition.build());
        this.definition = MultiblockDefinition.seriaBuilder();
        return this;
    }

    @Override
    public Multiblock4DRecipe buildRecipe() {
        this.definitions.add(this.definition.build());
        return new Multiblock4DRecipe(this.definitions.build(), Objects.requireNonNull(this.result));
    }

    @Override
    public void validate(ResourceLocation id) {
        if (this.result == null) {
            throw new IllegalArgumentException("Recipe result must not be null, Recipe: " + id);
        }
        for (MultiblockDefinition pattern : this.definitions.build()) {
            if (pattern.definition().isEmpty()) {
                throw new IllegalArgumentException("Recipe definition must not be empty: " + id);
            }
        }
        if (this.definition.build().definition().isEmpty()) {
            throw new IllegalArgumentException("Recipe definition must not be empty: " + id);
        }
    }

    @Override
    public String getType() {
        return "4d_multiblock";
    }

    @Override
    public Item getResult() {
        return Objects.requireNonNull(this.result).getItem();
    }
}
