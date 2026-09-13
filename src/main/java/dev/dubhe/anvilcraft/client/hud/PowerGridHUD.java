package dev.dubhe.anvilcraft.client.hud;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class PowerGridHUD {
    private static final int NORMAL_COLOR = 0x55AAFF;
    private static final int OVERLOADED_COLOR = 0x8B2020;
    private static final int EXPERIENCE_BAR_WIDTH = 182;
    private static final int TRANSITION_TICKS = 4;
    private static @Nullable LocalPlayer trackedPlayer;
    private static float previousVisibility;
    private static float visibility;
    private static int color = NORMAL_COLOR;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (trackedPlayer != minecraft.player) {
            trackedPlayer = minecraft.player;
            previousVisibility = 0;
            visibility = 0;
            color = NORMAL_COLOR;
        }
        if (trackedPlayer == null || minecraft.level == null || minecraft.isPaused()) return;
        previousVisibility = visibility;
        boolean inGrid = trackedPlayer.getData(ModDataAttachments.IN_POWER_GRID);
        visibility = Mth.clamp(visibility + (inGrid ? 1f : -1f) / TRANSITION_TICKS, 0, 1);
        if (inGrid) {
            color = trackedPlayer.getData(ModDataAttachments.POWER_GRID_OVERLOADED) ? OVERLOADED_COLOR : NORMAL_COLOR;
        }
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.level == null
            || trackedPlayer != minecraft.player || minecraft.player.isCreative() || minecraft.player.isSpectator()) return;
        float partialTick = minecraft.isPaused() ? 1 : deltaTracker.getGameTimeDeltaPartialTick(false);
        float amount = Mth.lerp(partialTick, previousVisibility, visibility);
        int alpha = Math.round(amount * 255);
        if (alpha == 0) return;

        int drawColor = alpha << 24 | color;
        int slide = Math.round((1 - amount) * 8);
        int left = graphics.guiWidth() / 2 - EXPERIENCE_BAR_WIDTH / 2 + slide;
        int right = graphics.guiWidth() / 2 + EXPERIENCE_BAR_WIDTH / 2 - slide;
        int top = graphics.guiHeight() - 30;
        for (int row = 0; row < 7; row++) {
            int inset = Math.abs(row - 3);
            graphics.fill(left - 8 + inset, top + row, left - 6 + inset, top + row + 1, drawColor);
            graphics.fill(right + 6 - inset, top + row, right + 8 - inset, top + row + 1, drawColor);
        }
    }
}
