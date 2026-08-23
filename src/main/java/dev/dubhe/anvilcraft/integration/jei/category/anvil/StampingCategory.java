package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiItemUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.StampingUniqueItemsRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StampingCategory extends AbstractProgressCategory<StampingRecipe> {
    public StampingCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(Blocks.ANVIL.defaultBlockState(), ModBlocks.STAMPING_PLATFORM.getDefaultState()),
            Component.translatable("gui.anvilcraft.category.stamping")
        );
    }

    @Override
    public RecipeType<RecipeHolder<StampingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.STAMPING;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<StampingRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        Integer requiredCount = getMultipleToOneCount(recipeHolder);
        if (requiredCount == null) {
            super.setRecipe(builder, recipeHolder, focuses);
            return;
        }

        List<ItemStack> templates = Arrays.stream(Ingredient.of(ModItemTags.TEMPLATES).getItems())
            .map(template -> template.copyWithCount(requiredCount))
            .toList();
        builder.addSlot(RecipeIngredientRole.INPUT, JeiSlotUtil.INPUT_X, JeiSlotUtil.DEFAULT_Y)
            .addItemStacks(templates);
        JeiItemUtil.addDefaultOutputSlots(builder, recipeHolder.value().getResultItems());
    }

    @Override
    public void draw(
        RecipeHolder<StampingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        final StampingRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderSupport.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            22 + anvilYOffset,
            20,
            12,
            RenderSupport.SINGLE_BLOCK);
        RenderSupport.renderBlock(
            guiGraphics, ModBlocks.STAMPING_PLATFORM.getDefaultState(), 81, 40, 0, 12, RenderSupport.SINGLE_BLOCK);

        arrowIn.draw(guiGraphics, 54, 30);
        arrowOutputFromBelow.draw(guiGraphics, 92, 29);

        if (getMultipleToOneCount(recipeHolder) != null) {
            slotDefault.draw(guiGraphics, JeiSlotUtil.INPUT_X - 1, JeiSlotUtil.DEFAULT_Y - 1);
            Component text = Component.translatable(
                "jei.anvilcraft.tooltip.stamping.templates",
                getMultipleToOneCount(recipeHolder)
            );
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                text,
                (AbstractProgressCategory.WIDTH - Minecraft.getInstance().font.width(text)) / 2,
                AbstractProgressCategory.HEIGHT - 10,
                0xFF555555,
                false
            );
        } else {
            JeiSlotUtil.drawDefaultInputSlots(guiGraphics, slotDefault, recipe.getInputItems().size());
        }

        if (JeiRecipeUtil.isChance(recipe.getResultItems())) {
            JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, slotProbability, recipe.getResultItems().size());
        } else {
            JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, slotDefault, recipe.getResultItems().size());
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<StampingRecipe>> recipes = new ArrayList<>(
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.STAMPING_TYPE.get())
        );
        for (RecipeHolder<StampingUniqueItemsRecipe> holder : JeiRecipeUtil.getRecipeHoldersFromType(
            ModRecipeTypes.STAMPING_UNIQUE_ITEMS_TYPE.get()
        )) {
            recipes.add(new RecipeHolder<>(holder.id(), toDisplayRecipe(holder.value())));
        }
        registration.addRecipes(
            AnvilCraftJeiPlugin.STAMPING,
            recipes);
    }

    private static StampingRecipe toDisplayRecipe(StampingUniqueItemsRecipe recipe) {
        List<ItemIngredientPredicate> inputs = recipe.getIngredients()
            .stream()
            .map(StampingCategory::toDisplayIngredient)
            .toList();
        return new StampingRecipe(inputs, recipe.getResults());
    }

    private static ItemIngredientPredicate toDisplayIngredient(Ingredient ingredient) {
        return ItemIngredientPredicate.Builder.item()
            .of(ingredient.getItems()[0])
            .build();
    }

    private static @Nullable Integer getMultipleToOneCount(RecipeHolder<StampingRecipe> recipeHolder) {
        List<ChanceItemStack> results = recipeHolder.value().getResultItems();
        if (results.stream().anyMatch(result -> result.stack().is(ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE.get()))) return 8;
        if (results.stream().anyMatch(result -> result.stack().is(ModItems.FOUR_TO_ONE_SMITHING_TEMPLATE.get()))) return 4;
        if (results.stream().anyMatch(result -> result.stack().is(ModItems.TWO_TO_ONE_SMITHING_TEMPLATE.get()))) return 2;
        return null;
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.STAMPING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.STAMPING_PLATFORM), AnvilCraftJeiPlugin.STAMPING);
    }
}
