package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.event.AmuletAbilitiesEventListener;
import dev.dubhe.anvilcraft.mixin.accessor.TargetingConditionsAccessor;
import dev.dubhe.anvilcraft.util.mixin.ModifiedSelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Optional;

@Mixin(NearestAttackableTargetGoal.class)
public abstract class NearestAttackableTargetGoalMixin extends TargetGoal {
    @Shadow
    @Nullable
    protected LivingEntity target;

    public NearestAttackableTargetGoalMixin(Mob mob, boolean mustSee) {
        super(mob, mustSee);
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
    private TargetingConditions addAmuletScare(TargetingConditions conditions) {
        LivingEntity mob = this.mob;
        return conditions.selector(
            Optional.ofNullable(((TargetingConditionsAccessor) conditions).getSelector())
                .map(p -> ModifiedSelector.toModified(
                    p,
                    () -> entity -> NearestAttackableTargetGoalMixin.anvilcraft$canTarget(mob, entity)
                ))
                .orElse(entity -> NearestAttackableTargetGoalMixin.anvilcraft$canTarget(mob, entity))
        );
    }

    @Unique
    private static boolean anvilcraft$canTarget(LivingEntity mob, Entity entity) {
        return !(entity instanceof Player player)
               || !AmuletAbilitiesEventListener.shouldIgnoreTarget(player, mob.getType());
    }
}
