package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.TooltipUtil;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BoilingRecipe;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;

public class BoilingCategory extends AbstractProgressCategory<BoilingRecipe> {
    public BoilingCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(
                CauldronUtil.fullState(Blocks.WATER_CAULDRON),
                Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)
            ),
            Component.translatable("gui.anvilcraft.category.boiling")
        );
    }

    @Override
    public IRecipeHolderType<BoilingRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.BOILING;
    }

    @Override
    public void draw(
        RecipeHolder<BoilingRecipe> recipeHolder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        final BoilingRecipe recipe = recipeHolder.value();
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 81, 12 + anvilYOffset, 12);
        RenderSupport.renderBlock(graphics, CauldronUtil.fullState(Blocks.WATER_CAULDRON), 81, 30, 12);
        RenderSupport.renderBlock(graphics, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 81, 40, 12);

        this.arrowIn.draw(graphics, 54, 20);
        this.arrowOutFromBelow.draw(graphics, 92, 19);

        JeiSlotUtil.drawInputSlots(graphics, this.slotDefault, recipe.getInputItems().size());
        if (JeiRecipeUtil.isChance(recipe.getResultItems())) {
            JeiSlotUtil.drawOutputSlots(graphics, this.slotProbability, recipe.getResultItems().size());
        } else {
            JeiSlotUtil.drawOutputSlots(graphics, this.slotDefault, recipe.getResultItems().size());
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<BoilingRecipe> recipeHolder,
        IRecipeSlotsView view,
        double mouseX,
        double mouseY
    ) {
        super.getTooltip(tooltip, recipeHolder, view, mouseX, mouseY);
        if (MathUtil.isInRange(mouseX, mouseY, 72, 34, 90, 53)) {
            tooltip.addAll(TooltipUtil.tooltip(Blocks.WATER_CAULDRON));
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.BOILING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.BOILING.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.BOILING);
        AnvilCraftJeiPlugin.addCauldronCatalysts(registration, AnvilCraftJeiPlugin.BOILING);
        registration.addCraftingStation(AnvilCraftJeiPlugin.BOILING, Items.CAMPFIRE);
    }
}
