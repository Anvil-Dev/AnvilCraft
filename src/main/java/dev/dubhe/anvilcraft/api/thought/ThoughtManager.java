package dev.dubhe.anvilcraft.api.thought;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

public class ThoughtManager {
    private static boolean onThought = false;
    @Getter
    private static long lastThoughtTime = -1L;
    @Getter
    private static final double MAX_SECONDS = 1.0;

    public static void onThought() {
        boolean checked = ThoughtManager.check();
        if (!checked) {
            ThoughtManager.onEndThought();
            return;
        }
        if (ThoughtManager.onThought) return;
        ThoughtManager.onThought = true;
        ThoughtManager.lastThoughtTime = Minecraft.getInstance().gui.getGuiTicks();
    }

    private static boolean check() {
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) return false;
        Slot slot = screen.getSlotUnderMouse();
        if (slot == null) return false;
        return slot.hasItem() && slot.getItem().getItem() instanceof Thinkable;
    }

    public static void onEndThought() {
        if (!ThoughtManager.onThought) return;
        ThoughtManager.onThought = false;
        ThoughtManager.lastThoughtTime = -1L;
    }

    public static void onPostThought() {
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) return;
        Slot slot = screen.getSlotUnderMouse();
        if (slot == null) return;
        if (slot.hasItem() && slot.getItem().getItem() instanceof Thinkable thinkable) {
            thinkable.onThought();
        }
    }
}
