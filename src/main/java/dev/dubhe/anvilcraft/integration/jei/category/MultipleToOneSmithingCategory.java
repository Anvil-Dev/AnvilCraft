package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.recipe.multiple.BaseMultipleToOneSmithingRecipe;
import lombok.Getter;
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
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultipleToOneSmithingCategory implements
    IRecipeCategory<RecipeHolder<BaseMultipleToOneSmithingRecipe>> {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 64;

    private static final ResourceLocation BACKGROUND =
        AnvilCraft.of("textures/gui/container/smithing/background/multiple_to_one_smithing_jei.png");
    private static final ResourceLocation DISABLED_SLOT =
        AnvilCraft.of("textures/gui/container/machine/disabled_slot.png");
    private static final Component TOOLTIP_NOT_CONSUMED =
        Component.translatable("jei.anvilcraft.tooltip.not_consumed").withStyle(ChatFormatting.GOLD);

    private final IDrawable background;
    @Getter
    private final IDrawable icon;
    private final IDrawable disabledSlotIcon;
    @Getter
    private final Component title;

    private static final int CENTER_INPUT_X = 80;
    private static final int CENTER_INPUT_Y = 23;
    private static final int OUTPUT_X = 151;
    private static final int OUTPUT_Y = 35;
    private static final int TEMPLATE_X = 8;
    private static final int TEMPLATE_Y = 35;
    private static final int[] INPUT_X = {80, 80, 62, 98, 62, 98, 62, 98};
    private static final int[] INPUT_Y = {5, 41, 23, 23, 5, 5, 41, 41};

    public MultipleToOneSmithingCategory(IGuiHelper helper) {
        this.background = helper.drawableBuilder(BACKGROUND, 0, 0, WIDTH, HEIGHT)
            .setTextureSize(WIDTH, HEIGHT)
            .build();
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.EMBER_SMITHING_TABLE));
        this.disabledSlotIcon = helper.drawableBuilder(DISABLED_SLOT, 0, 0, 16, 16)
            .setTextureSize(16, 16)
            .build();
        this.title = Component.translatable("gui.anvilcraft.category.multiple_to_one_smithing");
    }

    @Override
    public RecipeType<RecipeHolder<BaseMultipleToOneSmithingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.MULTIPLE_TO_ONE_SMITHING;
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BaseMultipleToOneSmithingRecipe> recipe, IFocusGroup focuses) {
        BaseMultipleToOneSmithingRecipe smithingRecipe = recipe.value();
        builder.addSlot(RecipeIngredientRole.CATALYST, TEMPLATE_X, TEMPLATE_Y)
            .addIngredients(Ingredient.of(smithingRecipe.getTemplate().getItems()))
            .addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(TOOLTIP_NOT_CONSUMED));
        builder.addSlot(RecipeIngredientRole.INPUT, CENTER_INPUT_X, CENTER_INPUT_Y)
            .addIngredients(Ingredient.of(smithingRecipe.getMaterial().getItems()));
        for (int i = 0; i < Math.min(8, smithingRecipe.getInputs().size()); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X[i], INPUT_Y[i])
                .addIngredients(Ingredient.of(smithingRecipe.getInputs().get(i).getItems()));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
            .addItemStack(smithingRecipe.getResult().getResult());
    }

    @Override
    public void draw(
        RecipeHolder<BaseMultipleToOneSmithingRecipe> recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        background.draw(guiGraphics);
        for (int i = Math.min(8, recipe.value().getInputs().size()); i < 8; i++) {
            disabledSlotIcon.draw(guiGraphics, INPUT_X[i], INPUT_Y[i]);
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.MULTIPLE_TO_ONE_SMITHING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MULTIPLE_TO_ONE_SMITHING_TYPE.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EMBER_SMITHING_TABLE), AnvilCraftJeiPlugin.MULTIPLE_TO_ONE_SMITHING);
    }
}
