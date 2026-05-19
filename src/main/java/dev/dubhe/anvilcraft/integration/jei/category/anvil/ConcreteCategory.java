package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.TooltipUtil;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.recipe.ColoredConcreteRecipe;
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

import java.util.List;

public class ConcreteCategory implements IRecipeCategory<ColoredConcreteRecipe> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final Component title;
    private final ITickTimer timer;

    private final IDrawable arrowIn;
    private final IDrawable arrowOutFromBelow;

    public ConcreteCategory(IGuiHelper helper) {
        this.icon = new DrawableBlockStateIcon(
            Blocks.ANVIL.defaultBlockState(),
            ModBlocks.CEMENT_CAULDRONS.get(Color.PINK).getDefaultState()
        );
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.title = Component.translatable("gui.anvilcraft.category.concrete");
        this.timer = helper.createTickTimer(30, 60, true);

        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOutFromBelow = JeiRenderHelper.getArrowOutputFromBelow(helper);
    }

    @Override
    public IRecipeType<ColoredConcreteRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.COLORED_CONCRETE;
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
    public void setRecipe(IRecipeLayoutBuilder builder, ColoredConcreteRecipe recipe, IFocusGroup focuses) {
        JeiSlotUtil.addInputSlots(builder, recipe.ingredients());
        JeiSlotUtil.addOutputSlots(builder, List.of(recipe.result()));

        CauldronFluidContent input = CauldronFluidContent.getForBlock(ModBlocks.CEMENT_CAULDRONS.get(recipe.color()).get());
        if (input != null) builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).add(input.fluid);
    }

    @Override
    public void draw(
        ColoredConcreteRecipe recipe,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 81, 22 + anvilYOffset, 20);
        RenderSupport.renderBlock(graphics, ModBlocks.CEMENT_CAULDRONS.get(recipe.color()).getDefaultState(), 81, 40, 20);

        this.arrowIn.draw(graphics, 54, 30);
        this.arrowOutFromBelow.draw(graphics, 92, 29);

        JeiSlotUtil.drawInputSlots(graphics, this.slotDefault, recipe.ingredients().size());
        JeiSlotUtil.drawOutputSlots(graphics, this.slotDefault, 1);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        ColoredConcreteRecipe recipe,
        IRecipeSlotsView view,
        double mouseX,
        double mouseY
    ) {
        if (MathUtil.isInRange(mouseX, mouseY, 72, 34, 90, 53)) {
            tooltip.addAll(TooltipUtil.tooltip(ModBlocks.CEMENT_CAULDRONS.get(recipe.color()).get()));
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AnvilCraftJeiPlugin.COLORED_CONCRETE, ColoredConcreteRecipe.getAllRecipes());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.COLORED_CONCRETE);
        AnvilCraftJeiPlugin.addCauldronCatalysts(registration, AnvilCraftJeiPlugin.COLORED_CONCRETE);
    }
}
