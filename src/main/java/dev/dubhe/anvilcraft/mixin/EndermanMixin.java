package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.item.EquipmentAbilities;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderMan.class)
abstract class EndermanMixin {
    @Inject(method = "isBeingStaredBy", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$applyEndermanProtection(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (EquipmentAbilities.hasEndermanProtection(player)) cir.setReturnValue(false);
    }
}
