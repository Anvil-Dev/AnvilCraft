package dev.dubhe.anvilcraft.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractRecipeBookScreen.class)
public interface PocketRecipeScreenAccessor {
    @Accessor("widthTooNarrow")
    boolean anvilcraft$isWidthTooNarrow();

    @Accessor("recipeBookComponent")
    RecipeBookComponent<?> anvilcraft$getRecipeBookComponent();
}
