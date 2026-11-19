package dev.dubhe.anvilcraft.integration.jei.util;

import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.inventory.PocketLayout;
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
        return areas(screen.getGuiLeft(), screen.getGuiTop(), screen.getXSize(), PocketInventory.capacity(player), creative);
    }

    public static List<Rect2i> areas(int left, int top, int imageWidth, int capacity, boolean creative) {
        if (capacity == 0) {
            return List.of();
        }
        int posY = creative ? 40 : 70;
        PocketLayout leftLayout = PocketLayout.of(capacity, 0, imageWidth, posY);
        PocketLayout rightLayout = PocketLayout.of(capacity, 1, imageWidth, posY);
        List<Rect2i> areas = new ArrayList<>(2);
        if (!leftLayout.isEmpty()) {
            areas.add(new Rect2i(left + leftLayout.x(), top + leftLayout.y(), leftLayout.width(), leftLayout.height()));
        }
        if (!rightLayout.isEmpty()) {
            areas.add(new Rect2i(left + rightLayout.x(), top + rightLayout.y(), rightLayout.width(), rightLayout.height()));
        }
        return areas;
    }
}
