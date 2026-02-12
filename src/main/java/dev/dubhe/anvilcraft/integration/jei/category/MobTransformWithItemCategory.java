package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformWithItemRecipe;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MobTransformWithItemCategory implements IRecipeCategory<RecipeHolder<MobTransformWithItemRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;
    private static final String KEY_CATEGORY = "gui.anvilcraft.category.mob_transform_with_item";
    private static final String KEY_CHANCE = "gui.anvilcraft.category.mob_transform_with_item.chance_per_item";
    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final IDrawable slotProbability;
    private final Component title;
    private final IDrawable arrowDefault;

    public MobTransformWithItemCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(ModBlocks.CORRUPTED_BEACON.asStack());
        slotDefault = JeiRenderHelper.getSlotDefault(helper);
        slotProbability = JeiRenderHelper.getSlotProbability(helper);
        title = Component.translatable(KEY_CATEGORY);

        arrowDefault = JeiRenderHelper.getArrowDefault(helper);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.MOB_TRANSFORM_WITH_ITEM,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MOB_TRANSFORM_WITH_ITEM_TYPE.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CORRUPTED_BEACON), AnvilCraftJeiPlugin.MOB_TRANSFORM_WITH_ITEM);
    }

    @Override
    public RecipeType<RecipeHolder<MobTransformWithItemRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.MOB_TRANSFORM_WITH_ITEM;
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
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<MobTransformWithItemRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        MobTransformWithItemRecipe recipe = recipeHolder.value();

        SpawnEggItem spawnEggItemInput = SpawnEggItem.byId(recipe.input());
        if (spawnEggItemInput != null) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemLike(spawnEggItemInput);
        }

        JeiSlotUtil.addInputSlots(builder, recipe.itemIngredients());

        SpawnEggItem spawnEggItemOutput = SpawnEggItem.byId(recipe.specialResult().resultEntityType());
        if (spawnEggItemOutput != null) {
            String name = recipe.specialResult().resultEntityType().toShortString();
            ItemStack x = Items.BARRIER.getDefaultInstance();
            x.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(x);
        }

        outputStacks.add(ChanceItemStack.of(recipe.value().itemResult().copyWithCount(1)));
        JeiSlotUtil.addOutputSlots(builder, recipe.itemResult());

        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST)
            .addItemStack(ModBlocks.CORRUPTED_BEACON.asStack());
    }

    @Override
    public void draw(
        RecipeHolder<MobTransformWithItemRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        MobTransformWithItemRecipe recipe = recipeHolder.value();

    }
}

