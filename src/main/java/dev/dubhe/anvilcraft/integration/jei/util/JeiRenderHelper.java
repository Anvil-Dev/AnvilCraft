package dev.dubhe.anvilcraft.integration.jei.util;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3x2fStack;

public class JeiRenderHelper {
    // Animation
    public static int getAnvilAnimationOffset(ITickTimer timer) {
        return timer.getValue() < 30 ? JeiRenderHelper.getAnvilAnimationOffset(timer.getValue()) : 8;
    }

    public static int getAnvilAnimationOffset(float time) {
        return (int) Math.round(Math.sin(time / 30d * 2d * Math.PI + Math.PI / 2) * 8);
    }

    // Arrow
    public static IDrawable getArrowDefault(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextures.ARROW_DEFAULT, 0, 0, 16, 10)
            .setTextureSize(16, 10)
            .build();
    }

    public static IDrawable getArrowInput(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextures.ARROW_INPUT, 0, 0, 16, 8)
            .setTextureSize(16, 8)
            .build();
    }

    public static IDrawable getArrowLong(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextures.ARROW_LONG, 0, 0, 64, 10)
            .setTextureSize(64, 10)
            .build();
    }

    public static IDrawable getArrowOutput(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextures.ARROW_OUTPUT, 0, 0, 16, 10)
            .setTextureSize(16, 10)
            .build();
    }

    public static IDrawable getArrowOutputFromBelow(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextures.ARROW_OUTPUT_FROM_BELOW, 0, 0, 14, 18)
            .setTextureSize(14, 18)
            .build();
    }

    public static IDrawable getArrowBlockConversion(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextures.ARROW_BLOCK_CONVERSION, 0, 0, 14, 22)
            .setTextureSize(14, 22)
            .build();
    }

    // Slot
    public static IDrawable getSlotDefault(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextures.SLOT_DEFAULT, 0, 0, 18, 18)
            .setTextureSize(18, 18)
            .build();
    }

    public static IDrawable getSlotChoice(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextures.SLOT_CHOICE, 0, 0, 18, 18)
            .setTextureSize(18, 18)
            .build();
    }

    public static IDrawable getSlotProbability(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextures.SLOT_PROBABILITY, 0, 0, 18, 18)
            .setTextureSize(18, 18)
            .build();
    }

    // Other
    public static IDrawable getExplosion(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextures.EXPLOSION, 0, 0, 32, 32)
            .setTextureSize(32, 32)
            .build();
    }

    public static IDrawable getCycle(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextures.CYCLE, 0, 0, 16, 16)
            .setTextureSize(16, 16)
            .build();
    }

    public static void renderBlockWithSlot(
        GuiGraphicsExtractor graphics,
        IDrawable slot,
        BlockState state,
        float x,
        float y
    ) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y + 0.5F);
        slot.draw(graphics);
        // FIXME: Non-transparent blocks are rendered behind the slot
        RenderSupport.renderBlock(
            graphics,
            state,
            0,
            1,
            18
        );
        pose.popMatrix();
    }
}
