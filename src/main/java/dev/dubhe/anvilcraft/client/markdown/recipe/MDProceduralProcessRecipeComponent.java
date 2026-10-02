package dev.dubhe.anvilcraft.client.markdown.recipe;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.WipBlock;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessStep;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Blocks;

public class MDProceduralProcessRecipeComponent extends MDRecipeComponent {
    public static final Identifier TEXTURE = AnvilCraft.of("textures/gui/ageratum/procedural_process.png");
    public static final Identifier CYCLE = AnvilCraft.of("textures/gui/ageratum/cycle.png");
    public static final Identifier ARROW_LONG = AnvilCraft.of("textures/gui/ageratum/arrow_long.png");
    public static final int WIDTH = 384;
    public static final int HEIGHT = 110;
    public static final int STEPS_LENGTH = 210;
    public static final int STEP_X = (WIDTH - STEPS_LENGTH) / 2;
    public static final int STEP_LENGTH = 30;
    public static final int ANVIL_Y = 18;
    public static final int BLOCK_ROW = 3;
    public static final int FLOW_Y = 90;
    public static final int ITEM_Y_OFFSET = 8;
    public static final int ARROW_LONG_LENGTH = 64;
    private static final long LOOP_CYCLE_MILLIS = 1500L;
    private final ProceduralProcessRecipe recipe;
    private final BlockStatePredicate initialBlock;
    private final int stepSize;
    private final int stepX;
    private final int stepDx;

    public MDProceduralProcessRecipeComponent(ProceduralProcessRecipe recipe, boolean enableAlignCenter) {
        super(TEXTURE, WIDTH, HEIGHT, enableAlignCenter);
        this.recipe = recipe;
        this.initialBlock = recipe.initialBlock();
        this.stepSize = recipe.steps().size();
        int size = Math.clamp(this.stepSize, 1, 5);
        int gap = STEPS_LENGTH / size - STEP_LENGTH;
        this.stepX = STEP_X + gap / 2;
        this.stepDx = STEP_LENGTH + gap;
    }

    @Override
    protected void extractRecipeRenderState(MDRenderContext context, float mouseX, float mouseY) {
        var graphics = context.graphics();
        int blockY = AgeratumUtil.getRenderY(ANVIL_Y, BLOCK_ROW);
        AgeratumUtil.renderBlock(context, this.initialBlock, mouseX, mouseY, STEP_X - 20, blockY);
        int displayedLoop = getDisplayedLoop(this.recipe);
        for (int index = 0; index < this.stepSize; index++) {
            var step = getDisplayedStep(this.recipe, index, displayedLoop);
            if (step.getContent() instanceof AbstractProcessRecipe<?> stepRecipe) {
                this.renderStep(context, mouseX, mouseY, stepRecipe, index, displayedLoop);
            }
        }
        if (this.recipe.loop() > 1) {
            Component text = Component.literal((displayedLoop + 1) + "/" + this.recipe.loop()).withColor(0xB08E82);
            AgeratumUtil.renderText(graphics, text, STEP_X + 140, FLOW_Y + 4, 1.2F);
            graphics.blit(RenderPipelines.GUI_TEXTURED, CYCLE, STEP_X + 122, FLOW_Y, 0, 0, 16, 16, 16, 16);
            renderArrowLong(graphics, WIDTH / 2 - ARROW_LONG_LENGTH / 2 - 20, FLOW_Y);
        } else {
            renderArrowLong(graphics, WIDTH / 2 - ARROW_LONG_LENGTH / 2 - 10, FLOW_Y);
        }
        AgeratumUtil.renderBlock(context, this.recipe.resultBlock(), mouseX, mouseY, STEP_X + STEPS_LENGTH + 10, blockY);
    }

    public static void renderArrowLong(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, ARROW_LONG, x, y, 0, 0, ARROW_LONG_LENGTH, 14, ARROW_LONG_LENGTH, 14);
    }

    protected void renderStep(
        MDRenderContext context, float mouseX, float mouseY, AbstractProcessRecipe<?> recipe, int index, int displayedLoop
    ) {
        int anchor = RenderSupport.processAnchorIndex(recipe);
        int blockSize = Math.min(recipe.getInputBlocks().size(), 2);
        int blockX = this.getStepX(index, true);
        for (int block = blockSize - 1; block >= 0; block--) {
            var input = recipe.getInputBlocks().get(block);
            int blockY = AgeratumUtil.getRenderY(ANVIL_Y, BLOCK_ROW + block - anchor);
            if (isWip(input)) {
                if (input.constructStatesForRender().isEmpty()) continue;
                RenderSupport.renderWipBlockAt(context.graphics(),
                    this.recipe.getDisplayedModelForStep(displayedLoop * this.stepSize + index).orElse(null),
                    blockX, blockY, AgeratumUtil.BLOCK_SIZE);
                AgeratumUtil.renderTooltip(context, ModBlocks.WIP_BLOCK.getDefaultState(), mouseX, mouseY, blockX, blockY);
            } else {
                AgeratumUtil.renderBlock(context, input, mouseX, mouseY, blockX, blockY);
            }
        }
        AgeratumUtil.renderBlock(context, Blocks.ANVIL.defaultBlockState(), mouseX, mouseY, blockX, ANVIL_Y);
        if (!recipe.getInputItems().isEmpty()) {
            AgeratumUtil.renderItem(context, recipe.getInputItems().getFirst(), mouseX, mouseY,
                this.getStepX(index, false), AgeratumUtil.getRenderY(ANVIL_Y, 1) + ITEM_Y_OFFSET);
        }
    }

    protected int getStepX(int index, boolean isBlock) {
        return this.stepX + index * this.stepDx + (isBlock ? 8 : 0);
    }

    private static boolean isWip(BlockStatePredicate predicate) {
        return predicate.getBlocks().stream().allMatch(holder -> holder.value() instanceof WipBlock);
    }

    private static int getDisplayedLoop(ProceduralProcessRecipe recipe) {
        return recipe.loop() <= 1 ? 0 : (int) ((Util.getMillis() / LOOP_CYCLE_MILLIS) % recipe.loop());
    }

    private static ProceduralProcessStep getDisplayedStep(ProceduralProcessRecipe recipe, int stepIndex, int displayedLoop) {
        return stepIndex == 0 && displayedLoop > 0 ? recipe.multiLoopFirstStep().orElse(recipe.steps().getFirst())
            : recipe.steps().get(stepIndex);
    }
}
