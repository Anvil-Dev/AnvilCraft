package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.item.weapon.AnvilRailgunItem;
import dev.dubhe.anvilcraft.item.weapon.EnergyWeaponItem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
    @ModifyReturnValue(method = "isSlowDueToUsingItem", at = @At("RETURN"))
    private boolean shouldSlowDownUsingEnergyWeapon(boolean original) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        ItemStack stack = player.getUseItem();
        // 铁砧轨道炮改成弓式蓄力，蓄力时应当和弓一样减速
        if (!(stack.getItem() instanceof EnergyWeaponItem)) return original;
        if (stack.getItem() instanceof AnvilRailgunItem) return original;
        return false;
    }

    @ModifyReturnValue(method = "itemUseSpeedMultiplier", at = @At("RETURN"))
    private float modifyEnergyWeaponUseSpeed(float original) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof EnergyWeaponItem)) return original;
        if (stack.getItem() instanceof AnvilRailgunItem) return original;
        return 1.0F;
    }
}
