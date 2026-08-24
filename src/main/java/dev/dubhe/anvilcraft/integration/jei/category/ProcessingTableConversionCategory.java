package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.ProcessingTableConversionRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 展示四种加工台之间的右键转化关系。
 */
public class ProcessingTableConversionCategory
    implements IRecipeCategory<ProcessingTableConversionRecipe> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final IDrawable arrowIn;
    private final IDrawable arrowOut;
    private final Component title;
    private final Component convertTooltip;

    public ProcessingTableConversionCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(ModBlocks.STAMPING_PLATFORM.asStack());
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOut = JeiRenderHelper.getArrowOutput(helper);
        this.title = Component.translatable("gui.anvilcraft.category.processing_table_conversion");
        this.convertTooltip = Component.translatable(
                "gui.anvilcraft.category.processing_table_conversion.convert"
            )
            .withStyle(ChatFormatting.GOLD);
    }

    @Override
    public RecipeType<ProcessingTableConversionRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.PROCESSING_TABLE_CONVERSION;
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
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        ProcessingTableConversionRecipe recipe,
        IFocusGroup focuses
    ) {
        builder.addSlot(RecipeIngredientRole.CATALYST, JeiSlotUtil.INPUT_X, JeiSlotUtil.DEFAULT_Y)
            .addItemStack(recipe.toolStack())
            .addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(this.convertTooltip));
        builder.addSlot(RecipeIngredientRole.OUTPUT, JeiSlotUtil.OUTPUT_X, JeiSlotUtil.DEFAULT_Y)
            .addItemStack(recipe.outputStack())
            .addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(this.convertTooltip));
    }

    @Override
    public void draw(
        ProcessingTableConversionRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        RenderSupport.renderBlock(
            guiGraphics,
            recipe.inputTable().defaultBlockState(),
            81,
            30,
            10,
            12,
            RenderSupport.SINGLE_BLOCK
        );

        this.arrowIn.draw(guiGraphics, 54, 30);
        this.arrowOut.draw(guiGraphics, 92, 29);

        JeiSlotUtil.drawSlots(guiGraphics, this.slotDefault, 1, JeiSlotUtil.INPUT_X - 1, JeiSlotUtil.DEFAULT_Y - 1);

        JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, this.slotDefault, 1);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        ProcessingTableConversionRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        if (mouseX >= 50 && mouseX <= 100 && mouseY >= 15 && mouseY <= 45) {
            tooltip.add(this.convertTooltip);
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.PROCESSING_TABLE_CONVERSION,
            ProcessingTableConversionRecipe.getAllRecipes()
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
            new ItemStack(ModBlocks.STAMPING_PLATFORM),
            AnvilCraftJeiPlugin.PROCESSING_TABLE_CONVERSION
        );
        registration.addRecipeCatalyst(
            new ItemStack(ModBlocks.CRUSHING_TABLE),
            AnvilCraftJeiPlugin.PROCESSING_TABLE_CONVERSION
        );
        registration.addRecipeCatalyst(
            new ItemStack(ModBlocks.SIFTING_TABLE),
            AnvilCraftJeiPlugin.PROCESSING_TABLE_CONVERSION
        );
        registration.addRecipeCatalyst(
            new ItemStack(ModBlocks.UNPACKING_TABLE),
            AnvilCraftJeiPlugin.PROCESSING_TABLE_CONVERSION
        );
    }
}
