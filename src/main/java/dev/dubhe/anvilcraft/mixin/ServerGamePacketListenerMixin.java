package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.item.IInventoryCarriedAware;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
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
            target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;setRemoteCarried(Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    void onRemoteCarried(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        ItemStack itemStack = packet.getCarriedItem();
        if (itemStack.getItem() instanceof IInventoryCarriedAware inventoryCarriedAware) {
            inventoryCarriedAware.onCarriedUpdate(itemStack, this.player);
        }
    }
}
