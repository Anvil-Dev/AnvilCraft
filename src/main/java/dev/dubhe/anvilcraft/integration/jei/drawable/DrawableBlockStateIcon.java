package dev.dubhe.anvilcraft.integration.jei.drawable;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3x2fStack;

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
    public void draw(GuiGraphicsExtractor graphics, int offsetX, int offsetY) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(0F, -0.5F);
        RenderSupport.renderBlock(graphics, this.downState, offsetX + 2, offsetY + 6, 12);
        RenderSupport.renderBlock(graphics, this.upState, offsetX + 2, offsetY, 12);
        pose.popMatrix();
    }
}
