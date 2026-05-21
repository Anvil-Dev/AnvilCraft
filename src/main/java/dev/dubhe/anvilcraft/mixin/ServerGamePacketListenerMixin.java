package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.item.IInventoryCarriedAware;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(
        method = "handleContainerClick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;setRemoteCarried(Lnet/minecraft/network/HashedStack;)V"
        )
    )
    void onRemoteCarried(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (!(packet.carriedItem() instanceof HashedStack.ActualItem actualItem)) return;
        if (!(actualItem.item().value() instanceof IInventoryCarriedAware inventoryCarriedAware)) return;
        inventoryCarriedAware.onCarriedUpdate(packet.carriedItem(), this.player);
    }
}
