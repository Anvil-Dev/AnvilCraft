package dev.dubhe.anvilcraft.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public class StructureToolMenu extends AbstractContainerMenu {
    public StructureToolMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory) {
        super(menuType, containerId);

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        this.addSlot(new Slot(new SimpleContainer(ItemStack.EMPTY), 0, 98, 20));
    }

    @SuppressWarnings("DuplicatedCode")
    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        final ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < 4 * 9) {
            if (!moveItemStackTo(sourceStack, 4 * 9, 4 * 9 + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index == 4 * 9) {
            if (!moveItemStackTo(sourceStack, 0, 4 * 9, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack slotStack = slots.get(4 * 9).getItem();
            if (!slotStack.isEmpty()) {
                if (serverPlayer.isAlive() && !serverPlayer.hasDisconnected()) {
                    player.getInventory().placeItemBackInInventory(slotStack);
                } else {
                    serverPlayer.drop(slotStack, false);
                }
            }
        }
    }
}
