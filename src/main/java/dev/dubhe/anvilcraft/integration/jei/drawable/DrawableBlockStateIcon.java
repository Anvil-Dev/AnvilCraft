package dev.dubhe.anvilcraft.integration.jei.drawable;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.state.BlockState;

public class DrawableBlockStateIcon implements IDrawable {
    private final BlockState upState;
    private final BlockState downState;

    public DrawableBlockStateIcon(BlockState upState, BlockState downState) {
        this.upState = upState;
        this.downState = downState;
    }

    @Override
    public int getWidth() {
        return 16;
    }

    @Override
    public int getHeight() {
        return 16;
    }

    @Override
    public void draw(GuiGraphics guiGraphics, int offsetX, int offsetY) {
        RenderSupport.renderBlock(guiGraphics, upState, offsetX + 8, offsetY + 3, 10, 7, RenderSupport.SINGLE_BLOCK);
        RenderSupport.renderBlock(guiGraphics, downState, offsetX + 8, offsetY + 9, 0, 7, RenderSupport.SINGLE_BLOCK);
    }
}
