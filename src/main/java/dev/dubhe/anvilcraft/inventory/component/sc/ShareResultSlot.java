package dev.dubhe.anvilcraft.inventory.component.sc;

import dev.dubhe.anvilcraft.api.sc.upgrade.level.TransferLevel;
import dev.dubhe.anvilcraft.saved.sc.ContainerStorage;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class ShareResultSlot extends Slot {
    private final ContainerStorage storage;

    public ShareResultSlot(ContainerStorage storage, int x, int y) {
        super(new SimpleContainer(1), 0, x, y);
        this.storage = storage;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return super.mayPickup(player) && this.hasItem();
    }

    @Override
    public boolean isActive() {
        var upgrades = this.storage.getUpgrades();
        return upgrades.getTransfer().ordinal() >= TransferLevel.THREE.ordinal()
               && (
                   upgrades.isShare()
                   || Objects.equals(upgrades.getOwner(), ShareSlot.getPlayer().getGameProfile().getId())
               );
    }
}
