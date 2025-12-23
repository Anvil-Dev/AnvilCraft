package dev.dubhe.anvilcraft.inventory.component.sc;

import dev.dubhe.anvilcraft.api.sc.upgrade.Upgrade;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class UpgradeSlot extends Slot {
    private final Upgrade<?> upgrade;

    public UpgradeSlot(Upgrade<?> upgrade, int x, int y) {
        super(new SimpleContainer(1), 0, x, y);
        this.upgrade = upgrade;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.upgrade.getNext() != null && this.upgrade.getNext().isMaterial(stack);
    }

    @Override
    public int getMaxStackSize() {
        return this.upgrade.getNext() == null ? 0 : this.upgrade.getNext().getConsumedCount() - this.upgrade.getProgress();
    }

    @Override
    public boolean isActive() {
        return this.upgrade.getNext() != null;
    }
}
