package dev.dubhe.anvilcraft.integration.jei.drawable;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
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
    public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
        RenderSupport.renderBlock(guiGraphics, upState, xOffset + 8, yOffset + 3, 10, 7, RenderSupport.SINGLE_BLOCK);
        RenderSupport.renderBlock(guiGraphics, downState, xOffset + 8, yOffset + 9, 0, 7, RenderSupport.SINGLE_BLOCK);
    }
}
