package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.mixin.accessor.TargetingConditionsAccessor;
import dev.dubhe.anvilcraft.util.Util;
import dev.dubhe.anvilcraft.util.function.SafePredicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import javax.annotation.Nullable;
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
        AttachmentType<Boolean> type;
        switch (this.mob.getClass()) {
            case Class<?> clazz when clazz.isAssignableFrom(AbstractSkeleton.class) -> type = ModDataAttachments.SCARE_SKELETONS.get();
            case Class<?> clazz when clazz.isAssignableFrom(Creeper.class) -> type = ModDataAttachments.SCARE_CREEPERS.get();
            default -> {
                return conditions;
            }
        }
        return conditions.selector(
            Optional.ofNullable(((TargetingConditionsAccessor) conditions).getSelector())
                .flatMap(p -> Util.castSafely(p, SafePredicate.class))
                .map(Util::<SafePredicate<LivingEntity>>cast)
                .map(predicate -> predicate.and(entity -> !entity.getData(type)))
                .orElse(entity -> !entity.getData(type))
        );
    }
}
