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
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BulgingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.HasCauldronSimple;
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
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BulgingCategory implements IRecipeCategory<RecipeHolder<BulgingRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slot;
    private final Component title;
    private final ITickTimer timer;

    private final IDrawable arrowIn;
    private final IDrawable arrowOut;

    public BulgingCategory(IGuiHelper helper) {
        icon = new DrawableBlockStateIcon(
            Blocks.ANVIL.defaultBlockState(),
            CauldronUtil.fullState(Blocks.WATER_CAULDRON)
        );
        slot = helper.getSlotDrawable();
        title = Component.translatable("gui.anvilcraft.category.bulging");
        timer = helper.createTickTimer(30, 60, true);

        arrowIn = helper.createDrawable(TextureConstants.ANVIL_CRAFT_SPRITES, 0, 31, 16, 8);
        arrowOut = helper.createDrawable(TextureConstants.ANVIL_CRAFT_SPRITES, 0, 40, 16, 10);
    }

    @Override
    public RecipeType<RecipeHolder<BulgingRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.BULGING;
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BulgingRecipe> recipeHolder, IFocusGroup focuses) {
        BulgingRecipe recipe = recipeHolder.value();
        JeiSlotUtil.addInputSlots(builder, recipe.getInputItems());
        if (!recipe.getResultItems().isEmpty()) {
            JeiSlotUtil.addOutputSlots(builder, recipe.getResultItems());
        }
    }

    @Override
    public void draw(
        RecipeHolder<BulgingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        BulgingRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderHelper.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            22 + anvilYOffset,
            20,
            12,
            RenderHelper.SINGLE_BLOCK);
        BlockState state;
        if (recipe.isFromWater()) {
            state = CauldronUtil.fullState(Blocks.WATER_CAULDRON);
        } else if (recipe.isProduceFluid()) {
            state = Blocks.CAULDRON.defaultBlockState();
        } else {
            state = CauldronUtil.fullState(BuiltInRegistries.BLOCK.get(recipe.getHasCauldron().getTransform().withSuffix("_cauldron")));
        }
        RenderHelper.renderBlock(guiGraphics, state, 81, 40, 10, 12, RenderHelper.SINGLE_BLOCK);

        arrowIn.draw(guiGraphics, 54, 32);
        arrowOut.draw(guiGraphics, 92, 31);

        JeiSlotUtil.drawInputSlots(guiGraphics, slot, recipe.getInputItems().size());
        if (!recipe.getResultItems().isEmpty()) {
            JeiSlotUtil.drawOutputSlots(guiGraphics, slot, recipe.getResultItems().size());
            HasCauldronSimple hasCauldron = recipe.getHasCauldron();
            if (recipe.isConsumeFluid()) {
                PoseStack pose = guiGraphics.pose();
                pose.pushPose();
                pose.scale(0.8f, 0.8f, 1.0f);
                guiGraphics.drawString(
                    Minecraft.getInstance().font,
                    Component.translatable(
                        "gui.anvilcraft.category.bulging.consume_fluid",
                        hasCauldron.getConsume(),
                        Component.translatable("block." + hasCauldron.getFluid().toString().replace(':', '.'))),
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
                        "gui.anvilcraft.category.bulging.produce_fluid",
                        -hasCauldron.getConsume(),
                        Component.translatable("block." + hasCauldron.getTransform().toString().replace(':', '.'))),
                    0,
                    70,
                    0xFF000000,
                    false
                );
                pose.popPose();
            }
        } else {
            Block result = recipe.getHasCauldron().getTransformCauldron();
            if (recipe.isConsumeFluid()) {
                state = CauldronUtil.getStateFromContentAndLevel(result, CauldronUtil.maxLevel(result) - 1);
            } else if (recipe.isProduceFluid()) {
                state = CauldronUtil.getStateFromContentAndLevel(result, 1);
            } else {
                state = CauldronUtil.fullState(result);
            }
            RenderHelper.renderBlock(guiGraphics, state, 133, 30, 0, 12, RenderHelper.SINGLE_BLOCK);
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<BulgingRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        BulgingRecipe recipe = recipeHolder.value();
        if (mouseX >= 72 && mouseX <= 90) {
            if (mouseY >= 34 && mouseY <= 53) {
                Block material = recipe.getHasCauldron().getFluidCauldron();
                Component text;
                if (recipe.isFromWater()) {
                    text = Blocks.WATER_CAULDRON.getName();
                } else if (recipe.isConsumeFluid()) {
                    text = material.getName();
                } else if (recipe.isProduceFluid()) {
                    text = Blocks.CAULDRON.getName();
                } else {
                    text = material.getName();
                }
                tooltip.add(text);
            }
        }
        if (mouseX >= 124 && mouseX <= 140) {
            if (mouseY >= 24 && mouseY <= 42) {
                Block result = recipe.getHasCauldron().getTransformCauldron();
                Component text;
                if (recipe.getResultItems().isEmpty()) {
                    if (recipe.isConsumeFluid()) {
                        if (CauldronUtil.maxLevel(result) > 1) {
                            text = result.getName();
                        } else {
                            text = Blocks.CAULDRON.getName();
                        }
                    } else {
                        text = result.getName();
                    }
                    tooltip.add(text);
                }
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<BulgingRecipe>> holders = JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.BULGING_TYPE.get());
        holders.removeIf(holder -> holder.id().getPath().startsWith("concrete/"));
        registration.addRecipes(AnvilCraftJeiPlugin.BULGING, holders);
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilCraftJeiPlugin.BULGING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROYAL_ANVIL), AnvilCraftJeiPlugin.BULGING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EMBER_ANVIL), AnvilCraftJeiPlugin.BULGING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GIANT_ANVIL), AnvilCraftJeiPlugin.BULGING);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SPECTRAL_ANVIL), AnvilCraftJeiPlugin.BULGING);
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), AnvilCraftJeiPlugin.BULGING);
    }
}
