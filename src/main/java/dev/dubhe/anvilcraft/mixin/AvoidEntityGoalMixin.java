package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.init.item.ModAmulets;
import dev.dubhe.anvilcraft.mixin.accessor.TargetingConditionsAccessor;
import dev.dubhe.anvilcraft.util.dummy.DummyCat;
import dev.dubhe.anvilcraft.util.dummy.DummyWolf;
import dev.dubhe.anvilcraft.util.mixin.ModifiedSelector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Objects;
import java.util.Optional;

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
    @Shadow
    @Nullable
    protected T toAvoid;

    @Definition(
        id = "toAvoid",
        field = "Lnet/minecraft/world/entity/ai/goal/AvoidEntityGoal;toAvoid:Lnet/minecraft/world/entity/LivingEntity;"
    )
    @Definition(
        id = "getServerLevel",
        method = "Lnet/minecraft/world/entity/ai/goal/AvoidEntityGoal;"
                 + "getServerLevel(Lnet/minecraft/world/entity/Entity;)"
                 + "Lnet/minecraft/server/level/ServerLevel;"
    )
    // CHECKSTYLE.SUPPRESS: LineLength for +4 lines 因为 MC DEV 插件会在换行后报错
    @Definition(
        id = "getNearestEntity",
        method = "Lnet/minecraft/server/level/ServerLevel;"
                 + "getNearestEntity(Ljava/util/List;Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;Lnet/minecraft/world/entity/LivingEntity;DDD)"
                 + "Lnet/minecraft/world/entity/LivingEntity;"
    )
    @Expression("this.toAvoid = getServerLevel(?).getNearestEntity(?,?,?,?,?,?)")
    @WrapOperation(method = "canUse", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void addAvoidPlayerGoal(AvoidEntityGoal<T> instance, @Nullable T value, Operation<Void> original) {
        LivingEntity toAvoid = Util.<ServerLevel>cast(this.mob.level()).getNearestEntity(
            this.mob.level().getEntitiesOfClass(
                LivingEntity.class,
                this.mob.getBoundingBox().inflate(this.maxDist, 3.0, this.maxDist),
                entity -> Util.instanceOfAny(entity, this.avoidClass) || anvilcraft$is(this.avoidClass, entity)
            ),
            this.avoidEntityTargeting.selector(
                Optional.ofNullable(((TargetingConditionsAccessor) this.avoidEntityTargeting).getSelector())
                    .map(p -> ModifiedSelector.toModified(
                        p,
                        old -> (entity, level) -> {
                            if (anvilcraft$is(this.avoidClass, entity)) {
                                entity = anvilcraft$toDummy(this.avoidClass, entity);
                            }
                            return old.test(Objects.requireNonNull(entity), level);
                        }
                    ))
                    .orElse((entity, _) -> {
                        if (anvilcraft$is(this.avoidClass, entity)) {
                            entity = anvilcraft$toDummy(this.avoidClass, entity);
                        }
                        return Util.instanceOfAny(entity, this.avoidClass) || anvilcraft$is(this.avoidClass, entity);
                    })
            ),
            this.mob,
            this.mob.getX(),
            this.mob.getY(),
            this.mob.getZ()
        );
        if (anvilcraft$is(this.avoidClass, toAvoid)) {
            toAvoid = anvilcraft$toDummy(this.avoidClass, Objects.requireNonNull(toAvoid));
        }
        // noinspection DataFlowIssue
        this.toAvoid = Util.cast(toAvoid);
    }

    @Unique
    private static boolean anvilcraft$is(Class<? extends LivingEntity> avoiding, @Nullable LivingEntity entity) {
        if (Cat.class.isAssignableFrom(avoiding)) {
            return entity instanceof Player player
                   && AmuletManager.get(player.registryAccess()).hasAmuletInInventory(player, ModAmulets.CAT);
        }
        if (Wolf.class.isAssignableFrom(avoiding)) {
            return entity instanceof Player player
                   && AmuletManager.get(player.registryAccess()).hasAmuletInInventory(player, ModAmulets.DOG);
        }
        return false;
    }

    @Unique
    private static @Nullable LivingEntity anvilcraft$toDummy(Class<? extends LivingEntity> avoiding, LivingEntity entity) {
        if (Cat.class.isAssignableFrom(avoiding)) {
            return DummyCat.fromPlayer(entity.level(), Util.cast(entity));
        }
        if (Wolf.class.isAssignableFrom(avoiding)) {
            return DummyWolf.fromPlayer(entity.level(), Util.cast(entity));
        }
        return null;
    }
}
