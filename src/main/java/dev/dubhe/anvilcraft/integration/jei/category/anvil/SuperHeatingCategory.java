package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.util.RenderHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SuperHeatingCategory extends AbstractProgressCategory<SuperHeatingRecipe> {
    public SuperHeatingCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(Blocks.CAULDRON.defaultBlockState(),
                ModBlocks.HEATER.getDefaultState().setValue(HeaterBlock.OVERLOAD, false)),
            Component.translatable("gui.anvilcraft.category.super_heating")
        );
    }

    @Override
    public RecipeType<RecipeHolder<SuperHeatingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.SUPER_HEATING;
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

        JeiSlotUtil.drawInputSlots(guiGraphics, slot, recipe.getInputItems().size());
        JeiSlotUtil.drawOutputSlots(guiGraphics, slot, this.getResults(recipe).size());

        if (!recipe.getResultBlocks().isEmpty()) {
            BlockState result = recipe.getResultBlocks()
                .get((int) ((System.currentTimeMillis() / 1000) % recipe.getResultBlocks().size())).getState();
            RenderHelper.renderBlock(guiGraphics, result, 133, 30, 0, 12, RenderHelper.SINGLE_BLOCK);
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("gui.anvilcraft.category.super_heating.convert_to", result.getBlock().getName()),
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
