package dev.dubhe.anvilcraft.integration.jei.category.anvil.liquid;

import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.category.AbstractLiquidReactionCategory;
import dev.dubhe.anvilcraft.integration.jei.category.AbstractLiquidReactionCategory.SlotPosition;
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
import mezz.jei.api.gui.builder.ITooltipBuilder;
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

import java.util.List;

/**
 * 固液混合加工配方展示的抽象基类，提供铁砧+炼药锅+处理方块的统一布局。
 */
public abstract class AbstractLiquidCategory<T extends AbstractProcessRecipe<?>> implements IRecipeCategory<RecipeHolder<T>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;
    private static final float LARGE_MODEL_SCALE = 7.5F;
    private static final int PROCESS_INPUT_GRID_SIZE = 4;
    private static final int PROCESS_X = 100;
    private static final int PROCESS_Y = 41;
    private static final int IGNITION_X = 45;
    private static final int IGNITION_Y = 41;

    protected static final String INPUT_FLUID = "input_fluid";
    protected static final String OUTPUT_FLUID = "output_fluid";

    protected final IDrawable icon;
    protected final IDrawable slotDefault;
    protected final IDrawable slotProbability;
    protected final ITickTimer timer;
    protected final IDrawable arrowIn;
    protected final IDrawable arrowOut;
    protected final Component title;
    private final IDrawable ignition;
    private final BlockState largeCauldron;
    private final BlockState giantAnvil;

    public AbstractLiquidCategory(IGuiHelper helper, IDrawable icon, Component title) {
        this.icon = icon;
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.slotProbability = JeiRenderHelper.getSlotProbability(helper);
        this.timer = helper.createTickTimer(30, 60, true);
        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOut = JeiRenderHelper.getArrowOutput(helper);
        this.title = title;
        this.ignition = helper.getRecipeFlameFilled();
        this.largeCauldron = ModBlocks.LARGE_CAULDRON.getDefaultState()
            .setValue(LargeCauldronBlock.HALF, Cube3x3PartHalf.MID_CENTER);
        this.giantAnvil = JeiBlockIngredientUtil.getRenderablePreviewState(ModBlocks.GIANT_ANVIL.getDefaultState());
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
        if (useLargeLayout(recipe)) {
            this.setLargeCauldronRecipe(builder, recipe);
            return;
        }
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
        if (useLargeLayout(recipe)) {
            this.drawLargeCauldron(recipe, graphics);
            return;
        }

        // 加工图例及箭头
        boolean large = useLargeCauldron(cauldron);
        float modelScale = large ? 4 : 12;
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer) / (large ? 3.0F : 1.0F);
        RenderSupport.renderBlockAt(graphics, this.getProcessBlock(), 81, 40, 12);
        RenderSupport.renderBlockAt(graphics, large ? this.largeCauldron : Blocks.CAULDRON.defaultBlockState(),
            81, large ? 24 : 30, modelScale);
        RenderSupport.renderBlockAt(graphics, large ? this.giantAnvil : Blocks.ANVIL.defaultBlockState(),
            81, 12 + anvilYOffset, modelScale);
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

    private static boolean useLargeCauldron(HasCauldronSimple cauldron) {
        int requiredAmount = cauldron.fluid().isNegate() ? cauldron.consume() : Math.max(
            cauldron.consume(), cauldron.fluid().amount().flatMap(MinMaxBounds.Ints::min).orElse(0)
        );
        return requiredAmount > FluidType.BUCKET_VOLUME;
    }

    private static boolean useLargeLayout(AbstractProcessRecipe<?> recipe) {
        HasCauldronSimple cauldron = recipe.getHasCauldron();
        int inputCount = recipe.getDisplayInputItems().size() + (cauldron.hasFluid() ? 1 : 0);
        int outputCount = recipe.getResultItems().size() + cauldron.transforms().size();
        return useLargeCauldron(cauldron) && inputCount <= 6 && outputCount <= 6;
    }

    private static SlotPosition largeInputPosition(int count, int index) {
        int gridSize = Math.max(PROCESS_INPUT_GRID_SIZE, count);
        SlotPosition position = AbstractLiquidReactionCategory.inputPosition(gridSize, index);
        return gridSize > PROCESS_INPUT_GRID_SIZE ? new SlotPosition(position.x(), position.y() - 9) : position;
    }

    private static SlotPosition largeOutputPosition(int itemCount, int fluidCount, int index, boolean fluid) {
        boolean splitColumns = itemCount > 0 && fluidCount > 0;
        if (itemCount <= 3 && fluidCount <= 3) {
            return fluid
                   ? AbstractLiquidReactionCategory.fluidOutputPosition(fluidCount, index, splitColumns)
                   : AbstractLiquidReactionCategory.itemOutputPosition(itemCount, index, splitColumns);
        }
        int outputIndex = fluid ? itemCount + index : index;
        int firstRow = itemCount + fluidCount <= 4 ? 14 : 5;
        return new SlotPosition(119 + outputIndex % 2 * 19, firstRow + outputIndex / 2 * 19);
    }

    private void setLargeCauldronRecipe(IRecipeLayoutBuilder builder, T recipe) {
        HasCauldronSimple cauldron = recipe.getHasCauldron();
        int fluidInputCount = cauldron.hasFluid() ? 1 : 0;
        int inputCount = fluidInputCount + recipe.getDisplayInputItems().size();
        if (cauldron.hasFluid()) {
            SlotPosition position = largeInputPosition(inputCount, 0);
            JeiFluidUtil.addInputSlot(builder, INPUT_FLUID, position.x() + 1, position.y() + 1, 16, 16, cauldron);
        }
        for (int index = 0; index < recipe.getDisplayInputItems().size(); index++) {
            SlotPosition position = largeInputPosition(inputCount, fluidInputCount + index);
            boolean catalyst = index >= recipe.getInputItems().size();
            var ingredient = recipe.getDisplayInputItems().get(index);
            JeiItemUtil.addInputSlots(builder, catalyst ? List.of() : List.of(ingredient),
                catalyst ? List.of(ingredient) : List.of(), position.x() + 1, position.y() + 1);
        }
        for (int index = 0; index < recipe.getResultItems().size(); index++) {
            SlotPosition position = largeOutputPosition(
                recipe.getResultItems().size(), cauldron.transforms().size(), index, false
            );
            JeiItemUtil.addOutputSlot(builder, position.x() + 1, position.y() + 1, recipe.getResultItems().get(index));
        }
        for (int index = 0; index < cauldron.transforms().size(); index++) {
            SlotPosition position = largeOutputPosition(
                recipe.getResultItems().size(), cauldron.transforms().size(), index, true
            );
            JeiFluidUtil.addOutputSlot(builder, OUTPUT_FLUID, position.x() + 1, position.y() + 1, 16, 16, cauldron, index);
        }
    }

    private void drawLargeCauldron(T recipe, GuiGraphicsExtractor graphics) {
        HasCauldronSimple cauldron = recipe.getHasCauldron();
        int inputCount = recipe.getDisplayInputItems().size() + (cauldron.hasFluid() ? 1 : 0);
        for (int index = 0; index < inputCount; index++) {
            SlotPosition position = largeInputPosition(inputCount, index);
            this.slotDefault.draw(graphics, position.x(), position.y());
        }
        var outputSlot = JeiRecipeUtil.outputSlotFor(recipe.getResultItems(), this.slotDefault, this.slotProbability);
        for (int index = 0; index < recipe.getResultItems().size(); index++) {
            SlotPosition position = largeOutputPosition(
                recipe.getResultItems().size(), cauldron.transforms().size(), index, false
            );
            outputSlot.apply(index).draw(graphics, position.x(), position.y());
        }
        IDrawable fluidSlot = cauldron.chance() < 1.0f ? this.slotProbability : this.slotDefault;
        for (int index = 0; index < cauldron.transforms().size(); index++) {
            SlotPosition position = largeOutputPosition(
                recipe.getResultItems().size(), cauldron.transforms().size(), index, true
            );
            fluidSlot.draw(graphics, position.x(), position.y());
        }
        this.arrowIn.draw(graphics, 47, 30);
        this.arrowOut.draw(graphics, 99, 29);
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer) / 3.0F;
        RenderSupport.renderBlockAt(graphics, this.largeCauldron, 81, 45, LARGE_MODEL_SCALE);
        RenderSupport.renderBlockAt(graphics, this.giantAnvil, 81, 23 + anvilYOffset, LARGE_MODEL_SCALE);
        JeiRenderHelper.renderBlockWithSlot(graphics, this.slotDefault, this.getProcessBlock(), PROCESS_X, PROCESS_Y);
        if (cauldron.ignited()) {
            this.ignition.draw(graphics, IGNITION_X + 1, IGNITION_Y + 1);
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<T> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        HasCauldronSimple cauldron = recipeHolder.value().getHasCauldron();
        if (useLargeLayout(recipeHolder.value()) && cauldron.ignited()
            && mouseX >= IGNITION_X && mouseX < IGNITION_X + 18
            && mouseY >= IGNITION_Y && mouseY < IGNITION_Y + 18) {
            tooltip.add(Component.translatable("gui.anvilcraft.category.cauldron.need_ignite"));
        }
    }

    /**
     * 配方所需的处理方块
     */
    protected BlockState getProcessBlock() {
        return Blocks.AIR.defaultBlockState();
    }
}
