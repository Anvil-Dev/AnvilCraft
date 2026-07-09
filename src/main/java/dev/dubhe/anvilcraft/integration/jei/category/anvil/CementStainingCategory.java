package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import mezz.jei.api.gui.ITickTimer;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CementStainingCategory implements IRecipeCategory<RecipeHolder<BulgingRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final Component title;
    private final ITickTimer anvilTimer;
    private final ITickTimer colorTimer;

    private final IDrawable arrowIn;
    private final IDrawable arrowOut;

    public CementStainingCategory(IGuiHelper helper) {
        icon = new DrawableBlockStateIcon(
            Blocks.ANVIL.defaultBlockState(),
            ModBlocks.CEMENT_CAULDRONS.get(Color.PINK).getDefaultState());
        slotDefault = JeiRenderHelper.getSlotDefault(helper);
        title = Component.translatable("gui.anvilcraft.category.cement_staining");
        anvilTimer = helper.createTickTimer(30, 60, true);
        colorTimer = helper.createTickTimer(20 * Color.values().length, Color.values().length - 1, false);

        arrowIn = JeiRenderHelper.getArrowInput(helper);
        arrowOut = JeiRenderHelper.getArrowOutput(helper);
    }

    @Override
    public RecipeType<RecipeHolder<BulgingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.CEMENT_STAINING_BULGING;
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
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BulgingRecipe> recipeHolder, IFocusGroup focuses) {
        BulgingRecipe recipe = recipeHolder.value();
        JeiSlotUtil.addInputSlots(builder, recipe.getInputItems());
    }

    @Override
    public void draw(
        RecipeHolder<BulgingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(anvilTimer);
        Color color = Color.getColorByIndex(colorTimer.getValue());
        RenderSupport.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            22 + anvilYOffset,
            20,
            12,
            RenderSupport.SINGLE_BLOCK);
        RenderSupport.renderBlock(
            guiGraphics,
            ModBlocks.CEMENT_CAULDRONS.get(color).getDefaultState(),
            81,
            40,
            10,
            12,
            RenderSupport.SINGLE_BLOCK);
        arrowIn.draw(guiGraphics, 54, 30);
        arrowOut.draw(guiGraphics, 92, 29);
        BulgingRecipe recipe = recipeHolder.value();
        JeiSlotUtil.drawInputSlots(guiGraphics, slotDefault, recipe.getInputItems().size());

        RenderSupport.renderBlock(
            guiGraphics,
            recipe.getHasCauldron().getTransformCauldron().defaultBlockState(),
            133,
            30,
            0,
            12,
            RenderSupport.SINGLE_BLOCK);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<BulgingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY) {
        if (mouseX >= 72 && mouseX <= 90) {
            if (mouseY >= 34 && mouseY <= 53) {
                Color color = Color.getColorByIndex(colorTimer.getValue());
                tooltip.add(ModBlocks.CEMENT_CAULDRONS.get(color).get().getName());
            }
        }
        if (mouseX >= 124 && mouseX <= 140) {
            if (mouseY >= 24 && mouseY <= 42) {
                tooltip.add(recipeHolder.value().getHasCauldron().getTransformCauldron().getName());
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<BulgingRecipe>> holders = JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.BULGING_TYPE.get());
        holders.removeIf(holder -> !holder.id().getPath().startsWith("cement_staining/"));
        registration.addRecipes(AnvilCraftJeiPlugin.CEMENT_STAINING_BULGING, holders);
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.CEMENT_STAINING_BULGING);
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), AnvilCraftJeiPlugin.CEMENT_STAINING_BULGING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FISH_TANK), AnvilCraftJeiPlugin.CEMENT_STAINING_BULGING);
    }
}
