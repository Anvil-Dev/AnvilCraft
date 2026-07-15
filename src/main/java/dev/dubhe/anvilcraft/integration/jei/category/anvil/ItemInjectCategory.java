package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.MathUtil;
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
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
        this.icon = helper.createDrawableItemStack(new ItemStack(Items.ANVIL));
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.title = Component.translatable("gui.anvilcraft.category.item_inject");
        this.timer = helper.createTickTimer(30, 60, true);

        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOut = JeiRenderHelper.getArrowOutput(helper);
    }

    @Override
    public IRecipeHolderType<ItemInjectRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.ITEM_INJECT;
    }

    @Override
    public Component getTitle() {
        return this.title;
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
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ItemInjectRecipe> recipeHolder, IFocusGroup focuses) {
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
        IRecipeExtrasBuilder builder, RecipeHolder<ItemInjectRecipe> recipeHolder, IFocusGroup focuses
    ) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void draw(
        RecipeHolder<ItemInjectRecipe> recipeHolder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        ItemInjectRecipe recipe = recipeHolder.value();
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 81, 22 + anvilYOffset, 20);

        List<BlockState> input = recipe.getFirstInputBlock().constructStatesForRender();
        if (input.isEmpty()) return;
        BlockState renderedState = JeiBlockIngredientUtil.getDisplayedState(view, INPUT_BLOCK, input)
            .orElse(input.getFirst());
        int inputScale = JeiBlockIngredientUtil.getRenderablePreviewScale(renderedState, 20);
        RenderSupport.renderBlock(graphics, renderedState, 81, 40, inputScale);

        this.arrowIn.draw(graphics, 54, 30);
        this.arrowOut.draw(graphics, 92, 29);

        JeiSlotUtil.drawInputSlots(graphics, this.slotDefault, recipe.getInputItems().size());
        BlockState resultState = JeiBlockIngredientUtil.getRenderablePreviewState(recipe.getFirstResultBlock().state());
        int resultScale = JeiBlockIngredientUtil.getRenderablePreviewScale(resultState, 20);
        RenderSupport.renderBlock(graphics, resultState, 133, 30, resultScale);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<ItemInjectRecipe> recipeHolder,
        IRecipeSlotsView view,
        double mouseX,
        double mouseY
    ) {
        ItemInjectRecipe recipe = recipeHolder.value();
        Identifier id = this.getIdentifier(recipeHolder);
        if (MathUtil.isInRange(mouseX, mouseY, 72, 34, 90, 53)) {
            tooltip.addAll(TooltipUtil.tooltip(recipe.getFirstInputBlock().constructStatesForRender().getFirst().getBlock()));
        }
        if (MathUtil.isInRange(mouseX, mouseY, 124, 24, 140, 42)) {
            Block block = recipe.getFirstResultBlock().state().getBlock();
            if (id != null) {
                tooltip.addAll(TooltipUtil.recipeIDTooltip(block, id));
            } else {
                tooltip.addAll(TooltipUtil.tooltip(block));
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.ITEM_INJECT,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.ITEM_INJECT.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.ITEM_INJECT);
    }
}
