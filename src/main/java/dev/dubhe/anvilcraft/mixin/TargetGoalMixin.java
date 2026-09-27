package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.event.AmuletAbilitiesEventListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetGoal.class)
public class TargetGoalMixin {
    @Shadow
    @Nullable
    protected LivingEntity targetMob;

    @Shadow
    @Final
    protected Mob mob;

    @Inject(
        method = "canContinueToUse",
        at = @At("HEAD"),
        cancellable = true
    )
    private void stopTargetingByAmulet(CallbackInfoReturnable<Boolean> cir) {
        if (this.targetMob == null || !AmuletAbilitiesEventListener.shouldIgnoreTarget(this.targetMob, this.mob)) {
            return;
        }
        this.targetMob = null;
        cir.setReturnValue(false);
    }
}
