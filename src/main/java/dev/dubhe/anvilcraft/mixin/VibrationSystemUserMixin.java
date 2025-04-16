package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.util.AmuletUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VibrationSystem.User.class)
public interface VibrationSystemUserMixin {
    @Inject(
        method = "isValidVibration",
        at = @At("RETURN"),
        cancellable = true
    )
    private void addPlayerHasSilenceAmulet(Holder<GameEvent> gameEvent, GameEvent.Context context, CallbackInfoReturnable<Boolean> cir) {
        if (context.sourceEntity() instanceof Player player && AmuletUtil.hasAmuletInInventory(player, ModItems.SILENCE_AMULET)) {
            cir.setReturnValue(false);
        }
    }
}
