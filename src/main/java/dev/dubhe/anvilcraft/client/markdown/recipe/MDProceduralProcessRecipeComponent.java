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
    /**
     * 组件高度。底图完全透明，尺寸只用于在手册页面里占位。按内容实际高度取值，
     * 同时在各组元素之间留出便于分辨的间距。
     */
    public static final int HEIGHT = 110;
    public static final int STEPS_LENGTH = 210;
    public static final int STEP_X = (WIDTH - STEPS_LENGTH) / 2;
    public static final int STEP_LENGTH = 30;

    public static final int ANVIL_Y = 18;

    /**
     * 显示主线所在的行号：主体方块固定画在这一行，上方一行留给「从上面砸进去」的方块，
     * 下方一行留给垫底的方块（辐照器、加热器、腐化信标等）。
     */
    public static final int BLOCK_ROW = 3;

    /** 循环图标与长箭头所在的行。 */
    public static final int FLOW_Y = 90;

    /** 物品槽（图标与其底板）相对铁砧下方那行的纵向微调，决定物品与铁砧之间的间距。 */
    public static final int ITEM_Y_OFFSET = 8;

    public static final int ARROW_LONG_LENGTH = 64;

    /** 多圈配方的展示轮换周期，与 JEI 分类保持一致。 */

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
        int blockY = AgeratumUtil.getRenderY(ANVIL_Y, BLOCK_ROW);
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
            AgeratumUtil.renderText(graphics, text, STEP_X + 140, FLOW_Y + 4, 1.2f);
            graphics.blit(CYCLE, STEP_X + 122, FLOW_Y, 0, 0, 16, 16, 16, 16);
            renderArrowLong(graphics, WIDTH / 2 - ARROW_LONG_LENGTH / 2 - 20, FLOW_Y);
        } else {
            renderArrowLong(graphics, WIDTH / 2 - ARROW_LONG_LENGTH / 2 - 10, FLOW_Y);
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
        // 主体（产出方块落点所在的那个输入）固定占主线行，其余输入按与它的世界上下
        // 关系向上/下偏移，使各步的主线始终对齐。
        int anchor = anchorIndex(stepRecipe);
        int blockSize = Math.min(stepRecipe.getInputBlocks().size(), 2);
        for (int i = 0; i < blockSize; i++) {
            BlockStatePredicate inputBlock = stepRecipe.getInputBlocks().get(i);
            int blockX = this.getStepX(idx, true);
            int blockY = AgeratumUtil.getRenderY(ANVIL_Y, BLOCK_ROW + i - anchor);
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
            int itemY = AgeratumUtil.getRenderY(ANVIL_Y, 1) + ITEM_Y_OFFSET;
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
     * 该步骤的显示主线落在第几个输入方块上。
     *
     * <p>主线即该步骤正在被加工的主体，也就是产出方块落点所在的那格输入：下标 {@code i} 的
     * 输入位于 {@code blockInputOffset} 往下 {@code i} 格，产出位于 {@code blockOutputOffset}，
     * 两者重合的那个下标就是主体。方块压缩把两块压成一块、产物落在再下一格（偏移 -2），
     * 主体是下面那块、上面那块是从上面砸进去的；方块处理与物品注入原地成型（偏移 -1），
     * 主体就是铁砧正下方那块。主体固定在主线行上不动，其余输入按它在世界里的相对高度
     * 向上或向下偏移，避免主体在各步之间上下跳动。</p>
     */
    private static int anchorIndex(AbstractProcessRecipe<?> stepRecipe) {
        double inputY = stepRecipe.getProperty().getBlockInputOffset().y;
        double outputY = stepRecipe.getProperty().getBlockOutputOffset().y;
        int index = (int) Math.round(inputY - outputY);
        return Math.clamp(index, 0, Math.max(stepRecipe.getInputBlocks().size() - 1, 0));
    }

    /**
     * 当前展示到第几圈（从 0 开始），按时间轮换；单圈配方恒为 0。
     *
     * <p>与 JEI 分类保持相同的节奏与语义，使同一配方在两处的表现一致。</p>
     */
    private static int getDisplayedLoop(ProceduralProcessRecipe recipe) {
        if (recipe.getLoop() <= 1) return 0;
        return (int) ((Util.getMillis() / AnvilCraft.CLIENT_CONFIG.ui.recipePreviewCycleMillis) % recipe.getLoop());
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
