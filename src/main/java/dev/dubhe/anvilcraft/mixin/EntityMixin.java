package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.entity.IAnvilCraftEntityExtension;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.event.EntityThroughPortalEvent;
import dev.dubhe.anvilcraft.api.injection.entity.IEntityExtension;
import dev.dubhe.anvilcraft.api.portal.PortalType;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import dev.dubhe.anvilcraft.mixin.accessor.PortalProcessorAccessor;
import dev.dubhe.anvilcraft.util.AccelerateManager;
import dev.dubhe.anvilcraft.util.GravityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import javax.annotation.Nullable;

@Mixin(Entity.class)
public abstract class EntityMixin implements IEntityExtension {
    @Unique
    public Vec3 anvil$fixedDeltaMovement = Vec3.ZERO;

    @Unique
    public boolean anvil$isDeflected = false;

    @Unique
    private BlockPos anvil$hitDeflectionRing;

    @Unique
    private boolean anvil$isMovementFixed;

    @Unique
    private Vec3 anvil$beforeBoundingMovement = Vec3.ZERO;

    @Shadow
    private Level level;

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract void setPos(Vec3 pos);

    @Shadow
    public abstract void setPos(double x, double y, double z);

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract double getZ();

    @Shadow
    public abstract Vec3 getDeltaMovement();

    @Shadow
    public abstract void setPosRaw(double x, double y, double z);

    @Shadow
    public abstract void setBoundingBox(AABB bb);

    @Shadow
    protected abstract AABB makeBoundingBox();

    @Shadow
    public boolean horizontalCollision;

    @Shadow
    public abstract Pose getPose();

    @Shadow
    private Vec3 position;

    @Shadow
    @Nullable
    public PortalProcessor portalProcess;

    @Override
    public boolean anvilcraft$isDeflected() {
        return anvil$isDeflected;
    }

    @Override
    public Vec3 anvilcraft$getFixedDeltaMovement() {
        return anvil$fixedDeltaMovement;
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 anvilcraft$applySweptGravity(Vec3 movement) {
        return GravityManager.applySweptGravity((Entity) (Object) this, movement);
    }

    @WrapOperation(
        method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V", ordinal = 1
    )
    )
    public void anvilcraft$fixFallingBlockEntity(
        Entity instance,
        double x,
        double y,
        double z,
        Operation<Void> original
    ) {
        Vec3 vec3 = new Vec3(x - getX(), y - getY(), z - getZ());
        if (Util.instanceOfAny(this, Projectile.class, FallingBlockEntity.class, Player.class) && vec3.length() > 0.98) {
            Vec3 start = anvilcraft$getMovementCenter();
            if (!anvilcraft$findDeflectionRing(start, vec3)) {
                anvil$isDeflected = false;
                original.call(instance, x, y, z);
                return;
            }
            Vec3 target = anvilcraft$getDeflectionTarget();
            Vec3 fixedMovement = target.subtract(position());
            original.call(instance, target.x, target.y, target.z);
            anvil$isMovementFixed = true;
            anvil$fixedDeltaMovement = fixedMovement;
            anvil$isDeflected = true;

            return;
        }
        anvil$isDeflected = false;
        original.call(instance, x, y, z);
    }

    @WrapOperation(
        method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/util/Mth;equal(DD)Z", ordinal = 0
    )
    )
    public boolean anvilcraft$cancelCollision1(double x, double y, Operation<Boolean> original) {
        return anvil$isMovementFixed || original.call(x, y);
    }

    @WrapOperation(
        method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/util/Mth;equal(DD)Z", ordinal = 1
    )
    )
    public boolean anvilcraft$cancelCollision2(double x, double y, Operation<Boolean> original) {
        return anvil$isMovementFixed || original.call(x, y);
    }

    @WrapOperation(
        method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;"
                     + "updateEntityAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V"
        )
    )
    private void anvilcraft$preserveVerticalSpeedAtDeflectionRing(
        Block block,
        BlockGetter level,
        Entity entity,
        Operation<Void> original
    ) {
        if (!anvil$isMovementFixed) original.call(block, level, entity);
    }

    @Inject(method = "setPos(DDD)V", at = @At("HEAD"), cancellable = true)
    public void anvilcraft$changeProjectilePosSetResult(double x, double y, double z, CallbackInfo ci) {
        if (!Util.instanceOfAny(this, Projectile.class)) return;
        Vec3 vec3 = new Vec3(x - getX(), y - getY(), z - getZ());
        if (!anvilcraft$isProjectileMovement(vec3)) return;
        if (vec3.length() > 0.98) {
            if (!anvilcraft$findDeflectionRing(anvilcraft$getMovementCenter(), vec3)) return;
            Vec3 pos = anvilcraft$getDeflectionTarget();
            setPosRaw(pos.x, pos.y, pos.z);
            setBoundingBox(makeBoundingBox());
            ci.cancel();
        }
    }

    @Unique
    private boolean anvilcraft$isProjectileMovement(Vec3 movement) {
        double movementLength = movement.length();
        double velocityLength = getDeltaMovement().length();
        if (movementLength <= 0.98 || velocityLength < 1.0E-6) return false;
        double lengthRatio = movementLength / velocityLength;
        double directionSimilarity = movement.dot(getDeltaMovement()) / (movementLength * velocityLength);
        return lengthRatio >= 0.5 && lengthRatio <= 2.0 && directionSimilarity >= 0.95;
    }

    @Unique
    private Vec3 anvilcraft$getMovementCenter() {
        return AccelerateManager.getMovementCenter((Entity) (Object) this);
    }

    @Unique
    private Vec3 anvilcraft$getDeflectionTarget() {
        Entity entity = (Entity) (Object) this;
        return anvil$hitDeflectionRing.getCenter().subtract(AccelerateManager.getMovementOffset(entity));
    }

    @Unique
    private boolean anvilcraft$findDeflectionRing(Vec3 start, Vec3 movement) {
        anvil$hitDeflectionRing = DeflectionRingBlockEntity.findFirstRing((Entity) (Object) this, start, movement);
        return anvil$hitDeflectionRing != null;
    }

    @Inject(method = "move", at = @At("HEAD"))
    public void anvil$recordMovement(
        MoverType type,
        Vec3 pos,
        CallbackInfo ci
    ) {
        anvil$isMovementFixed = false;
        anvil$beforeBoundingMovement = this.getDeltaMovement();
    }

    @Inject(method = "move", at = @At("RETURN"))
    public void anvil$collisionCraft(
        MoverType type,
        Vec3 pos,
        CallbackInfo ci
    ) {
        Optional<FallingBlockEntity> entityOp = Util.castSafely(this, FallingBlockEntity.class);
        if (entityOp.isEmpty() || !this.horizontalCollision) return;
        FallingBlockEntity self = entityOp.get();
        if (self instanceof IAnvilCraftEntityExtension extension
            && !extension.anvilcraft$canCollisionCraft()) {
            return;
        }
        BlockPos blockPos = BlockPos.containing(this.position.add(anvil$beforeBoundingMovement
            .scale(0.55 / anvil$beforeBoundingMovement.length())
            .multiply(1, 0, 1)));
        NeoForge.EVENT_BUS.post(new AnvilEvent.CollisionBlock(level, blockPos, self, anvil$beforeBoundingMovement.length()));
    }

    @WrapOperation(
        method = "handlePortal",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;changeDimension("
                     + "Lnet/minecraft/world/level/portal/DimensionTransition;"
                     + ")"
                     + "Lnet/minecraft/world/entity/Entity;"
        )
    )
    private Entity handlePortal(Entity instance, DimensionTransition transition, Operation<Entity> original) {
        if (!(this.portalProcess instanceof PortalProcessorAccessor accessor)) return original.call(instance, transition);
        EntityThroughPortalEvent event = NeoForge.EVENT_BUS.post(new EntityThroughPortalEvent(
            this.level,
            instance,
            PortalType.assume(accessor.getPortal())
        ));
        if (event.isCanceled()) return instance;
        return original.call(event.getEntity(), transition);
    }

    @Inject(
        method = "getGravity", at = @At("RETURN"), cancellable = true
    )
    private void anvilcraft$ApplyGravity(CallbackInfoReturnable<Double> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity.isNoGravity()) {
            cir.setReturnValue(0.0);
            return;
        }
        if (AccelerateManager.isControlledByRing(entity)) {
            cir.setReturnValue(0.0);
            return;
        }
        Level level = entity.level();

        // 获取基础重力
        double vanillaGravity = cir.getReturnValue();
        double baseGravity = vanillaGravity * GravityManager.getGravityType(entity).getScalar();
        Vec3 localGravity = GravityManager.getGravityVector(entity, vanillaGravity);

        // 维度重力 = 基础重力 * 维度系数
        double dimensionGravity = baseGravity * GravityManager.getDimensionGravity(level);

        // 实际重力 = 维度重力 - 引力向量.y
        double newGravity = dimensionGravity - localGravity.y;

        // 返回实际重力
        cir.setReturnValue(newGravity);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void anvilcraft$ApplyHorizontalGravity(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        // 排除无重力实体和创造飞行玩家
        if (
            entity instanceof FallingBlockEntity
            || entity.isNoGravity()
            || AccelerateManager.isControlledByRing(entity)
            || (entity instanceof Player player && player.getAbilities().flying)
        ) {
            return;
        }

        // 应用引力向量的水平分量
        Vec3 localGravity = GravityManager.getGravityVector(entity);
        if (localGravity.x != 0 || localGravity.z != 0) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(localGravity.x, 0, localGravity.z));
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void anvilcraft$handleAcceleration(CallbackInfo ci) {
        AccelerateManager.handleAcceleration((Entity) (Object) this);
    }
}
