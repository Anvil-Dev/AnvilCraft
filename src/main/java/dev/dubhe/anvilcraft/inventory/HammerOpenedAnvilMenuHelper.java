package dev.dubhe.anvilcraft.inventory;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public final class HammerOpenedAnvilMenuHelper {
    public static final int NO_HAMMER_SLOT = -1;
    public static final int REMOTE_HAMMER_SLOT = -2;

    private HammerOpenedAnvilMenuHelper() {
    }

    public static boolean isValidInventorySlot(Inventory inventory, int slot) {
        return slot < 0 || slot >= inventory.getContainerSize();
    }

    public static boolean touchesOpenedHammerSlot(
        AbstractContainerMenu menu,
        Inventory inventory,
        int slotId,
        int button,
        ClickType clickType,
        int openedHammerSlot
    ) {
        if (isValidInventorySlot(inventory, openedHammerSlot)) return false;
        if (slotId >= 0 && slotId < menu.slots.size()) {
            Slot slot = menu.getSlot(slotId);
            if (slot.container == inventory && slot.getContainerSlot() == openedHammerSlot) {
                return true;
            }
        }
        return clickType == ClickType.SWAP
               && (button == openedHammerSlot && openedHammerSlot < 9
                   || button == Inventory.SLOT_OFFHAND && openedHammerSlot == Inventory.SLOT_OFFHAND);
    }

    public static void closeOnServer(Player player) {
        if (!player.level().isClientSide) {
            player.closeContainer();
        }
    }

    public static void playUseSound(Player player) {
        if (player.level().isClientSide) return;
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
