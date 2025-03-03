package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.item.amulet.AbstractAmuletItem;
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

    @Inject(
        method = "tick",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private void PreInventoryTick(CallbackInfo ci) {
        AbstractAmuletItem.resetWorkingAmuletData(this.player);
    }
}
