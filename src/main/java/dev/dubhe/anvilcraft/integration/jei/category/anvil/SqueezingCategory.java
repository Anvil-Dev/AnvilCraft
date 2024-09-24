package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.TextureConstants;
import dev.dubhe.anvilcraft.recipe.anvil.SqueezingRecipe;
import dev.dubhe.anvilcraft.util.RenderHelper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.Lazy;

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
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SqueezingCategory implements IRecipeCategory<RecipeHolder<SqueezingRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final Lazy<IDrawable> background;
    private final IDrawable progress;
    private final IDrawable icon;
    private final ITickTimer timer;
    private final Component title;

    public SqueezingCategory(IGuiHelper helper) {
        background = Lazy.of(() -> helper.createBlankDrawable(WIDTH, HEIGHT));
        progress = helper.drawableBuilder(TextureConstants.PROGRESS, 0, 0, 24, 16)
                .setTextureSize(24, 16)
                .build();
        icon = helper.createDrawableItemStack(new ItemStack(Items.ANVIL));
        title = Component.translatable("gui.anvilcraft.category.squeezing");
        timer = helper.createTickTimer(30, 60, true);
    }

    @Override
    public RecipeType<RecipeHolder<SqueezingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.SQUEEZING;
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
    public void setRecipe(
            IRecipeLayoutBuilder builder, RecipeHolder<SqueezingRecipe> recipeHolder, IFocusGroup focuses) {
        SqueezingRecipe recipe = recipeHolder.value();
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(new ItemStack(recipe.inputBlock));
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(new ItemStack(recipe.resultBlock));
    }

    @Override
    public void getTooltip(
            ITooltipBuilder tooltip,
            RecipeHolder<SqueezingRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            double mouseX,
            double mouseY) {
        SqueezingRecipe recipe = recipeHolder.value();
        if (mouseX >= 40 && mouseX <= 58) {
            if (mouseY >= 24 && mouseY <= 42) {
                tooltip.add(recipe.inputBlock.getName());
            }
            if (mouseY >= 42 && mouseY <= 52) {
                tooltip.add(Blocks.CAULDRON.getName());
            }
        }
        if (mouseX >= 100 && mouseX <= 120) {
            if (mouseY >= 24 && mouseY <= 42) {
                tooltip.add(recipe.resultBlock.getName());
            }
            if (mouseY >= 42 && mouseY <= 52) {
                tooltip.add(recipe.cauldron.getName());
            }
        }
    }

    @Override
    public void draw(
            RecipeHolder<SqueezingRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY) {
        SqueezingRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderHelper.renderBlock(
                guiGraphics,
                Blocks.ANVIL.defaultBlockState(),
                50,
                12 + anvilYOffset,
                20,
                12,
                RenderHelper.SINGLE_BLOCK);
        RenderHelper.renderBlock(
                guiGraphics, recipe.inputBlock.defaultBlockState(), 50, 30, 10, 12, RenderHelper.SINGLE_BLOCK);
        RenderHelper.renderBlock(
                guiGraphics, Blocks.CAULDRON.defaultBlockState(), 50, 40, 0, 12, RenderHelper.SINGLE_BLOCK);

        progress.draw(guiGraphics, 69, 30);

        RenderHelper.renderBlock(
                guiGraphics, Blocks.ANVIL.defaultBlockState(), 110, 20, 20, 12, RenderHelper.SINGLE_BLOCK);
        RenderHelper.renderBlock(
                guiGraphics, recipe.resultBlock.defaultBlockState(), 110, 30, 10, 12, RenderHelper.SINGLE_BLOCK);
        RenderHelper.renderBlock(
                guiGraphics, recipe.cauldron.defaultBlockState(), 110, 40, 0, 12, RenderHelper.SINGLE_BLOCK);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
                AnvilCraftJeiPlugin.SQUEEZING,
                JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.SQUEEZING_TYPE.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilCraftJeiPlugin.SQUEEZING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROYAL_ANVIL), AnvilCraftJeiPlugin.SQUEEZING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EMBER_ANVIL), AnvilCraftJeiPlugin.SQUEEZING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GIANT_ANVIL), AnvilCraftJeiPlugin.SQUEEZING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SPECTRAL_ANVIL), AnvilCraftJeiPlugin.SQUEEZING);
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), AnvilCraftJeiPlugin.SQUEEZING);
    }
}
