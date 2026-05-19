package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.recipe.CementStainingRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import org.jspecify.annotations.Nullable;

public class CementStainingCategory implements IRecipeCategory<CementStainingRecipe> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final Component title;
    private final ITickTimer anvilTimer;
    private final ITickTimer colorTimer;

    private final IDrawable arrowIn;
    private final IDrawable arrowOutFromBelow;

    public CementStainingCategory(IGuiHelper helper) {
        this.icon = new DrawableBlockStateIcon(
            Blocks.ANVIL.defaultBlockState(),
            ModBlocks.CEMENT_CAULDRONS.get(Color.PINK).getDefaultState());
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.title = Component.translatable("gui.anvilcraft.category.cement_staining");
        this.anvilTimer = helper.createTickTimer(30, 60, true);
        this.colorTimer = helper.createTickTimer(20 * Color.values().length, Color.values().length - 1, false);

        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOutFromBelow = JeiRenderHelper.getArrowOutputFromBelow(helper);
    }

    @Override
    public IRecipeType<CementStainingRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.CEMENT_STAINING;
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
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CementStainingRecipe recipe, IFocusGroup focuses) {
        JeiSlotUtil.addInputSlots(builder, recipe.ingredients());

        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).add(ModFluids.SOURCE_CEMENTS.get(Color.GRAY).get());
        CauldronFluidContent result = CauldronFluidContent.getForBlock(recipe.resultBlock());
        if (result != null) builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).add(result.fluid);
    }

    @Override
    public void draw(
        CementStainingRecipe recipe,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.anvilTimer);
        Color color = Color.getColorByIndex(this.colorTimer.getValue());
        RenderSupport.renderBlock(
            graphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            22 + anvilYOffset,
            20
        );
        RenderSupport.renderBlock(
            graphics,
            ModBlocks.CEMENT_CAULDRONS.get(color).getDefaultState(),
            81,
            40,
            20);
        this.arrowIn.draw(graphics, 54, 30);
        this.arrowOutFromBelow.draw(graphics, 92, 29);

        JeiSlotUtil.drawInputSlots(graphics, this.slotDefault, recipe.ingredients().size());

        RenderSupport.renderBlock(graphics, recipe.resultBlock().defaultBlockState(), 133, 30, 20);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        CementStainingRecipe recipe,
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
                tooltip.add(recipe.resultBlock().getName());
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AnvilCraftJeiPlugin.CEMENT_STAINING, CementStainingRecipe.getAllRecipes());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.CEMENT_STAINING);
        AnvilCraftJeiPlugin.addCauldronCatalysts(registration, AnvilCraftJeiPlugin.CEMENT_STAINING);
    }
}
