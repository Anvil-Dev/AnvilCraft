package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.common.gui.elements.DrawableText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

public class JewelCraftingCategory implements IRecipeCategory<RecipeHolder<JewelCraftingRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable arrowDefault;
    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final Component title;

    public JewelCraftingCategory(IGuiHelper helper) {
        this.arrowDefault = JeiRenderHelper.getArrowDefault(helper);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.JEWEL_CRAFTING_TABLE));
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.title = Component.translatable("gui.anvilcraft.category.jewel_crafting");
    }

    @Override
    public IRecipeHolderType<JewelCraftingRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.JEWEL_CRAFTING;
    }

    @Override
    public Component getTitle() {
        return this.title;
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
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<JewelCraftingRecipe> recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 59, 11).add(recipe.value().result.create().copyWithCount(1));
        for (int i = 0; i < recipe.value().mergedIngredients.size(); i++) {
            var entry = recipe.value().mergedIngredients.get(i);
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, 5 + i * 18, 37).add(entry.getKey());
            if (entry.getIntValue() > 1) {
                slot.setOverlay(new DrawableText("" + entry.getIntValue(), 2, 2, 0xFFFFFFFF), 12, 12);
            }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 135, 24).add(recipe.value().result.create());
    }

    @Override
    public void draw(
        RecipeHolder<JewelCraftingRecipe> recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        // source
        this.slotDefault.draw(graphics, 58, 10);
        // result
        this.slotDefault.draw(graphics, 134, 23);
        // input
        for (int i = 0; i < 4; i++) {
            this.slotDefault.draw(graphics, 4 + i * 18, 36);
        }
        this.arrowDefault.draw(graphics, 100, 27);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.JEWEL_CRAFTING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.JEWEL_CRAFTING.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AnvilCraftJeiPlugin.JEWEL_CRAFTING, ModBlocks.JEWEL_CRAFTING_TABLE);
    }
}
