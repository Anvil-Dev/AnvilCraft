package dev.dubhe.anvilcraft.integration.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.EndPortalConversionRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.integration.jei.util.TextureConstants;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

public class EndPortalConversionCategory implements IRecipeCategory<EndPortalConversionRecipe> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable slotDefault;
    private final IDrawable slotChoice;
    private final IDrawable preRenderedEndPortal;
    private final Component title;
    private final Component fallThroughTooltip;

    private final IDrawable arrowIn;
    private final IDrawable arrowOut;

    public EndPortalConversionCategory(IGuiHelper helper) {
        slotDefault = JeiRenderHelper.getSlotDefault(helper);
        slotChoice = JeiRenderHelper.getSlotChoice(helper);
        preRenderedEndPortal = helper.drawableBuilder(TextureConstants.PRE_RENDERED_END_PORTAL,
            0, 0, 400, 300).setTextureSize(400, 300).build();
        title = Component.translatable("gui.anvilcraft.category.end_portal_conversion");
        fallThroughTooltip = Component.translatable("gui.anvilcraft.category.end_portal_conversion.fall_through");

        arrowIn = JeiRenderHelper.getArrowInput(helper);
        arrowOut = JeiRenderHelper.getArrowOutput(helper);
    }

    @Override
    public RecipeType<EndPortalConversionRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.END_PORTAL_CONVERSION;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return null;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder, EndPortalConversionRecipe recipe, IFocusGroup focuses) {
        JeiSlotUtil.addInputSlots(builder, recipe.ingredients);
        JeiSlotUtil.addOutputSlots(builder, recipe.results);
    }

    @Override
    public void draw(
        EndPortalConversionRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(0.1f, 0.1f, 1.0f);
        preRenderedEndPortal.draw(guiGraphics, 600, 350);
        pose.popPose();
        arrowIn.draw(guiGraphics, 54, 30);
        arrowOut.draw(guiGraphics, 92, 29);

        JeiSlotUtil.drawInputSlots(guiGraphics, slotDefault, recipe.ingredients.size());
        JeiSlotUtil.drawOutputSlots(guiGraphics, slotChoice, recipe.results.size());
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        EndPortalConversionRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY) {
        if (mouseX >= 60 && mouseX <= 102) {
            if (mouseY >= 35 && mouseY <= 65) {
                tooltip.add(fallThroughTooltip);
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.END_PORTAL_CONVERSION,
            EndPortalConversionRecipe.getAllRecipes());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Blocks.END_PORTAL_FRAME), AnvilCraftJeiPlugin.END_PORTAL_CONVERSION);
    }
}
