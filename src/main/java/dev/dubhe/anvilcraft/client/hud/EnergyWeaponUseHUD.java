package dev.dubhe.anvilcraft.client.hud;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.network.WeaponChargeProgressPacket;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class EnergyWeaponUseHUD {
    @Nullable
    private static WeaponChargeProgressPacket sample;
    private static long receivedAt;
    private static double pausedElapsed;
    private static boolean paused;

    private EnergyWeaponUseHUD() {
    }

    public static void update(WeaponChargeProgressPacket progress) {
        sample = progress;
        receivedAt = Util.getMillis();
        paused = false;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        sample = null;
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isUsingItem()) {
            sample = null;
            return;
        }
        if (sample == null || sample.item() != BuiltInRegistries.ITEM.getId(player.getUseItem().getItem())
            || sample.hand() != player.getUsedItemHand().ordinal()) return;
        long now = Util.getMillis();
        if (minecraft.isPaused()) {
            if (!paused) pausedElapsed = now - receivedAt;
            paused = true;
        } else if (paused) {
            receivedAt = now - (long) pausedElapsed;
            paused = false;
        }
        if (minecraft.options.hideGui || minecraft.screen != null) return;
        float progress = sample.progressAfter(paused ? pausedElapsed : now - receivedAt);
        if (progress > 0) AttackIndicatorProgressHUD.render(graphics, progress);
    }
}
