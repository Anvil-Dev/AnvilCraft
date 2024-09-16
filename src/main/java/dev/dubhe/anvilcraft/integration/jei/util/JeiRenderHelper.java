package dev.dubhe.anvilcraft.integration.jei.util;

import mezz.jei.api.gui.ITickTimer;

public class JeiRenderHelper {
    public static float getAnvilAnimationOffset(ITickTimer timer) {
        return timer.getValue() < 30 ? getAnvilAnimationOffset(timer.getValue()) : 8;
    }

    public static float getAnvilAnimationOffset(float time) {
        return (float) Math.sin(time / 30d * 2d * Math.PI + Math.PI / 2) * 8;
    }
}
