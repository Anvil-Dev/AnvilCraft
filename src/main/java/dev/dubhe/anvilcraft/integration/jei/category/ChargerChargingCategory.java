package dev.dubhe.anvilcraft.integration.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.ChargerChargingRecipe;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
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
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ChargerChargingCategory implements IRecipeCategory<RecipeHolder<ChargerChargingRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final Component title;

    private final IDrawable arrowIn;
    private final IDrawable arrowOut;

    private static final String KEY_CATEGORY = "gui.anvilcraft.category.charger_charging";
    private static final String KEY_POWER_CONSUME = KEY_CATEGORY + ".power_consume";
    private static final String KEY_POWER_PRODUCE = KEY_CATEGORY + ".power_produce";
    private static final String KEY_TIME = KEY_CATEGORY + ".time";

    public ChargerChargingCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(ModBlocks.CHARGER.asStack());
        slotDefault = JeiRenderHelper.getSlotDefault(helper);
        title = Component.translatable("gui.anvilcraft.category.charger_charging");

        arrowIn = JeiRenderHelper.getArrowInput(helper);
        arrowOut = JeiRenderHelper.getArrowOutput(helper);
    }

    @Override
    public RecipeType<RecipeHolder<ChargerChargingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.CHARGER_CHARGING;
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
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder, RecipeHolder<ChargerChargingRecipe> recipeHolder, IFocusGroup focuses) {
        ChargerChargingRecipe recipe = recipeHolder.value();
        builder.addSlot(RecipeIngredientRole.INPUT, 21, 24)
            .addIngredients(recipe.getIngredient());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 125, 24)
            .addItemStack(recipe.getResult());
        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST)
            .addItemStack(recipe.getProcessingBlock().asItem().getDefaultInstance());
    }

    @Override
    public void draw(
        RecipeHolder<ChargerChargingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        ChargerChargingRecipe recipe = recipeHolder.value();
        RenderSupport.renderBlock(
            guiGraphics,
            recipe.getProcessingBlock().defaultBlockState(),
            81,
            40,
            10,
            12,
            RenderSupport.SINGLE_BLOCK);

        arrowIn.draw(guiGraphics, 54, 30);
        arrowOut.draw(guiGraphics, 92, 29);

        JeiSlotUtil.drawInputSlots(guiGraphics, slotDefault, 1);
        JeiSlotUtil.drawOutputSlots(guiGraphics, slotDefault, 1);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(0.8f, 0.8f, 1.0f);
        guiGraphics.drawString(Minecraft.getInstance().font,
            Component.translatable(recipe.getPower() < 0 ? KEY_POWER_CONSUME : KEY_POWER_PRODUCE,
                Math.abs(recipe.getPower())),
            0, 10, 0xFF000000, false);
        guiGraphics.drawString(Minecraft.getInstance().font,
            Component.translatable(KEY_TIME, 0.05 * recipe.getTime()),
            0, 70, 0xFF000000, false);
        pose.popPose();
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<ChargerChargingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY) {
        ChargerChargingRecipe recipe = recipeHolder.value();
        if (mouseX >= 72 && mouseX <= 90) {
            if (mouseY >= 34 && mouseY <= 53) {
                tooltip.add(recipe.getProcessingBlock().getName());
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.CHARGER_CHARGING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.CHARGER_CHARGING_TYPE.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CHARGER), AnvilCraftJeiPlugin.CHARGER_CHARGING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.DISCHARGER), AnvilCraftJeiPlugin.CHARGER_CHARGING);
    }
}
