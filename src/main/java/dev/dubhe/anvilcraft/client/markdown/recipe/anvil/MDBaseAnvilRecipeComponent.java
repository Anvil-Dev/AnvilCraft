package dev.dubhe.anvilcraft.client.markdown.recipe.anvil;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public abstract class MDBaseAnvilRecipeComponent extends MDRecipeComponent {
    public static final Identifier TEXTURE = AnvilCraft.of("textures/gui/ageratum/256back.png");

    public static final int BLOCK_Y = 64;
    public static final int INPUT_BLOCK_X = 128;
    public static final int OUTPUT_BLOCK_X = 210;

    public MDBaseAnvilRecipeComponent(boolean enableAlignCenter) {
        super(TEXTURE, 256, 128, enableAlignCenter);
    }

    protected List<ItemIngredientPredicate> getIngredients() {
        return List.of();
    }

    protected List<ChanceItemStack> getResultItems() {
        return List.of();
    }

    protected List<BlockState> getInputBlockStates() {
        return List.of();
    }

    protected BlockState getOutputBlockState() {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void extractRecipeRenderState(MDRenderContext context, float mouseX, float mouseY) {
        GuiGraphicsExtractor g = context.graphics();
        AgeratumUtil.renderItems(context, this.getIngredients(), mouseX, mouseY, 40, 46);
        if (!this.getIngredients().isEmpty()) {
            AgeratumUtil.renderArrow(g, 86, 40);
        }
        int anvilY = BLOCK_Y - 2 * AgeratumUtil.BLOCK_SIZE;
        AgeratumUtil.renderBlock(context, Blocks.ANVIL.defaultBlockState(), mouseX, mouseY, INPUT_BLOCK_X, anvilY);
        for (int i = 0; i < this.getInputBlockStates().size(); i++) {
            BlockState inputBlock = this.getInputBlockStates().get(i);
            int y = BLOCK_Y + i * AgeratumUtil.BLOCK_SIZE - 1;
            AgeratumUtil.renderBlock(context, inputBlock, mouseX, mouseY, INPUT_BLOCK_X, y);
        }
        AgeratumUtil.renderArrow(g, 138, 40);
        AgeratumUtil.renderItems(context, this.getResultItems(), mouseX, mouseY, 194, 46);
        AgeratumUtil.renderBlock(context, this.getOutputBlockState(), mouseX, mouseY, OUTPUT_BLOCK_X, BLOCK_Y);
    }
}
