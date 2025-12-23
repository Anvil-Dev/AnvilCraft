package dev.dubhe.anvilcraft.inventory.component.sc;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ShareResultSlot extends Slot {
    public ShareResultSlot(int x, int y) {
        super(new SimpleContainer(1), 0, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return super.mayPickup(player) && this.hasItem();
    }
}
