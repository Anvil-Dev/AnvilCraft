package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.mixin.accessor.AbstractContainerMenuSlotsAccessor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * {@code selectTab} 直接改写 {@code menu.slots}，绕过了
 * {@link AbstractContainerMenu#addSlot} 对 {@code lastSlots} / {@code remoteSlots} 的同步，
 * 而 {@code broadcastChanges} 按下标索引后两者，长度不一致即越界。
 * 原版包装 inventoryMenu 后未超过选择器菜单初始的 54，口袋系统追加槽位后达到 59，问题才显现。
 */
@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeModeInventoryScreenSlotsMixin {
    @Inject(method = "selectTab", at = @At("TAIL"))
    private void anvilcraft$resyncSlotLists(CallbackInfo ci) {
        AbstractContainerMenu menu = ((CreativeModeInventoryScreen) (Object) this).getMenu();
        AbstractContainerMenuSlotsAccessor accessor = (AbstractContainerMenuSlotsAccessor) menu;
        resizeTo(accessor.getLastSlots(), menu.slots.size());
        resizeTo(accessor.getRemoteSlots(), menu.slots.size());
    }

    private static void resizeTo(List<ItemStack> list, int size) {
        while (list.size() < size) {
            list.add(ItemStack.EMPTY);
        }
        while (list.size() > size) {
            list.remove(list.size() - 1);
        }
    }
}
