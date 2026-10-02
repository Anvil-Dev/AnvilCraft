package dev.dubhe.anvilcraft.client.gui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class SmithingPreview {
    private static final Vector3f TRANSLATION = new Vector3f(0.0F, 1.0F, 0.0F);

    private SmithingPreview() {
    }

    public static void extract(GuiGraphicsExtractor graphics, ArmorStandRenderState state, Quaternionf angle, int left, int top) {
        // PIP renders around its rectangle center; preserve the source preview origin at (149, 75).
        graphics.entity(state, 25, TRANSLATION, angle, null,
            left + 129, top + 20, left + 169, top + 80);
    }
}
