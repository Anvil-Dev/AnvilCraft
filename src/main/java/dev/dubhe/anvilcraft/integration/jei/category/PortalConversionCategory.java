package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.predicate.WeightedChanceBlockStates;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.PortalConversionRecipe;
import dev.dubhe.anvilcraft.util.TooltipUtil;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class PortalConversionCategory implements IRecipeCategory<RecipeHolder<PortalConversionRecipe>> {
    private static final String INPUT_BLOCK = "input_block";
    private static final String OUTPUT_BLOCK = "output_block";

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
    public IRecipeHolderType<PortalConversionRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.PORTAL_CONVERSION;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return PortalConversionCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return PortalConversionCategory.HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return null;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PortalConversionRecipe> holder, IFocusGroup focuses) {
        PortalConversionRecipe recipe = holder.value();
        JeiBlockIngredientUtil.addInputSlot(builder, PortalConversionCategory.INPUT_BLOCK, 4, 4, 18, 18, recipe.getInput());
        JeiBlockIngredientUtil.addSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            PortalConversionCategory.OUTPUT_BLOCK,
            142,
            4,
            18,
            18,
            recipe.getResults().states().stream()
                .map(result -> new ItemStack(result.state().state().getBlock()))
                .toList()
        );
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder, RecipeHolder<PortalConversionRecipe> holder, IFocusGroup focuses
    ) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void draw(
        RecipeHolder<PortalConversionRecipe> holder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        PortalConversionRecipe recipe = holder.value();
        RENDER_INPUT: {
            List<BlockState> input = recipe.getInput().constructStatesForRender();
            if (input.isEmpty()) break RENDER_INPUT;
            BlockState renderedState = JeiBlockIngredientUtil.getDisplayedState(view, PortalConversionCategory.INPUT_BLOCK, input)
                .orElse(input.getFirst());
            JeiRenderHelper.renderBlockWithSlot(
                graphics,
                this.slotDefault,
                renderedState,
                4,
                4
            );
        }

        graphics.centeredText(Minecraft.getInstance().font, "WIP", 81, 32, 0xFFFFFFFF);

        List<WeightedChanceBlockStates.Entry> results = recipe.getResults().states();
        if (!results.isEmpty()) {
            List<BlockState> resultStates = results.stream().map(result -> result.state().state()).toList();
            BlockState displayedState = JeiBlockIngredientUtil.getDisplayedState(view, PortalConversionCategory.OUTPUT_BLOCK, resultStates)
                .orElse(resultStates.getFirst());
            WeightedChanceBlockStates.Entry result = results.stream()
                .filter(entry -> entry.state().state().is(displayedState.getBlock()))
                .findFirst()
                .orElse(results.getFirst());
            JeiRenderHelper.renderBlockWithSlot(
                graphics,
                result.state().chance() instanceof ConstantValue(float value) && value == 1.0F ? this.slotDefault : this.slotProbability,
                displayedState,
                142,
                4
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
            List<Component> tooltips = TooltipUtil.recipeIDTooltip(result.state().state().getBlock(), recipe.id().identifier());
            tooltips.addAll(tooltips.size() - 1, JeiRecipeUtil.getTooltips(result.state().chance()));
            tooltip.addAll(tooltips);
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.PORTAL_CONVERSION,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.PORTAL_CONVERSION.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AnvilCraftJeiPlugin.PORTAL_CONVERSION, Blocks.END_PORTAL_FRAME);
        registration.addCraftingStation(AnvilCraftJeiPlugin.PORTAL_CONVERSION, Blocks.OBSIDIAN);
    }
}
