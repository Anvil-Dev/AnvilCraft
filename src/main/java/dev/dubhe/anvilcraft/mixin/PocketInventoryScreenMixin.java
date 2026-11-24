package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.gui.component.PocketRenderer;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.inventory.PocketLayout;
import dev.dubhe.anvilcraft.inventory.PocketSlot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
abstract class PocketInventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {
    @Unique
    private int anvilcraft$pocketCapacity;

    protected PocketInventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void anvilcraft$rememberPockets(CallbackInfo ci) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.anvilcraft$pocketCapacity = PocketInventory.capacity(this.minecraft.player);
            this.anvilcraft$updatePocketSlots();
        }
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void anvilcraft$updatePocketLayout(CallbackInfo ci) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        if (this.anvilcraft$pocketCapacity != PocketInventory.capacity(this.minecraft.player)) {
            this.rebuildWidgets();
            return;
        }
        this.anvilcraft$updatePocketSlots();
    }

    @Unique
    private void anvilcraft$updatePocketSlots() {
        if (this.menu == null) {
            return;
        }
        for (Slot slot : this.menu.slots) {
            if (slot instanceof PocketSlot pocket) {
                pocket.updatePosition();
            }
        }
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void anvilcraft$drawPockets(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        int capacity = PocketInventory.capacity(this.minecraft.player);
        if (capacity == 0) {
            return;
        }
        PocketRenderer.render(
            graphics,
            PocketLayout.of(capacity, 0, this.imageWidth, PocketSlot.SCREEN_PANEL_TOP),
            this.leftPos,
            this.topPos
        );
        PocketRenderer.render(
            graphics,
            PocketLayout.of(capacity, 1, this.imageWidth, PocketSlot.SCREEN_PANEL_TOP),
            this.leftPos,
            this.topPos
        );
    }

}
