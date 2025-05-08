package dev.dubhe.anvilcraft.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.RecipeGenerator;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.common.conditions.WithConditions;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Optional;

@Mixin(RecipeManager.class)
abstract class RecipeManagerMixin {

    @Shadow
    @Final
    private HolderLookup.Provider registries;

    @Shadow
    private Map<ResourceLocation, RecipeHolder<?>> byName;

    @Shadow
    private Multimap<RecipeType<?>, RecipeHolder<?>> byType;

    @Inject(
        method = "lambda$apply$0",
        at = @At(value = "INVOKE",
            target =
                "Lcom/google/common/collect/ImmutableMap$Builder;put(Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableMap$Builder;",
            shift = At.Shift.AFTER
        )
    )
    private static void onBuildRecipe(
        ResourceLocation resourceLocation,
        ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> byTypeBuilder,
        ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> byNameBuilder,
        WithConditions<Recipe<?>> r,
        CallbackInfo ci,
        @Local @NotNull Recipe<?> recipe,
        @Local RecipeHolder<?> recipeHolder
    ) {
        RecipeGenerator.handleVanillaRecipe(recipe.getType(), recipeHolder)
            .ifPresent(v -> {
                byTypeBuilder.put(v.value().getType(), v);
                byNameBuilder.put(v.id(), v);
            });
    }

    @Inject(
        method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;"
            + "Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;")
    )
    private void beforeBuildRecipe(
        Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci,
        @Share("jewelsCache") LocalRef<RecipeGenerator.JewelCraftingRecipeBuildingCache> jewelsCache
    ) {
        RecipeGenerator.JewelCraftingRecipeBuildingCache cache = new RecipeGenerator.JewelCraftingRecipeBuildingCache(this.registries);
        jewelsCache.set(cache);
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "CodeBlock2Expr"})
    @Inject(
        method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;"
            + "Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Optional;ifPresentOrElse(Ljava/util/function/Consumer;Ljava/lang/Runnable;)V"
        )
    )
    private void onBuildRecipe1(
        Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci,
        @Local(name = "resourcelocation") ResourceLocation resourceLocation,
        @Local(name = "decoded") Optional<WithConditions<Recipe<?>>> decoded,
        @Share("jewelsCache") LocalRef<RecipeGenerator.JewelCraftingRecipeBuildingCache> jewelsCache
    ) {
        decoded.ifPresent(recipeWithConditions -> {
            RecipeGenerator.handleJewelsRemove(
                jewelsCache.get(), new RecipeHolder<>(resourceLocation, recipeWithConditions.carrier())
            );
        });
    }

    @Inject(
        method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;"
            + "Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At(value = "INVOKE_ASSIGN", target = "Lcom/google/common/collect/ImmutableMap$Builder;build()"
            + "Lcom/google/common/collect/ImmutableMap;")
    )
    private void afterBuildRecipe(
        Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci,
        @Local ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> byTypeBuilder,
        @Local ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> byNameBuilder,
        @Share("jewelsCache") LocalRef<RecipeGenerator.JewelCraftingRecipeBuildingCache> jewelsCache
    ) {
        jewelsCache.get().buildRecipes()
            .ifPresent(recipeHolders -> {
                byTypeBuilder.putAll(ModRecipeTypes.JEWEL_CRAFTING_TYPE.get(), recipeHolders);
                for (RecipeHolder<JewelCraftingRecipe> holder : recipeHolders) {
                    byNameBuilder.put(holder.id(), holder);
                }
            });
        this.byType = byTypeBuilder.build();
        this.byName = byNameBuilder.build();
    }
}
