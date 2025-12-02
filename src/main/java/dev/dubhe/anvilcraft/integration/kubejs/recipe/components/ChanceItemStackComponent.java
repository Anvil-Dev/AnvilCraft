package dev.dubhe.anvilcraft.integration.kubejs.recipe.components;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.rhino.type.TypeInfo;

public record ChanceItemStackComponent() implements RecipeComponent<ChanceItemStack> {
    public static final ChanceItemStackComponent INSTANCE = new ChanceItemStackComponent();
    public static final RecipeComponentType<ChanceItemStack> TYPE = RecipeComponentType.unit(
        AnvilCraft.of("chance_item_stack"),
        INSTANCE
    );

    @Override
    public RecipeComponentType<?> type() {
        return TYPE;
    }

    @Override
    public Codec<ChanceItemStack> codec() {
        return ChanceItemStack.CODEC;
    }

    @Override
    public TypeInfo typeInfo() {
        return TypeInfo.of(ChanceItemStack.class);
    }

    @Override
    public String toString() {
        return "chance_item_stack";
    }
}
