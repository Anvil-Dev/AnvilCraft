package dev.dubhe.anvilcraft.mixin.client;

import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDComponent;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.client.markdown.recipe.MDVanillaCraftingComponent;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent$MDRecipeComponentProxy",
    remap = false)
abstract class HandbookRecipeMixin {
    @Shadow
    @Final
    private Identifier location;
    @Shadow
    @Final
    private MDComponent emptyComponent;
    @Shadow
    private @Nullable MDRecipeComponent component;
    @Unique
    private @Nullable RecipeMap anvilcraft$recipes;

    @Shadow
    public abstract <T extends Recipe<?>> boolean setComponent(RecipeHolder<?> holder);

    @Inject(method = "setComponent", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$crafting(RecipeHolder<?> holder, CallbackInfoReturnable<Boolean> cir) {
        if (Minecraft.getInstance().level != null && holder.value() instanceof CraftingRecipe recipe) {
            this.component = new MDVanillaCraftingComponent(recipe, ((MDRecipeComponent) (Object) this).isEnableAlignCenter());
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void anvilcraft$resolve(Minecraft minecraft) {
        RecipeMap recipes = minecraft.level == null ? null : RecipesRecord.CLIENTSIDE;
        if (recipes == this.anvilcraft$recipes) return;
        this.anvilcraft$recipes = recipes;
        this.component = null;
        if (recipes == null) return;
        RecipeHolder<?> holder = recipes.byKey(ResourceKey.create(Registries.RECIPE, this.location));
        if (holder != null) this.setComponent(holder);
    }

    @Inject(method = {"getPreferredWidth", "getHeight"}, at = @At("HEAD"))
    private void anvilcraft$resolveBeforeLayout(Minecraft minecraft, int maxX, int maxY, CallbackInfoReturnable<Integer> cir) {
        this.anvilcraft$resolve(minecraft);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$resolveBeforeRender(MDRenderContext context, CallbackInfo ci) {
        this.anvilcraft$resolve(context.minecraft());
        if (this.component == null) {
            this.emptyComponent.extractRenderState(context.child());
            ci.cancel();
        }
    }
}
