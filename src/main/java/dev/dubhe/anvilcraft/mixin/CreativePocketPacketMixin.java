package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.inventory.PocketInventory;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class CreativePocketPacketMixin {
    @Shadow
    public ServerPlayer player;

    @ModifyConstant(method = "handleSetCreativeModeSlot", constant = @Constant(intValue = 45))
    private int anvilcraft$allowPocketSlots(int original) {
        return original + PocketInventory.capacity(this.player);
    }

    @Inject(method = "handleSetCreativeModeSlot", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;"
            + "Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
        shift = At.Shift.AFTER), cancellable = true)
    private void anvilcraft$lockLeggings(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        if (packet.slotNum() == 7 && PocketInventory.isLocked(this.player)) {
            this.player.inventoryMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }
}
