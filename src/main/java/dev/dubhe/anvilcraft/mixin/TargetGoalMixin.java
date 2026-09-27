package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.event.AmuletAbilitiesEventListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.Nullable;

@Mixin(TargetGoal.class)
public class TargetGoalMixin {
    @Shadow
    @Nullable
    protected LivingEntity targetMob;

    @Shadow
    @Final
    protected Mob mob;

    @WrapOperation(
        method = "canContinueToUse",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/ai/goal/target/TargetGoal;targetMob:Lnet/minecraft/world/entity/LivingEntity;",
            opcode = Opcodes.GETFIELD
        )
    )
    private @Nullable LivingEntity stopTargetingByAmulet(TargetGoal instance, Operation<LivingEntity> original) {
        if (this.targetMob == null || !AmuletAbilitiesEventListener.shouldIgnoreTarget(this.targetMob, this.mob)) {
            return original.call(instance);
        }
        this.targetMob = null;
        return null;
    }
}
