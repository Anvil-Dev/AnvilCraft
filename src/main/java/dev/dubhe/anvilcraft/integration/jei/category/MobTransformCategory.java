package dev.dubhe.anvilcraft.integration.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformRecipe;
import dev.dubhe.anvilcraft.recipe.transform.TransformResult;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MobTransformCategory implements IRecipeCategory<RecipeHolder<MobTransformRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final IDrawable slotChoice;
    private final IDrawable slotProbability;
    private final Component title;

    private final IDrawable arrowDefault;

    private static final String KEY_CATEGORY = "gui.anvilcraft.category.mob_transform";

    public MobTransformCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(ModBlocks.CORRUPTED_BEACON.asStack());
        slotDefault = JeiRenderHelper.getSlotDefault(helper);
        slotChoice = JeiRenderHelper.getSlotChoice(helper);
        slotProbability = JeiRenderHelper.getSlotProbability(helper);
        title = Component.translatable(KEY_CATEGORY);

        arrowDefault = JeiRenderHelper.getArrowDefault(helper);
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
    public @Nullable IDrawable getIcon() {
        return icon;
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
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<MobTransformRecipe> recipe,
        IFocusGroup focuses) {

        Ingredient inputIngredient;
        SpawnEggItem spawnEggItemInput = SpawnEggItem.byId(recipe.value().input());
        if (spawnEggItemInput == null) {
            String name = recipe.value().input().toShortString();
            ItemStack x = Items.BARRIER.getDefaultInstance();
            x.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            inputIngredient = Ingredient.of(x);
        } else inputIngredient = Ingredient.of(spawnEggItemInput);

        builder.addSlot(RecipeIngredientRole.INPUT, 21, 24).addIngredients(inputIngredient);

        List<ChanceItemStack> outputStacks = new ArrayList<>();
        for (TransformResult result : recipe.value().results()) {
            SpawnEggItem spawnEggOutput = SpawnEggItem.byId(result.resultEntityType());
            if (spawnEggOutput == null) {
                String name = result.resultEntityType().toShortString();
                ItemStack x = Items.BARRIER.getDefaultInstance();
                x.set(DataComponents.CUSTOM_NAME, Component.literal(name));
                outputStacks.add(ChanceItemStack.of(x, (float) result.probability()));
            } else outputStacks.add(ChanceItemStack.of(spawnEggOutput.getDefaultInstance(), (float) result.probability()));
        }
        JeiSlotUtil.addOutputSlots(builder, outputStacks);

        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST)
            .addItemStack(ModBlocks.CORRUPTED_BEACON.asStack());
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.MOB_TRANSFORM,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MOB_TRANSFORM_TYPE.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CORRUPTED_BEACON), AnvilCraftJeiPlugin.MOB_TRANSFORM);
    }

    @Override
    public void draw(
        RecipeHolder<MobTransformRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        final MobTransformRecipe recipe = recipeHolder.value();

        BlockState block = ModBlocks.CORRUPTED_BEACON
            .get()
            .defaultBlockState()
            .trySetValue(BlockStateProperties.WATERLOGGED, false);

        RenderSupport.renderBlock(
            guiGraphics,
            block,
            81,
            40,
            10,
            12,
            RenderSupport.SINGLE_BLOCK);

        arrowDefault.draw(guiGraphics, 74, 22);

        JeiSlotUtil.drawInputSlots(guiGraphics, slotDefault, 1);
        if (isChance(recipe.results())) {
            JeiSlotUtil.drawOutputSlots(guiGraphics, slotChoice, recipe.results().size());
        } else {
            JeiSlotUtil.drawOutputSlots(guiGraphics, slotProbability, recipe.results().size());
        }

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(0.8f, 0.8f, 1.0f);
        pose.popPose();
    }

    private boolean isChance(List<TransformResult> results) {
        for (TransformResult result : results) {
            return result.probability() != 1.0;
        }
        return false;
    }
}
