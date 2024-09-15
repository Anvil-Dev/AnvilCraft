package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.recipe.GenerateRecipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.conditions.WithConditions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
    @Inject(
            method = "lambda$apply$0",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/google/common/collect/ImmutableMap$Builder;put(Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableMap$Builder;",
                            shift = At.Shift.AFTER))
    private static void onBuildRecipe(
            ResourceLocation resourceLocation,
            ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> byTypeBuilder,
            ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> byNameBuilder,
            WithConditions<Recipe<?>> r,
            CallbackInfo ci,
            @Local Recipe<?> recipe,
            @Local RecipeHolder<?> recipeHolder) {
        GenerateRecipe.handleVanillaRecipe(recipe.getType(), recipeHolder).ifPresent(v -> {
            byTypeBuilder.put(v.value().getType(), v);
            byNameBuilder.put(v.id(), v);
        });
    }
}
