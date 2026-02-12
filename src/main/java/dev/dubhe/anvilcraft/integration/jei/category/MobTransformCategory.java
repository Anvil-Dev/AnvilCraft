package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformRecipe;
import dev.dubhe.anvilcraft.recipe.transform.TransformResult;
import dev.dubhe.anvilcraft.util.TooltipUtil;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MobTransformCategory implements IRecipeCategory<RecipeHolder<MobTransformRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private static final String KEY_CATEGORY = "gui.anvilcraft.category.mob_transform";
    private final IDrawable icon;
    private final Component title;
    private final IDrawable arrowOut;

    public MobTransformCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(ModBlocks.CORRUPTED_BEACON.asStack());
        title = Component.translatable(KEY_CATEGORY);
        arrowOut = JeiRenderHelper.getArrowOutput(helper);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.MOB_TRANSFORM,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MOB_TRANSFORM_TYPE.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CORRUPTED_BEACON), AnvilCraftJeiPlugin.MOB_TRANSFORM);
    }

    @Override
    public RecipeType<RecipeHolder<MobTransformRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.MOB_TRANSFORM;
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
        RecipeHolder<MobTransformRecipe> recipe,
        IFocusGroup focuses
    ) {
        Ingredient inputIngredient;
        SpawnEggItem spawnEggItemInput = SpawnEggItem.byId(recipe.value().input());
        if (spawnEggItemInput == null) {
            String name = recipe.value().input().toShortString();
            ItemStack x = Items.BARRIER.getDefaultInstance();
            x.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            inputIngredient = Ingredient.of(x);
        } else {
            inputIngredient = Ingredient.of(spawnEggItemInput);
        }

        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addIngredients(inputIngredient);

        for (TransformResult result : recipe.value().results()) {
            SpawnEggItem spawnEggOutput = SpawnEggItem.byId(result.resultEntityType());
            if (spawnEggOutput == null) {
                ItemStack x = Items.BARRIER.getDefaultInstance();
                builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(x);
            } else {
                builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemLike(spawnEggOutput);
            }
        }
    }

    @Override
    public void draw(
        RecipeHolder<MobTransformRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        MobTransformRecipe recipe = recipeHolder.value();

        RenderSupport.renderBlock(
            guiGraphics,
            ModBlocks.CORRUPTED_BEACON.getDefaultState(),
            50,
            40,
            10,
            16,
            RenderSupport.SINGLE_BLOCK
        );
        arrowOut.draw(guiGraphics, 55, 23);

        RenderSupport.renderEntityFollowsMouse(recipe.input(), guiGraphics, 40, 25, WIDTH, HEIGHT, 42, mouseX, mouseY);


        List<TransformResult> results = recipe.results();
        for (int i = 0; i < results.size(); i++) {
            TransformResult result = recipe.results().get(i);
            EntityType<?> entityType = result.resultEntityType();
            if (results.size() == 1) {
                RenderSupport.renderEntityFollowsMouse(entityType, guiGraphics, 250, 25, WIDTH, HEIGHT, 40, mouseX, mouseY);
            } else {
                RenderSupport.renderEntityFollowsMouse(entityType, guiGraphics, 175 + i * 50, 25, WIDTH, HEIGHT, 42, mouseX, mouseY);
            }
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<MobTransformRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        IRecipeCategory.super.getTooltip(
            tooltip,
            recipeHolder,
            recipeSlotsView,
            mouseX, mouseY
        );

        MobTransformRecipe recipe =  recipeHolder.value();

        if (mouseX >= 2 && mouseX <= 38) {
            if (mouseY >= 2 && mouseY <= 60) {
                tooltip.addAll(TooltipUtil.entityTypeTooltip(recipe.input()));
            }
        }

        if (mouseX >= 60 && mouseX <= 162) {
            if (mouseY >= 2 && mouseY <= 60) {
                tooltip.addAll(TooltipUtil.transListTooltip(recipe.results()));
                tooltip.addAll(TooltipUtil.recipeIDTooltip(recipeHolder.id()));
            }
        }
    }
}
