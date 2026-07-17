package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.FastCookingRecipe;
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

public class FastCookingCategory extends AbstractProgressCategory<FastCookingRecipe> {
    public FastCookingCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(
                Blocks.CAULDRON.defaultBlockState(),
                Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)
            ),
            Component.translatable("gui.anvilcraft.category.fast_cooking")
        );
    }

    @Override
    public IRecipeHolderType<FastCookingRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.FAST_COOKING;
    }

    @Override
    public void draw(
        RecipeHolder<FastCookingRecipe> recipeHolder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        final FastCookingRecipe recipe = recipeHolder.value();
        this.arrowIn.draw(graphics, 54, 20);
        this.arrowOut.draw(graphics, 92, 19);
        RenderSupport.renderBlock(
            graphics,
            Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true),
            71,
            35,
            20
        );
        RenderSupport.renderBlock(
            graphics,
            CauldronUtil.fullState(recipe.getHasCauldron().getFluidCauldron()),
            71,
            25,
            20
        );
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 71, 7 + anvilYOffset, 20);

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
        RecipeHolder<FastCookingRecipe> recipeHolder,
        IRecipeSlotsView view,
        double mouseX,
        double mouseY
    ) {
        if (mouseX < 72 || mouseX > 90 || mouseY < 24 || mouseY > 43) return;
        tooltip.add(recipeHolder.value().getHasCauldron().getFluidCauldron().getName());
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.FAST_COOKING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.FAST_COOKING.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.FAST_COOKING);
        AnvilCraftJeiPlugin.addCauldronCatalysts(registration, AnvilCraftJeiPlugin.FAST_COOKING);
        registration.addCraftingStation(AnvilCraftJeiPlugin.FAST_COOKING, Items.CAMPFIRE);
    }
}
