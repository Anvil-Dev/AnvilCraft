package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.TooltipUtil;
import dev.dubhe.anvilcraft.block.power.consumer.HeaterBlock;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3x2fStack;

public class SuperHeatingCategory extends AbstractProgressCategory<SuperHeatingRecipe> {
    public SuperHeatingCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(
                Blocks.CAULDRON.defaultBlockState(),
                ModBlocks.HEATER.getDefaultState().setValue(HeaterBlock.OVERLOAD, false)
            ),
            Component.translatable("gui.anvilcraft.category.super_heating")
        );
    }

    @Override
    public IRecipeHolderType<SuperHeatingRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.SUPER_HEATING;
    }

    @Override
    public void draw(
        RecipeHolder<SuperHeatingRecipe> recipeHolder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        final SuperHeatingRecipe recipe = recipeHolder.value();
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 81, 12 + anvilYOffset, 12);
        RenderSupport.renderBlock(graphics, Blocks.CAULDRON.defaultBlockState(), 81, 30, 12);
        RenderSupport.renderBlock(graphics, ModBlocks.HEATER.getDefaultState().setValue(HeaterBlock.OVERLOAD, false), 81, 40, 12);

        arrowIn.draw(graphics, 54, 20);
        arrowOut.draw(graphics, 92, 19);

        JeiSlotUtil.drawInputSlots(graphics, slotDefault, recipe.getInputItems().size());
        if (JeiRecipeUtil.isChance(recipe.getResultItems())) {
            JeiSlotUtil.drawOutputSlots(graphics, slotProbability, recipe.getResultItems().size());
        } else {
            JeiSlotUtil.drawOutputSlots(graphics, slotDefault, recipe.getResultItems().size());
        }

        HasCauldronSimple hasCauldron = recipe.getHasCauldron();
        if (!HasCauldron.isNotEmpty(hasCauldron.transform())) return;
        BlockState cauldron = CauldronUtil.fullState(hasCauldron.getTransformCauldron());
        RenderSupport.renderBlock(graphics, cauldron, 133, 30, 12);

        if (recipe.isConsumeFluid()) {
            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            pose.scale(0.8f, 0.8f);
            graphics.text(
                Minecraft.getInstance().font,
                Component.translatable(
                    "gui.anvilcraft.category.super_heating.consume_fluid",
                    recipe.getHasCauldron().consume(),
                    recipe.getHasCauldron().getFluidCauldron().getName()
                ),
                0,
                70,
                0xFF000000,
                false
            );
            pose.popMatrix();
        } else if (recipe.isProduceFluid()) {
            Matrix3x2fStack pose = graphics.pose();
            pose.pushMatrix();
            pose.scale(0.8f, 0.8f);
            graphics.text(
                Minecraft.getInstance().font,
                Component.translatable(
                    "gui.anvilcraft.category.super_heating.produce_fluid",
                    -recipe.getHasCauldron().consume(),
                    recipe.getHasCauldron().getTransformCauldron().getName()
                ),
                0,
                70,
                0xFF000000,
                false
            );
            pose.popMatrix();
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<SuperHeatingRecipe> recipeHolder,
        IRecipeSlotsView view,
        double mouseX,
        double mouseY
    ) {
        SuperHeatingRecipe recipe = recipeHolder.value();
        if (MathUtil.isInRange(mouseX, 72, 90)) {
            if (MathUtil.isInRange(mouseY, 24, 43)) {
                if (recipe.isProduceFluid()) {
                    tooltip.addAll(TooltipUtil.tooltip(Blocks.CAULDRON));
                } else {
                    tooltip.addAll(TooltipUtil.tooltip(recipe.getHasCauldron().getFluidCauldron()));
                }
            }
            if (MathUtil.isInRange(mouseY, 43, 53)) {
                tooltip.add(ModBlocks.HEATER.get().getName());
                tooltip.add(Component.translatable("gui.anvilcraft.category.super_heating.need_activated").withStyle(ChatFormatting.RED));
            }
        }
        if (MathUtil.isInRange(mouseX, mouseY, 124, 24, 140, 42)) {
            if (!recipe.getResultItems().isEmpty()) return;
            tooltip.addAll(TooltipUtil.tooltip(recipe.getHasCauldron().getTransformCauldron()));
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.SUPER_HEATING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.SUPER_HEATING.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.SUPER_HEATING);
        AnvilCraftJeiPlugin.addCauldronCatalysts(registration, AnvilCraftJeiPlugin.SUPER_HEATING);
        registration.addCraftingStation(AnvilCraftJeiPlugin.SUPER_HEATING, ModBlocks.HEATER);
    }
}
