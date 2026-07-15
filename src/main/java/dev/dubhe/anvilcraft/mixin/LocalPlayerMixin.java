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
        if (!(stack.getItem() instanceof EnergyWeaponItem)) return original;
        return stack.getItem() instanceof AnvilRailgunItem
               && AnvilRailgunItem.isLoading(player, stack, player.getUsedItemHand());
    }

    @ModifyReturnValue(method = "itemUseSpeedMultiplier", at = @At("RETURN"))
    private float modifyEnergyWeaponUseSpeed(float original) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof EnergyWeaponItem)) return original;
        if (stack.getItem() instanceof AnvilRailgunItem
            && AnvilRailgunItem.isLoading(player, stack, player.getUsedItemHand())
        ) {
            return original;
        }
        return 1.0F;
    }
}
