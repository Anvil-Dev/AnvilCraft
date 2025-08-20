package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.integration.jei.util.TextureConstants;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.TimeWarpRecipe;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import dev.dubhe.anvilcraft.util.RenderHelper;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
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
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TimeWarpCategory implements IRecipeCategory<RecipeHolder<TimeWarpRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slot;
    private final Component title;
    private final ITickTimer timer;

    private final IDrawable arrowIn;
    private final IDrawable arrowOut;

    public TimeWarpCategory(IGuiHelper helper) {
        icon = new DrawableBlockStateIcon(Blocks.CAULDRON.defaultBlockState(),
            ModBlocks.CORRUPTED_BEACON.getDefaultState());
        slot = helper.getSlotDrawable();
        title = Component.translatable("gui.anvilcraft.category.time_warp");
        timer = helper.createTickTimer(30, 60, true);

        arrowIn = helper.createDrawable(TextureConstants.ANVIL_CRAFT_SPRITES, 0, 31, 16, 8);
        arrowOut = helper.createDrawable(TextureConstants.ANVIL_CRAFT_SPRITES, 0, 40, 16, 10);
    }

    @Override
    public RecipeType<RecipeHolder<TimeWarpRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.TIME_WARP;
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
        IRecipeLayoutBuilder builder, RecipeHolder<TimeWarpRecipe> recipeHolder, IFocusGroup focuses) {
        TimeWarpRecipe recipe = recipeHolder.value();
        JeiSlotUtil.addInputSlots(builder, recipe.getInputItems());
        if (!recipe.getResultItems().isEmpty()) {
            JeiSlotUtil.addOutputSlots(builder, recipe.getResultItems());
        }
    }

    @Override
    public void draw(
        RecipeHolder<TimeWarpRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        TimeWarpRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderHelper.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            12 + anvilYOffset,
            20,
            12,
            RenderHelper.SINGLE_BLOCK);
        Block material = recipe.getHasCauldron().getFluidCauldron();
        RenderHelper.renderBlock(
            guiGraphics,
            CauldronUtil.fullState(material),
            81,
            30,
            10,
            12,
            RenderHelper.SINGLE_BLOCK
        );
        RenderHelper.renderBlock(
            guiGraphics,
            ModBlocks.CORRUPTED_BEACON.getDefaultState(),
            81,
            40,
            0,
            12,
            RenderHelper.SINGLE_BLOCK
        );

        arrowIn.draw(guiGraphics, 54, 32);
        arrowOut.draw(guiGraphics, 92, 31);

        JeiSlotUtil.drawInputSlots(guiGraphics, slot, recipe.getInputItems().size());
        if (!recipe.getResultItems().isEmpty()) {
            JeiSlotUtil.drawOutputSlots(guiGraphics, slot, recipe.getResultItems().size());
            if (recipe.isConsumeFluid()) {
                PoseStack pose = guiGraphics.pose();
                pose.pushPose();
                pose.scale(0.8f, 0.8f, 1.0f);
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    Component.translatable(
                        "gui.anvilcraft.category.time_warp.consume_fluid",
                        recipe.getHasCauldron().getConsume(),
                        material.getName()),
                    0,
                    70,
                    0xFF000000,
                    false);
                pose.popPose();
            } else if (recipe.isProduceFluid()) {
                PoseStack pose = guiGraphics.pose();
                pose.pushPose();
                pose.scale(0.8f, 0.8f, 1.0f);
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    Component.translatable(
                        "gui.anvilcraft.category.time_warp.produce_fluid",
                        -recipe.getHasCauldron().getConsume(),
                        recipe.getHasCauldron().getTransformCauldron().getName()),
                    0,
                    70,
                    0xFF000000,
                    false);
                pose.popPose();
            }
        } else {
            Block result = recipe.getHasCauldron().getTransformCauldron();
            BlockState cauldronState;
            if (recipe.isConsumeFluid()) {
                cauldronState = CauldronUtil.getStateFromContentAndLevel(result, CauldronUtil.maxLevel(result) - 1);
            } else if (recipe.isProduceFluid()) {
                cauldronState = CauldronUtil.getStateFromContentAndLevel(result, 1);
            } else {
                cauldronState = recipe.getHasCauldron().getTransformCauldron().defaultBlockState();
            }
            RenderHelper.renderBlock(guiGraphics, cauldronState, 133, 30, 0, 12, RenderHelper.SINGLE_BLOCK);
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<TimeWarpRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY) {
        TimeWarpRecipe recipe = recipeHolder.value();
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
                tooltip.add(ModBlocks.CORRUPTED_BEACON.get().getName());
                tooltip.add(Component.translatable("gui.anvilcraft.category.time_warp.need_activated")
                    .withStyle(ChatFormatting.RED));
            }
        }
        if (mouseX >= 124 && mouseX <= 140) {
            if (mouseY >= 24 && mouseY <= 42) {
                Component text;
                if (recipe.getResultItems().isEmpty()) {
                    if (recipe.isConsumeFluid() && CauldronUtil.maxLevel(recipe.getHasCauldron().getTransformCauldron()) <= 1) {
                        text = Blocks.CAULDRON.getName();
                    } else {
                        text = recipe.getHasCauldron().getTransformCauldron().getName();
                    }
                    tooltip.add(text);
                }
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.TIME_WARP,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.TIME_WARP_TYPE.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilCraftJeiPlugin.TIME_WARP);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROYAL_ANVIL), AnvilCraftJeiPlugin.TIME_WARP);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EMBER_ANVIL), AnvilCraftJeiPlugin.TIME_WARP);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GIANT_ANVIL), AnvilCraftJeiPlugin.TIME_WARP);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SPECTRAL_ANVIL), AnvilCraftJeiPlugin.TIME_WARP);
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), AnvilCraftJeiPlugin.TIME_WARP);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CORRUPTED_BEACON), AnvilCraftJeiPlugin.TIME_WARP);
    }
}
