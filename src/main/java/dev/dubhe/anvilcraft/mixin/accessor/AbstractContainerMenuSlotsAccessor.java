package dev.dubhe.anvilcraft.mixin.accessor;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露 {@code lastSlots} 与 {@code remoteSlots}：两者由 {@link AbstractContainerMenu#addSlot}
 * 与 {@code slots} 同步维护，某些原版代码会绕过它，需要外部校正长度。
 */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuSlotsAccessor {
    @Accessor("lastSlots")
    NonNullList<ItemStack> getLastSlots();

    @Accessor("remoteSlots")
    NonNullList<ItemStack> getRemoteSlots();
}
