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
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;

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
        this.icon = new DrawableBlockStateIcon(
            Blocks.ANVIL.defaultBlockState(),
            ModBlocks.CEMENT_CAULDRONS.get(Color.PINK).getDefaultState());
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.title = Component.translatable("gui.anvilcraft.category.cement_staining");
        this.anvilTimer = helper.createTickTimer(30, 60, true);
        this.colorTimer = helper.createTickTimer(20 * Color.values().length, Color.values().length - 1, false);

        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOut = JeiRenderHelper.getArrowOutput(helper);
    }

    @Override
    public IRecipeHolderType<BulgingRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.CEMENT_STAINING_BULGING;
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BulgingRecipe> recipeHolder, IFocusGroup focuses) {
        JeiSlotUtil.addInputSlots(builder, recipeHolder.value().getInputItems());
    }

    @Override
    public void draw(
        RecipeHolder<BulgingRecipe> recipeHolder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        final BulgingRecipe recipe = recipeHolder.value();
        this.arrowIn.draw(graphics, 54, 30);
        this.arrowOut.draw(graphics, 91, 29);

        Color color = Color.getColorByIndex(this.colorTimer.getValue());
        RenderSupport.renderBlock(
            graphics,
            ModBlocks.CEMENT_CAULDRONS.get(color).getDefaultState(),
            71,
            35,
            20
        );
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.anvilTimer);
        RenderSupport.renderBlock(
            graphics,
            Blocks.ANVIL.defaultBlockState(),
            71,
            17 + anvilYOffset,
            20
        );

        JeiSlotUtil.drawInputSlots(graphics, this.slotDefault, recipe.getInputItems().size());

        RenderSupport.renderBlock(
            graphics,
            recipe.getHasCauldron().getTransformCauldron().defaultBlockState(),
            122,
            25,
            20
        );
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<BulgingRecipe> recipeHolder,
        IRecipeSlotsView view,
        double mouseX,
        double mouseY) {
        if (mouseX >= 72 && mouseX <= 90) {
            if (mouseY >= 34 && mouseY <= 53) {
                Color color = Color.getColorByIndex(this.colorTimer.getValue());
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
        List<RecipeHolder<BulgingRecipe>> holders = JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.BULGING.get());
        holders.removeIf(holder -> !holder.id().identifier().getPath().startsWith("cement_staining/"));
        registration.addRecipes(AnvilCraftJeiPlugin.CEMENT_STAINING_BULGING, holders);
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.CEMENT_STAINING_BULGING);
        AnvilCraftJeiPlugin.addCauldronCatalysts(registration, AnvilCraftJeiPlugin.CEMENT_STAINING_BULGING);
    }
}
