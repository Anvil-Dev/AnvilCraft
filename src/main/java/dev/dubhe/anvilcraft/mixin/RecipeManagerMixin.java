package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessStepManager;
import dev.dubhe.anvilcraft.recipe.generate.JewelCraftingRecipeGeneratingCache;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(RecipeManager.class)
abstract class RecipeManagerMixin {
    @Shadow
    @Final
    private HolderLookup.Provider registries;

    @Inject(
        method = "prepare("
                 + "Lnet/minecraft/server/packs/resources/ResourceManager;"
                 + "Lnet/minecraft/util/profiling/ProfilerFiller;)"
                 + "Lnet/minecraft/world/item/crafting/RecipeMap;",
        at = @At(value = "INVOKE", target = "Ljava/util/SortedMap;forEach(Ljava/util/function/BiConsumer;)V")
    )
    private void beforeBuildRecipe(
        ResourceManager manager,
        ProfilerFiller profiler,
        CallbackInfoReturnable<RecipeMap> cir,
        @Local(name = "recipeHolders") List<RecipeHolder<?>> recipeHolders
    ) {
        new JewelCraftingRecipeGeneratingCache(this.registries)
            .buildRecipes()
            .ifPresent(recipeHolders::addAll);
    }

    @Inject(
        method = "apply("
                 + "Lnet/minecraft/world/item/crafting/RecipeMap;"
                 + "Lnet/minecraft/server/packs/resources/ResourceManager;"
                 + "Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At("TAIL")
    )
    private void afterApplyRecipe(
        RecipeMap recipes,
        ResourceManager manager,
        ProfilerFiller profiler,
        CallbackInfo ci
    ) {
        ProceduralProcessStepManager.initialize(List.copyOf(recipes.values()));
    }
}
