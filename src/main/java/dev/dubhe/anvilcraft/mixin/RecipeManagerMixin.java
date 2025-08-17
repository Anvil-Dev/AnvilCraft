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
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeManager;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.MeshRecipe;
import dev.dubhe.anvilcraft.recipe.generate.JewelCraftingRecipeGeneratingCache;
import dev.dubhe.anvilcraft.recipe.generate.MeshRecipeGeneratingCache;
import dev.dubhe.anvilcraft.recipe.generate.RecipeGenerator;
import dev.dubhe.anvilcraft.util.injection.IRecipeManager;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(RecipeManager.class)
abstract class RecipeManagerMixin implements IRecipeManager {
    @Shadow
    @Final
    private HolderLookup.Provider registries;

    @Shadow
    private Map<ResourceLocation, RecipeHolder<?>> byName;

    @Shadow
    private Multimap<RecipeType<?>, RecipeHolder<?>> byType;
    @Unique
    private InWorldRecipeManager anc$inWorldRecipeManager = null;

    @Inject(
        method = "lambda$apply$0",
        at = @At(
            value = "INVOKE",
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
        @Share("jewelsCache") LocalRef<JewelCraftingRecipeGeneratingCache> jewelsCache,
        @Share("meshesCache") LocalRef<MeshRecipeGeneratingCache> meshesCache
    ) {
        JewelCraftingRecipeGeneratingCache jewelsCache1 = new JewelCraftingRecipeGeneratingCache(this.registries);
        jewelsCache.set(jewelsCache1);

        MeshRecipeGeneratingCache meshesCache1 = new MeshRecipeGeneratingCache(this.registries);
        meshesCache.set(meshesCache1);
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
        @Share("jewelsCache") LocalRef<JewelCraftingRecipeGeneratingCache> jewelsCache,
        @Share("meshesCache") LocalRef<MeshRecipeGeneratingCache> meshesCache
    ) {
        jewelsCache.get().buildRecipes()
            .ifPresent(recipeHolders -> {
                byTypeBuilder.putAll(ModRecipeTypes.JEWEL_CRAFTING_TYPE.get(), recipeHolders);
                for (RecipeHolder<JewelCraftingRecipe> holder : recipeHolders) {
                    byNameBuilder.put(holder.id(), holder);
                }
            });
        meshesCache.get().buildRecipes()
            .ifPresent(recipeHolders -> {
                byTypeBuilder.putAll(ModRecipeTypes.MESH_TYPE.get(), recipeHolders);
                for (RecipeHolder<MeshRecipe> holder : recipeHolders) {
                    byNameBuilder.put(holder.id(), holder);
                }
            });
        this.byType = byTypeBuilder.build();
        this.byName = byNameBuilder.build();
    }

    @Override
    public void anc$setInWorldRecipeManager(InWorldRecipeManager manager) {
        this.anc$inWorldRecipeManager = manager;
    }

    @Override
    public InWorldRecipeManager anc$getInWorldRecipeManager() {
        return this.anc$inWorldRecipeManager;
    }

    @Override
    public HolderLookup.Provider anc$getRegistries() {
        return this.registries;
    }

    @Override
    public void anc$addRecipes(@NotNull List<RecipeHolder<InWorldRecipe>> recipes) {
        ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> byNameBuilder = ImmutableMap.builder();
        ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> byTypeBuilder = ImmutableMultimap.builder();
        Set<ResourceLocation> keys = new HashSet<>();
        this.byName.forEach((key, value) -> {
            if (key == null || value == null) return;
            if (keys.contains(key)) return;
            keys.add(key);
            byNameBuilder.put(key, value);
        });
        this.byType.forEach((key, value) -> {
            if (key == null || value == null) return;
            byTypeBuilder.put(key, value);
        });
        recipes.forEach(recipe -> {
            if (keys.contains(recipe.id())) return;
            keys.add(recipe.id());
            byNameBuilder.put(recipe.id(), recipe);
            byTypeBuilder.put(recipe.value().getType(), recipe);
        });
        this.byName = byNameBuilder.build();
        this.byType = byTypeBuilder.build();
    }
}
