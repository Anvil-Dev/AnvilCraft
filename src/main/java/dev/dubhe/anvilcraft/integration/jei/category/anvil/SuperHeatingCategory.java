package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.integration.jei.util.TextureConstants;
import dev.dubhe.anvilcraft.recipe.anvil.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.util.RenderHelper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.Lazy;

import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SuperHeatingCategory implements IRecipeCategory<RecipeHolder<SuperHeatingRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final Lazy<IDrawable> background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final Component title;
    private final ITickTimer timer;

    private final IDrawable arrowIn;
    private final IDrawable arrowOut;

    public SuperHeatingCategory(IGuiHelper helper) {
        background = Lazy.of(() -> helper.createBlankDrawable(WIDTH, HEIGHT));
        icon = new DrawableBlockStateIcon(
                Blocks.CAULDRON.defaultBlockState(),
                ModBlocks.HEATER.getDefaultState().setValue(HeaterBlock.OVERLOAD, false));
        slot = helper.getSlotDrawable();
        title = Component.translatable("gui.anvilcraft.category.super_heating");
        timer = helper.createTickTimer(30, 60, true);

        arrowIn = helper.createDrawable(TextureConstants.ANVIL_CRAFT_SPRITES, 0, 31, 16, 8);
        arrowOut = helper.createDrawable(TextureConstants.ANVIL_CRAFT_SPRITES, 0, 40, 16, 10);
    }

    @Override
    public RecipeType<RecipeHolder<SuperHeatingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.SUPER_HEATING;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background.get();
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder, RecipeHolder<SuperHeatingRecipe> recipeHolder, IFocusGroup focuses) {
        SuperHeatingRecipe recipe = recipeHolder.value();
        JeiSlotUtil.addInputSlots(builder, recipe.mergedIngredients);
        JeiSlotUtil.addOutputSlots(builder, recipe.results);
    }

    @Override
    public void draw(
            RecipeHolder<SuperHeatingRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY) {
        SuperHeatingRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderHelper.renderBlock(
                guiGraphics,
                Blocks.ANVIL.defaultBlockState(),
                81,
                12 + anvilYOffset,
                20,
                12,
                RenderHelper.SINGLE_BLOCK);
        RenderHelper.renderBlock(
                guiGraphics, Blocks.CAULDRON.defaultBlockState(), 81, 30, 10, 12, RenderHelper.SINGLE_BLOCK);
        RenderHelper.renderBlock(
                guiGraphics,
                ModBlocks.HEATER.getDefaultState().setValue(HeaterBlock.OVERLOAD, false),
                81,
                40,
                0,
                12,
                RenderHelper.SINGLE_BLOCK);

        arrowIn.draw(guiGraphics, 54, 32);
        arrowOut.draw(guiGraphics, 92, 31);

        JeiSlotUtil.drawInputSlots(guiGraphics, slot, recipe.mergedIngredients.size());
        JeiSlotUtil.drawOutputSlots(guiGraphics, slot, recipe.results.size());

        if (recipe.blockResult != Blocks.AIR) {
            RenderHelper.renderBlock(
                    guiGraphics, recipe.blockResult.defaultBlockState(), 133, 30, 0, 12, RenderHelper.SINGLE_BLOCK);
            guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    Component.translatable(
                            "gui.anvilcraft.category.super_heating.convert_to", recipe.blockResult.getName()),
                    10,
                    54,
                    0xFF000000,
                    false);
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
                AnvilCraftJeiPlugin.SUPER_HEATING,
                JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.SUPER_HEATING_TYPE.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilCraftJeiPlugin.SUPER_HEATING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROYAL_ANVIL), AnvilCraftJeiPlugin.SUPER_HEATING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EMBER_ANVIL), AnvilCraftJeiPlugin.SUPER_HEATING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GIANT_ANVIL), AnvilCraftJeiPlugin.SUPER_HEATING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SPECTRAL_ANVIL), AnvilCraftJeiPlugin.SUPER_HEATING);
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), AnvilCraftJeiPlugin.SUPER_HEATING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.HEATER), AnvilCraftJeiPlugin.SUPER_HEATING);
    }
}
