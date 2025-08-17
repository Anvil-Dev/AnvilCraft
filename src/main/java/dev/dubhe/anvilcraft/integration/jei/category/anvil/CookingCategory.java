package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.CookingRecipe;
import dev.dubhe.anvilcraft.util.RenderHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CookingCategory extends AbstractProgressCategory<CookingRecipe> {
    public CookingCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(Blocks.CAULDRON.defaultBlockState(),
                Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)),
            Component.translatable("gui.anvilcraft.category.cooking")
        );
    }

    @Override
    public RecipeType<RecipeHolder<CookingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.COOKING;
    }

    @Override
    public void draw(
        RecipeHolder<CookingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        CookingRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderHelper.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            12 + anvilYOffset,
            20,
            12,
            RenderHelper.SINGLE_BLOCK);
        RenderHelper.renderBlock(
            guiGraphics, Blocks.CAULDRON.defaultBlockState(), 81, 30, 10, 12, RenderHelper.SINGLE_BLOCK);
        RenderHelper.renderBlock(
            guiGraphics,
            Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true),
            81,
            40,
            0,
            12,
            RenderHelper.SINGLE_BLOCK);

        arrowIn.draw(guiGraphics, 54, 32);
        arrowOut.draw(guiGraphics, 92, 31);

        JeiSlotUtil.drawInputSlots(guiGraphics, slot, recipe.getInputItems().size());
        JeiSlotUtil.drawOutputSlots(guiGraphics, slot, this.getResults(recipe).size());
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.COOKING, JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.COOKING_TYPE.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilCraftJeiPlugin.COOKING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROYAL_ANVIL), AnvilCraftJeiPlugin.COOKING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EMBER_ANVIL), AnvilCraftJeiPlugin.COOKING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GIANT_ANVIL), AnvilCraftJeiPlugin.COOKING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SPECTRAL_ANVIL), AnvilCraftJeiPlugin.COOKING);
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), AnvilCraftJeiPlugin.COOKING);
        registration.addRecipeCatalyst(new ItemStack(Items.CAMPFIRE), AnvilCraftJeiPlugin.COOKING);
    }
}
