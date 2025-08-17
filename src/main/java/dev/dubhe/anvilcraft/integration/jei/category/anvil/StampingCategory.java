package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingRecipe;
import dev.dubhe.anvilcraft.util.RenderHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class StampingCategory extends AbstractProgressCategory<StampingRecipe> {
    public StampingCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(Blocks.ANVIL.defaultBlockState(), ModBlocks.STAMPING_PLATFORM.getDefaultState()),
            Component.translatable("gui.anvilcraft.category.stamping")
        );
    }

    @Override
    public RecipeType<RecipeHolder<StampingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.STAMPING;
    }

    @Override
    public void draw(
        RecipeHolder<StampingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        StampingRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderHelper.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            22 + anvilYOffset,
            20,
            12,
            RenderHelper.SINGLE_BLOCK);
        RenderHelper.renderBlock(
            guiGraphics, ModBlocks.STAMPING_PLATFORM.getDefaultState(), 81, 40, 0, 12, RenderHelper.SINGLE_BLOCK);

        arrowIn.draw(guiGraphics, 54, 32);
        arrowOut.draw(guiGraphics, 92, 31);

        // TODO: 等待重构StampingUniqueItemsRecipe（目前仅多合一模板使用），重构后直接取消注释并修复import即可
//        if (recipe instanceof StampingUniqueItemsRecipe) {
//            ItemStack input = recipe.getItemIngredients().getFirst()
//                .getItems()[(int) System.currentTimeMillis() / 1000 % recipe.getItemIngredients().size()];
//            JeiSlotUtil.drawInputSlots(guiGraphics, slot, input.getCount());
//        } else {
        JeiSlotUtil.drawInputSlots(guiGraphics, slot, recipe.getInputItems().size());
//        }

        JeiSlotUtil.drawOutputSlots(guiGraphics, slot, this.getResults(recipe).size());
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.STAMPING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.STAMPING_TYPE.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilCraftJeiPlugin.STAMPING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROYAL_ANVIL), AnvilCraftJeiPlugin.STAMPING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EMBER_ANVIL), AnvilCraftJeiPlugin.STAMPING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GIANT_ANVIL), AnvilCraftJeiPlugin.STAMPING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SPECTRAL_ANVIL), AnvilCraftJeiPlugin.STAMPING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.STAMPING_PLATFORM), AnvilCraftJeiPlugin.STAMPING);
    }
}
