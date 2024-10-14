package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.integration.jei.util.TextureConstants;
import dev.dubhe.anvilcraft.recipe.anvil.BulgingRecipe;
import dev.dubhe.anvilcraft.util.RenderHelper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.Lazy;

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
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BulgingCategory implements IRecipeCategory<RecipeHolder<BulgingRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final Lazy<IDrawable> background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final Component title;
    private final ITickTimer timer;

    private final IDrawable arrowIn;
    private final IDrawable arrowOut;

    public BulgingCategory(IGuiHelper helper) {
        background = Lazy.of(() -> helper.createBlankDrawable(WIDTH, HEIGHT));
        icon = new DrawableBlockStateIcon(
                Blocks.ANVIL.defaultBlockState(),
                Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
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
    public IDrawable getBackground() {
        return background.get();
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<BulgingRecipe> recipeHolder, IFocusGroup focuses) {
        BulgingRecipe recipe = recipeHolder.value();
        JeiSlotUtil.addInputSlots(builder, recipe.mergedIngredients);
        if (!recipe.results.isEmpty()) {
            JeiSlotUtil.addOutputSlots(builder, recipe.results);
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
            state = Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
        } else {
            if (recipe.isConsumeFluid()) {
                if (recipe.cauldron instanceof LayeredCauldronBlock) {
                    state = recipe.cauldron.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
                } else {
                    state = recipe.cauldron.defaultBlockState();
                }
            } else if (recipe.isProduceFluid()) {
                state = Blocks.CAULDRON.defaultBlockState();
            } else {
                if (recipe.cauldron instanceof LayeredCauldronBlock) {
                    state = recipe.cauldron.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
                } else {
                    state = recipe.cauldron.defaultBlockState();
                }
            }
        }
        RenderHelper.renderBlock(guiGraphics, state, 81, 40, 10, 12, RenderHelper.SINGLE_BLOCK);

        arrowIn.draw(guiGraphics, 54, 32);
        arrowOut.draw(guiGraphics, 92, 31);

        JeiSlotUtil.drawInputSlots(guiGraphics, slot, recipe.mergedIngredients.size());
        if (!recipe.results.isEmpty()) {
            JeiSlotUtil.drawOutputSlots(guiGraphics, slot, 1);
            if (recipe.isConsumeFluid()) {
                guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        Component.translatable(
                                "gui.anvilcraft.category.bulging.consume_fluid", recipe.cauldron.getName()),
                        10,
                        54,
                        0xFF000000,
                        false);
            } else if (recipe.isProduceFluid()) {
                guiGraphics.drawString(
                        Minecraft.getInstance().font,
                        Component.translatable(
                                "gui.anvilcraft.category.bulging.produce_fluid", recipe.cauldron.getName()),
                        10,
                        54,
                        0xFF000000,
                        false);
            }
        } else {
            if (recipe.isConsumeFluid()) {
                if (recipe.cauldron instanceof LayeredCauldronBlock) {
                    state = recipe.cauldron.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2);
                } else {
                    state = Blocks.CAULDRON.defaultBlockState();
                }
            } else if (recipe.isProduceFluid()) {
                if (recipe.cauldron instanceof LayeredCauldronBlock) {
                    state = recipe.cauldron.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1);
                } else {
                    state = recipe.cauldron.defaultBlockState();
                }
            } else {
                state = recipe.cauldron.defaultBlockState();
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
            double mouseY) {
        BulgingRecipe recipe = recipeHolder.value();
        if (mouseX >= 72 && mouseX <= 90) {
            if (mouseY >= 34 && mouseY <= 53) {
                Component text;
                if (recipe.isFromWater()) {
                    text = Blocks.WATER_CAULDRON.getName();
                } else if (recipe.isConsumeFluid()) {
                    text = recipe.cauldron.getName();
                } else if (recipe.isProduceFluid()) {
                    text = Blocks.CAULDRON.getName();
                } else {
                    text = recipe.cauldron.getName();
                }
                tooltip.add(text);
            }
        }
        if (mouseX >= 124 && mouseX <= 140) {
            if (mouseY >= 24 && mouseY <= 42) {
                Component text;
                if (recipe.results.isEmpty()) {
                    if (recipe.isConsumeFluid()) {
                        if (recipe.cauldron instanceof LayeredCauldronBlock) {
                            text = recipe.cauldron.getName();
                        } else {
                            text = Blocks.CAULDRON.getName();
                        }
                    } else if (recipe.isProduceFluid()) {
                        text = recipe.cauldron.getName();
                    } else {
                        text = recipe.cauldron.getName();
                    }
                    tooltip.add(text);
                }
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
                AnvilCraftJeiPlugin.BULGING, JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.BULGING_TYPE.get()));
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
