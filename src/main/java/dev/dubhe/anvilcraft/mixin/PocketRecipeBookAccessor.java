package dev.dubhe.anvilcraft.mixin;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeBookComponent.class)
public interface PocketRecipeBookAccessor {
    @Accessor("xOffset")
    int anvilcraft$getHorizontalOffset();
}
