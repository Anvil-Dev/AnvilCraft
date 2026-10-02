package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.inventory.PocketInventory;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Equippable.class)
abstract class EquipmentPocketLockMixin {
    @Inject(method = "swapWithEquipmentSlot", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$lockFilledPockets(ItemStack stack, Player player, CallbackInfoReturnable<InteractionResult> callback) {
        if (((Equippable) (Object) this).slot() == EquipmentSlot.LEGS && PocketInventory.isLocked(player)) {
            callback.setReturnValue(InteractionResult.FAIL);
        }
    }
}
