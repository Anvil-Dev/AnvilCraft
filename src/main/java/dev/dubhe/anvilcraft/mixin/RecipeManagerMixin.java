package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.recipe.generate.JewelCraftingRecipeGeneratingCache;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
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

    @Shadow
    public RecipeMap recipes;

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

    @Inject(method = "finalizeRecipeLoading", at = @At("RETURN"))
    private void sendRecipes2C(FeatureFlagSet enabledFlags, CallbackInfo ci) {
        RecipesRecord.RECIPES.syncFrom(this.recipes);
        RecipesRecord.sync2C(
            PacketDistributor::sendToAllPlayers,
            this.recipes.values(),
            ServerLifecycleHooks.getCurrentServer().registryAccess()
        );
    }
}
