package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.predicate.WeightedChanceBlockStates;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.PortalConversionRecipe;
import dev.dubhe.anvilcraft.util.TooltipUtil;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PortalConversionCategory implements IRecipeCategory<RecipeHolder<PortalConversionRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final Component title;
    private final IDrawable slotDefault;
    private final IDrawable slotProbability;

    public PortalConversionCategory(IGuiHelper helper) {
        this.title = Component.translatable("gui.anvilcraft.category.portal_conversion");
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.slotProbability = JeiRenderHelper.getSlotProbability(helper);
    }

    @Override
    public RecipeType<RecipeHolder<PortalConversionRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.PORTAL_CONVERSION;
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
        return null;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PortalConversionRecipe> holder, IFocusGroup focuses) {
        PortalConversionRecipe recipe = holder.value();
        IIngredientAcceptor<?> acceptor = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
        for (Holder<Block> block : recipe.getInput().getBlocks()) {
            acceptor.addItemLike(block.value());
        }
        acceptor = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT);
        for (WeightedChanceBlockStates.Entry state : recipe.getResults().states()) {
            acceptor.addItemLike(state.state().state().getBlock());
        }
    }

    @Override
    public void draw(
        RecipeHolder<PortalConversionRecipe> holder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        PortalConversionRecipe recipe = holder.value();
        RENDER_INPUT: {
            List<BlockState> input = recipe.getInput().constructStatesForRender();
            if (input.isEmpty()) break RENDER_INPUT;
            BlockState renderedState = input.get((int) ((System.currentTimeMillis() / 1000) % input.size()));
            if (renderedState == null) break RENDER_INPUT;
            JeiRenderHelper.renderBlockWithSlot(
                guiGraphics,
                this.slotDefault,
                renderedState,
                4,
                4,
                20,
                RenderSupport.SINGLE_BLOCK
            );
        }

        guiGraphics.drawCenteredString(Minecraft.getInstance().font, "WIP", 81, 32, 0xFFFFFF);

        List<WeightedChanceBlockStates.Entry> results = recipe.getResults().states();
        if (results.size() == 1) {
            WeightedChanceBlockStates.Entry result = results.getFirst();
            JeiRenderHelper.renderBlockWithSlot(
                guiGraphics,
                result.state().chance() instanceof ConstantValue(float value) && value == 1.0F ? this.slotDefault : this.slotProbability,
                result.state().state(),
                142,
                4,
                20,
                RenderSupport.SINGLE_BLOCK
            );
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<PortalConversionRecipe> recipe,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        if (MathUtil.isInRange(mouseX, mouseY, 4, 4, 21, 21)) {
            List<BlockState> input = recipe.value().getInput().constructStatesForRender();
            if (input.isEmpty()) return;
            BlockState renderedState = input.get((int) ((System.currentTimeMillis() / 1000) % input.size()));
            if (renderedState == null) return;
            tooltip.addAll(TooltipUtil.tooltip(renderedState.getBlock()));
            return;
        } else if (MathUtil.isInRange(mouseX, mouseY, 24, 0, 138, 64)) {
            tooltip.add(Component.translatable(
                "gui.anvilcraft.category.portal_conversion.fall_through",
                recipe.value().getPortalType().getPortalName()
            ));
            return;
        }

        List<WeightedChanceBlockStates.Entry> results = recipe.value().getResults().states();
        if (results.size() == 1) {
            if (!MathUtil.isInRange(mouseX, mouseY, 142, 4, 159, 21)) return;
            WeightedChanceBlockStates.Entry result = results.getFirst();
            List<Component> tooltips = TooltipUtil.recipeIDTooltip(result.state().state().getBlock(), recipe.id());
            tooltips.addAll(tooltips.size() - 1, JeiRecipeUtil.getTooltips(result.state().chance()));
            tooltip.addAll(tooltips);
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.PORTAL_CONVERSION,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.PORTAL_CONVERSION_TYPE.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Blocks.END_PORTAL_FRAME), AnvilCraftJeiPlugin.PORTAL_CONVERSION);
        registration.addRecipeCatalyst(new ItemStack(Blocks.OBSIDIAN), AnvilCraftJeiPlugin.PORTAL_CONVERSION);
    }
}
