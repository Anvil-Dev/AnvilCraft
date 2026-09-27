package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.event.AmuletAbilitiesEventListener;
import dev.dubhe.anvilcraft.mixin.accessor.TargetingConditionsAccessor;
import dev.dubhe.anvilcraft.util.mixin.ModifiedSelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Optional;
import javax.annotation.Nullable;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class NearestAttackableTargetGoalMixin<T extends LivingEntity> extends TargetGoal {
    @Shadow
    @Nullable
    protected LivingEntity target;

    @Shadow
    @Final
    protected Class<T> targetType;

    public NearestAttackableTargetGoalMixin(Mob mob, boolean mustSee) {
        super(mob, mustSee);
    }

    @ModifyArg(
        method = "findTarget",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getNearestEntity("
                     + "Ljava/util/List;Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;"
                     + "Lnet/minecraft/world/entity/LivingEntity;DDD"
                     + ")Lnet/minecraft/world/entity/LivingEntity;"
        ),
        index = 1
    )
    private TargetingConditions addEntityAmuletScare(
        TargetingConditions conditions
    ) {
        return conditions.selector(
            Optional.ofNullable(((TargetingConditionsAccessor) conditions).getSelector())
                .map(p -> ModifiedSelector.toModified(
                    p,
                    () -> entity -> NearestAttackableTargetGoalMixin.anvilcraft$canTarget(this.mob, entity)
                ))
                .orElse(entity -> NearestAttackableTargetGoalMixin.anvilcraft$canTarget(this.mob, entity))
        );
    }

    @ModifyArg(
        method = "findTarget",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getNearestPlayer("
                     + "Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;"
                     + "Lnet/minecraft/world/entity/LivingEntity;DDD)"
                     + "Lnet/minecraft/world/entity/player/Player;"
        ),
        index = 0
    )
    private TargetingConditions addPlayerAmuletScare(TargetingConditions conditions) {
        return conditions.selector(
            Optional.ofNullable(((TargetingConditionsAccessor) conditions).getSelector())
                .map(p -> ModifiedSelector.toModified(
                    p,
                    () -> entity -> NearestAttackableTargetGoalMixin.anvilcraft$canTarget(this.mob, entity)
                ))
                .orElse(entity -> NearestAttackableTargetGoalMixin.anvilcraft$canTarget(this.mob, entity))
        );
    }

    @Unique
    private static boolean anvilcraft$canTarget(Mob mob, LivingEntity entity) {
        return !AmuletAbilitiesEventListener.shouldIgnoreTarget(entity, mob);
    }
}
