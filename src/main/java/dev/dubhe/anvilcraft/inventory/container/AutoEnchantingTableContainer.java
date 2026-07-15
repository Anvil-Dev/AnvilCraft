package dev.dubhe.anvilcraft.inventory.container;

import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class AutoEnchantingTableContainer extends SimpleContainer implements WorldlyContainer {
    public AutoEnchantingTableContainer(ItemStack... items) {
        super(items);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        return 1;
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return new int[] { 0, 1 };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction direction) {
        return slot == 0 && this.getItems().getFirst().isEmpty();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return slot == 1 && !this.getItems().get(1).isEmpty();
    }
}
