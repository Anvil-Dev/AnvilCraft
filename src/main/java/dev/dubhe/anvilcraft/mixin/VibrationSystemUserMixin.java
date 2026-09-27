package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VibrationSystem.User.class)
interface VibrationSystemUserMixin {
    @Inject(
        method = "isValidVibration",
        at = @At("RETURN"),
        cancellable = true
    )
    private void addPlayerHasSilenceAmulet(
        Holder<GameEvent> gameEvent,
        GameEvent.Context context,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(context.sourceEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (!AmuletManager.shouldEvaluate(entity)) {
            return;
        }
        AmuletEffectContext ctx = new AmuletEffectContext();
        AmuletManager.get(entity.registryAccess()).trigger(entity, ctx);
        if (!ctx.get(ModAmuletEffectContextKeys.IMMUNE_VIBRATION).orElse(false)) return;
        cir.setReturnValue(false);
    }
}
