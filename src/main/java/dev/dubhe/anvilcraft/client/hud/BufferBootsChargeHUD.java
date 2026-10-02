package dev.dubhe.anvilcraft.client.hud;

import dev.dubhe.anvilcraft.client.init.ModRenderPipelines;
import dev.dubhe.anvilcraft.util.ColorUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class BufferBootsChargeHUD {
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("hud/experience_bar_background");
    private static final Identifier PROGRESS = Identifier.withDefaultNamespace("hud/experience_bar_progress");
    /** 蓄力中：随进度由蓝渐变到橙。 */
    private static final int CHARGING_LOW = 0xFF328CFF;
    private static final int CHARGING_HIGH = 0xFFFF9500;
    /** 停留阶段：进度已定格，换用亮绿表示此刻可以起跳。 */
    private static final int HELD = 0xFF3CE65A;

    private BufferBootsChargeHUD() {
    }

    public static int color(float progress) {
        return ColorUtil.lerpColor(Math.clamp(progress, 0, 1), CHARGING_LOW, CHARGING_HIGH);
    }

    /**
     * 停留阶段固定为 {@link #HELD}，其余情况随进度渐变。
     */
    public static int color(float progress, boolean held) {
        return held ? HELD : color(progress);
    }

    public static void render(GuiGraphicsExtractor graphics, float progress, boolean held) {
        float fraction = Math.clamp(progress, 0, 1);
        int left = graphics.guiWidth() / 2 - 91;
        int top = graphics.guiHeight() - 29;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, 182, 5);
        int width = Math.min(182, (int) (fraction * 183));
        if (width <= 0) return;
        graphics.blitSprite(ModRenderPipelines.EQUIPMENT_CHARGE, PROGRESS, 182, 5, 0, 0, left, top, width, 5, color(fraction, held));
    }
}
