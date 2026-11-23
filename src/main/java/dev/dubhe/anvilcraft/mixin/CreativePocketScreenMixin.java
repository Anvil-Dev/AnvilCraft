package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.client.gui.component.PocketRenderer;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.inventory.PocketLayout;
import dev.dubhe.anvilcraft.inventory.PocketSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
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
            pocket.updatePosition();
            args.set(2, pocket.x < 0 ? pocket.x : pocket.x + PocketLayout.CREATIVE_X_SHIFT);
            args.set(3, pocket.y + PocketLayout.CREATIVE_Y_SHIFT);
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$lockLeggings(@Nullable Slot slot, int slotId, int button, ClickType type, CallbackInfo ci) {
        if (selectedTab.getType() != CreativeModeTab.Type.INVENTORY || slotId != 7
            || this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        // 创造模式直接置物，不经过 ArmorSlot#mayPlace，这里按光标物品判断本次换装是否丢失口袋。
        ItemStack target = switch (type) {
            case PICKUP, QUICK_MOVE -> ItemStack.EMPTY;
            case SWAP -> this.minecraft.player.getInventory().getItem(button);
            default -> this.menu.getCarried();
        };
        if (PocketInventory.blocksLeggingsChange(this.minecraft.player, target)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void anvilcraft$drawPockets(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (selectedTab.getType() != CreativeModeTab.Type.INVENTORY || this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        int capacity = PocketInventory.capacity(this.minecraft.player);
        if (capacity == 0) {
            return;
        }
        PocketRenderer.render(graphics, PocketLayout.of(capacity, 0, this.imageWidth, 40), this.leftPos, this.topPos);
        PocketRenderer.render(graphics, PocketLayout.of(capacity, 1, this.imageWidth, 40), this.leftPos, this.topPos);
    }

    @ModifyReturnValue(method = "hasClickedOutside", at = @At("RETURN"))
    private boolean anvilcraft$includePockets(boolean original, double mouseX, double mouseY, int left, int top, int button) {
        if (selectedTab.getType() != CreativeModeTab.Type.INVENTORY
            || this.minecraft == null || this.minecraft.player == null) {
            return original;
        }
        int capacity = PocketInventory.capacity(this.minecraft.player);
        if (capacity == 0) {
            return original;
        }
        double localX = mouseX - left;
        double localY = mouseY - top;
        if (PocketLayout.containsPocket(capacity, this.imageWidth, PocketLayout.CREATIVE_PANEL_TOP, localX, localY)) {
            return false;
        }
        return original;
    }
}
