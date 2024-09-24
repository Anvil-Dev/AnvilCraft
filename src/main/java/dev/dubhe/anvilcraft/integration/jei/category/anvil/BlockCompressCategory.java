package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.TextureConstants;
import dev.dubhe.anvilcraft.recipe.anvil.BlockCompressRecipe;
import dev.dubhe.anvilcraft.util.RenderHelper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
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
public class BlockCompressCategory implements IRecipeCategory<RecipeHolder<BlockCompressRecipe>> {

    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final Lazy<IDrawable> background;
    private final IDrawable progress;
    private final IDrawable icon;
    private final ITickTimer timer;
    private final Component title;

    public BlockCompressCategory(IGuiHelper helper) {
        background = Lazy.of(() -> helper.createBlankDrawable(WIDTH, HEIGHT));
        progress = helper.drawableBuilder(TextureConstants.PROGRESS, 0, 0, 24, 16)
                .setTextureSize(24, 16)
                .build();
        icon = helper.createDrawableItemStack(new ItemStack(Items.ANVIL));
        title = Component.translatable("gui.anvilcraft.category.block_compress");
        timer = helper.createTickTimer(30, 60, true);
    }

    @Override
    public RecipeType<RecipeHolder<BlockCompressRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.BLOCK_COMPRESS;
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
            IRecipeLayoutBuilder builder, RecipeHolder<BlockCompressRecipe> recipeHolder, IFocusGroup focuses) {
        BlockCompressRecipe recipe = recipeHolder.value();
        for (Block block : recipe.inputs) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(new ItemStack(block));
        }
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(new ItemStack(recipe.result));
    }

    @Override
    public void draw(
            RecipeHolder<BlockCompressRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY) {
        BlockCompressRecipe recipe = recipeHolder.value();

        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        progress.draw(guiGraphics, 69, 30);

        RenderHelper.renderBlock(
                guiGraphics,
                Blocks.ANVIL.defaultBlockState(),
                50,
                12 + anvilYOffset,
                20,
                12,
                RenderHelper.SINGLE_BLOCK);

        for (int i = recipe.inputs.size() - 1; i >= 0; i--) {
            Block input = recipe.inputs.get(i);
            RenderHelper.renderBlock(
                    guiGraphics,
                    input.defaultBlockState(),
                    50,
                    30 + 10 * i,
                    10 - 10 * i,
                    12,
                    RenderHelper.SINGLE_BLOCK);
        }

        RenderHelper.renderBlock(
                guiGraphics, Blocks.ANVIL.defaultBlockState(), 110, 30, 10, 12, RenderHelper.SINGLE_BLOCK);
        RenderHelper.renderBlock(
                guiGraphics, recipe.result.defaultBlockState(), 110, 40, 0, 12, RenderHelper.SINGLE_BLOCK);
    }

    @Override
    public void getTooltip(
            ITooltipBuilder tooltip,
            RecipeHolder<BlockCompressRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            double mouseX,
            double mouseY) {
        IRecipeCategory.super.getTooltip(tooltip, recipeHolder, recipeSlotsView, mouseX, mouseY);
        BlockCompressRecipe recipe = recipeHolder.value();

        if (mouseX >= 40 && mouseX <= 58) {
            if (mouseY >= 24 && mouseY <= 42) {
                tooltip.add(recipe.inputs.getFirst().getName());
            }
            if (mouseY >= 42 && mouseY <= 52) {
                tooltip.add(recipe.inputs.getLast().getName());
            }
        }
        if (mouseX >= 100 && mouseX <= 120) {
            if (mouseY >= 42 && mouseY <= 52) {
                tooltip.add(recipe.result.getName());
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
                AnvilCraftJeiPlugin.BLOCK_COMPRESS,
                JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.BLOCK_COMPRESS_TYPE.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilCraftJeiPlugin.BLOCK_COMPRESS);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROYAL_ANVIL), AnvilCraftJeiPlugin.BLOCK_COMPRESS);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EMBER_ANVIL), AnvilCraftJeiPlugin.BLOCK_COMPRESS);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GIANT_ANVIL), AnvilCraftJeiPlugin.BLOCK_COMPRESS);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SPECTRAL_ANVIL), AnvilCraftJeiPlugin.BLOCK_COMPRESS);
    }
}
