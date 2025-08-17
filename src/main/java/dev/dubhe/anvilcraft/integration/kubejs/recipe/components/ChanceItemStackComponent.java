package dev.dubhe.anvilcraft.integration.kubejs.recipe.components;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.rhino.type.TypeInfo;
import org.jetbrains.annotations.NotNull;

public record ChanceItemStackComponent() implements RecipeComponent<ChanceItemStack> {
    public static final ChanceItemStackComponent INSTANCE = new ChanceItemStackComponent();

    @Override
    public Codec<ChanceItemStack> codec() {
        return ChanceItemStack.CODEC;
    }

    @Override
    public TypeInfo typeInfo() {
        return TypeInfo.of(ChanceItemStack.class);
    }

    @Override
    public @NotNull String toString() {
        return "chance_item_stack";
    }
}
