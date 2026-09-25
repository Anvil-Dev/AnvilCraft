package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.inventory.PocketSlot;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class CreativePocketPacketMixin {
    @Shadow
    public ServerPlayer player;

    @ModifyExpressionValue(method = "handleSetCreativeModeSlot", at = @At(value = "CONSTANT", args = "intValue=45"))
    private int anvilcraft$allowPocketSlots(int original, ServerboundSetCreativeModeSlotPacket packet) {
        int slot = packet.slotNum();
        if (slot >= 0 && slot < this.player.inventoryMenu.slots.size()
            && this.player.inventoryMenu.getSlot(slot) instanceof PocketSlot pocket && pocket.isActive()) {
            return Math.max(original, slot);
        }
        return original;
    }

    @Inject(method = "handleSetCreativeModeSlot", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;"
            + "Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
        shift = At.Shift.AFTER), cancellable = true)
    private void anvilcraft$validateSlot(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        int slot = packet.slotNum();
        if (slot >= this.player.inventoryMenu.slots.size()
            || slot >= 0 && this.player.inventoryMenu.getSlot(slot) instanceof PocketSlot pocket && !pocket.isActive()
            || slot == 7 && PocketInventory.isLocked(this.player)) {
            this.player.inventoryMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }
}
