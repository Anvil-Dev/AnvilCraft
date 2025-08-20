package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.TimeWarpRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.HasCauldronSimple;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.RenderHelper;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
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
    public RecipeType<RecipeHolder<SuperHeatingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.SUPER_HEATING;
    }

    @Override
    public void draw(
        RecipeHolder<SuperHeatingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        SuperHeatingRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderHelper.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            12 + anvilYOffset,
            20,
            12,
            RenderHelper.SINGLE_BLOCK
        );
        RenderHelper.renderBlock(guiGraphics, Blocks.CAULDRON.defaultBlockState(), 81, 30, 10, 12, RenderHelper.SINGLE_BLOCK);
        RenderHelper.renderBlock(
            guiGraphics,
            ModBlocks.HEATER.getDefaultState().setValue(HeaterBlock.OVERLOAD, false),
            81,
            40,
            0,
            12,
            RenderHelper.SINGLE_BLOCK
        );

        arrowIn.draw(guiGraphics, 54, 30);
        arrowOut.draw(guiGraphics, 92, 29);

        JeiSlotUtil.drawInputSlots(guiGraphics, slot, recipe.getInputItems().size());
        JeiSlotUtil.drawOutputSlots(guiGraphics, slot, this.getResults(recipe).size());

        HasCauldronSimple hasCauldron = recipe.getHasCauldron();
        if (!HasCauldron.isNotEmpty(hasCauldron.getTransform())) return;
        BlockState cauldron = CauldronUtil.fullState(hasCauldron.getTransformCauldron());
        RenderHelper.renderBlock(guiGraphics, cauldron, 133, 30, 0, 12, RenderHelper.SINGLE_BLOCK);

        if (recipe.isConsumeFluid()) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.scale(0.8f, 0.8f, 1.0f);
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable(
                    "gui.anvilcraft.category.super_heating.consume_fluid",
                    recipe.getHasCauldron().getConsume(),
                    recipe.getHasCauldron().getFluidCauldron().getName()
                ),
                0,
                70,
                0xFF000000,
                false
            );
            pose.popPose();
        } else if (recipe.isProduceFluid()) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            pose.scale(0.8f, 0.8f, 1.0f);
            guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable(
                    "gui.anvilcraft.category.super_heating.produce_fluid",
                    recipe.getHasCauldron().getConsume(),
                    recipe.getHasCauldron().getTransformCauldron().getName()
                ),
                0,
                70,
                0xFF000000,
                false
            );
            pose.popPose();
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<SuperHeatingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        SuperHeatingRecipe recipe = recipeHolder.value();
        if (mouseX >= 72 && mouseX <= 90) {
            if (mouseY >= 24 && mouseY <= 43) {
                Component text;
                if (recipe.isProduceFluid()) {
                    text = Blocks.CAULDRON.getName();
                } else {
                    text = recipe.getHasCauldron().getFluidCauldron().getName();
                }
                tooltip.add(text);
            }
            if (mouseY >= 34 && mouseY <= 53) {
                tooltip.add(ModBlocks.HEATER.get().getName());
                tooltip.add(Component.translatable("gui.anvilcraft.category.super_heating.need_activated")
                                .withStyle(ChatFormatting.RED));
            }
        }
        if (mouseX >= 124 && mouseX <= 140) {
            if (mouseY >= 24 && mouseY <= 42) {
                if (recipe.getResultItems().isEmpty()) {
                    Component text = recipe.getHasCauldron().getTransformCauldron().getName();
                    tooltip.add(text);
                }
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.SUPER_HEATING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.SUPER_HEATING_TYPE.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilCraftJeiPlugin.SUPER_HEATING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROYAL_ANVIL), AnvilCraftJeiPlugin.SUPER_HEATING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EMBER_ANVIL), AnvilCraftJeiPlugin.SUPER_HEATING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GIANT_ANVIL), AnvilCraftJeiPlugin.SUPER_HEATING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SPECTRAL_ANVIL), AnvilCraftJeiPlugin.SUPER_HEATING);
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), AnvilCraftJeiPlugin.SUPER_HEATING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.HEATER), AnvilCraftJeiPlugin.SUPER_HEATING);
    }
}
