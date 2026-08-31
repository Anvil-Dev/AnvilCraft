package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCrushRecipe;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;

public class ItemCrushCategory extends AbstractProgressCategory<ItemCrushRecipe> {
    public ItemCrushCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(Blocks.ANVIL.defaultBlockState(), ModBlocks.CRUSHING_TABLE.getDefaultState()),
            Component.translatable("gui.anvilcraft.category.item_crush")
        );
    }

    @Override
    public IRecipeHolderType<ItemCrushRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.ITEM_CRUSH;
    }

    @Override
    public void draw(
        RecipeHolder<ItemCrushRecipe> recipeHolder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        final ItemCrushRecipe recipe = recipeHolder.value();
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, ModBlocks.CRUSHING_TABLE.getDefaultState(), 71, 35, 20);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 71, 17 + anvilYOffset, 20);

        this.arrowIn.draw(graphics, 54, 30);
        this.arrowOutFromBelow.draw(graphics, 92, 29);

        JeiSlotUtil.drawDefaultInputSlots(graphics, this.slotDefault, recipe.getInputItems().size());
        if (JeiRecipeUtil.isChance(recipe.getResultItems())) {
            JeiSlotUtil.drawDefaultOutputSlots(graphics, this.slotProbability, recipe.getResultItems().size());
        } else {
            JeiSlotUtil.drawDefaultOutputSlots(graphics, this.slotDefault, recipe.getResultItems().size());
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.ITEM_CRUSH,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.ITEM_CRUSH.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.ITEM_CRUSH);
        registration.addCraftingStation(AnvilCraftJeiPlugin.ITEM_CRUSH, ModBlocks.CRUSHING_TABLE);
    }
}
