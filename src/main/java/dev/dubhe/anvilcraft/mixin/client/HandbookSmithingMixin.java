package dev.dubhe.anvilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.recipe.MDSmithingTableRecipeComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MDSmithingTableRecipeComponent.class, remap = false)
abstract class HandbookSmithingMixin {
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/core/NonNullList;create()Lnet/minecraft/core/NonNullList;"))
    private static NonNullList<Ingredient> anvilcraft$allocateSlots(
        Operation<NonNullList<Ingredient>> original, SmithingRecipe recipe, boolean enableAlignCenter
    ) {
        return NonNullList.withSize(3, recipe.baseIngredient());
    }
}
