package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.Util;
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
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BaseStampingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingDiffRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;

public class StampingCategory extends AbstractProgressCategory<BaseStampingRecipe<?>> {
    public StampingCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(Blocks.ANVIL.defaultBlockState(), ModBlocks.STAMPING_PLATFORM.getDefaultState()),
            Component.translatable("gui.anvilcraft.category.stamping")
        );
    }

    @Override
    public IRecipeHolderType<BaseStampingRecipe<?>> getRecipeType() {
        return AnvilCraftJeiPlugin.STAMPING;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BaseStampingRecipe<?>> recipeHolder, IFocusGroup focuses) {
        BaseStampingRecipe<?> recipe = recipeHolder.value();
        int requiredCount = getMultipleToOneCount(recipe);
        if (requiredCount > 0) {
            JeiSlotUtil.addSlotWithCount(builder, JeiSlotUtil.INPUT_X, JeiSlotUtil.DEFAULT_Y,
                ItemIngredientPredicate.of(BuiltInRegistries.ITEM, ModItemTags.TEMPLATES).withCount(requiredCount).build());
        } else if (recipe instanceof StampingDiffRecipe diff) {
            JeiSlotUtil.addDiffInputSlots(builder, diff.getDiffInputItems().getFirst());
        } else {
            JeiItemUtil.addDefaultInputSlots(builder, recipe.getInputItems());
        }
        JeiItemUtil.addDefaultOutputSlots(builder, recipe.getResultItems());
    }

    @Override
    public void draw(
        RecipeHolder<BaseStampingRecipe<?>> recipeHolder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        final BaseStampingRecipe<?> recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlockAt(graphics, ModBlocks.STAMPING_PLATFORM.getDefaultState(), 81, 40, 12);
        RenderSupport.renderBlockAt(graphics, Blocks.ANVIL.defaultBlockState(), 81, 22 + anvilYOffset, 12);

        this.arrowIn.draw(graphics, 54, 30);
        this.arrowOutFromBelow.draw(graphics, 92, 29);

        int requiredCount = getMultipleToOneCount(recipe);
        if (requiredCount > 0) {
            this.slotDefault.draw(graphics, JeiSlotUtil.INPUT_X - 1, JeiSlotUtil.DEFAULT_Y - 1);
            Component text = Component.translatable("jei.anvilcraft.tooltip.stamping.templates", requiredCount);
            graphics.text(Minecraft.getInstance().font, text,
                (AbstractProgressCategory.WIDTH - Minecraft.getInstance().font.width(text)) / 2,
                AbstractProgressCategory.HEIGHT - 10, 0xFF555555, false);
        } else if (recipe instanceof StampingDiffRecipe) {
            JeiSlotUtil.drawDefaultInputSlots(graphics, this.slotDefault, recipe.getDiffInputItems().size());
        } else {
            JeiSlotUtil.drawDefaultInputSlots(graphics, this.slotDefault, recipe.getInputItems().size());
        }

        JeiSlotUtil.drawDefaultOutputSlots(graphics,
            JeiRecipeUtil.outputSlotFor(recipe.getResultItems(), this.slotDefault, this.slotProbability),
            recipe.getResultItems().size());
    }

    private static int getMultipleToOneCount(BaseStampingRecipe<?> recipe) {
        for (var result : recipe.getResultItems()) {
            var item = result.stack().item().value();
            if (item == ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE.get()) return 8;
            if (item == ModItems.FOUR_TO_ONE_SMITHING_TEMPLATE.get()) return 4;
            if (item == ModItems.TWO_TO_ONE_SMITHING_TEMPLATE.get()) return 2;
        }
        return 0;
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.STAMPING,
            Util.cast(JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.STAMPING.get()))
        );
        registration.addRecipes(
            AnvilCraftJeiPlugin.STAMPING,
            Util.cast(JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.STAMPING_DIFF.get()))
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.STAMPING);
        registration.addCraftingStation(AnvilCraftJeiPlugin.STAMPING, ModBlocks.STAMPING_PLATFORM);
    }
}
