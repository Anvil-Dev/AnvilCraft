package dev.dubhe.anvilcraft.mixin.accessor;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuSlotsAccessor {
    @Accessor("lastSlots")
    NonNullList<ItemStack> anvilcraft$getLastSlots();

    @Accessor("remoteSlots")
    NonNullList<RemoteSlot> anvilcraft$getRemoteSlots();

    @Accessor("synchronizer")
    @Nullable ContainerSynchronizer anvilcraft$getSynchronizer();
}
