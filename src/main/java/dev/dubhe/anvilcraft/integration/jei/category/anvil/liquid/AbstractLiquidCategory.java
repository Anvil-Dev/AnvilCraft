package dev.dubhe.anvilcraft.integration.jei.category.anvil.liquid;

import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
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
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidType;
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
        final boolean hasInputItems = !recipe.getDisplayInputItems().isEmpty();
        final boolean hasOutputItems = !recipe.getResultItems().isEmpty();
        final boolean hasInputFluid = cauldron.hasFluid();
        final boolean hasOutputFluid = !cauldron.transforms().isEmpty();

        final boolean inputMixed = hasInputItems && hasInputFluid;
        final boolean outputMixed = hasOutputItems && hasOutputFluid;

        // 输入 — 仅存在一种时居中，二者皆有则分上下
        if (hasInputItems) {
            JeiItemUtil.addInputSlots(builder, recipe.getInputItems(), recipe.getCatalysts(), JeiSlotUtil.INPUT_X,
                inputMixed ? JeiSlotUtil.ITEM_Y : JeiSlotUtil.DEFAULT_Y);
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
                JeiFluidUtil.addFluidOutputSlots(builder, AbstractLiquidCategory.OUTPUT_FLUID, 16, 16, cauldron);
            } else {
                JeiFluidUtil.addDefaultOutputSlots(builder, AbstractLiquidCategory.OUTPUT_FLUID, 16, 16, cauldron);
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

        T recipe = recipeHolder.value();
        HasCauldronSimple cauldron = recipe.getHasCauldron();
        int requiredAmount = cauldron.fluid().isNegate() ? cauldron.consume() : Math.max(
            cauldron.consume(), cauldron.fluid().amount().flatMap(MinMaxBounds.Ints::min).orElse(0)
        );
        boolean useLargeCauldron = requiredAmount > FluidType.BUCKET_VOLUME;
        final BlockState anvilState = useLargeCauldron
                                      ? JeiBlockIngredientUtil.getRenderablePreviewState(ModBlocks.GIANT_ANVIL.getDefaultState())
                                      : Blocks.ANVIL.defaultBlockState();
        final BlockState cauldronState = useLargeCauldron
                                         ? ModBlocks.LARGE_CAULDRON.getDefaultState()
                                             .setValue(LargeCauldronBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                                         : Blocks.CAULDRON.defaultBlockState();
        float modelScale = useLargeCauldron ? 4 : 12;

        // 加工图例及箭头
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        if (useLargeCauldron) anvilYOffset /= 3;
        RenderSupport.renderBlockAt(graphics, this.getProcessBlock(), 81, 40, 12);
        RenderSupport.renderBlockAt(graphics, cauldronState, 81, useLargeCauldron ? 24 : 30, modelScale);
        RenderSupport.renderBlockAt(graphics, anvilState, 81, 12 + anvilYOffset, modelScale);
        this.arrowIn.draw(graphics, 54, 22);
        this.arrowOut.draw(graphics, 92, 22);

        final boolean hasInputItems = !recipe.getDisplayInputItems().isEmpty();
        final boolean hasOutputItems = !recipe.getResultItems().isEmpty();
        final boolean hasInputFluid = cauldron.hasFluid();
        final boolean hasOutputFluid = !cauldron.transforms().isEmpty();

        final boolean inputMixed = hasInputItems && hasInputFluid;
        final boolean outputMixed = hasOutputItems && hasOutputFluid;

        // 输入物品
        if (hasInputItems) {
            if (inputMixed) {
                JeiSlotUtil.drawItemInputSlots(graphics, this.slotDefault, recipe.getDisplayInputItems().size());
            } else {
                JeiSlotUtil.drawDefaultInputSlots(graphics, this.slotDefault, recipe.getDisplayInputItems().size());
            }
        }
        // 输出物品（子类可重写）
        var slot = JeiRecipeUtil.outputSlotFor(recipe.getResultItems(), this.slotDefault, this.slotProbability);
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
            IDrawable fluidSlot = cauldron.chance() < 1.0f ? this.slotProbability : this.slotDefault;
            if (outputMixed) {
                JeiSlotUtil.drawFluidOutputSlots(graphics, fluidSlot, cauldron.transforms().size());
            } else {
                JeiSlotUtil.drawDefaultOutputSlots(graphics, fluidSlot, cauldron.transforms().size());
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
