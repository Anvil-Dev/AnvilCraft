package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerRegistry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.inventory.PocketSlot;
import dev.dubhe.anvilcraft.mixin.accessor.AbstractContainerMenuSlotsAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
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
            args.set(2, pocket.x < 0 ? pocket.x : pocket.x + 19);
            args.set(3, pocket.y - 30);
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
        if (selectedTab.getType() != CreativeModeTab.Type.INVENTORY || this.minecraft == null || this.minecraft.player == null) return;
        int capacity = PocketInventory.capacity(this.minecraft.player);
        if (capacity == 0) return;
        int width = capacity == 12 ? 44 : 26;
        String texture = capacity == 12 ? "weatherproof_spacesuit_leggings" : "pockets_leggings";
        for (int side = 0; side < 2; side++) {
            int posX = this.leftPos + (side == 0 ? -width - 2 : this.imageWidth + 2);
            graphics.blit(RenderPipelines.GUI_TEXTURED, AnvilCraft.of("textures/gui/misc/equipment/" + texture + ".png"),
                posX, this.topPos + 40, 0, 0, width, 73, width, 73);
        }
    }

    @ModifyReturnValue(method = "hasClickedOutside", at = @At("RETURN"))
    private boolean anvilcraft$includePockets(boolean original, double mouseX, double mouseY, int left, int top) {
        if (!original || selectedTab.getType() != CreativeModeTab.Type.INVENTORY
            || this.minecraft == null || this.minecraft.player == null) return original;
        int capacity = PocketInventory.capacity(this.minecraft.player);
        if (capacity == 0 || mouseY < top + 40 || mouseY >= top + 113) return original;
        int width = capacity == 12 ? 44 : 26;
        return !(mouseX >= left - width - 2 && mouseX < left - 2
            || mouseX >= left + this.imageWidth + 2 && mouseX < left + this.imageWidth + width + 2);
    }
}
