package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.client.gui.component.PocketRenderer;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.inventory.PocketLayout;
import dev.dubhe.anvilcraft.inventory.PocketSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
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

    // 配方书展开时 InventoryScreen 会把 GUI 右移让位，右侧口袋面板可能被推出屏幕。
    // 这里按两侧面板宽度抬高窄屏阈值，让界面在空间不足时改用配方书覆盖 GUI 的原版窄屏表现。
    @ModifyConstant(method = "init", constant = @Constant(intValue = 379))
    private int anvilcraft$allowRoomForPockets(int original) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return original;
        }
        int capacity = PocketInventory.capacity(this.minecraft.player);
        if (capacity == 0) {
            return original;
        }
        return original + PocketLayout.of(capacity, 0, 0, 0).width()
            + PocketLayout.of(capacity, 1, 0, 0).width() + 4;
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void anvilcraft$drawPockets(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        int capacity = PocketInventory.capacity(this.minecraft.player);
        if (capacity == 0) {
            return;
        }
        PocketRenderer.render(graphics, PocketLayout.of(capacity, 0, this.imageWidth, 70), this.leftPos, this.topPos);
        PocketRenderer.render(graphics, PocketLayout.of(capacity, 1, this.imageWidth, 70), this.leftPos, this.topPos);
    }

    @ModifyReturnValue(method = "hasClickedOutside", at = @At("RETURN"))
    private boolean anvilcraft$includePockets(boolean original, double mouseX, double mouseY, int left, int top, int button) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return original;
        }
        int capacity = PocketInventory.capacity(this.minecraft.player);
        if (capacity == 0) {
            return original;
        }
        double localX = mouseX - left;
        double localY = mouseY - top;
        if (PocketLayout.of(capacity, 0, this.imageWidth, 70).contains(localX, localY)
            || PocketLayout.of(capacity, 1, this.imageWidth, 70).contains(localX, localY)) {
            return false;
        }
        return original;
    }
}
