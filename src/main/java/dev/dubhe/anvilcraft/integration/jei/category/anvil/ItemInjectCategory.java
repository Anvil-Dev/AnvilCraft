package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import dev.dubhe.anvilcraft.util.TooltipUtil;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ItemInjectCategory implements IRecipeCategory<RecipeHolder<ItemInjectRecipe>> {
    private static final String INPUT_BLOCK = "input_block";

    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final Component title;
    private final ITickTimer timer;

    private final IDrawable arrowIn;
    private final IDrawable arrowOut;

    public ItemInjectCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(new ItemStack(Items.ANVIL));
        slotDefault = JeiRenderHelper.getSlotDefault(helper);
        title = Component.translatable("gui.anvilcraft.category.item_inject");
        timer = helper.createTickTimer(30, 60, true);

        arrowIn = JeiRenderHelper.getArrowInput(helper);
        arrowOut = JeiRenderHelper.getArrowOutput(helper);
    }

    @Override
    public RecipeType<RecipeHolder<ItemInjectRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.ITEM_INJECT;
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
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder, RecipeHolder<ItemInjectRecipe> recipeHolder, IFocusGroup focuses) {
        ItemInjectRecipe recipe = recipeHolder.value();
        JeiSlotUtil.addInputSlots(builder, recipe.getInputItems());
        JeiBlockIngredientUtil.addInputSlot(builder, INPUT_BLOCK, 72, 34, 18, 19, recipe.getFirstInputBlock());
        JeiBlockIngredientUtil.addSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            "output_block",
            124,
            24,
            16,
            18,
            recipe.getFirstResultBlock().state().getBlock()
        );
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder, RecipeHolder<ItemInjectRecipe> recipeHolder, IFocusGroup focuses) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void draw(
        RecipeHolder<ItemInjectRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        ItemInjectRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderSupport.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            22 + anvilYOffset,
            20,
            12,
            RenderSupport.SINGLE_BLOCK);

        List<BlockState> input = recipe.getFirstInputBlock().constructStatesForRender();
        if (input.isEmpty()) return;
        BlockState renderedState = JeiBlockIngredientUtil.getDisplayedState(recipeSlotsView, INPUT_BLOCK, input)
            .orElse(input.getFirst());
        int inputScale = JeiBlockIngredientUtil.getRenderablePreviewScale(renderedState, 12);
        RenderSupport.renderBlock(guiGraphics, renderedState, 81, 40, 10, inputScale, RenderSupport.SINGLE_BLOCK);

        arrowIn.draw(guiGraphics, 54, 30);
        arrowOut.draw(guiGraphics, 92, 29);

        JeiSlotUtil.drawInputSlots(guiGraphics, slotDefault, recipe.getInputItems().size());
        BlockState resultState = JeiBlockIngredientUtil.getRenderablePreviewState(recipe.getFirstResultBlock().state());
        int resultScale = JeiBlockIngredientUtil.getRenderablePreviewScale(resultState, 12);
        RenderSupport.renderBlock(
            guiGraphics,
            resultState,
            133,
            30,
            0,
            resultScale,
            RenderSupport.SINGLE_BLOCK
        );
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<ItemInjectRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY) {
        ItemInjectRecipe recipe = recipeHolder.value();
        ResourceLocation id = getRegistryName(recipeHolder);
        if (mouseX >= 72 && mouseX <= 90) {
            if (mouseY >= 34 && mouseY <= 53) {
                tooltip.addAll(TooltipUtil.tooltip(recipe.getFirstInputBlock().constructStatesForRender().getFirst().getBlock()));
            }
        }
        if (mouseX >= 124 && mouseX <= 140) {
            if (mouseY >= 24 && mouseY <= 42) {
                Block block = recipe.getFirstResultBlock().state().getBlock();
                if (id != null) {
                    tooltip.addAll(TooltipUtil.recipeIDTooltip(block, id));
                } else {
                    tooltip.addAll(TooltipUtil.tooltip(block));
                }
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.ITEM_INJECT,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.ITEM_INJECT_TYPE.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.ITEM_INJECT);
    }
}
