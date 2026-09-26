package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.amulet.Amulet;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.init.item.ModAmulets;
import dev.dubhe.anvilcraft.mixin.accessor.TargetingConditionsAccessor;
import dev.dubhe.anvilcraft.util.dummy.DummyArmadillo;
import dev.dubhe.anvilcraft.util.dummy.DummyCat;
import dev.dubhe.anvilcraft.util.dummy.DummyWolf;
import dev.dubhe.anvilcraft.util.mixin.ModifiedSelector;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

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
        ResourceKey<Amulet> amulet = anvilcraft$amuletKey(this.avoidClass);
        if (amulet == null) {
            // 该 goal 与护符伪装无关，保持原版行为
            this.toAvoid = value;
            return;
        }
        // 一次 canUse 内只判定一个 avoidClass，同一个玩家会被反复询问，这里按玩家缓存判定结果
        Map<Player, Boolean> cache = new IdentityHashMap<>();
        LivingEntity toAvoid = Util.<ServerLevel>cast(this.mob.level()).getNearestEntity(
            this.mob.level().getEntitiesOfClass(
                LivingEntity.class,
                this.mob.getBoundingBox().inflate(this.maxDist, 3.0, this.maxDist),
                entity -> Util.instanceOfAny(entity, this.avoidClass) || anvilcraft$is(amulet, entity, cache)
            ),
            this.avoidEntityTargeting.selector(
                Optional.ofNullable(((TargetingConditionsAccessor) this.avoidEntityTargeting).getSelector())
                    .map(p -> ModifiedSelector.toModified(
                        p,
                        old -> entity -> {
                            if (anvilcraft$is(amulet, entity, cache)) {
                                entity = anvilcraft$toDummy(this.avoidClass, entity);
                            }
                            return old.test(entity);
                        }
                    ))
                    .orElse(entity -> {
                        if (anvilcraft$is(amulet, entity, cache)) {
                            entity = anvilcraft$toDummy(this.avoidClass, entity);
                        }
                        return Util.instanceOfAny(entity, this.avoidClass) || anvilcraft$is(amulet, entity, cache);
                    })
            ),
            this.mob,
            this.mob.getX(),
            this.mob.getY(),
            this.mob.getZ()
        );
        if (anvilcraft$is(amulet, toAvoid, cache)) {
            toAvoid = anvilcraft$toDummy(this.avoidClass, Objects.requireNonNull(toAvoid));
        }
        // noinspection DataFlowIssue
        this.toAvoid = Util.cast(toAvoid);
    }

    /// 获取玩家需要佩戴哪个护符才会被视作给定生物
    ///
    /// @param avoiding 规避目标的生物类型
    /// @return 对应的护符资源键，该生物不规避任何护符伪装时为 null
    @Unique
    private static @Nullable ResourceKey<Amulet> anvilcraft$amuletKey(Class<? extends LivingEntity> avoiding) {
        if (Cat.class.isAssignableFrom(avoiding)) {
            return ModAmulets.CAT.getKey();
        }
        if (Wolf.class.isAssignableFrom(avoiding)) {
            return ModAmulets.DOG.getKey();
        }
        if (Armadillo.class.isAssignableFrom(avoiding)) {
            return ModAmulets.ARMADILLO.getKey();
        }
        return null;
    }

    /// 判断玩家是否应被视作给定生物
    ///
    /// @param amulet 需要佩戴的护符
    /// @param entity 待判定的实体
    /// @param cache  本次判定中按玩家缓存的判定结果
    /// @return 玩家是否应被视作给定生物
    @Unique
    private static boolean anvilcraft$is(
        ResourceKey<Amulet> amulet,
        @Nullable LivingEntity entity,
        Map<Player, Boolean> cache
    ) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        Boolean cached = cache.get(player);
        if (cached != null) {
            return cached;
        }
        boolean result = AmuletManager.get(player.registryAccess()).isAmuletActive(player, amulet);
        cache.put(player, result);
        return result;
    }

    @Unique
    private static @Nullable LivingEntity anvilcraft$toDummy(Class<? extends LivingEntity> avoiding, LivingEntity entity) {
        if (Cat.class.isAssignableFrom(avoiding)) {
            return DummyCat.fromPlayer(entity.level(), Util.cast(entity));
        }
        if (Wolf.class.isAssignableFrom(avoiding)) {
            return DummyWolf.fromPlayer(entity.level(), Util.cast(entity));
        }
        if (Armadillo.class.isAssignableFrom(avoiding)) {
            return DummyArmadillo.fromPlayer(entity.level(), Util.cast(entity));
        }
        return null;
    }
}
