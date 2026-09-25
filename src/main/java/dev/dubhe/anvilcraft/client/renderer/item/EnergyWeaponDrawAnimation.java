package dev.dubhe.anvilcraft.client.renderer.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EnergyWeaponDrawAnimation {
    private static final float DURATION_MILLIS = 200;
    private Item item = Items.AIR;
    private int slot = -1;
    private boolean waiting;
    private float elapsed = DURATION_MILLIS;
    private float lowered;
    private long lastFrame;

    public void observe(ItemStack held, int selectedSlot) {
        if (held.getItem() == this.item && selectedSlot == this.slot) return;
        this.item = held.getItem();
        this.slot = selectedSlot;
        this.waiting = true;
        this.elapsed = 0;
        this.lowered = 1;
    }

    public float render(ItemStack shown, float vanillaEquip, long now, boolean paused) {
        // Let the outgoing item finish its vanilla lowering before drawing the newly selected weapon.
        if (shown.getItem() != this.item) return vanillaEquip;
        if (this.waiting) {
            this.waiting = false;
            this.lastFrame = now;
        }
        if (!paused) this.elapsed += Math.max(0, now - this.lastFrame);
        this.lastFrame = now;
        this.lowered = 1 - Math.clamp(this.elapsed / DURATION_MILLIS, 0, 1);
        return this.lowered;
    }

    public boolean isReady(ItemStack weapon) {
        return !this.waiting && this.item == weapon.getItem();
    }

    public float lowered() {
        return this.lowered;
    }

    public void reset() {
        this.item = Items.AIR;
        this.slot = -1;
        this.waiting = false;
        this.elapsed = DURATION_MILLIS;
        this.lowered = 0;
    }
}
