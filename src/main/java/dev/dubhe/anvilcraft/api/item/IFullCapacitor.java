package dev.dubhe.anvilcraft.api.item;

import dev.anvilcraft.lib.v2.util.InventoryUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 满电容器物品
 */
public interface IFullCapacitor {
    static boolean tryForceChargeTarget(
        IFullCapacitor capacitor,
        ItemStack capacitorStack,
        Slot slot,
        ClickAction clickAction,
        Player player
    ) {
        if (clickAction != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        ItemStack target = slot.getItem();
        if (!(target.getItem() instanceof ICapacitorChargeable chargeable)) return false;

        if (!chargeable.chargeForce(target, capacitor, capacitorStack.copy())) return false;

        ItemStack empty = capacitor.getEmpty(capacitorStack);
        chargeable.onCharged(target, capacitor, capacitorStack.copy());
        capacitorStack.shrink(1);
        player.getInventory().placeItemBackInInventory(empty);
        slot.setChanged();
        return true;
    }

    int getEnergyStored(ItemStack stack);

    ItemStack getEmpty(ItemStack full);

    default void inventoryTick(ItemStack stack, Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        Inventory inv = player.getInventory();
        List<ItemStack> chargeables = InventoryUtil.getItems(inv);
        chargeables.addAll(InventoryUtil.getCompatItems(player));

        for (ItemStack chargeableStack : chargeables) {
            if (
                !(chargeableStack.getItem() instanceof ICapacitorChargeable chargeable)
                || !chargeable.getEnergyStorage(chargeableStack).canReceive()
            ) {
                continue;
            }

            if (!chargeable.charge(chargeableStack, this, stack.copy())) {
                continue;
            }

            ItemStack empty = this.getEmpty(stack);
            chargeable.onCharged(chargeableStack, this, stack.copy());
            stack.shrink(1);
            inv.placeItemBackInInventory(empty.copy());
            break;
        }
    }
}
