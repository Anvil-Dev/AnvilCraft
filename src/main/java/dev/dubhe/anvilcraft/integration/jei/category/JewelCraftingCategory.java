package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.TextureConstants;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.common.gui.elements.DrawableText;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.util.Lazy;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class JewelCraftingCategory implements IRecipeCategory<RecipeHolder<JewelCraftingRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final Lazy<IDrawable> background;
    private final IDrawable progress;
    private final IDrawable icon;
    private final IDrawable slot;
    private final Component title;

    public JewelCraftingCategory(IGuiHelper helper) {
        background = Lazy.of(() -> helper.createBlankDrawable(WIDTH, HEIGHT));
        progress = helper.drawableBuilder(TextureConstants.PROGRESS, 0, 0, 24, 16)
            .setTextureSize(24, 16)
            .build();
        icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.JEWEL_CRAFTING_TABLE));
        slot = helper.getSlotDrawable();
        title = Component.translatable("gui.anvilcraft.category.jewel_crafting");
    }

    @Override
    public RecipeType<RecipeHolder<JewelCraftingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.JEWEL_CRAFTING;
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<JewelCraftingRecipe> recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 59, 11).addItemStack(recipe.value().result.copyWithCount(1));
        for (int i = 0; i < recipe.value().mergedIngredients.size(); i++) {
            var entry = recipe.value().mergedIngredients.get(i);
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, 5 + i * 18, 37)
                .addIngredients(entry.getKey());
            if (entry.getIntValue() > 1) {
                slot.setOverlay(new DrawableText("" + entry.getIntValue(), 2, 2, 0xFFFFFFFF), 12, 12);
            }
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 135, 24).addItemStack(recipe.value().result.copy());
    }

    @Override
    public void draw(RecipeHolder<JewelCraftingRecipe> recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // source
        slot.draw(guiGraphics, 58, 10);
        // result
        slot.draw(guiGraphics, 134, 23);
        // input
        for (int i = 0; i < 4; i++) {
            slot.draw(guiGraphics, 4 + i * 18, 36);
        }
        progress.draw(guiGraphics, 95, 24);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.JEWEL_CRAFTING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.JEWEL_CRAFTING_TYPE.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.JEWEL_CRAFTING_TABLE), AnvilCraftJeiPlugin.JEWEL_CRAFTING);
    }
}
