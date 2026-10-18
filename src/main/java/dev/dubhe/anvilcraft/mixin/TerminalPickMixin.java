package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class TerminalPickMixin {
    @Shadow
    public ServerPlayer player;

    @WrapOperation(
        method = "tryPickItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;findSlotMatchingItem(Lnet/minecraft/world/item/ItemStack;)I"
        )
    )
    private int anvilcraft$pickFromTerminal(Inventory inventory, ItemStack stack, Operation<Integer> original) {
        int slot = original.call(inventory, stack);
        if (slot != -1) return slot;
        StorageServerStub.withdrawForPickItem(this.player, stack);
        return original.call(inventory, stack);
    }
}
