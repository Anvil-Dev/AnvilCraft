package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

public class FluidReactionCategory extends AbstractLiquidReactionCategory {
    public FluidReactionCategory(IGuiHelper helper) {
        super(helper, helper.createDrawableItemStack(ModBlocks.LARGE_CAULDRON.asStack()));
    }

    @Override
    public RecipeType<RecipeHolder<FluidMixingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.FLUID_REACTION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.anvilcraft.category.fluid_mixing");
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<FluidMixingRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        setFluidMixingRecipe(builder, recipeHolder.value());
    }

    @Override
    public void draw(
        RecipeHolder<FluidMixingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        drawBigCauldron(recipeHolder, recipeSlotsView, guiGraphics, mouseX, mouseY);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.FLUID_REACTION,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.FLUID_MIXING_TYPE.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.LARGE_CAULDRON.asStack(), AnvilCraftJeiPlugin.FLUID_REACTION);
        registration.addRecipeCatalyst(ModBlocks.GIANT_ANVIL.asStack(), AnvilCraftJeiPlugin.FLUID_REACTION);
    }
}
