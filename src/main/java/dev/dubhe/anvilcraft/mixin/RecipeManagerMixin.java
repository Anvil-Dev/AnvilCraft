package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessStepManager;
import dev.dubhe.anvilcraft.recipe.generate.JewelCraftingRecipeGeneratingCache;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashSet;

@Mixin(RecipeManager.class)
abstract class RecipeManagerMixin {
    @Shadow
    @Final
    private HolderLookup.Provider registries;

    @Shadow
    private RecipeMap recipes;

    @Inject(method = "finalizeRecipeLoading", at = @At("HEAD"))
    private void appendJewelRecipes(FeatureFlagSet flags, CallbackInfo ci) {
        var values = new ArrayList<RecipeHolder<?>>(this.recipes.values());
        var ids = new HashSet<>(values.stream().map(RecipeHolder::id).toList());
        new JewelCraftingRecipeGeneratingCache(this.registries, values).buildRecipes().ifPresent(generated -> {
            for (var recipe : generated) {
                if (ids.add(recipe.id())) values.add(recipe);
            }
        });
        this.recipes = RecipeMap.create(values);
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
        ProceduralProcessStepManager.initialize(recipes);
    }
}
