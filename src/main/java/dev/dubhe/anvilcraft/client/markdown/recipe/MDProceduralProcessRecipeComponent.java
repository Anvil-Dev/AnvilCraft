package dev.dubhe.anvilcraft.client.markdown.recipe;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
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
    public static final ResourceLocation CIRCLE = AnvilCraft.of("textures/gui/ageratum/procedural_process_circle.png");
    public static final int STEP_Y = 10;
    public static final int STEP_X = 80;
    public static final int STEPS_LENGTH = 210;
    public static final int STEP_LENGTH = 30;

    public static final int ANVIL_Y = STEP_Y + 16;

    private final ProceduralProcessRecipe recipe;
    private final BlockStatePredicate initialBlock;

    private final int stepSize;
    private final int stepX;
    private final int stepDx;

    public MDProceduralProcessRecipeComponent(ProceduralProcessRecipe recipe, boolean enableAlignCenter) {
        super(TEXTURE, 384, 128, enableAlignCenter);
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
        AgeratumUtil.renderBlock(context, this.initialBlock, mouseX, mouseY, 34, blockY, 0);

        for (int i = 0; i < this.stepSize; i++) {
            ProceduralProcessStep step = this.recipe.getSteps().get(i);
            if (!(step.getContent() instanceof AbstractProcessRecipe<?> stepRecipe)) continue;
            this.renderStep(context, mouseX, mouseY, stepRecipe, i);
        }

        // loop
        if (recipe.getLoop() > 1) {
            Component text = Component.literal(String.valueOf(recipe.getLoop())).withColor(0xB08E82);
            AgeratumUtil.renderText(graphics, text, 280, 107);
            graphics.blit(CIRCLE, 266, 93, 0, 0, 32, 32, 32, 32);
        }

        // result
        AgeratumUtil.renderBlock(context, this.recipe.getResultBlock(), mouseX, mouseY, 324, blockY, 0);
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
            if (!inputBlock.constructStatesForRender().getFirst().is(ModBlocks.WIP_BLOCK)) continue;

            AgeratumUtil.renderBlock(context, this.initialBlock, mouseX, mouseY, this.getStepX(idx, true), blockY, (blockSize - i) * 10);
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
