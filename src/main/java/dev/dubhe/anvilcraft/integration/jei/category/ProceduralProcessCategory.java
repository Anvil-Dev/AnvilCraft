package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.block.WipBlock;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessStep;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;

public class ProceduralProcessCategory implements IRecipeCategory<RecipeHolder<ProceduralProcessRecipe>> {
    private static final String INITIAL_BLOCK = "initial_block";
    private static final String OUTPUT_BLOCK = "output_block";
    public static final int WIDTH = 162;
    public static final int HEIGHT = 90;
    private static final int MAX_VISIBLE_STEPS = 5;
    private static final int STEPS_LENGTH = 120;
    private static final int STEP_X = (ProceduralProcessCategory.WIDTH - ProceduralProcessCategory.STEPS_LENGTH) / 2 + 10;
    private static final int STEP_LENGTH = 20;
    private static final int ITEM_Y = 20;
    private static final int BLOCK_Y = 50;
    private static final int FLOW_Y = 72;
    private static final long LOOP_CYCLE_MILLIS = 1500L;

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable longArrow;
    private final IDrawable cycle;
    private final Component title;

    public ProceduralProcessCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(Items.ANVIL));
        this.slot = JeiRenderHelper.getSlotDefault(helper);
        this.longArrow = JeiRenderHelper.getArrowLong(helper);
        this.cycle = JeiRenderHelper.getCycle(helper);
        this.title = Component.translatable("gui.anvilcraft.category.procedural_process");
    }

    @Override
    public IRecipeHolderType<ProceduralProcessRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.PROCEDURAL_PROCESS;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return ProceduralProcessCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return ProceduralProcessCategory.HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<ProceduralProcessRecipe> holder,
        IFocusGroup focuses
    ) {
        ProceduralProcessRecipe recipe = holder.value();
        JeiBlockIngredientUtil.addInputSlot(
            builder,
            ProceduralProcessCategory.INITIAL_BLOCK,
            ProceduralProcessCategory.STEP_X - 29,
            ProceduralProcessCategory.BLOCK_Y - 6,
            18,
            18,
            recipe.initialBlock()
        );
        JeiBlockIngredientUtil.addSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            ProceduralProcessCategory.OUTPUT_BLOCK,
            ProceduralProcessCategory.STEP_X + ProceduralProcessCategory.STEPS_LENGTH - 9,
            ProceduralProcessCategory.BLOCK_Y - 6,
            18,
            18,
            recipe.resultBlock().state().getBlock()
        );

        int visibleSteps = Math.min(recipe.steps().size(), ProceduralProcessCategory.MAX_VISIBLE_STEPS);
        for (int index = 0; index < visibleSteps; index++) {
            ProceduralProcessStep step = recipe.steps().get(index);
            if (!(step.getContent() instanceof AbstractProcessRecipe<?> process)) continue;
            for (int blockIndex = 0; blockIndex < process.getInputBlocks().size(); blockIndex++) {
                int y = blockIndex == 0 ? ProceduralProcessCategory.BLOCK_Y - 6
                                        : ProceduralProcessCategory.BLOCK_Y + 12 + 10 * (blockIndex - 1);
                int height = blockIndex == 0 ? 18 : 10;
                JeiBlockIngredientUtil.addInputSlot(
                    builder,
                    ProceduralProcessCategory.stepBlockSlotName(index, blockIndex),
                    ProceduralProcessCategory.stepX(index, visibleSteps) - 9,
                    y,
                    18,
                    height,
                    process.getInputBlocks().get(blockIndex)
                );
            }
            if (process.getInputItems().isEmpty()) continue;

            ItemIngredientPredicate ingredient = process.getInputItems().getFirst();
            IRecipeSlotBuilder slotBuilder = builder.addSlot(
                RecipeIngredientRole.INPUT,
                ProceduralProcessCategory.stepX(index, visibleSteps) - 8,
                ProceduralProcessCategory.ITEM_Y + 1
            );
            slotBuilder.addItemStacks(
                Arrays.stream(ingredient.getItems()).map(ItemStackTemplate::create).toList()
            );
        }
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder, RecipeHolder<ProceduralProcessRecipe> holder, IFocusGroup focuses
    ) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void draw(
        RecipeHolder<ProceduralProcessRecipe> holder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        ProceduralProcessRecipe recipe = holder.value();
        ProceduralProcessCategory.renderPredicate(
            graphics, view, ProceduralProcessCategory.INITIAL_BLOCK, recipe.initialBlock(), 0, holder,
            ProceduralProcessCategory.STEP_X - 29, ProceduralProcessCategory.BLOCK_Y - 10, 18
        );

        int visibleSteps = Math.min(recipe.steps().size(), ProceduralProcessCategory.MAX_VISIBLE_STEPS);
        int displayedLoop = ProceduralProcessCategory.getDisplayedLoop(recipe);
        for (int index = 0; index < visibleSteps; index++) {
            ProceduralProcessStep step = ProceduralProcessCategory.getDisplayedStep(recipe, index, displayedLoop);
            if (!(step.getContent() instanceof AbstractProcessRecipe<?> process)) continue;
            int x = ProceduralProcessCategory.stepX(index, visibleSteps);
            RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), x - 9, 0, 20);
            if (!process.getInputItems().isEmpty()) {
                this.slot.draw(graphics, x - 9, ProceduralProcessCategory.ITEM_Y);
            }
            List<BlockStatePredicate> inputBlocks = process.getInputBlocks();
            for (int inputIndex = inputBlocks.size() - 1; inputIndex >= 0; inputIndex--) {
                ProceduralProcessCategory.renderPredicate(
                    graphics,
                    view,
                    ProceduralProcessCategory.stepBlockSlotName(index, inputIndex),
                    inputBlocks.get(inputIndex),
                    displayedLoop * recipe.steps().size() + index,
                    holder,
                    x - 9,
                    ProceduralProcessCategory.BLOCK_Y - 10 + inputIndex * 10,
                    18
                );
            }
        }

        this.longArrow.draw(graphics, ProceduralProcessCategory.WIDTH / 2 - 32, ProceduralProcessCategory.FLOW_Y + 4);
        if (recipe.loop() > 1) {
            this.cycle.draw(graphics, ProceduralProcessCategory.WIDTH / 2 + 47, ProceduralProcessCategory.FLOW_Y);
            AgeratumUtil.renderText(
                graphics,
                Component.literal(String.valueOf(recipe.loop())),
                ProceduralProcessCategory.WIDTH / 2 + 68,
                ProceduralProcessCategory.FLOW_Y + 4,
                1.2F
            );
        }
        BlockState outputState = JeiBlockIngredientUtil.getRenderablePreviewState(recipe.resultBlock().state());
        int outputScale = JeiBlockIngredientUtil.getRenderablePreviewScale(outputState, 20);
        RenderSupport.renderBlock(graphics, outputState, ProceduralProcessCategory.STEP_X - 10 + ProceduralProcessCategory.STEPS_LENGTH, ProceduralProcessCategory.BLOCK_Y - 10, outputScale);
    }

    private static int stepX(int index, int visibleSteps) {
        int gap = ProceduralProcessCategory.STEPS_LENGTH / Math.max(visibleSteps, 1) - ProceduralProcessCategory.STEP_LENGTH;
        return ProceduralProcessCategory.STEP_X + gap / 2 + index * (ProceduralProcessCategory.STEP_LENGTH + gap);
    }

    private static void renderPredicate(
        GuiGraphicsExtractor graphics,
        IRecipeSlotsView view,
        String slotName,
        BlockStatePredicate predicate,
        int stepCount,
        RecipeHolder<ProceduralProcessRecipe> holder,
        int x,
        int y,
        int size
    ) {
        List<BlockState> states = predicate.constructStatesForRender();
        if (states.isEmpty()) return;
        BlockState state = JeiBlockIngredientUtil.getDisplayedState(view, slotName, states).orElse(states.getFirst());
        size = JeiBlockIngredientUtil.getRenderablePreviewScale(state, size);
        if (state.getBlock() instanceof WipBlock) {
            RenderSupport.renderWipBlock(graphics, holder.id().identifier(), stepCount, x, y, size);
        } else {
            RenderSupport.renderBlock(graphics, state, x, y, size);
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.PROCEDURAL_PROCESS,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.PROCEDURAL_PROCESS.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.PROCEDURAL_PROCESS);
    }

    private static String stepBlockSlotName(int step, int block) {
        return "step_" + step + "_block_" + block;
    }

    private static int getDisplayedLoop(ProceduralProcessRecipe recipe) {
        if (recipe.loop() <= 1) return 0;
        return (int) ((System.currentTimeMillis() / ProceduralProcessCategory.LOOP_CYCLE_MILLIS) % recipe.loop());
    }

    private static ProceduralProcessStep getDisplayedStep(
        ProceduralProcessRecipe recipe,
        int stepIndex,
        int displayedLoop
    ) {
        if (stepIndex == 0 && displayedLoop > 0) {
            return recipe.multiLoopFirstStep().orElse(recipe.steps().getFirst());
        }
        return recipe.steps().get(stepIndex);
    }
}
