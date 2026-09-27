package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.event.AmuletAbilitiesEventListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(Phantom.PhantomSweepAttackGoal.class)
public abstract class PhantomGoalMixin {

    // CHECKSTYLE:OFF
    @Shadow
    @Final
    Phantom this$0;
    // CHECKSTYLE:ON

    @WrapOperation(
        method = "canContinueToUse",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/monster/Phantom$PhantomSweepAttackGoal;isScaredOfCat:Z",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void addAvoidPlayerGoal(Phantom.PhantomSweepAttackGoal instance, boolean value, Operation<Void> original) {
        List<LivingEntity> entities = this.this$0.level().getEntitiesOfClass(
            LivingEntity.class,
            this.this$0.getBoundingBox().inflate(16.0),
            EntitySelector.NO_SPECTATORS.and(
                entity -> entity instanceof LivingEntity living
                          && AmuletAbilitiesEventListener.shouldIgnoreTarget(living, this.this$0)
            )
        );

        for (LivingEntity living : entities) {
            living.makeSound(SoundEvents.CAT_HISS);
        }

        original.call(instance, value || !entities.isEmpty());
    }
}
