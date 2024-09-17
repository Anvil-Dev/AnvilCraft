package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.block.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.recipe.ColoredConcreteRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.integration.jei.util.TextureConstants;
import dev.dubhe.anvilcraft.util.RenderHelper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.Lazy;

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
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ConcreteCategory implements IRecipeCategory<ColoredConcreteRecipe> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final Lazy<IDrawable> background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final Component title;
    private final ITickTimer timer;

    private final IDrawable arrowIn;
    private final IDrawable arrowOut;

    public ConcreteCategory(IGuiHelper helper) {
        background = Lazy.of(() -> helper.createBlankDrawable(WIDTH, HEIGHT));
        icon = new DrawableBlockStateIcon(
                Blocks.ANVIL.defaultBlockState(),
                ModBlocks.CEMENT_CAULDRON.getDefaultState().setValue(CementCauldronBlock.COLOR, Color.PINK));
        slot = helper.getSlotDrawable();
        title = Component.translatable("gui.anvilcraft.category.concrete");
        timer = helper.createTickTimer(30, 60, true);

        arrowIn = helper.createDrawable(TextureConstants.ANVIL_CRAFT_SPRITES, 0, 31, 16, 8);
        arrowOut = helper.createDrawable(TextureConstants.ANVIL_CRAFT_SPRITES, 0, 40, 16, 10);
    }

    @Override
    public RecipeType<ColoredConcreteRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.COLORED_CONCRETE;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background.get();
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ColoredConcreteRecipe recipe, IFocusGroup focuses) {
        JeiSlotUtil.addInputSlots(builder, recipe.mergedIngredients);
        JeiSlotUtil.addOutputSlots(builder, List.of(recipe.result));
    }

    @Override
    public void draw(
            ColoredConcreteRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY) {
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderHelper.renderBlock(
                guiGraphics,
                Blocks.ANVIL.defaultBlockState(),
                81,
                22 + anvilYOffset,
                20,
                12,
                RenderHelper.SINGLE_BLOCK);
        RenderHelper.renderBlock(
                guiGraphics,
                ModBlocks.CEMENT_CAULDRON.getDefaultState().setValue(CementCauldronBlock.COLOR, recipe.color),
                81,
                40,
                10,
                12,
                RenderHelper.SINGLE_BLOCK);

        arrowIn.draw(guiGraphics, 54, 32);
        arrowOut.draw(guiGraphics, 92, 31);

        JeiSlotUtil.drawInputSlots(guiGraphics, slot, recipe.mergedIngredients.size());
        JeiSlotUtil.drawOutputSlots(guiGraphics, slot, 1);
    }

    @Override
    public void getTooltip(
            ITooltipBuilder tooltip,
            ColoredConcreteRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            double mouseX,
            double mouseY) {
        if (mouseX >= 72 && mouseX <= 90) {
            if (mouseY >= 34 && mouseY <= 53) {
                tooltip.add(ModBlocks.CEMENT_CAULDRON.get().getName());
                tooltip.add(Component.translatable("color.minecraft." + recipe.color.getSerializedName()));
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AnvilCraftJeiPlugin.COLORED_CONCRETE, ColoredConcreteRecipe.getAllRecipes());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilCraftJeiPlugin.COLORED_CONCRETE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROYAL_ANVIL), AnvilCraftJeiPlugin.COLORED_CONCRETE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EMBER_ANVIL), AnvilCraftJeiPlugin.COLORED_CONCRETE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GIANT_ANVIL), AnvilCraftJeiPlugin.COLORED_CONCRETE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SPECTRAL_ANVIL), AnvilCraftJeiPlugin.COLORED_CONCRETE);
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), AnvilCraftJeiPlugin.COLORED_CONCRETE);
    }
}
