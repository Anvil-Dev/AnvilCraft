package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerRegistry;
import dev.dubhe.anvilcraft.client.gui.component.PocketRenderer;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.inventory.PocketLayout;
import dev.dubhe.anvilcraft.inventory.PocketSlot;
import dev.dubhe.anvilcraft.mixin.accessor.AbstractContainerMenuSlotsAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;
import java.util.function.Supplier;

@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativePocketScreenMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    /**
     * 创造界面与普通物品栏的差异：GUI 宽度 195 对 176、面板顶位 40 对 70。
     * 槽位坐标由 {@code PocketSlot} 按普通物品栏基准算出，这里统一平移，
     * {@code CreativePocketSlotMixin} 复用同一对常量。
     */
    public static final int POCKET_X_SHIFT = 195 - PocketSlot.SCREEN_IMAGE_WIDTH;
    public static final int POCKET_Y_SHIFT = 40 - PocketSlot.SCREEN_PANEL_TOP;
    @Shadow
    private static CreativeModeTab selectedTab;
    @Shadow
    private @Nullable Slot destroyItemSlot;
    @Shadow
    @Final
    private static SimpleContainer CONTAINER;

    protected CreativePocketScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @ModifyVariable(method = "slotClicked", at = @At("HEAD"), argsOnly = true)
    private ContainerInput anvilcraft$shiftDestroy(ContainerInput type, @Nullable Slot slot) {
        return slot != null && slot == this.destroyItemSlot && type == ContainerInput.PICKUP && this.minecraft.hasShiftDown()
            ? ContainerInput.QUICK_MOVE : type;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        if (selectedTab.getType() != CreativeModeTab.Type.CATEGORY) return;
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.leftPos, this.topPos);
        graphics.nextStratum();
        for (Slot slot : this.menu.slots) {
            if (slot.container != CONTAINER || !slot.isActive()
                || !CreativeVariantPickerRegistry.isCreativePickerEnabled(slot.getItem())) continue;
            graphics.text(this.font, "+", slot.x + 10, slot.y + 1, 0xFFFFFFFF, true);
        }
        graphics.pose().popMatrix();
    }

    @ModifyArgs(method = "selectTab", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper;"
            + "<init>(Lnet/minecraft/world/inventory/Slot;III)V"))
    private void anvilcraft$positionPockets(Args args) {
        if (args.get(0) instanceof PocketSlot pocket) {
            pocket.updatePosition();
            args.set(2, pocket.x < 0 ? pocket.x : pocket.x + POCKET_X_SHIFT);
            args.set(3, pocket.y + POCKET_Y_SHIFT);
        }
    }

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void anvilcraft$resyncSlotLists(CallbackInfo ci) {
        var accessor = (AbstractContainerMenuSlotsAccessor) this.menu;
        int size = this.menu.slots.size();
        CreativePocketScreenMixin.anvilcraft$resizeSlots(accessor.anvilcraft$getLastSlots(), size, () -> ItemStack.EMPTY);
        var synchronizer = accessor.anvilcraft$getSynchronizer();
        CreativePocketScreenMixin.anvilcraft$resizeSlots(accessor.anvilcraft$getRemoteSlots(), size,
            synchronizer == null ? () -> RemoteSlot.PLACEHOLDER : synchronizer::createSlot);
    }

    @Unique
    private static <T> void anvilcraft$resizeSlots(List<T> slots, int size, Supplier<T> empty) {
        while (slots.size() < size) slots.add(empty.get());
        while (slots.size() > size) slots.removeLast();
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$lockLeggings(@Nullable Slot slot, int slotId, int button, ContainerInput type, CallbackInfo ci) {
        int targetSlot = slot == null ? slotId : slot.getContainerSlot();
        if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY && targetSlot == 7 && this.minecraft != null
            && this.minecraft.player != null && PocketInventory.isLocked(this.minecraft.player)) ci.cancel();
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void anvilcraft$drawPockets(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
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
    private boolean anvilcraft$includePockets(boolean original, double mouseX, double mouseY, int left, int top) {
        if (!original || selectedTab.getType() != CreativeModeTab.Type.INVENTORY
            || this.minecraft == null || this.minecraft.player == null) {
            return original;
        }
        int capacity = PocketInventory.capacity(this.minecraft.player);
        if (capacity == 0) {
            return original;
        }
        double localX = mouseX - left;
        double localY = mouseY - top;
        if (PocketLayout.of(capacity, 0, this.imageWidth, 40).contains(localX, localY)
            || PocketLayout.of(capacity, 1, this.imageWidth, 40).contains(localX, localY)) {
            return false;
        }
        return original;
    }
}
