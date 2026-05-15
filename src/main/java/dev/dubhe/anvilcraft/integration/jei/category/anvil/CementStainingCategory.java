package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
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
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import org.jetbrains.annotations.Nullable;

public class CementStainingCategory implements IRecipeCategory<CementStainingRecipe> {
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
    public RecipeType<CementStainingRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.CEMENT_STAINING;
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
    public void setRecipe(IRecipeLayoutBuilder builder, CementStainingRecipe recipe, IFocusGroup focuses) {
        JeiSlotUtil.addInputSlots(builder, recipe.ingredients());

        // 将水泥锅里面的流体加入输入输出(在别处写CauldronFluidContent不保证能正常运行
        // TODO: 等流体系统出来后可能要重写
        if (recipe.resultBlock() != null) {
            CauldronFluidContent cauldronFluidContent = CauldronFluidContent.getForBlock(recipe.resultBlock());
            if (cauldronFluidContent == null) return;
            Fluid fluid = cauldronFluidContent.fluid;
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addFluidStack(fluid);
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addFluidStack(fluid);
        }
    }

    @Override
    public void draw(
        CementStainingRecipe recipe,
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

        JeiSlotUtil.drawInputSlots(guiGraphics, slotDefault, recipe.ingredients().size());

        RenderSupport.renderBlock(guiGraphics, recipe.resultBlock().defaultBlockState(), 133, 30, 0, 12, RenderSupport.SINGLE_BLOCK);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        CementStainingRecipe recipe,
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
                tooltip.add(recipe.resultBlock().getName());
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AnvilCraftJeiPlugin.CEMENT_STAINING, CementStainingRecipe.getAllRecipes());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.CEMENT_STAINING);
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), AnvilCraftJeiPlugin.CEMENT_STAINING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FISH_TANK), AnvilCraftJeiPlugin.CEMENT_STAINING);
    }
}
