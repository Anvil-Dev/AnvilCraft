package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiFluidIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.FastCookingRecipe;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class FastCookingCategory extends AbstractProgressCategory<FastCookingRecipe> {
    private static final String INPUT_FLUID = "input_fluid";
    private static final String OUTPUT_FLUID = "output_fluid";

    public FastCookingCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(Blocks.CAULDRON.defaultBlockState(),
                Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)),
            Component.translatable("gui.anvilcraft.category.fast_cooking")
        );
    }

    @Override
    public RecipeType<RecipeHolder<FastCookingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.FAST_COOKING;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<FastCookingRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        super.setRecipe(builder, recipeHolder, focuses);
        FastCookingRecipe recipe = recipeHolder.value();
        HasCauldronSimple cauldron = recipe.getHasCauldron();
        JeiFluidIngredientUtil.addInputSlot(builder, INPUT_FLUID, 72, 24, 18, 19, cauldron);
        if (recipe.getResultItems().isEmpty()) {
            JeiFluidIngredientUtil.addOutputSlot(builder, OUTPUT_FLUID, 124, 24, 18, 19, cauldron);
        } else {
            JeiFluidIngredientUtil.addOutputIngredients(builder, cauldron);
        }
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder,
        RecipeHolder<FastCookingRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        JeiFluidIngredientUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void draw(
        RecipeHolder<FastCookingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        final FastCookingRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderSupport.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            12 + anvilYOffset,
            20,
            12,
            RenderSupport.SINGLE_BLOCK);
        RenderSupport.renderBlock(
            guiGraphics,
            CauldronUtil.fullState(recipe.getHasCauldron().getFluidCauldron()),
            81,
            30,
            10,
            12,
            RenderSupport.SINGLE_BLOCK
        );
        RenderSupport.renderBlock(
            guiGraphics,
            Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true),
            81,
            40,
            0,
            12,
            RenderSupport.SINGLE_BLOCK);

        arrowIn.draw(guiGraphics, 54, 20);
        arrowOut.draw(guiGraphics, 92, 19);

        JeiSlotUtil.drawInputSlots(guiGraphics, slotDefault, recipe.getInputItems().size());
        if (JeiRecipeUtil.isChance(recipe.getResultItems())) {
            JeiSlotUtil.drawOutputSlots(guiGraphics, slotProbability, recipe.getResultItems().size());
        } else {
            JeiSlotUtil.drawOutputSlots(guiGraphics, slotDefault, recipe.getResultItems().size());
        }

        HasCauldronSimple hasCauldron = recipe.getHasCauldron();
        if (HasCauldron.isNotEmpty(hasCauldron.transform())) {
            BlockState cauldron = CauldronUtil.fullState(hasCauldron.getTransformCauldron());
            RenderSupport.renderBlock(guiGraphics, cauldron, 133, 30, 0, 12, RenderSupport.SINGLE_BLOCK);
        }

        Component fluidText = null;
        if (recipe.isProduceFluid()) {
            fluidText = Component.translatable(
                "gui.anvilcraft.category.fast_cooking.produce_fluid",
                hasCauldron.produce(),
                hasCauldron.getTransformCauldron().getName()
            );
        } else if (recipe.isConsumeFluid()) {
            fluidText = Component.translatable(
                "gui.anvilcraft.category.fast_cooking.consume_fluid",
                hasCauldron.consume(),
                hasCauldron.getFluidCauldron().getName()
            );
        }
        if (fluidText != null) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.scale(0.8F, 0.8F, 1.0F);
            guiGraphics.drawString(Minecraft.getInstance().font, fluidText, 0, 70, 0xFF000000, false);
            pose.popPose();
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<FastCookingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        FastCookingRecipe recipe = recipeHolder.value();
        if (mouseX >= 72 && mouseX <= 90 && mouseY >= 24 && mouseY <= 43) {
            tooltip.add(recipe.getHasCauldron().getFluidCauldron() == Blocks.CAULDRON
                ? Blocks.CAULDRON.getName()
                : CauldronUtil.fullState(recipe.getHasCauldron().getFluidCauldron()).getBlock().getName());
        }
        if (mouseX >= 124 && mouseX <= 140 && mouseY >= 24 && mouseY <= 42
            && HasCauldron.isNotEmpty(recipe.getHasCauldron().transform())) {
            tooltip.add(recipe.getHasCauldron().getTransformCauldron().getName());
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.FAST_COOKING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.FAST_COOKING_TYPE.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.FAST_COOKING);
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), AnvilCraftJeiPlugin.FAST_COOKING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FISH_TANK), AnvilCraftJeiPlugin.FAST_COOKING);
        registration.addRecipeCatalyst(new ItemStack(Items.CAMPFIRE), AnvilCraftJeiPlugin.FAST_COOKING);
    }
}
