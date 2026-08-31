package dev.dubhe.anvilcraft.integration.jei.category.anvil.liquid;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiFluidUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiItemUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
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
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

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
        return this.title;
    }

    @Override
    public int getWidth() {
        return AbstractLiquidCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return AbstractLiquidCategory.HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder, RecipeHolder<T> recipeHolder, IFocusGroup focuses) {
        T recipe = recipeHolder.value();
        HasCauldronSimple cauldron = recipe.getHasCauldron();
        final boolean hasInputItems = !recipe.getInputItems().isEmpty();
        final boolean hasOutputItems = !recipe.getResultItems().isEmpty();
        final boolean hasInputFluid = cauldron.fluidTag() != null || HasCauldron.isNotEmpty(cauldron.fluid());
        final boolean hasOutputFluid = HasCauldron.isNotEmpty(cauldron.transform());

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
                JeiFluidUtil.addFluidInputSlot(builder, AbstractLiquidCategory.INPUT_FLUID, 16, 16, cauldron);
            } else {
                JeiFluidUtil.addDefaultInputSlot(builder, AbstractLiquidCategory.INPUT_FLUID, 16, 16, cauldron);
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
                JeiFluidUtil.addFluidOutputSlot(builder, AbstractLiquidCategory.OUTPUT_FLUID, 16, 16, cauldron);
            } else {
                JeiFluidUtil.addDefaultOutputSlot(builder, AbstractLiquidCategory.OUTPUT_FLUID, 16, 16, cauldron);
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
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {

        // 加工图例及箭头
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, this.getProcessBlock(), 71, 38, 20);
        RenderSupport.renderBlock(graphics, Blocks.CAULDRON.defaultBlockState(), 71, 28, 20);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 71, 10 + anvilYOffset, 20);
        this.arrowIn.draw(graphics, 54, 22);
        this.arrowOut.draw(graphics, 92, 22);

        T recipe = recipeHolder.value();
        HasCauldronSimple cauldron = recipe.getHasCauldron();

        final boolean hasInputItems = !recipe.getInputItems().isEmpty();
        final boolean hasOutputItems = !recipe.getResultItems().isEmpty();
        final boolean hasInputFluid = cauldron.fluidTag() != null || HasCauldron.isNotEmpty(cauldron.fluid());
        final boolean hasOutputFluid = HasCauldron.isNotEmpty(cauldron.transform());

        final boolean inputMixed = hasInputItems && hasInputFluid;
        final boolean outputMixed = hasOutputItems && hasOutputFluid;

        // 输入物品
        if (hasInputItems) {
            if (inputMixed) {
                JeiSlotUtil.drawItemInputSlots(graphics, this.slotDefault, recipe.getInputItems().size());
            } else {
                JeiSlotUtil.drawDefaultInputSlots(graphics, this.slotDefault, recipe.getInputItems().size());
            }
        }
        // 输出物品（子类可重写）
        IDrawable slot = JeiRecipeUtil.isChance(recipe.getResultItems()) ? this.slotProbability : this.slotDefault;
        if (hasOutputItems) {
            if (outputMixed) {
                JeiSlotUtil.drawItemOutputSlots(graphics, slot, recipe.getResultItems().size());
            } else {
                JeiSlotUtil.drawDefaultOutputSlots(graphics, slot, recipe.getResultItems().size());
            }
        }

        // 输入流体
        if (hasInputFluid) {
            if (inputMixed) {
                JeiSlotUtil.drawFluidInputSlots(graphics, this.slotDefault, 1);
            } else {
                JeiSlotUtil.drawDefaultInputSlots(graphics, this.slotDefault, 1);
            }
        }
        // 输出流体
        if (hasOutputFluid) {
            if (outputMixed) {
                JeiSlotUtil.drawFluidOutputSlots(graphics, this.slotDefault, 1);
            } else {
                JeiSlotUtil.drawDefaultOutputSlots(graphics, this.slotDefault, 1);
            }
        }

        // 火锅
        if (cauldron.ignited()) {
            Component text = Component.translatable("gui.anvilcraft.category.cauldron.need_ignite");
            int textWidth = Minecraft.getInstance().font.width(text);
            graphics.text(Minecraft.getInstance().font, text, 81 - textWidth / 2, 55, 0xFF000000, false);
        }
    }

    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilCauldronCatalysts(registration, this.getRecipeType());
    }

    /**
     * 炼药锅下方的处理方块
     */
    protected BlockState getProcessBlock() {
        return Blocks.AIR.defaultBlockState();
    }
}
