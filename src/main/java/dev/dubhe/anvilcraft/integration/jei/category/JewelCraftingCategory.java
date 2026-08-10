package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import dev.dubhe.anvilcraft.recipe.generate.JewelCraftingRecipeGeneratingCache;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.common.util.RegistryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        return JewelCraftingCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return JewelCraftingCategory.HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<JewelCraftingRecipe> recipe, IFocusGroup focuses) {
        List<ItemStack> source = RecipeUtil.getItems(
            recipe.value().source(),
            RegistryUtil.getRegistryAccess().lookupOrThrow(Registries.ITEM)
        );
        builder.addSlot(RecipeIngredientRole.INPUT, 59, 11).addItemStacks(source);
        for (int i = 0; i < recipe.value().ingredients().size(); i++) {
            JeiSlotUtil.addSlotWithCount(builder, 5 + i * 18, 37, recipe.value().ingredients().get(i));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 135, 24).addItemStacks(source);
    }

    @Override
    public void draw(
        RecipeHolder<JewelCraftingRecipe> recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        // result
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
        List<RecipeHolder<JewelCraftingRecipe>> recipes = new ArrayList<>(
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.JEWEL_CRAFTING.get())
        );
        Set<ResourceKey<Recipe<?>>> recipeIds = new HashSet<>();
        recipes.forEach(recipe -> recipeIds.add(recipe.id()));
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            new JewelCraftingRecipeGeneratingCache(connection.registryAccess()).buildRecipes()
                .ifPresent(generated -> generated.stream()
                    .filter(recipe -> recipeIds.add(recipe.id()))
                    .forEach(recipes::add));
        }
        registration.addRecipes(
            AnvilCraftJeiPlugin.JEWEL_CRAFTING,
            recipes
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AnvilCraftJeiPlugin.JEWEL_CRAFTING, ModBlocks.JEWEL_CRAFTING_TABLE);
    }
}
