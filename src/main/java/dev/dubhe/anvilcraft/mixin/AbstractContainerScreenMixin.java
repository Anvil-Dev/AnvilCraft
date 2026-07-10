package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.gui.screen.AnvilHammerSlotOverlay;
import dev.dubhe.anvilcraft.inventory.HammerOpenedAnvilMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
}
