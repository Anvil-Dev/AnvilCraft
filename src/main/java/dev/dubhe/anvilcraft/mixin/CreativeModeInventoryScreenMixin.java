package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.support.TerminalRemoteOverlay;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * 创造背包界面重写了 {@code slotClicked}，不走 {@code AbstractContainerScreen.slotClicked}。
 * 在入口兜底拦截：点击落在绑定终端槽位上且已通过滚轮选择过物品时阻止 vanilla 的
 * 交换/捏起逻辑（创造性界面 INVENTORY 标签页会调用 {@code player.inventoryMenu.clicked}
 * 执行交换）。未选择时空手点击允许 vanilla 拿起终端，故不拦截。
 */
@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeModeInventoryScreenMixin {
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$blockTerminalSlotClick(@Nullable Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        if (
            slot != null
            && TerminalRemoteOverlay.isBoundTerminal(slot.getItem())
            && TerminalRemoteOverlay.hasSelection()
            && !TerminalRemoteOverlay.isDismissed()
        ) {
            ci.cancel();
        }
    }
}
