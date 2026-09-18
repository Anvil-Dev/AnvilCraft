package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.inventory.PocketSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import javax.annotation.Nullable;

@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativePocketScreenMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow
    private static CreativeModeTab selectedTab;

    protected CreativePocketScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @ModifyArgs(method = "selectTab", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;"
            + "<init>(Lnet/minecraft/world/inventory/Slot;III)V"))
    private void anvilcraft$positionPockets(Args args) {
        if (args.get(0) instanceof PocketSlot pocket) {
            args.set(2, pocket.x < 0 ? pocket.x : pocket.x + 19);
            args.set(3, pocket.y - 30);
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$lockLeggings(@Nullable Slot slot, int slotId, int button, ClickType type, CallbackInfo ci) {
        if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY && slotId == 7 && this.minecraft != null
            && this.minecraft.player != null && PocketInventory.isLocked(this.minecraft.player)) ci.cancel();
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void anvilcraft$drawPockets(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (selectedTab.getType() != CreativeModeTab.Type.INVENTORY || this.minecraft == null || this.minecraft.player == null) return;
        int capacity = PocketInventory.capacity(this.minecraft.player);
        if (capacity == 0) return;
        int width = capacity == 12 ? 44 : 26;
        String texture = capacity == 12 ? "weatherproof_spacesuit_leggings" : "pockets_leggings";
        for (int side = 0; side < 2; side++) {
            int posX = this.leftPos + (side == 0 ? -width - 2 : this.imageWidth + 2);
            graphics.blit(AnvilCraft.of("textures/gui/misc/equipment/" + texture + ".png"),
                posX, this.topPos + 40, 0, 0, width, 73, width, 73);
        }
    }

    @ModifyReturnValue(method = "hasClickedOutside", at = @At("RETURN"))
    private boolean anvilcraft$includePockets(boolean original, double mouseX, double mouseY, int left, int top, int button) {
        if (!original || selectedTab.getType() != CreativeModeTab.Type.INVENTORY
            || this.minecraft == null || this.minecraft.player == null) return original;
        int capacity = PocketInventory.capacity(this.minecraft.player);
        if (capacity == 0 || mouseY < top + 40 || mouseY >= top + 113) return original;
        int width = capacity == 12 ? 44 : 26;
        return !(mouseX >= left - width - 2 && mouseX < left - 2
            || mouseX >= left + this.imageWidth + 2 && mouseX < left + this.imageWidth + width + 2);
    }
}
