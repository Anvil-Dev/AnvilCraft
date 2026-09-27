package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
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
        // 已无通用方法预先判断躲避的实体类是否有护符覆盖
        List<LivingEntity> entities = this.mob.level().getEntitiesOfClass(
            LivingEntity.class,
            this.mob.getBoundingBox().inflate(this.maxDist, 3.0, this.maxDist),
            entity -> anvilcraft$is(this.avoidClass, entity)
        );
        List<T> avoidingList = new ArrayList<>();
        for (LivingEntity living : entities) {
            avoidingList.add(Util.castSafely(living, this.avoidClass).orElseGet(() -> anvilcraft$toDummy(this.avoidClass, living)));
        }
        this.toAvoid = Util.<ServerLevel>cast(this.mob.level()).getNearestEntity(
            avoidingList,
            this.avoidEntityTargeting,
            this.mob,
            this.mob.getX(),
            this.mob.getY(),
            this.mob.getZ()
        );
    }

    /// 判断玩家是否应被视作给定生物
    ///
    /// @param entity 待判定的实体
    /// @return 玩家是否应被视作给定生物
    @Unique
    private static boolean anvilcraft$is(
        Class<? extends LivingEntity> avoiding,
        @Nullable LivingEntity entity
    ) {
        if (entity == null || avoiding.isInstance(entity)) return false;

        AmuletEffectContext ctx = new AmuletEffectContext();
        ctx.set(ModAmuletEffectContextKeys.LIVING_ENTITY_CLASS, avoiding);
        ctx.set(ModAmuletEffectContextKeys.SIMULATE, true);
        AmuletManager.get(entity.registryAccess()).trigger(entity, ctx);
        return ctx.getOrDefault(ModAmuletEffectContextKeys.MASK_VALID, false);
    }

    @Unique
    private static <T extends LivingEntity> @Nullable T anvilcraft$toDummy(Class<T> avoiding, LivingEntity entity) {
        AmuletEffectContext ctx = new AmuletEffectContext();
        ctx.set(ModAmuletEffectContextKeys.LIVING_ENTITY_CLASS, avoiding);
        AmuletManager.get(entity.registryAccess()).trigger(entity, ctx);
        return ctx.get(ModAmuletEffectContextKeys.TO_AVOID_ENTITY)
            .filter(avoiding::isInstance)
            .map(avoiding::cast)
            .orElse(null);
    }
}
