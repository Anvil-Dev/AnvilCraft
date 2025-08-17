package dev.dubhe.anvilcraft.integration.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.CorruptedBeaconBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.integration.jei.util.TextureConstants;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformRecipe;
import dev.dubhe.anvilcraft.recipe.transform.TransformResult;
import dev.dubhe.anvilcraft.util.RenderHelper;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MobTransformCategory implements IRecipeCategory<RecipeHolder<MobTransformRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slot;
    private final Component title;

    private final IDrawable arrowIn;

    private static final String KEY_CATEGORY = "gui.anvilcraft.category.mob_transform";

    public MobTransformCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(ModBlocks.CORRUPTED_BEACON.asStack());
        slot = helper.getSlotDrawable();
        title = Component.translatable(KEY_CATEGORY);

        arrowIn = helper.drawableBuilder(TextureConstants.PROGRESS, 0, 0, 24, 16)
            .setTextureSize(24, 16)
            .build();
    }

    @Override
    public @NotNull RecipeType<RecipeHolder<MobTransformRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.MOB_TRANSFORM;
    }

    @Override
    public @NotNull Component getTitle() {
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
        @NotNull IRecipeLayoutBuilder builder,
        @NotNull RecipeHolder<MobTransformRecipe> recipe,
        @NotNull IFocusGroup focuses) {

        Ingredient inputIngredient;
        SpawnEggItem spawnEggItemInput = SpawnEggItem.byId(recipe.value().getInput());
        if (spawnEggItemInput == null) {
            String name = recipe.value().getInput().toShortString();
            ItemStack x = Items.BARRIER.getDefaultInstance();
            x.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            inputIngredient = Ingredient.of(x);
        } else inputIngredient = Ingredient.of(spawnEggItemInput);

        builder.addSlot(RecipeIngredientRole.INPUT, 21, 24).addIngredients(inputIngredient);

        List<ChanceItemStack> outputStacks = new ArrayList<>();
        for (TransformResult result : recipe.value().getResults()) {
            SpawnEggItem spawnEggOutput = SpawnEggItem.byId(result.resultEntityType());
            if (spawnEggOutput == null) {
                String name = result.resultEntityType().toShortString();
                ItemStack x = Items.BARRIER.getDefaultInstance();
                x.set(DataComponents.CUSTOM_NAME, Component.literal(name));
                outputStacks.add(ChanceItemStack.of(x, (float) result.probability()));
            } else
                outputStacks.add(ChanceItemStack.of(spawnEggOutput.getDefaultInstance(), (float) result.probability()));
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
        @NotNull IRecipeSlotsView recipeSlotsView,
        @NotNull GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        MobTransformRecipe recipe = recipeHolder.value();

        BlockState block = ModBlocks.CORRUPTED_BEACON.get().defaultBlockState();
        block.setValue(CorruptedBeaconBlock.LIT, true);
        if (block.hasProperty(BlockStateProperties.WATERLOGGED)) {
            block.setValue(BlockStateProperties.WATERLOGGED, false);
        }

        RenderHelper.renderBlock(
            guiGraphics,
            block,
            81,
            40,
            10,
            12,
            RenderHelper.SINGLE_BLOCK);

        arrowIn.draw(guiGraphics, 69, 15);

        JeiSlotUtil.drawInputSlots(guiGraphics, slot, 1);
        JeiSlotUtil.drawOutputSlots(guiGraphics, slot, recipe.getResults().size());

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(0.8f, 0.8f, 1.0f);
        pose.popPose();
    }
}
