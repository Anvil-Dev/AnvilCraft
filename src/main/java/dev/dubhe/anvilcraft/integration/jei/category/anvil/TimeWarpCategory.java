package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.TooltipUtil;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.TimeWarpRecipe;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

public class TimeWarpCategory implements IRecipeCategory<RecipeHolder<TimeWarpRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable slotDefault;
    private final IDrawable slotProbability;
    private final Component title;
    private final ITickTimer timer;
    private final IDrawable arrowIn;
    private final IDrawable arrowOut;

    public TimeWarpCategory(IGuiHelper helper) {
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.slotProbability = JeiRenderHelper.getSlotProbability(helper);
        this.title = Component.translatable("gui.anvilcraft.category.time_warp");
        this.timer = helper.createTickTimer(30, 60, true);
        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOut = JeiRenderHelper.getArrowOutput(helper);
    }

    @Override
    public IRecipeHolderType<TimeWarpRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.TIME_WARP;
    }

    @Override
    public Component getTitle() {
        return this.title;
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
        return new DrawableBlockStateIcon(
            Blocks.CAULDRON.defaultBlockState(),
            ModBlocks.CORRUPTED_BEACON
                .get()
                .defaultBlockState()
                .trySetValue(BlockStateProperties.WATERLOGGED, false)
        );
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<TimeWarpRecipe> recipeHolder, IFocusGroup focuses) {
        TimeWarpRecipe recipe = recipeHolder.value();
        JeiSlotUtil.addInputSlots(builder, recipe.getInputItems());
        if (!recipe.getResultItems().isEmpty()) JeiSlotUtil.addOutputSlots(builder, recipe.getResultItems());
    }

    @Override
    public void draw(
        RecipeHolder<TimeWarpRecipe> recipeHolder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        TimeWarpRecipe recipe = recipeHolder.value();
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 81, 12 + anvilYOffset, 20);
        Block material = recipe.getHasCauldron().getFluidCauldron();
        RenderSupport.renderBlock(graphics, CauldronUtil.fullState(material), 81, 30, 20);

        RenderSupport.renderBlock(graphics, ModBlocks.CORRUPTED_BEACON.getDefaultState(), 81, 40, 20);

        if (!recipe.getInputItems().isEmpty()) {
            this.arrowIn.draw(graphics, 54, 20);
        }
        this.arrowOut.draw(graphics, 92, 19);

        JeiSlotUtil.drawInputSlots(graphics, this.slotDefault, recipe.getInputItems().size());
        if (!recipe.getResultItems().isEmpty()) {
            if (JeiRecipeUtil.isChance(recipe.getResultItems())) {
                JeiSlotUtil.drawOutputSlots(graphics, this.slotProbability, recipe.getResultItems().size());
            } else {
                JeiSlotUtil.drawOutputSlots(graphics, this.slotDefault, recipe.getResultItems().size());
            }
            if (recipe.isConsumeFluid()) {
                Matrix3x2fStack pose = graphics.pose();
                pose.pushMatrix();
                pose.scale(0.8f, 0.8f);
                graphics.text(
                    Minecraft.getInstance().font,
                    Component.translatable(
                        "gui.anvilcraft.category.time_warp.consume_fluid",
                        recipe.getHasCauldron().consume(),
                        material.getName()),
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
                        "gui.anvilcraft.category.time_warp.produce_fluid",
                        recipe.getHasCauldron().produce(),
                        recipe.getHasCauldron().getTransformCauldron().getName()),
                    0,
                    70,
                    0xFF000000,
                    false
                );
                pose.popMatrix();
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
            RenderSupport.renderBlock(graphics, cauldronState, 133, 30, 20);
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<TimeWarpRecipe> recipeHolder,
        IRecipeSlotsView view,
        double mouseX,
        double mouseY
    ) {
        TimeWarpRecipe recipe = recipeHolder.value();
        if (MathUtil.isInRange(mouseX, 72, 90)) {
            if (MathUtil.isInRange(mouseX, 24, 43)) {
                if (recipe.isProduceFluid()) {
                    tooltip.addAll(TooltipUtil.tooltip(Blocks.CAULDRON));
                } else {
                    tooltip.addAll(TooltipUtil.tooltip(recipe.getHasCauldron().getFluidCauldron()));
                }
            }
            if (MathUtil.isInRange(mouseX, 43, 53)) {
                tooltip.add(ModBlocks.CORRUPTED_BEACON.get().getName());
                tooltip.add(Component.translatable("gui.anvilcraft.category.time_warp.need_activated").withStyle(ChatFormatting.RED));
            }
        }
        if (MathUtil.isInRange(mouseX, mouseY, 124, 24, 140, 42)) {
            if (!recipe.getResultItems().isEmpty()) return;
            if (recipe.isConsumeFluid() && CauldronUtil.maxLevel(recipe.getHasCauldron().getTransformCauldron()) <= 1) {
                tooltip.addAll(TooltipUtil.tooltip(Blocks.CAULDRON));
            } else {
                tooltip.addAll(TooltipUtil.tooltip(recipe.getHasCauldron().getTransformCauldron()));
            }
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.TIME_WARP,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.TIME_WARP.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.TIME_WARP);
        AnvilCraftJeiPlugin.addCauldronCatalysts(registration, AnvilCraftJeiPlugin.TIME_WARP);
        registration.addCraftingStation(AnvilCraftJeiPlugin.TIME_WARP, ModBlocks.CORRUPTED_BEACON);
    }
}
