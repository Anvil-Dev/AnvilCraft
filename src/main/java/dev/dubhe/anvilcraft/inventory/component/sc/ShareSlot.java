package dev.dubhe.anvilcraft.inventory.component.sc;

import dev.dubhe.anvilcraft.api.sc.upgrade.level.TransferLevel;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.ContainerStorageRef;
import dev.dubhe.anvilcraft.saved.sc.ContainerStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

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
        return this.storage.getUpgrades().getTransfer().ordinal() > TransferLevel.THREE.ordinal()
               && (
                   this.storage.getUpgrades().isShare()
                   || Objects.equals(this.storage.getUpgrades().getOwner(), Minecraft.getInstance().getGameProfile().getId())
               )
               && stack.is(ModBlocks.SINGULARITY_CRYSTAL.asItem());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        ItemStack stack = this.getItem().copy();
        stack.set(ModComponents.CONTAINER_STORAGE, new ContainerStorageRef(this.storage.getId()));
        ItemStack remain = this.result.safeInsert(stack);
        this.remove(this.getItem().getCount() - remain.getCount());
    }
}
