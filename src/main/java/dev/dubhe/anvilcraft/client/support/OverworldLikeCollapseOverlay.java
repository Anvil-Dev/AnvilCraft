package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Full-screen white flash shown to players caught in a collapsing generation. */
public final class OverworldLikeCollapseOverlay {
    private OverworldLikeCollapseOverlay() {
    }

    public static void render(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !CelestialTravelManager.isOverworldLike(minecraft.level.dimension())) return;
        float progress = OverworldLikeClientState.collapseProgress();
        if (progress <= 0.0F) return;
        float eased = progress * progress * (3.0F - 2.0F * progress);
        int alpha = Mth.clamp(Math.round(40.0F + eased * 215.0F), 0, 255);
        guiGraphics.fill(0, 0, guiGraphics.guiWidth(), guiGraphics.guiHeight(), alpha << 24 | 0x00FFFFFF);
    }
}
