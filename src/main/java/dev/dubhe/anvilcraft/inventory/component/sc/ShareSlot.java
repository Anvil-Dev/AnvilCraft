package dev.dubhe.anvilcraft.inventory.component.sc;

import dev.dubhe.anvilcraft.api.sc.upgrade.level.TransferLevel;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.ShulkerContainerMenu;
import dev.dubhe.anvilcraft.item.property.component.ContainerStorageRef;
import dev.dubhe.anvilcraft.saved.sc.ContainerStorage;
import dev.dubhe.anvilcraft.util.DistExecutor;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class ShareSlot extends Slot {
    private final ContainerStorage storage;
    private final Slot result;

    public ShareSlot(ContainerStorage storage, Slot result, int x, int y) {
        super(new SimpleContainer(1), 0, x, y);
        this.storage = storage;
        this.result = result;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        var upgrades = this.storage.getUpgrades();
        return upgrades.getTransfer().ordinal() >= TransferLevel.THREE.ordinal()
               && (
                   upgrades.isShare()
                   || Objects.equals(upgrades.getOwner(), ShareSlot.getPlayer().getGameProfile().getId())
               )
               && stack.is(ModBlocks.SINGULARITY_CRYSTAL.asItem());
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

    @Override
    public void setChanged() {
        super.setChanged();
        ItemStack stack = this.getItem().copy();
        stack.set(ModComponents.CONTAINER_STORAGE, new ContainerStorageRef(this.storage.getId()));
        ItemStack remain = this.result.safeInsert(stack);
        if (remain.getCount() == stack.getCount()) return;
        this.remove(this.getItem().getCount() - remain.getCount());
    }

    static Player getPlayer() {
        AtomicReference<Player> ref = new AtomicReference<>();
        if (Util.isClient()) {
            DistExecutor.run(Dist.CLIENT, () -> () -> ref.set(Minecraft.getInstance().player));
        } else {
            for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (player.containerMenu instanceof ShulkerContainerMenu) {
                    ref.set(player);
                    break;
                }
            }
        }
        return ref.get();
    }
}
