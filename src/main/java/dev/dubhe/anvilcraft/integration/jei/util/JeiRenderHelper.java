package dev.dubhe.anvilcraft.integration.jei.util;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.state.BlockState;

public class JeiRenderHelper {
    // Animation
    public static float getAnvilAnimationOffset(ITickTimer timer) {
        return timer.getValue() < 30 ? getAnvilAnimationOffset(timer.getValue()) : 8;
    }

    public static float getAnvilAnimationOffset(float time) {
        return (float) Math.sin(time / 30d * 2d * Math.PI + Math.PI / 2) * 8;
    }

    // Arrow
    public static IDrawable getArrowDefault(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextureConstants.ARROW_DEFAULT, 0, 0, 16, 10)
            .setTextureSize(16, 10)
            .build();
    }

    public static IDrawable getArrowInput(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextureConstants.ARROW_INPUT, 0, 0, 16, 8)
            .setTextureSize(16, 8)
            .build();
    }

    public static IDrawable getArrowOutput(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextureConstants.ARROW_OUTPUT, 0, 0, 16, 10)
            .setTextureSize(16, 10)
            .build();
    }

    public static IDrawable getArrowOutputFromBelow(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextureConstants.ARROW_OUTPUT_FROM_BELOW, 0, 0, 14, 18)
            .setTextureSize(14, 18)
            .build();
    }

    public static IDrawable getArrowBlockConversion(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextureConstants.ARROW_BLOCK_CONVERSION, 0, 0, 14, 22)
            .setTextureSize(14, 22)
            .build();
    }

    public static IDrawable getArrowLong(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextureConstants.ARROW_LONG, 0, 0, 64, 10)
            .setTextureSize(64, 10)
            .build();
    }

    // Slot
    public static IDrawable getSlotDefault(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextureConstants.SLOT_DEFAULT, 0, 0, 18, 18)
            .setTextureSize(18, 18)
            .build();
    }

    public static IDrawable getSlotChoice(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextureConstants.SLOT_CHOICE, 0, 0, 18, 18)
            .setTextureSize(18, 18)
            .build();
    }

    public static IDrawable getSlotProbability(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextureConstants.SLOT_PROBABILITY, 0, 0, 18, 18)
            .setTextureSize(18, 18)
            .build();
    }

    // Other
    public static IDrawable getExplosion(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextureConstants.EXPLOSION, 0, 0, 32, 32)
            .setTextureSize(32, 32)
            .build();
    }

    public static IDrawable getCycle(IGuiHelper helper) {
        return helper.drawableBuilder(JeiTextureConstants.CYCLE, 0, 0, 16, 16)
            .setTextureSize(16, 16)
            .build();
    }

    public static void renderBlockWithSlot(
        GuiGraphics graphics,
        IDrawable slot,
        BlockState state,
        float x,
        float y,
        float z,
        RenderSupport.BlockRenderFunction function
    ) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, z - 1);
        slot.draw(graphics);
        RenderSupport.renderBlock(
            graphics,
            state,
            9,
            5.5F,
            z,
            11.5F,
            function
        );
        pose.popPose();
    }
}
