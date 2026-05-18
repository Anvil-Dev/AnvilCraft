package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCrushRecipe;
import dev.dubhe.anvilcraft.util.TooltipUtil;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class BlockCrushCategory implements IRecipeCategory<RecipeHolder<BlockCrushRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable arrowDefault;
    private final IDrawable icon;
    private final Component title;
    private final ITickTimer timer;

    public BlockCrushCategory(IGuiHelper helper) {
        this.arrowDefault = JeiRenderHelper.getArrowDefault(helper);
        this.icon = helper.createDrawableItemStack(new ItemStack(Items.ANVIL));
        this.title = Component.translatable("gui.anvilcraft.category.block_crush");
        this.timer = helper.createTickTimer(30, 60, true);
    }

    @Override
    public IRecipeHolderType<BlockCrushRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.BLOCK_CRUSH;
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BlockCrushRecipe> recipeHolder, IFocusGroup focuses) {
        BlockCrushRecipe recipe = recipeHolder.value();
        JeiRecipeUtil.addInvisibleInput(builder, recipe.getFirstInputBlock());
        JeiRecipeUtil.addInvisibleOutput(builder, recipe.getFirstResultBlock());
    }

    @Override
    public void draw(
        RecipeHolder<BlockCrushRecipe> recipe,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        this.arrowDefault.draw(graphics, 73, 35);

        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 50, 22 + anvilYOffset, 12);

        renderInput: {
            List<BlockState> input = recipe.value().getFirstInputBlock().constructStatesForRender();
            if (input.isEmpty()) break renderInput;
            BlockState renderedState = input.get((int) ((System.currentTimeMillis() / 1000) % input.size()));
            if (renderedState == null) break renderInput;
            RenderSupport.renderBlock(graphics, renderedState, 50, 40, 12);
        }

        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 110, 30, 12);
        RenderSupport.renderBlock(graphics, recipe.value().getFirstResultBlock().state(), 110, 40, 12);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<BlockCrushRecipe> recipeHolder,
        IRecipeSlotsView view,
        double mouseX,
        double mouseY
    ) {
        IRecipeCategory.super.getTooltip(tooltip, recipeHolder, view, mouseX, mouseY);
        BlockCrushRecipe recipe = recipeHolder.value();
        Identifier id = this.getIdentifier(recipeHolder);

        if (MathUtil.isInRange(mouseX, mouseY, 40, 42, 58, 52)) {
            tooltip.addAll(TooltipUtil.tooltip(recipe.getFirstInputBlock().constructStatesForRender().getFirst().getBlock()));
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
            AnvilCraftJeiPlugin.BLOCK_CRUSH,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.BLOCK_CRUSH.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.BLOCK_CRUSH);
    }
}
