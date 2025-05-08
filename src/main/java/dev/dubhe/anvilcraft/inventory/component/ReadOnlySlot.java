package dev.dubhe.anvilcraft.inventory.component;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ReadOnlySlot extends Slot {
    public ReadOnlySlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return false;
    }
}
