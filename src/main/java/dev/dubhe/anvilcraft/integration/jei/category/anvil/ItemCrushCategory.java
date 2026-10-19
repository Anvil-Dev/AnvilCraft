package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

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
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlockAt(graphics, ModBlocks.CRUSHING_TABLE.getDefaultState(), 81, 40, 12);
        RenderSupport.renderBlockAt(graphics, Blocks.ANVIL.defaultBlockState(), 81, 22 + anvilYOffset, 12);

        this.arrowIn.draw(graphics, 54, 30);
        this.arrowOutFromBelow.draw(graphics, 92, 29);

        JeiSlotUtil.drawDefaultInputSlots(graphics, this.slotDefault, recipe.getInputItems().size());
        JeiSlotUtil.drawDefaultOutputSlots(graphics,
            JeiRecipeUtil.outputSlotFor(recipe.getResultItems(), this.slotDefault, this.slotProbability),
            recipe.getResultItems().size());
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<ItemCrushRecipe>> recipes = new ArrayList<>(
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.ITEM_CRUSH.get())
        );
        recipes.add(new RecipeHolder<>(
            ResourceKey.create(Registries.RECIPE, AnvilCraft.of("item_crush/item_crush/gem_from_chromatic_stone")),
            chromaticStoneRecipe()
        ));
        registration.addRecipes(AnvilCraftJeiPlugin.ITEM_CRUSH, recipes);
    }

    private static ItemCrushRecipe chromaticStoneRecipe() {
        return ItemCrushRecipe.builder()
            .requires(ModBlocks.CHROMATIC_STONE)
            .result(ModItems.RUBY, 0.25f)
            .result(ModItems.TOPAZ, 0.25f)
            .result(ModItems.SAPPHIRE, 0.25f)
            .result(Items.EMERALD, 0.25f)
            .result(Items.QUARTZ, 3)
            .buildRecipe();
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.ITEM_CRUSH);
        registration.addCraftingStation(AnvilCraftJeiPlugin.ITEM_CRUSH, ModBlocks.CRUSHING_TABLE);
    }
}
