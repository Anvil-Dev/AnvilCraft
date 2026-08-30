package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BaseStampingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingDiffRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;

public class StampingCategory extends AbstractProgressCategory<BaseStampingRecipe<?>> {
    public StampingCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(Blocks.ANVIL.defaultBlockState(), ModBlocks.STAMPING_PLATFORM.getDefaultState()),
            Component.translatable("gui.anvilcraft.category.stamping")
        );
    }

    @Override
    public IRecipeHolderType<BaseStampingRecipe<?>> getRecipeType() {
        return AnvilCraftJeiPlugin.STAMPING;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BaseStampingRecipe<?>> recipeHolder, IFocusGroup focuses) {
        BaseStampingRecipe<?> recipe = recipeHolder.value();
        switch (recipe) {
            case StampingRecipe normal -> JeiSlotUtil.addInputSlots(builder, normal.getInputItems());
            case StampingDiffRecipe diff -> JeiSlotUtil.addDiffInputSlots(builder, diff.getDiffInputItems().getFirst());
            default -> {}
        }
        JeiSlotUtil.addOutputSlots(builder, recipe.getResultItems());
    }

    @Override
    public void draw(
        RecipeHolder<BaseStampingRecipe<?>> recipeHolder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        final BaseStampingRecipe<?> recipe = recipeHolder.value();
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, ModBlocks.STAMPING_PLATFORM.getDefaultState(), 71, 35, 20);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 71, 17 + anvilYOffset, 20);

        this.arrowIn.draw(graphics, 54, 30);
        this.arrowOutFromBelow.draw(graphics, 92, 29);

        if (recipe instanceof StampingDiffRecipe) {
            JeiSlotUtil.drawDefaultInputSlots(graphics, this.slotDefault, recipe.getDiffInputItems().size());
        } else {
            JeiSlotUtil.drawDefaultInputSlots(graphics, this.slotDefault, recipe.getInputItems().size());
        }

        if (JeiRecipeUtil.isChance(recipe.getResultItems())) {
            JeiSlotUtil.drawDefaultOutputSlots(graphics, this.slotProbability, recipe.getResultItems().size());
        } else {
            JeiSlotUtil.drawDefaultOutputSlots(graphics, this.slotDefault, recipe.getResultItems().size());
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.STAMPING,
            Util.cast(JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.STAMPING.get()))
        );
        registration.addRecipes(
            AnvilCraftJeiPlugin.STAMPING,
            Util.cast(JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.STAMPING_DIFF.get()))
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.STAMPING);
        registration.addCraftingStation(AnvilCraftJeiPlugin.STAMPING, ModBlocks.STAMPING_PLATFORM);
    }
}
