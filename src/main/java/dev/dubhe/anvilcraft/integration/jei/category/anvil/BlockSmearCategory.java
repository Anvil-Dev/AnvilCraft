package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.BlockTagUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockSmearRecipe;
import dev.dubhe.anvilcraft.util.TooltipUtil;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlockSmearCategory implements IRecipeCategory<RecipeHolder<BlockSmearRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable arrowDefault;
    private final IDrawable icon;
    private final Component title;
    private final ITickTimer timer;

    public BlockSmearCategory(IGuiHelper helper) {
        this.arrowDefault = JeiRenderHelper.getArrowDefault(helper);
        this.icon = helper.createDrawableItemStack(new ItemStack(Items.ANVIL));
        this.title = Component.translatable("gui.anvilcraft.category.block_smear");
        this.timer = helper.createTickTimer(30, 60, true);
    }

    @Override
    public RecipeType<RecipeHolder<BlockSmearRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.BLOCK_SMEAR;
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BlockSmearRecipe> recipeHolder, IFocusGroup focuses) {
        BlockSmearRecipe recipe = recipeHolder.value();
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(
            recipe.getInputBlocks().stream().flatMap(
                blockStatePredicate -> blockStatePredicate.getBlocks().stream().map(
                    blockHolder -> new ItemStack(blockHolder.value())
                )
            ).toList()
        );
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
            .addItemStack(new ItemStack(recipe.getFirstResultBlock().state().getBlock()));
    }

    @Override
    public void draw(
        RecipeHolder<BlockSmearRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        BlockSmearRecipe recipe = recipeHolder.value();

        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        arrowDefault.draw(guiGraphics, 73, 35);

        RenderSupport.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            50,
            12 + anvilYOffset,
            20,
            12,
            RenderSupport.SINGLE_BLOCK
        );

        for (int i = recipe.getInputBlocks().size() - 1; i >= 0; i--) {
            List<BlockState> input = recipe.getInputBlocks().get(i).constructStatesForRender();
            if (input.isEmpty()) continue;
            BlockState renderedState = input.get((int) ((System.currentTimeMillis() / 1000) % input.size()));
            if (renderedState == null) continue;
            RenderSupport.renderBlock(
                guiGraphics,
                renderedState,
                50,
                30 + 10 * i,
                10 - 10 * i,
                12,
                RenderSupport.SINGLE_BLOCK
            );
        }

        RenderSupport.renderBlock(
            guiGraphics, Blocks.ANVIL.defaultBlockState(), 110, 20, 20, 12, RenderSupport.SINGLE_BLOCK
        );
        List<BlockState> input = recipe.getFirstInputBlock().constructStatesForRender();
        BlockState renderedState = input.get((int) ((System.currentTimeMillis() / 1000) % input.size()));
        RenderSupport.renderBlock(
            guiGraphics,
            renderedState,
            110,
            30,
            10,
            12,
            RenderSupport.SINGLE_BLOCK
        );
        RenderSupport.renderBlock(
            guiGraphics, recipe.getFirstResultBlock().state(), 110, 40, 0, 12, RenderSupport.SINGLE_BLOCK
        );
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<BlockSmearRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY) {
        IRecipeCategory.super.getTooltip(tooltip, recipeHolder, recipeSlotsView, mouseX, mouseY);
        BlockSmearRecipe recipe = recipeHolder.value();
        ResourceLocation id = getRegistryName(recipeHolder);

        if (mouseX >= 40 && mouseX <= 58) {
            if (mouseY >= 24 && mouseY < 42) {
                tooltip.addAll(BlockTagUtil.getTooltipsForInput(recipe.getInputBlocks().getFirst()));
            }
            if (mouseY >= 42 && mouseY <= 52) {
                tooltip.addAll(BlockTagUtil.getTooltipsForInput(recipe.getInputBlocks().getLast()));
            }
        }
        if (mouseX >= 100 && mouseX <= 118) {
            if (mouseY >= 24 && mouseY < 42) {
                tooltip.addAll(BlockTagUtil.getTooltipsForInput(recipe.getInputBlocks().getFirst()));
            }
            if (mouseY >= 42 && mouseY <= 52) {
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
            AnvilCraftJeiPlugin.BLOCK_SMEAR,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.BLOCK_SMEAR_TYPE.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilCraftJeiPlugin.BLOCK_SMEAR);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROYAL_ANVIL), AnvilCraftJeiPlugin.BLOCK_SMEAR);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EMBER_ANVIL), AnvilCraftJeiPlugin.BLOCK_SMEAR);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GIANT_ANVIL), AnvilCraftJeiPlugin.BLOCK_SMEAR);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SPECTRAL_ANVIL), AnvilCraftJeiPlugin.BLOCK_SMEAR);
    }
}
