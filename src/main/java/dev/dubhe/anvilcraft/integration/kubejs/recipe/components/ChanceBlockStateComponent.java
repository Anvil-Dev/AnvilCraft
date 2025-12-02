package dev.dubhe.anvilcraft.integration.kubejs.recipe.components;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.recipe.component.ChanceBlockState;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.rhino.type.TypeInfo;

public record ChanceBlockStateComponent() implements RecipeComponent<ChanceBlockState> {
    public static final ChanceBlockStateComponent INSTANCE = new ChanceBlockStateComponent();
    public static final RecipeComponentType<ChanceBlockState> TYPE = RecipeComponentType.unit(
        AnvilCraft.of("chance_block_state"),
        INSTANCE
    );

    @Override
    public RecipeComponentType<?> type() {
        return TYPE;
    }

    @Override
    public Codec<ChanceBlockState> codec() {
        return ChanceBlockState.CODEC.codec();
    }

    @Override
    public TypeInfo typeInfo() {
        return TypeInfo.of(ChanceBlockState.class);
    }

    @Override
    public String toString() {
        return "chance_block_state";
    }
}
