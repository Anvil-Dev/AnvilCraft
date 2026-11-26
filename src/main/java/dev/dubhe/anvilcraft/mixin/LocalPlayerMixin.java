package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.client.selection.ButtonOverhangPicking;
import dev.dubhe.anvilcraft.item.weapon.AnvilRailgunItem;
import dev.dubhe.anvilcraft.item.weapon.EnergyWeaponItem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerMixin {
    @ModifyReturnValue(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At("RETURN"))
    private static HitResult pickOverhangingButton(
        HitResult original, Entity camera, double blockRange, double entityRange, float partialTick
    ) {
        return ButtonOverhangPicking.pick(original, camera, blockRange, partialTick);
    }

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
