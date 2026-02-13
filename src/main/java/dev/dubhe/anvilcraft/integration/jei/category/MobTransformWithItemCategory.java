package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformWithItemRecipe;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class MobTransformWithItemCategory implements IRecipeCategory<RecipeHolder<MobTransformWithItemRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private static final String KEY_CATEGORY = "gui.anvilcraft.category.mob_transform_with_item";

    private final IDrawable icon;
    private final IDrawable arrowOut;
    private final Component title;

    public MobTransformWithItemCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(ModBlocks.CORRUPTED_BEACON.asStack());
        title = Component.translatable(KEY_CATEGORY);
        arrowOut = JeiRenderHelper.getArrowOutput(helper);
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

        recipe.itemIngredients()
            .forEach(
                ingredientPredicate -> builder.addInputSlot(30, 5)
                .addItemStacks(
                    Arrays.stream(ingredientPredicate.getItems()).toList()
                )
                    .addRichTooltipCallback(
                    (recipeSlotView, tooltip) -> tooltip.add(Component.translatable(""))
                        )
            );

        SpawnEggItem spawnEggItemOutput = SpawnEggItem.byId(recipe.specialResult().resultEntityType());
        if (spawnEggItemOutput != null) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemLike(spawnEggItemOutput);
        }

        builder
            .addOutputSlot(130, 20)
            .addItemStack(recipe.itemResult())
            .addRichTooltipCallback(
                (view, tooltip) -> tooltip.add(
                    Component
                        .translatable(
                        "gui.anvilcraft.category.mob_transform_with_item.chance_per_item", recipe.chancePercentPerItem()
                        )
                        .withStyle(ChatFormatting.GRAY)
                )
            );
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

        RenderSupport.renderEntityWithItemFollowsMouse(
            recipe.input(),
            recipe.itemIngredients().getFirst().getItems()[0],
            guiGraphics,
            40,
            25,
            WIDTH,
            HEIGHT,
            42,
            mouseX,
            mouseY
        );

        RenderSupport.renderEntityWithItemFollowsMouse(
            recipe.specialResult().resultEntityType(),
            recipe.itemResult(),
            guiGraphics,
            230,
            50,
            WIDTH,
            HEIGHT,
            48,
            mouseX,
            mouseY
        );
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<MobTransformWithItemRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        IRecipeCategory.super.getTooltip(tooltip, recipeHolder, recipeSlotsView, mouseX, mouseY);

        MobTransformWithItemRecipe recipe = recipeHolder.value();

        if (mouseX >= 2 && mouseX <= 38) {
            if (mouseY >= 2 && mouseY <= 60) {
                tooltip.addAll(TooltipUtil.entityTypeTooltip(recipe.input()));
            }
        }

        if (mouseX >= 90 && mouseX <= 130) {
            if (mouseY >= 2 && mouseY <= 60) {
                tooltip.addAll(TooltipUtil.entityTypeTooltip(recipe.specialResult().resultEntityType()));
                tooltip.add(
                    Component
                        .translatable("gui.anvilcraft.category.mob_transform_with_item.chance_per_item", recipe.chancePercentPerItem())
                        .withStyle(ChatFormatting.GRAY)
                );
                tooltip.addAll(TooltipUtil.recipeIDTooltip(recipeHolder.id()));
            }
        }
    }
}

