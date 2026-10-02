package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.inventory.ArmorSlot")
abstract class ArmorSlotMixin {
    @Shadow
    @Final
    private LivingEntity owner;
    @Shadow
    @Final
    private EquipmentSlot slot;

    @ModifyReturnValue(method = {"mayPickup", "mayPlace"}, at = @At("RETURN"))
    private boolean anvilcraft$lockFilledPockets(boolean original) {
        return original && !(this.slot == EquipmentSlot.LEGS && this.owner instanceof Player player
            && PocketInventory.isLocked(player));
    }
}
