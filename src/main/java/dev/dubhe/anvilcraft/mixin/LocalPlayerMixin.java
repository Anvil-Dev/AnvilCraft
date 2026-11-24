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
        // 铁砧轨道炮改成弓式蓄力，蓄力时应当和弓一样减速
        if (stack.getItem() instanceof AnvilRailgunItem) return true;
        return !(stack.getItem() instanceof EnergyWeaponItem);
    }
}
