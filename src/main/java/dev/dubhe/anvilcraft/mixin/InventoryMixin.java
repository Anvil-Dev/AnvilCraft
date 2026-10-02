package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.inventory.PocketInventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
abstract class InventoryMixin {
    @Shadow
    @Final
    public Player player;

    @Inject(method = "dropAll", at = @At("HEAD"))
    private void anvilcraft$dropPockets(CallbackInfo ci) {
        PocketInventory.get(this.player).dropAll(this.player);
    }
}
