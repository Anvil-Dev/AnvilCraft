package dev.dubhe.anvilcraft.integration.jei.util;

import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.inventory.PocketLayout;
import dev.dubhe.anvilcraft.inventory.PocketSlot;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class PocketGuiHandler<T extends AbstractContainerScreen<?>> implements IGuiContainerHandler<T> {
    @Override
    public List<Rect2i> getGuiExtraAreas(T screen) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return List.of();
        boolean creative = screen instanceof CreativeModeInventoryScreen;
        if (creative && !((CreativeModeInventoryScreen) screen).isInventoryOpen()) return List.of();
        return areas(screen.getLeftPos(), screen.getTopPos(), screen.getImageWidth(), PocketInventory.capacity(player), creative);
    }

    public static List<Rect2i> areas(int left, int top, int imageWidth, int capacity, boolean creative) {
        if (capacity == 0) {
            return List.of();
        }
        int posY = creative ? PocketLayout.CREATIVE_PANEL_TOP : PocketSlot.SCREEN_PANEL_TOP;
        List<Rect2i> areas = new ArrayList<>(4);
        for (int side = 0; side < 2; side++) {
            for (PocketLayout.Segment segment : PocketLayout.of(capacity, side, imageWidth, posY).segments()) {
                areas.add(new Rect2i(
                    left + segment.x(),
                    top + segment.y(),
                    segment.width(),
                    segment.height()
                ));
            }
        }
        return areas;
    }
}
