package dev.dubhe.anvilcraft.integration.jei.category;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.VoidDecayRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.util.LevelLike;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.placement.VerticalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IScrollGridWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.common.util.RegistryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VoidDecayCategory implements IRecipeCategory<VoidDecayRecipe> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 128;
    public static final int MAX_SHOWN_ROW = 7;
    public static final int MAX_SHOWN_COLUMN = 5;

    private final IDrawable slot;
    private final Component title;
    private final IDrawable arrowDefault;
    private final Component randomTickTooltip;
    private final Component centerTooltip;
    private final Component aroundTooltip;
    private final Component notConsumedTooltip;

    private final Map<VoidDecayRecipe, LevelLike> cache = new HashMap<>();

    private static final ImmutableList<BlockPos> CATALYST_POS = ImmutableList.of(
        new BlockPos(1, 0, 1),
        new BlockPos(1, 1, 0),
        new BlockPos(1, 1, 2),
        new BlockPos(1, 2, 1),
        new BlockPos(0, 1, 1)
    );
    private static final BlockPos CENTER_POS = new BlockPos(1, 1, 1);

    public VoidDecayCategory(IGuiHelper helper) {
        this.slot = JeiRenderHelper.getSlotChoice(helper);
        this.title = Component.translatable("gui.anvilcraft.category.void_decay");
        this.randomTickTooltip = Component.translatable("gui.anvilcraft.category.void_decay.random_tick");
        this.centerTooltip = Component.translatable("gui.anvilcraft.category.void_decay.center")
            .withStyle(ChatFormatting.GOLD);
        this.aroundTooltip = Component.translatable("gui.anvilcraft.category.void_decay.around")
            .withStyle(ChatFormatting.GOLD);
        this.notConsumedTooltip = Component.translatable("gui.anvilcraft.category.void_decay.not_consumed")
            .withStyle(ChatFormatting.GOLD);
        this.arrowDefault = JeiRenderHelper.getArrowDefault(helper);
    }

    @Override
    public IRecipeType<VoidDecayRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.VOID_DECAY;
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
        return null;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder, VoidDecayRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 84).add(recipe.center().asItem().getDefaultInstance())
            .addRichTooltipCallback((_, tooltip) -> tooltip.add(this.centerTooltip));
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 102).add(new ItemStack(recipe.catalyst().asItem(), recipe.catalystCount()))
            .addRichTooltipCallback((_, tooltip) ->
                tooltip.addAll(List.of(this.aroundTooltip, this.notConsumedTooltip)));
        RegistryUtil.getRegistry(Registries.BLOCK)
            .getOrThrow(recipe.result())
            .stream()
            .map(h -> h.value().asItem().getDefaultInstance())
            .forEach(stack -> builder.addOutputSlot().add(stack));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, VoidDecayRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotDrawablesView recipeSlots = builder.getRecipeSlots();
        List<IRecipeSlotDrawable> outputSlots = recipeSlots.getSlots(RecipeIngredientRole.OUTPUT);

        IScrollGridWidget scrollGridWidget = builder.addScrollGridWidget(outputSlots, MAX_SHOWN_COLUMN, MAX_SHOWN_ROW);
        scrollGridWidget.setPosition(60, 4, this.getWidth(), this.getHeight(), HorizontalAlignment.LEFT, VerticalAlignment.TOP);
    }

    @Override
    public void draw(
        VoidDecayRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        LevelLike level = this.cache.get(recipe);
        if (level == null) {
            LevelLike showCase = new LevelLike(Minecraft.getInstance().level);
            CATALYST_POS.forEach(pos -> showCase.setBlockState(pos, recipe.catalyst().defaultBlockState()));
            showCase.setBlockState(CENTER_POS, recipe.center().defaultBlockState());
            this.cache.put(recipe, showCase);
            level = showCase;
        }

        RenderSupport.renderLevelLike(level, graphics, 24, 36, 60, 0.5F);

        this.slot.draw(graphics, 7, 83);
        this.slot.draw(graphics, 7, 101);
        this.arrowDefault.draw(graphics, 35, 87);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        VoidDecayRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY) {
        if (mouseX >= 5 && mouseX <= 45 && mouseY >= 15 && mouseY <= 65) {
            tooltip.add(this.randomTickTooltip);
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AnvilCraftJeiPlugin.VOID_DECAY, VoidDecayRecipe.getAllRecipes());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AnvilCraftJeiPlugin.VOID_DECAY, ModBlocks.VOID_MATTER_BLOCK);
    }
}
