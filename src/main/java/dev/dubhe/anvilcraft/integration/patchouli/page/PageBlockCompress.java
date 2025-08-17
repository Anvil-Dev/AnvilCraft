package dev.dubhe.anvilcraft.integration.patchouli.page;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.patchouli.util.PatchouliRenderHelper;
import dev.dubhe.anvilcraft.recipe.anvil.util.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceBlockState;
import dev.dubhe.anvilcraft.util.RenderHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.patchouli.client.book.gui.GuiBook;
import vazkii.patchouli.client.book.page.abstr.PageDoubleRecipeRegistry;

import java.util.List;

public class PageBlockCompress extends PageDoubleRecipeRegistry<BlockCompressRecipe> {
    public PageBlockCompress() {
        super(ModRecipeTypes.BLOCK_COMPRESS_TYPE.get());
    }

    @Override
    protected void drawRecipe(
        GuiGraphics graphics, BlockCompressRecipe recipe,
        int recipeX, int recipeY, int mouseX, int mouseY, boolean second
    ) {
        PatchouliRenderHelper.renderAnvilWithAnimation(parent, graphics, recipeX + 20, recipeY + 10);

        List<BlockStatePredicate> inputs = recipe.getInputs();
        for (int i = 0; i < Math.min(inputs.size(), 2); i++) {
            List<BlockState> states = inputs.get(i).constructStatesForRender();
            BlockState state = states.get((parent.ticksInBook / 20) % states.size());
            RenderHelper.renderBlock(
                graphics, state,
                recipeX + 20, recipeY + 26 + i * 10, -i * 10,
                12,
                RenderHelper.SINGLE_BLOCK);
        }

        PatchouliRenderHelper.renderArray(graphics, recipeX + 45, recipeY + 25);

        List<ChanceBlockState> results = recipe.getResults();
        RenderHelper.renderBlock(
            graphics, results.get((parent.ticksInBook / 20) % results.size()).getState(),
            recipeX + 80, recipeY + 37, 0,
            12,
            RenderHelper.SINGLE_BLOCK);

        parent.drawCenteredStringNoShadow(
            graphics, getTitle(second).getVisualOrderText(),
            GuiBook.PAGE_WIDTH / 2, recipeY - 5,
            book.headerColor);
    }

    @Override
    protected ItemStack getRecipeOutput(Level level, BlockCompressRecipe recipe) {
        return recipe.getResultItem(level.registryAccess());
    }

    @Override
    protected int getRecipeHeight() {
        return 78;
    }
}
