package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.util.dummy.DummyCat;
import dev.dubhe.anvilcraft.util.dummy.DummyWolf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

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
    protected float maxDist;

    @Shadow
    @Final
    private TargetingConditions avoidEntityTargeting;

    @Shadow
    @Nullable
    protected T toAvoid;

    @Redirect(
        method = "canUse",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/ai/goal/AvoidEntityGoal;toAvoid:Lnet/minecraft/world/entity/LivingEntity;",
            opcode = Opcodes.PUTFIELD
        ))
    @SuppressWarnings("unchecked")
    private void addAvoidPlayerGoal(AvoidEntityGoal<T> instance, T value) {
        if (this.avoidClass.isAssignableFrom(Cat.class)) {
            DummyCat cache = DummyCat.fromPlayer(this.mob.level(), this.mob.level().getNearestEntity(
                Player.class,
                this.avoidEntityTargeting.selector(
                    player1 -> player1.getData(ModDataAttachments.SCARE_ENTITIES).getBoolean("creepers")
                ),
                this.mob,
                this.mob.getX(),
                this.mob.getY(),
                this.mob.getZ(),
                this.mob.getBoundingBox().inflate(this.maxDist, 3.0, this.maxDist)
            ));
            if (cache == null) {
                ((AvoidEntityGoalMixin<T>) (Object) instance).anvilcraft$setToAvoid(value);
            } else {
                ((AvoidEntityGoalMixin<T>) (Object) instance).anvilcraft$setToAvoid((T) cache);
            }
        } else if (this.avoidClass.isAssignableFrom(Wolf.class)) {
            DummyWolf cache = DummyWolf.fromPlayer(this.mob.level(), this.mob.level().getNearestEntity(
                Player.class,
                this.avoidEntityTargeting.selector(
                    player1 -> player1.getData(ModDataAttachments.SCARE_ENTITIES).getBoolean("creepers")
                ),
                this.mob,
                this.mob.getX(),
                this.mob.getY(),
                this.mob.getZ(),
                this.mob.getBoundingBox().inflate(this.maxDist, 3.0, this.maxDist)
            ));
            if (cache == null) {
                ((AvoidEntityGoalMixin<T>) (Object) instance).anvilcraft$setToAvoid(value);
            } else {
                ((AvoidEntityGoalMixin<T>) (Object) instance).anvilcraft$setToAvoid((T) cache);
            }
        }
    }

    @Unique
    private void anvilcraft$setToAvoid(T toAvoid) {
        this.toAvoid = toAvoid;
    }
}
