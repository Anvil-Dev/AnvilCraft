package dev.dubhe.anvilcraft.integration.kubejs.recipe.components;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.recipe.component.BlockStatePredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.rhino.type.TypeInfo;

public record BlockStatePredicateComponent() implements RecipeComponent<BlockStatePredicate> {
    public static final BlockStatePredicateComponent INSTANCE = new BlockStatePredicateComponent();
    public static final RecipeComponentType<BlockStatePredicate> TYPE = RecipeComponentType.unit(
        AnvilCraft.of("block_state_predicate"),
        INSTANCE
    );

    @Override
    public RecipeComponentType<?> type() {
        return TYPE;
    }

    @Override
    public Codec<BlockStatePredicate> codec() {
        return BlockStatePredicate.CODEC;
    }

    @Override
    public TypeInfo typeInfo() {
        return TypeInfo.of(BlockStatePredicate.class);
    }

    @Override
    public String toString() {
        return "block_state_predicate";
    }
}
