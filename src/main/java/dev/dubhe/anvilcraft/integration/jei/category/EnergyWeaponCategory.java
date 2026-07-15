package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.EnergyWeaponMakeRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

public class EnergyWeaponCategory implements IRecipeCategory<RecipeHolder<EnergyWeaponMakeRecipe>> {

    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final Component title;

    private final IDrawable arrow;

    public EnergyWeaponCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemLike(ModItems.ENERGY_WEAPON_PLATFORM);
        slotDefault = JeiRenderHelper.getSlotDefault(helper);
        title = Component.translatable("gui.anvilcraft.category.energy_weapon");

        arrow = JeiRenderHelper.getArrowDefault(helper);
    }

    @Override
    public RecipeType<RecipeHolder<EnergyWeaponMakeRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.ENERGY_WEAPON;
    }

    @Override
    public Component getTitle() {
        return title;
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
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder, RecipeHolder<EnergyWeaponMakeRecipe> recipeHolder, IFocusGroup focuses) {
        EnergyWeaponMakeRecipe recipe = recipeHolder.value();
        ArrayList<ItemIngredientPredicate> ingredients = new ArrayList<>(recipe.ingredients());
        ingredients.addFirst(ItemIngredientPredicate.of(ModItems.ENERGY_WEAPON_PLATFORM).build());
        JeiSlotUtil.addInputSlots(builder, ingredients);
        JeiSlotUtil.addOutputSlots(builder, List.of(ChanceItemStack.of(recipe.result())));
    }

    @Override
    public void draw(
        RecipeHolder<EnergyWeaponMakeRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        arrow.draw(guiGraphics, 80, 27);
        JeiSlotUtil.drawInputSlots(guiGraphics, slotDefault, recipeHolder.value().ingredients().size() + 1);
        JeiSlotUtil.drawOutputSlots(guiGraphics, slotDefault, 1);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<EnergyWeaponMakeRecipe>> recipes =
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.ENERGY_WEAPON_MAKE_TYPE.get());
        registration.addRecipes(AnvilCraftJeiPlugin.ENERGY_WEAPON, recipes);
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModItems.ENERGY_WEAPON_PLATFORM, AnvilCraftJeiPlugin.ENERGY_WEAPON);
    }
}
