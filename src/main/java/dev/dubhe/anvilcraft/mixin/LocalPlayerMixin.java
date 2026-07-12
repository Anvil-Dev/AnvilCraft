package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.item.weapon.AnvilRailgunItem;
import dev.dubhe.anvilcraft.item.weapon.EnergyWeaponItem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
    @WrapOperation(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z",
            ordinal = 0
        )
    )
    private boolean shouldSlowDownUsingEnergyWeapon(LocalPlayer player, Operation<Boolean> original) {
        if (!original.call(player)) return false;
        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof EnergyWeaponItem)) return true;
        if (stack.getItem() instanceof AnvilRailgunItem) {
            return AnvilRailgunItem.isLoading(player, stack, player.getUsedItemHand());
        }
        return false;
    }
}
