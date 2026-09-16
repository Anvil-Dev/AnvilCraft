package dev.dubhe.anvilcraft.client.markdown.recipe;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.WipBlock;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessStep;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class MDProceduralProcessRecipeComponent extends MDRecipeComponent {
    public static final ResourceLocation TEXTURE = AnvilCraft.of("textures/gui/ageratum/procedural_process.png");
    public static final ResourceLocation CYCLE = AnvilCraft.of("textures/gui/ageratum/cycle.png");
    public static final ResourceLocation ARROW_LONG = AnvilCraft.of("textures/gui/ageratum/arrow_long.png");

    public static final int WIDTH = 384;
    public static final int HEIGHT = 128;
    public static final int STEPS_LENGTH = 210;
    public static final int STEP_X = (WIDTH - STEPS_LENGTH) / 2;
    public static final int STEP_Y = 5;
    public static final int STEP_LENGTH = 30;

    public static final int ANVIL_Y = STEP_Y + 16;

    public static final int ARROW_LONG_LENGTH = 64;

    /** 多圈配方的展示轮换周期，与 JEI 分类保持一致。 */
    private static final long LOOP_CYCLE_MILLIS = 1500L;

    private final ProceduralProcessRecipe recipe;
    private final BlockStatePredicate initialBlock;

    private final int stepSize;
    private final int stepX;
    private final int stepDx;

    public MDProceduralProcessRecipeComponent(ProceduralProcessRecipe recipe, boolean enableAlignCenter) {
        super(TEXTURE, WIDTH, HEIGHT, enableAlignCenter);
        this.recipe = recipe;
        this.initialBlock = this.recipe.getInitialBlock();
        this.stepSize = recipe.getSteps().size();
        // 使得step均匀分布
        int size = Math.clamp(this.stepSize, 1, 5);
        int gap = STEPS_LENGTH / size - STEP_LENGTH;
        this.stepX = STEP_X + gap / 2;
        this.stepDx = STEP_LENGTH + gap;
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        GuiGraphics graphics = context.graphics();

        // input
        int blockY = AgeratumUtil.getRenderY(ANVIL_Y, 3);
        AgeratumUtil.renderBlock(context, this.initialBlock, mouseX, mouseY, STEP_X - 20, blockY, 0);

        // step
        // 多圈配方按时间轮换展示每一圈，使首步随圈数替换的方块（multi_loop_first_step）也能看到
        int displayedLoop = getDisplayedLoop(this.recipe);
        for (int i = 0; i < this.stepSize; i++) {
            ProceduralProcessStep step = getDisplayedStep(this.recipe, i, displayedLoop);
            if (!(step.getContent() instanceof AbstractProcessRecipe<?> stepRecipe)) continue;
            this.renderStep(context, mouseX, mouseY, stepRecipe, i, displayedLoop);
        }

        // loop
        if (recipe.getLoop() > 1) {
            Component text = Component.literal((displayedLoop + 1) + "/" + recipe.getLoop()).withColor(0xB08E82);
            AgeratumUtil.renderText(graphics, text, STEP_X + 140, 100, 1.2f);
            graphics.blit(CYCLE, STEP_X + 122, 96, 0, 0, 16, 16, 16, 16);
            renderArrowLong(graphics, WIDTH / 2 - ARROW_LONG_LENGTH / 2 - 20, 96);
        } else {
            renderArrowLong(graphics, WIDTH / 2 - ARROW_LONG_LENGTH / 2 - 10, 96);
        }

        // result
        AgeratumUtil.renderBlock(context, this.recipe.getResultBlock(), mouseX, mouseY, STEP_X + STEPS_LENGTH + 10, blockY, 0);
    }

    public static void renderArrowLong(GuiGraphics g, int x, int y) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        g.blit(ARROW_LONG, 0, 0, 0, 0, ARROW_LONG_LENGTH, 14, ARROW_LONG_LENGTH, 14);
        pose.popPose();
    }

    protected void renderStep(
        MDRenderContext context,
        float mouseX,
        float mouseY,
        AbstractProcessRecipe<?> stepRecipe,
        int idx,
        int displayedLoop
    ) {
        // Anvil
        AgeratumUtil.renderBlock(context, Blocks.ANVIL.defaultBlockState(), mouseX, mouseY, this.getStepX(idx, true), ANVIL_Y, 100);

        // Block
        int blockSize = Math.min(stepRecipe.getInputBlocks().size(), 2);
        for (int i = 0; i < blockSize; i++) {
            BlockStatePredicate inputBlock = stepRecipe.getInputBlocks().get(i);
            int blockX = this.getStepX(idx, true);
            int blockY = AgeratumUtil.getRenderY(ANVIL_Y, i + 3);
            int z = (blockSize - i) * 10;
            // WIP 中间态的外观取自配方的 displayedModels，与其自身 blockstate 无关。
            // 步数取该圈已完成的步数，与运行时一致。
            // 进程方块自身的半透明虚影仍需一并绘制，故走 renderBlock 叠加而非替换。
            if (isWip(inputBlock)) {
                AgeratumUtil.renderBlock(
                    context, inputBlock, mouseX, mouseY, blockX, blockY, z,
                    RenderSupport.wipDisplay(this.recipe, displayedLoop * this.stepSize + idx)
                );
            } else {
                AgeratumUtil.renderBlock(context, inputBlock, mouseX, mouseY, blockX, blockY, z);
            }
        }

        // Item
        int itemSize = Math.min(stepRecipe.getInputItems().size(), 1);
        for (int i = 0; i < itemSize; i++) {
            ItemIngredientPredicate inputItem = stepRecipe.getInputItems().get(i);
            int itemY = AgeratumUtil.getRenderY(ANVIL_Y, 1) + 8;
            AgeratumUtil.renderItem(context, inputItem, mouseX, mouseY, this.getStepX(idx, false), itemY);
        }
    }

    protected int getStepX(int idx, boolean isBlock) {
        return this.stepX + idx * this.stepDx + (isBlock ? 8 : 0);
    }

    /** 该方块谓词是否只指向进程方块（WIP 中间态）。 */
    private static boolean isWip(BlockStatePredicate predicate) {
        return predicate.getBlocks().stream().allMatch(holder -> holder.value() instanceof WipBlock);
    }

    /**
     * 当前展示到第几圈（从 0 开始），按时间轮换；单圈配方恒为 0。
     *
     * <p>与 JEI 分类保持相同的节奏与语义，使同一配方在两处的表现一致。</p>
     */
    private static int getDisplayedLoop(ProceduralProcessRecipe recipe) {
        if (recipe.getLoop() <= 1) return 0;
        return (int) ((Util.getMillis() / LOOP_CYCLE_MILLIS) % recipe.getLoop());
    }

    /**
     * 取该圈实际执行的步骤：多圈配方第一圈之外的第一个步骤会被替换为
     * {@code multi_loop_first_step}，与运行时的执行顺序一致。
     */
    private static ProceduralProcessStep getDisplayedStep(
        ProceduralProcessRecipe recipe,
        int stepIndex,
        int displayedLoop
    ) {
        if (stepIndex == 0 && displayedLoop > 0) {
            return recipe.getMultiLoopFirstStep().orElse(recipe.getSteps().getFirst());
        }
        return recipe.getSteps().get(stepIndex);
    }
}
