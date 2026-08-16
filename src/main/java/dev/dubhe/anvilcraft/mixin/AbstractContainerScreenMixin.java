package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.gui.screen.AnvilHammerSlotOverlay;
import dev.dubhe.anvilcraft.client.support.TerminalRemoteOverlay;
import dev.dubhe.anvilcraft.inventory.HammerOpenedAnvilMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> {
    @Shadow
    @Final
    protected T menu;

    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void anvilcraft$renderHammerOpenedAnvilSlot(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        if (this.menu instanceof HammerOpenedAnvilMenu hammerOpenedAnvilMenu) {
            AnvilHammerSlotOverlay.render(guiGraphics, hammerOpenedAnvilMenu, slot);
        }
    }

    /**
     * 兜底拦截：即使鼠标点击的 Pre 事件未被取消（如创造性界面下 hoveredSlot 滞后导致
     * 漏判），只要点击落在绑定终端槽位上且已通过滚轮选择过物品，就阻止 vanilla 的
     * 槽位交换/捏起逻辑。未选择时空手点击允许 vanilla 拿起终端，故不拦截。
     */
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
