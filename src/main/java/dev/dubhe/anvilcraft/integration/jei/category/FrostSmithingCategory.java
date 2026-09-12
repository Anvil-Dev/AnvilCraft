package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.frost.DeformationRecipe;
import dev.dubhe.anvilcraft.recipe.frost.IFrostSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.frost.PermutationRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public class FrostSmithingCategory implements IRecipeCategory<FrostSmithingCategory.Display> {
    private final IDrawable icon;
    private final IDrawable arrow;

    public FrostSmithingCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(ModBlocks.FROST_SMITHING_TABLE.asStack());
        this.arrow = JeiRenderHelper.getArrowDefault(helper);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<IFrostSmithingRecipe> recipes = new ArrayList<>();
        JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.PERMUTATION_TYPE.get())
            .forEach(holder -> recipes.add(holder.value()));
        JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.DEFORMATION_TYPE.get())
            .forEach(holder -> recipes.add(holder.value()));
        List<Display> displays = new ArrayList<>();
        for (IFrostSmithingRecipe recipe : recipes) {
            if (recipe.inputs().size() < 2) continue;
            recipe.inputs().forEach(input -> displays.add(new Display(recipe, input.result().getDefaultInstance())));
        }
        registration.addRecipes(AnvilCraftJeiPlugin.FROST_SMITHING, displays);
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.FROST_SMITHING_TABLE.asStack(), AnvilCraftJeiPlugin.FROST_SMITHING);
        registration.addRecipeCatalyst(ModBlocks.TRANSCENDENCE_SMITHING_TABLE.asStack(), AnvilCraftJeiPlugin.FROST_SMITHING);
    }

    @Override
    public RecipeType<Display> getRecipeType() {
        return AnvilCraftJeiPlugin.FROST_SMITHING;
    }

    @Override
    public Component getTitle() {
        return ModBlocks.FROST_SMITHING_TABLE.get().getName();
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return 144;
    }

    @Override
    public int getHeight() {
        return 36;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Display display, IFocusGroup focuses) {
        IFrostSmithingRecipe recipe = display.recipe();
        Ingredient template;
        Ingredient material;
        if (recipe instanceof PermutationRecipe permutation) {
            template = Ingredient.of(permutation.template().getItems());
            material = Ingredient.of(permutation.material().getItems());
        } else if (recipe instanceof DeformationRecipe deformation) {
            template = Ingredient.of(deformation.template().getItems());
            material = Ingredient.of(DeformationRecipe.DEFAULT_MATERIAL.getItems());
        } else {
            return;
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, 1, 10).setStandardSlotBackground().addIngredients(template)
            .addRichTooltipCallback((slot, tooltip) -> tooltip.add(
                Component.translatable("jei.anvilcraft.tooltip.not_consumed").withStyle(ChatFormatting.GOLD)
            ));
        builder.addSlot(RecipeIngredientRole.INPUT, 37, 10).setStandardSlotBackground().addIngredients(material);
        builder.addSlot(RecipeIngredientRole.INPUT, 55, 10).setStandardSlotBackground().addItemStack(display.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 123, 10).setStandardSlotBackground().addItemStacks(
            recipe.inputs(display.input()).stream().map(result -> result.result().getDefaultInstance()).toList()
        );
    }

    @Override
    public void draw(Display recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        this.arrow.draw(graphics, 88, 9);
    }

    public record Display(IFrostSmithingRecipe recipe, ItemStack input) {
    }
}
