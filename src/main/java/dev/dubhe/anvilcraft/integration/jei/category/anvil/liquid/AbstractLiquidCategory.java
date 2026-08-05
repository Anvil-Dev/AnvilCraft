package dev.dubhe.anvilcraft.integration.jei.category.anvil.liquid;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.integration.jei.util.JeiFluidUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiItemUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 固液混合加工配方展示的抽象基类，提供铁砧+炼药锅+处理方块的统一布局。
 */
public abstract class AbstractLiquidCategory<T extends AbstractProcessRecipe<?>> implements IRecipeCategory<RecipeHolder<T>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    protected static final String INPUT_FLUID = "input_fluid";
    protected static final String OUTPUT_FLUID = "output_fluid";

    protected final IDrawable icon;
    protected final IDrawable slotDefault;
    protected final IDrawable slotProbability;
    protected final ITickTimer timer;
    protected final IDrawable arrowIn;
    protected final IDrawable arrowOut;
    protected final Component title;

    public AbstractLiquidCategory(IGuiHelper helper, IDrawable icon, Component title) {
        this.icon = icon;
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.slotProbability = JeiRenderHelper.getSlotProbability(helper);
        this.timer = helper.createTickTimer(30, 60, true);
        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOut = JeiRenderHelper.getArrowOutput(helper);
        this.title = title;
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
        IRecipeLayoutBuilder builder, RecipeHolder<T> recipeHolder, IFocusGroup focuses) {
        T recipe = recipeHolder.value();
        HasCauldronSimple cauldron = recipe.getHasCauldron();
        final boolean hasInputItems = !recipe.getInputItems().isEmpty();
        final boolean hasOutputItems = !recipe.getResultItems().isEmpty();
        final boolean hasInputFluid = cauldron.hasFluid();
        final boolean hasOutputFluid = cauldron.transform().isPresent();

        final boolean inputMixed = hasInputItems && hasInputFluid;
        final boolean outputMixed = hasOutputItems && hasOutputFluid;

        // 输入 — 仅存在一种时居中，二者皆有则分上下
        if (hasInputItems) {
            if (inputMixed) {
                JeiItemUtil.addItemInputSlots(builder, recipe.getInputItems());
            } else {
                JeiItemUtil.addDefaultInputSlots(builder, recipe.getInputItems());
            }
        }
        if (hasInputFluid) {
            if (inputMixed) {
                JeiFluidUtil.addFluidInputSlot(builder, INPUT_FLUID, 16, 16, cauldron);
            } else {
                JeiFluidUtil.addDefaultInputSlot(builder, INPUT_FLUID, 16, 16, cauldron);
            }
        }

        // 输出 — 仅存在一种时居中，二者皆有则分上下
        if (hasOutputItems) {
            if (outputMixed) {
                JeiItemUtil.addItemOutputSlots(builder, recipe.getResultItems());
            } else {
                JeiItemUtil.addDefaultOutputSlots(builder, recipe.getResultItems());
            }
        }
        if (hasOutputFluid) {
            if (outputMixed) {
                JeiFluidUtil.addFluidOutputSlot(builder, OUTPUT_FLUID, 16, 16, cauldron);
            } else {
                JeiFluidUtil.addDefaultOutputSlot(builder, OUTPUT_FLUID, 16, 16, cauldron);
            }
        }
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder, RecipeHolder<T> recipeHolder, IFocusGroup focuses) {
        JeiFluidUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void draw(
        RecipeHolder<T> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {

        // 加工图例及箭头
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderSupport.renderBlock(guiGraphics, Blocks.ANVIL.defaultBlockState(), 81, 12 + anvilYOffset, 20, 12, RenderSupport.SINGLE_BLOCK);
        RenderSupport.renderBlock(guiGraphics, Blocks.CAULDRON.defaultBlockState(), 81, 30, 10, 12, RenderSupport.SINGLE_BLOCK);
        RenderSupport.renderBlock(guiGraphics, getProcessBlock(), 81, 40, 0, 12, RenderSupport.SINGLE_BLOCK);
        arrowIn.draw(guiGraphics, 54, 22);
        arrowOut.draw(guiGraphics, 92, 22);

        T recipe = recipeHolder.value();
        HasCauldronSimple cauldron = recipe.getHasCauldron();

        final boolean hasInputItems = !recipe.getInputItems().isEmpty();
        final boolean hasOutputItems = !recipe.getResultItems().isEmpty();
        final boolean hasInputFluid = cauldron.hasFluid();
        final boolean hasOutputFluid = cauldron.transform().isPresent();

        final boolean inputMixed = hasInputItems && hasInputFluid;
        final boolean outputMixed = hasOutputItems && hasOutputFluid;

        // 输入物品
        if (hasInputItems) {
            if (inputMixed) {
                JeiSlotUtil.drawItemInputSlots(guiGraphics, slotDefault, recipe.getInputItems().size());
            } else {
                JeiSlotUtil.drawDefaultInputSlots(guiGraphics, slotDefault, recipe.getInputItems().size());
            }
        }
        // 输出物品（子类可重写）
        IDrawable slot = JeiRecipeUtil.isChance(recipe.getResultItems()) ? slotProbability : slotDefault;
        if (hasOutputItems) {
            if (outputMixed) {
                JeiSlotUtil.drawItemOutputSlots(guiGraphics, slot, recipe.getResultItems().size());
            } else {
                JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, slot, recipe.getResultItems().size());
            }
        }

        // 输入流体
        if (hasInputFluid) {
            if (inputMixed) {
                JeiSlotUtil.drawFluidInputSlots(guiGraphics, slotDefault, 1);
            } else {
                JeiSlotUtil.drawDefaultInputSlots(guiGraphics, slotDefault, 1);
            }
        }
        // 输出流体
        if (hasOutputFluid) {
            if (outputMixed) {
                JeiSlotUtil.drawFluidOutputSlots(guiGraphics, slotDefault, 1);
            } else {
                JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, slotDefault, 1);
            }
        }

        // 火锅
        if (cauldron.ignited()) {
            Component text = Component.translatable("gui.anvilcraft.category.cauldron.need_ignite");
            int textWidth = Minecraft.getInstance().font.width(text);
            guiGraphics.drawString(Minecraft.getInstance().font, text, 81 - textWidth / 2, 55, 0xFF000000, false);
        }
    }

    /**
     * 炼药锅下方的处理方块
     */
    protected BlockState getProcessBlock() {
        return Blocks.AIR.defaultBlockState();
    }
}
