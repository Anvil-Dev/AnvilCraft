package dev.dubhe.anvilcraft.mixin;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.util.dummy.DummyCat;
import dev.dubhe.anvilcraft.util.dummy.DummyWolf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(AvoidEntityGoal.class)
public abstract class AvoidEntityGoalMixin<T extends LivingEntity> {
    @Shadow
    @Final
    protected Class<T> avoidClass;

    @Shadow
    @Final
    protected PathfinderMob mob;

    @Shadow
    @Final
    private TargetingConditions avoidEntityTargeting;

    @Shadow
    @Nullable
    protected Path path;

    @Shadow
    @Final
    protected PathNavigation pathNav;

    @Shadow
    @Nullable
    protected T toAvoid;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void addAvoidPlayerGoal(CallbackInfoReturnable<Boolean> cir) {
        if (this.avoidClass.isAssignableFrom(Cat.class)) {
            Player nearest = this.mob.level().getNearestPlayer(
                this.avoidEntityTargeting.selector(
                    player -> player.getData(ModDataAttachments.SCARE_CREEPERS)
                ),
                this.mob,
                this.mob.getX(),
                this.mob.getY(),
                this.mob.getZ()
            );
            if (nearest == null) return;
            Vec3 posAway = DefaultRandomPos.getPosAway(this.mob, 16, 7, nearest.position());
            if (posAway == null || nearest.distanceToSqr(posAway.x, posAway.y, posAway.z) < nearest.distanceToSqr(this.mob)) return;
            this.path = this.pathNav.createPath(posAway.x, posAway.y, posAway.z, 0);
            this.toAvoid = Util.cast(Objects.requireNonNull(DummyCat.fromPlayer(this.mob.level(), nearest)));
            cir.setReturnValue(this.path != null);
        } else if (this.avoidClass.isAssignableFrom(Wolf.class)) {
            Player nearest = this.mob.level().getNearestPlayer(
                this.avoidEntityTargeting.selector(
                    player -> player.getData(ModDataAttachments.SCARE_SKELETONS)
                ),
                this.mob,
                this.mob.getX(),
                this.mob.getY(),
                this.mob.getZ()
            );
            if (nearest == null) return;
            Vec3 posAway = DefaultRandomPos.getPosAway(this.mob, 16, 7, nearest.position());
            if (posAway == null || nearest.distanceToSqr(posAway.x, posAway.y, posAway.z) < nearest.distanceToSqr(this.mob)) return;
            this.path = this.pathNav.createPath(posAway.x, posAway.y, posAway.z, 0);
            this.toAvoid = Util.cast(Objects.requireNonNull(DummyWolf.fromPlayer(this.mob.level(), nearest)));
            cir.setReturnValue(this.path != null);
        }
    }
}
