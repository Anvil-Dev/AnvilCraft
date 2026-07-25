package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
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
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3x2fStack;

public class FastCookingCategory extends AbstractProgressCategory<FastCookingRecipe> {
    private static final String INPUT_FLUID = "input_fluid";
    private static final String OUTPUT_FLUID = "output_fluid";

    public FastCookingCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(
                Blocks.CAULDRON.defaultBlockState(),
                Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)
            ),
            Component.translatable("gui.anvilcraft.category.fast_cooking")
        );
    }

    @Override
    public IRecipeHolderType<FastCookingRecipe> getRecipeType() {
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
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        final FastCookingRecipe recipe = recipeHolder.value();
        this.arrowIn.draw(graphics, 54, 20);
        this.arrowOut.draw(graphics, 92, 19);
        RenderSupport.renderBlock(
            graphics,
            Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true),
            71,
            35,
            20
        );
        RenderSupport.renderBlock(
            graphics,
            CauldronUtil.fullState(recipe.getHasCauldron().getFluidCauldron()),
            71,
            25,
            20
        );
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 71, 7 + anvilYOffset, 20);

        JeiSlotUtil.drawInputSlots(graphics, this.slotDefault, recipe.getInputItems().size());
        if (JeiRecipeUtil.isChance(recipe.getResultItems())) {
            JeiSlotUtil.drawOutputSlots(graphics, this.slotProbability, recipe.getResultItems().size());
        } else {
            JeiSlotUtil.drawOutputSlots(graphics, this.slotDefault, recipe.getResultItems().size());
        }

        HasCauldronSimple hasCauldron = recipe.getHasCauldron();
        if (HasCauldron.isNotEmpty(hasCauldron.transform())) {
            BlockState cauldron = CauldronUtil.fullState(hasCauldron.getTransformCauldron());
            RenderSupport.renderBlock(graphics, cauldron, 133, 30, 20);
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
            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            pose.scale(0.8f, 0.8f);
            graphics.text(Minecraft.getInstance().font, fluidText, 0, 70, 0xFF000000, false);
            pose.popMatrix();
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<FastCookingRecipe> recipeHolder,
        IRecipeSlotsView view,
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
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.FAST_COOKING.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.FAST_COOKING);
        AnvilCraftJeiPlugin.addCauldronCatalysts(registration, AnvilCraftJeiPlugin.FAST_COOKING);
        registration.addCraftingStation(AnvilCraftJeiPlugin.FAST_COOKING, Items.CAMPFIRE);
    }
}
