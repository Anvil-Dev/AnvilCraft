package dev.dubhe.anvilcraft.mixin.client;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.recipe.MDFurnaceRecipeComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MDFurnaceRecipeComponent.class, remap = false)
abstract class HandbookFurnaceMixin {
    @Shadow @Final @Mutable private @Nullable Ingredient ingredient;
    @Shadow @Final @Mutable private @Nullable ItemStack resultItem;
    @Shadow @Final @Mutable private @Nullable ItemStack toastSymbol;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void anvilcraft$readRecipe(AbstractCookingRecipe recipe, boolean center, CallbackInfo ci) {
        if (Minecraft.getInstance().level == null) return;
        this.ingredient = recipe.input();
        this.resultItem = recipe.result.create();
        this.toastSymbol = new ItemStack(((HandbookCookingAccessor) recipe).anvilcraft$furnaceIcon());
    }
}
