package dev.dubhe.anvilcraft.mixin;

import dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerRegistry;
import dev.dubhe.anvilcraft.client.support.TerminalRemoteOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * 创造背包界面重写了 {@code slotClicked}，不走 {@code AbstractContainerScreen.slotClicked}。
 * 在入口兜底拦截：点击落在绑定终端槽位上且已通过滚轮选择过物品时阻止 vanilla 的
 * 交换/捏起逻辑（创造性界面 INVENTORY 标签页会调用 {@code player.inventoryMenu.clicked}
 * 执行交换）。未选择时空手点击允许 vanilla 拿起终端，故不拦截。
 */
@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeModeInventoryScreenMixin
    extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow
    private static CreativeModeTab selectedTab;
    @Shadow
    @Nullable
    private Slot destroyItemSlot;
    @Shadow
    @Final
    private static SimpleContainer CONTAINER;

    protected CreativeModeInventoryScreenMixin(
        CreativeModeInventoryScreen.ItemPickerMenu menu,
        Inventory inventory,
        Component title
    ) {
        super(menu, inventory, title);
    }

    @ModifyVariable(method = "slotClicked", at = @At("HEAD"), argsOnly = true)
    private ClickType anvilcraft$shiftDestroy(ClickType type, @Nullable Slot slot) {
        return slot != null && slot == this.destroyItemSlot && type == ClickType.PICKUP && Screen.hasShiftDown()
            ? ClickType.QUICK_MOVE : type;
    }

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

    @Inject(method = "renderLabels", at = @At("TAIL"))
    private void anvilcraft$renderVariantIndicators(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (selectedTab.getType() != CreativeModeTab.Type.CATEGORY) return;
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        for (Slot slot : this.menu.slots) {
            if (slot.container != CONTAINER || !slot.isActive()
                || !CreativeVariantPickerRegistry.isCreativePickerEnabled(slot.getItem())) {
                continue;
            }
            graphics.drawString(this.font, "+", slot.x + 10, slot.y + 1, 0xFFFFFFFF, true);
        }
        graphics.pose().popPose();
    }
}
