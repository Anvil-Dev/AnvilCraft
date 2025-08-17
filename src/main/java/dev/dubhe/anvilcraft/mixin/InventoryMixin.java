package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import net.minecraft.server.level.ServerPlayer;
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

    @SuppressWarnings("PatternVariableHidesField")
    @Inject(
        method = "tick",
        at = @At(value = "HEAD")
    )
    private void preInventoryTick(CallbackInfo ci) {
        if (this.player instanceof ServerPlayer player) {
            AmuletManager.INSTANCE.inventoryTick(player);
        }
    }
}
