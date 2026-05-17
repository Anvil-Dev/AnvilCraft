package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
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
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ItemInjectCategory implements IRecipeCategory<RecipeHolder<ItemInjectRecipe>> {
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
    public void setRecipe(
        IRecipeLayoutBuilder builder, RecipeHolder<ItemInjectRecipe> recipeHolder, IFocusGroup focuses) {
        ItemInjectRecipe recipe = recipeHolder.value();
        JeiSlotUtil.addInputSlots(builder, recipe.getInputItems());
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).add(Ingredient.of(
            recipe.getFirstInputBlock().getBlocks().stream().map(Holder::value)
        ));
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).add(new ItemStack(recipe.getFirstResultBlock().state().getBlock()));
    }

    @Override
    public void draw(
        RecipeHolder<ItemInjectRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor guiGraphics,
        double mouseX,
        double mouseY) {
        ItemInjectRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            22 + anvilYOffset,
            12);

        List<BlockState> input = recipe.getFirstInputBlock().constructStatesForRender();
        if (input.isEmpty()) return;
        BlockState renderedState = input.get((int) ((System.currentTimeMillis() / 1000) % input.size()));
        if (renderedState == null) return;
        RenderSupport.renderBlock(guiGraphics, renderedState, 81, 40, 12);

        this.arrowIn.draw(guiGraphics, 54, 30);
        this.arrowOut.draw(guiGraphics, 92, 29);

        JeiSlotUtil.drawInputSlots(guiGraphics, this.slotDefault, recipe.getInputItems().size());
        RenderSupport.renderBlock(
            guiGraphics, recipe.getFirstResultBlock().state(), 133, 30, 12);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<ItemInjectRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY) {
        ItemInjectRecipe recipe = recipeHolder.value();
        Identifier id = this.getIdentifier(recipeHolder);
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
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.ITEM_INJECT.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.ITEM_INJECT);
    }
}
