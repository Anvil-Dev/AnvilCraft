package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.init.item.ModAmulets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AvoidEntityGoal.class)
public abstract class AvoidEntityGoalMixin<T extends LivingEntity> {
    @Shadow
    @Final
    protected PathfinderMob mob;
    @Shadow
    @Final
    protected Class<T> avoidClass;
    @Shadow
    @Final
    protected float maxDist;
    @Shadow
    @Final
    private TargetingConditions avoidEntityTargeting;
    @Unique
    @Nullable
    protected LivingEntity anvilcraft$toAvoid;

    @Definition(
        id = "toAvoid",
        field = "Lnet/minecraft/world/entity/ai/goal/AvoidEntityGoal;toAvoid:Lnet/minecraft/world/entity/LivingEntity;"
    )
    @Definition(id = "mob", field = "Lnet/minecraft/world/entity/ai/goal/AvoidEntityGoal;mob:Lnet/minecraft/world/entity/PathfinderMob;")
    @Definition(id = "level", method = "Lnet/minecraft/world/entity/PathfinderMob;level()Lnet/minecraft/world/level/Level;")
    @Definition(
        id = "getNearestEntity",
        // CHECKSTYLE.SUPPRESS: LineLength for +2 lines - 换行后 MC DEV 插件会报错
        method = "Lnet/minecraft/world/level/Level;"
                 + "getNearestEntity(Ljava/util/List;Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;Lnet/minecraft/world/entity/LivingEntity;DDD)"
                 + "Lnet/minecraft/world/entity/LivingEntity;"
    )
    @Expression("this.toAvoid = this.mob.level().getNearestEntity(?,?,?,?,?,?)")
    @WrapOperation(method = "canUse", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void addAvoidPlayerGoal(AvoidEntityGoal<T> instance, @Nullable T value, Operation<Void> original) {
        this.anvilcraft$toAvoid = Util.<ServerLevel>cast(this.mob.level()).getNearestEntity(
            this.mob.level().getEntitiesOfClass(
                LivingEntity.class,
                this.mob.getBoundingBox().inflate(this.maxDist, 3.0, this.maxDist),
                entity -> anvilcraft$is(this.avoidClass, entity)
            ),
            this.avoidEntityTargeting,
            this.mob,
            this.mob.getX(),
            this.mob.getY(),
            this.mob.getZ()
        );
    }

    @Definition(
        id = "toAvoid",
        field = "Lnet/minecraft/world/entity/ai/goal/AvoidEntityGoal;toAvoid:Lnet/minecraft/world/entity/LivingEntity;"
    )
    @Expression("this.toAvoid == null")
    @WrapOperation(method = "canUse", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean replaceVanillaToOurs(Object left, Object right, Operation<Boolean> original) {
        return original.call(this.anvilcraft$toAvoid, right);
    }

    @WrapOperation(
        method = "canUse",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;position()Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 useOurs(LivingEntity instance, Operation<Vec3> original) {
        return original.call(this.anvilcraft$toAvoid);
    }

    @WrapOperation(
        method = "canUse",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"
        )
    )
    private double useOurs(LivingEntity instance, Entity entity, Operation<Double> original) {
        return original.call(this.anvilcraft$toAvoid, entity);
    }

    @WrapOperation(
        method = "canUse",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;distanceToSqr(DDD)D"
        )
    )
    private double useOurs(LivingEntity instance, double x2, double y2, double z2, Operation<Double> original) {
        return original.call(this.anvilcraft$toAvoid, x2, y2, z2);
    }

    @WrapOperation(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/PathfinderMob;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"
        )
    )
    private double useOurs(PathfinderMob instance, Entity entity, Operation<Double> original) {
        return original.call(instance, this.anvilcraft$toAvoid);
    }

    @Unique
    private static boolean anvilcraft$is(Class<? extends LivingEntity> avoiding, LivingEntity entity) {
        if (Cat.class.isAssignableFrom(avoiding)) {
            return entity instanceof Cat
                   || entity instanceof Player player
                      && AmuletManager.get(player.registryAccess()).hasAmuletInInventory(player, ModAmulets.CAT);
        }
        if (Wolf.class.isAssignableFrom(avoiding)) {
            return entity instanceof Wolf
                   || entity instanceof Player player
                      && AmuletManager.get(player.registryAccess()).hasAmuletInInventory(player, ModAmulets.DOG);
        }
        return Util.instanceOfAny(entity, avoiding);
    }
}
