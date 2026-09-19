package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.inventory.PocketInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.ClientHooks;

import java.util.ArrayList;
import java.util.List;

public final class PocketEffectLayout {
    private PocketEffectLayout() {
    }

    public static boolean isEnabled(AbstractContainerScreen<?> screen) {
        var player = Minecraft.getInstance().player;
        return player != null && PocketInventory.capacity(player) > 0
            && (screen instanceof InventoryScreen
            || screen instanceof CreativeModeInventoryScreen creative && creative.isInventoryOpen());
    }

    public static List<MobEffectInstance> visibleEffects() {
        var player = Minecraft.getInstance().player;
        return player == null ? List.of() : player.getActiveEffects().stream()
            .filter(ClientHooks::shouldRenderEffect).sorted().toList();
    }

    public static List<Rect2i> areas(AbstractContainerScreen<?> screen, int count) {
        return areas(screen.width, screen.getGuiLeft(), screen.getGuiTop(), screen.getXSize(),
            screen instanceof CreativeModeInventoryScreen, count);
    }

    public static List<Rect2i> areas(int screenWidth, int left, int top, int imageWidth, boolean creative, int count) {
        if (count == 0) return List.of();
        int bottom = Math.max(18, top - (creative ? 28 : 0));
        int size = Math.min(32, bottom - 2);
        List<Rect2i> areas = new ArrayList<>();
        if (creative) {
            int halfWidth = (imageWidth - 48 - 36) / 2;
            int firstCount = (count + 1) / 2;
            size = Math.min(size, Math.max(16, halfWidth / firstCount));
            int posY = Math.max(0, bottom - size - 2);
            addRow(areas, left + 24, posY, halfWidth, size, firstCount);
            addRow(areas, left + imageWidth / 2 + 18, posY, halfWidth, size, count - firstCount);
        } else {
            int available = Math.max(size, screenWidth - 4);
            int width = Math.min(available, count * (size + 1) - 1);
            int start = Math.clamp(left + (imageWidth - width) / 2, 2, Math.max(2, screenWidth - width - 2));
            addRow(areas, start, Math.max(0, bottom - size - 2), width, size, count);
        }
        return areas;
    }

    private static void addRow(List<Rect2i> areas, int left, int top, int width, int size, int count) {
        if (count == 0) return;
        int spacing = count == 1 ? 0 : Math.min(size + 1, (width - size) / (count - 1));
        int start = left + (width - size - spacing * (count - 1)) / 2;
        for (int index = 0; index < count; index++) areas.add(new Rect2i(start + index * spacing, top, size, size));
    }
}
