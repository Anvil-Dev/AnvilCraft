package dev.dubhe.anvilcraft.client.markdown.recipe;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessStep;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
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
        for (int i = 0; i < this.stepSize; i++) {
            ProceduralProcessStep step = this.recipe.getSteps().get(i);
            if (!(step.getContent() instanceof AbstractProcessRecipe<?> stepRecipe)) continue;
            this.renderStep(context, mouseX, mouseY, stepRecipe, i);
        }

        // loop
        if (recipe.getLoop() > 1) {
            Component text = Component.literal(String.valueOf(recipe.getLoop())).withColor(0xB08E82);
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

    protected void renderStep(MDRenderContext context, float mouseX, float mouseY, AbstractProcessRecipe<?> stepRecipe, int idx) {
        // Anvil
        AgeratumUtil.renderBlock(context, Blocks.ANVIL.defaultBlockState(), mouseX, mouseY, this.getStepX(idx, true), ANVIL_Y, 100);

        // Block
        int blockSize = Math.min(stepRecipe.getInputBlocks().size(), 2);
        for (int i = 0; i < blockSize; i++) {
            BlockStatePredicate inputBlock = stepRecipe.getInputBlocks().get(i);
            int blockY = AgeratumUtil.getRenderY(ANVIL_Y, i + 3);
            AgeratumUtil.renderBlock(context, inputBlock, mouseX, mouseY, this.getStepX(idx, true), blockY, (blockSize - i) * 10);
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
}
