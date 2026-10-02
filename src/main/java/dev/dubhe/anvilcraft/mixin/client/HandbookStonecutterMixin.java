package dev.dubhe.anvilcraft.mixin.client;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.recipe.MDStonecutterRecipeComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MDStonecutterRecipeComponent.class, remap = false)
abstract class HandbookStonecutterMixin {
    @Shadow @Final @Mutable private @Nullable Ingredient ingredient;
    @Shadow @Final @Mutable private @Nullable ItemStack resultItem;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void anvilcraft$readRecipe(StonecutterRecipe recipe, boolean center, CallbackInfo ci) {
        if (Minecraft.getInstance().level == null) return;
        this.ingredient = recipe.input();
        this.resultItem = recipe.result.create();
    }
}
