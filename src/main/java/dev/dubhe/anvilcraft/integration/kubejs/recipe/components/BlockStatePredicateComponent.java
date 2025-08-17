package dev.dubhe.anvilcraft.integration.kubejs.recipe.components;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.rhino.type.TypeInfo;
import org.jetbrains.annotations.NotNull;

public record BlockStatePredicateComponent() implements RecipeComponent<BlockStatePredicate> {
    public static final BlockStatePredicateComponent INSTANCE = new BlockStatePredicateComponent();

    @Override
    public Codec<BlockStatePredicate> codec() {
        return BlockStatePredicate.CODEC;
    }

    @Override
    public TypeInfo typeInfo() {
        return TypeInfo.of(BlockStatePredicate.class);
    }

    @Override
    public @NotNull String toString() {
        return "block_state_predicate";
    }
}
