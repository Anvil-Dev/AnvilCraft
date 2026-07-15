package dev.dubhe.anvilcraft.client.hud;

import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class AnvilHammerUseHUD {
    private AnvilHammerUseHUD() {
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.screen != null) return;
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isUsingItem()) return;
        if (!(player.getUseItem().getItem() instanceof AnvilHammerItem)) return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(minecraft.isPaused());
        int remainingTicks = player.getUseItemRemainingTicks();
        float progress = Mth.clamp(
            (AnvilHammerItem.PORTABLE_ANVIL_USE_TICKS - remainingTicks + partialTick)
            / AnvilHammerItem.PORTABLE_ANVIL_USE_TICKS,
            0.0F,
            1.0F
        );
        if (progress <= 0.0F) return;

        AttackIndicatorProgressHUD.render(graphics, progress);
    }
}
