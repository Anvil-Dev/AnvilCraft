package dev.dubhe.anvilcraft.integration.kubejs.recipe.components;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceBlockState;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.rhino.type.TypeInfo;
import org.jetbrains.annotations.NotNull;

public record ChanceBlockStateComponent() implements RecipeComponent<ChanceBlockState> {
    public static final ChanceBlockStateComponent INSTANCE = new ChanceBlockStateComponent();

    @Override
    public Codec<ChanceBlockState> codec() {
        return ChanceBlockState.CODEC.codec();
    }

    @Override
    public TypeInfo typeInfo() {
        return TypeInfo.of(ChanceBlockState.class);
    }

    @Override
    public @NotNull String toString() {
        return "chance_block_state";
    }
}
