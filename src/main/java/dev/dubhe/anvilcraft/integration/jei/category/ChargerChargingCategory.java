package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.TooltipUtil;
import dev.dubhe.anvilcraft.block.power.generator.ChargerBlock;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import org.joml.Matrix3x2fStack;

public class ChargerChargingCategory implements IRecipeCategory<RecipeHolder<ChargerChargingRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final Component title;

    private final IDrawable arrowIn;
    private final IDrawable arrowOut;

    private static final String KEY_CATEGORY = "gui.anvilcraft.category.charger_charging";
    private static final String KEY_POWER_CONSUME = ChargerChargingCategory.KEY_CATEGORY + ".power_consume";
    private static final String KEY_POWER_PRODUCE = ChargerChargingCategory.KEY_CATEGORY + ".power_produce";
    private static final String KEY_TIME = ChargerChargingCategory.KEY_CATEGORY + ".time";

    public ChargerChargingCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(ModBlocks.CHARGER.asStack());
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.title = Component.translatable("gui.anvilcraft.category.charger_charging");

        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOut = JeiRenderHelper.getArrowOutput(helper);
    }

    @Override
    public IRecipeHolderType<ChargerChargingRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.CHARGER_CHARGING;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return ChargerChargingCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return ChargerChargingCategory.HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder, RecipeHolder<ChargerChargingRecipe> recipeHolder, IFocusGroup focuses) {
        ChargerChargingRecipe recipe = recipeHolder.value();
        builder.addSlot(RecipeIngredientRole.INPUT, JeiSlotUtil.INPUT_X, JeiSlotUtil.DEFAULT_Y)
            .add(recipe.ingredient());
        builder.addSlot(RecipeIngredientRole.OUTPUT, JeiSlotUtil.OUTPUT_X, JeiSlotUtil.DEFAULT_Y)
            .add(recipe.result());
        builder.addInvisibleIngredients(RecipeIngredientRole.CRAFTING_STATION)
            .add(recipe.getProcessingBlock().asItem().getDefaultInstance());
    }

    @Override
    public void draw(
        RecipeHolder<ChargerChargingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        ChargerChargingRecipe recipe = recipeHolder.value();
        RenderSupport.renderBlock(
            graphics,
            recipe.getProcessingBlock().defaultBlockState().setValue(ChargerBlock.OVERLOAD, false),
            71,
            35,
            20
        );

        this.arrowIn.draw(graphics, 54, 30);
        this.arrowOut.draw(graphics, 92, 29);

        JeiSlotUtil.drawDefaultInputSlots(graphics, this.slotDefault, 1);
        JeiSlotUtil.drawDefaultOutputSlots(graphics, this.slotDefault, 1);

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.scale(0.8F, 0.8F);
        graphics.text(
            Minecraft.getInstance().font,
            Component.translatable(
                recipe.power() < 0 ? ChargerChargingCategory.KEY_POWER_CONSUME : ChargerChargingCategory.KEY_POWER_PRODUCE,
                Math.abs(recipe.power())
            ),
            0,
            10,
            0xFF000000,
            false
        );
        graphics.text(
            Minecraft.getInstance().font,
            Component.translatable(ChargerChargingCategory.KEY_TIME, 0.05 * recipe.time()),
            0,
            70,
            0xFF000000,
            false
        );
        pose.popMatrix();
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<ChargerChargingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        ChargerChargingRecipe recipe = recipeHolder.value();
        if (MathUtil.isInRange(mouseX, mouseY, 72, 34, 90, 53)) {
            tooltip.addAll(TooltipUtil.tooltip(recipe.getProcessingBlock()));
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.CHARGER_CHARGING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.CHARGER_CHARGING.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AnvilCraftJeiPlugin.CHARGER_CHARGING, ModBlocks.CHARGER);
        registration.addCraftingStation(AnvilCraftJeiPlugin.CHARGER_CHARGING, ModBlocks.DISCHARGER);
    }
}
