package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.BlockTagUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
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

public class BlockCompressCategory implements IRecipeCategory<RecipeHolder<BlockCompressRecipe>> {
    private static final String INPUT_BLOCK_PREFIX = "input_block_";

    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable arrowDefault;
    private final IDrawable icon;
    private final ITickTimer timer;
    private final Component title;

    public BlockCompressCategory(IGuiHelper helper) {
        this.arrowDefault = JeiRenderHelper.getArrowDefault(helper);
        this.icon = helper.createDrawableItemStack(new ItemStack(Items.ANVIL));
        this.title = Component.translatable("gui.anvilcraft.category.block_compress");
        this.timer = helper.createTickTimer(30, 60, true);
    }

    @Override
    public IRecipeHolderType<BlockCompressRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.BLOCK_COMPRESS;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return BlockCompressCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return BlockCompressCategory.HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BlockCompressRecipe> recipeHolder, IFocusGroup focuses) {
        BlockCompressRecipe recipe = recipeHolder.value();
        for (int i = 0; i < recipe.getInputBlocks().size(); i++) {
            int y = i == 0 ? 24 : 42 + 10 * (i - 1);
            int height = i == 0 ? 18 : 10;
            JeiBlockIngredientUtil.addInputSlot(
                builder, BlockCompressCategory.INPUT_BLOCK_PREFIX + i, 40, y, 18, height, recipe.getInputBlocks().get(i)
            );
        }
        JeiBlockIngredientUtil.addSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            "output_block",
            100,
            42,
            20,
            10,
            recipe.getFirstResultBlock().state().getBlock()
        );
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder, RecipeHolder<BlockCompressRecipe> recipeHolder, IFocusGroup focuses
    ) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void draw(
        RecipeHolder<BlockCompressRecipe> recipeHolder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        BlockCompressRecipe recipe = recipeHolder.value();

        this.arrowDefault.draw(graphics, 73, 40);

        for (int i = recipe.getInputBlocks().size() - 1; i >= 0; i--) {
            List<BlockState> input = recipe.getInputBlocks().get(i).constructStatesForRender();
            if (input.isEmpty()) continue;
            BlockState renderedState = JeiBlockIngredientUtil
                .getDisplayedState(view, BlockCompressCategory.INPUT_BLOCK_PREFIX + i, input)
                .orElse(input.getFirst());
            RenderSupport.renderBlock(graphics, renderedState, 40, 30 + 10 * i, 20);
        }
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 40, 12 + anvilYOffset, 20);

        RenderSupport.renderBlock(graphics, recipe.getFirstResultBlock().state(), 100, 40, 20);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 100, 30, 20);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<BlockCompressRecipe> recipeHolder,
        IRecipeSlotsView view,
        double mouseX,
        double mouseY
    ) {
        IRecipeCategory.super.getTooltip(tooltip, recipeHolder, view, mouseX, mouseY);
        BlockCompressRecipe recipe = recipeHolder.value();
        Identifier id = this.getIdentifier(recipeHolder);

        if (MathUtil.isInRange(mouseX, 40, 58)) {
            if (MathUtil.isInRange(mouseY, 24, 42)) {
                tooltip.addAll(BlockTagUtil.getTooltipsForInput(recipe.getInputBlocks().getFirst()));
            }
            if (MathUtil.isInRange(mouseY, 42, 52)) {
                tooltip.addAll(BlockTagUtil.getTooltipsForInput(recipe.getInputBlocks().getLast()));
            }
        }
        if (MathUtil.isInRange(mouseX, mouseY, 100, 42, 120, 52)) {
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
            AnvilCraftJeiPlugin.BLOCK_COMPRESS,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.BLOCK_COMPRESS.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.BLOCK_COMPRESS);
    }
}
