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
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

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
    public @Nullable PortalProcessor portalProcess;

    @Override
    public boolean anvilcraft$isDeflected() {
        return this.anvil$isDeflected;
    }

    @Override
    public Vec3 anvilcraft$getFixedDeltaMovement() {
        return this.anvil$fixedDeltaMovement;
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 anvilcraft$applyGravityMovementEffects(Vec3 movement) {
        return GravityManager.applyMovementEffects((Entity) (Object) this, movement);
    }

    @WrapOperation(
        method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/phys/Vec3;add(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
        ordinal = 1
    )
    )
    public Vec3 anvilcraft$fixFallingBlockEntity(
        Vec3 position,
        Vec3 movement,
        Operation<Vec3> original
    ) {
        Vec3 targetPosition = original.call(position, movement);
        Vec3 actualMovement = targetPosition.subtract(position);
        if (Util.instanceOfAny(this, Projectile.class, FallingBlockEntity.class, Player.class)
            && actualMovement.length() > 0.98) {
            Vec3 start = this.anvilcraft$getMovementCenter();
            if (!this.anvilcraft$findDeflectionRing(start, actualMovement)) {
                this.anvil$isDeflected = false;
                return targetPosition;
            }
            Vec3 target = this.anvilcraft$getDeflectionTarget();
            this.anvil$isMovementFixed = true;
            this.anvil$fixedDeltaMovement = target.subtract(position);
            this.anvil$isDeflected = true;
            return target;
        }
        this.anvil$isDeflected = false;
        return targetPosition;
    }

    @WrapOperation(
        method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/util/Mth;equal(DD)Z", ordinal = 0
    )
    )
    public boolean anvilcraft$cancelCollision1(double x, double y, Operation<Boolean> original) {
        return this.anvil$isMovementFixed || original.call(x, y);
    }

    @WrapOperation(
        method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/util/Mth;equal(DD)Z", ordinal = 1
    )
    )
    public boolean anvilcraft$cancelCollision2(double x, double y, Operation<Boolean> original) {
        return this.anvil$isMovementFixed || original.call(x, y);
    }

    @WrapOperation(
        method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;"
                     + "updateEntityMovementAfterFallOn("
                     + "Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V"
        )
    )
    private void anvilcraft$preserveVerticalSpeedAtDeflectionRing(
        Block block,
        BlockGetter level,
        Entity entity,
        Operation<Void> original
    ) {
        if (!this.anvil$isMovementFixed) original.call(block, level, entity);
    }

    @Inject(method = "setPos(DDD)V", at = @At("HEAD"), cancellable = true)
    public void anvilcraft$changeProjectilePosSetResult(double x, double y, double z, CallbackInfo ci) {
        if (!Util.instanceOfAny(this, Projectile.class)) return;
        Vec3 vec3 = new Vec3(x - this.getX(), y - this.getY(), z - this.getZ());
        if (!this.anvilcraft$isProjectileMovement(vec3)) return;
        if (vec3.length() > 0.98) {
            if (!this.anvilcraft$findDeflectionRing(this.anvilcraft$getMovementCenter(), vec3)) return;
            Vec3 pos = this.anvilcraft$getDeflectionTarget();
            this.setPosRaw(pos.x, pos.y, pos.z);
            this.setBoundingBox(this.makeBoundingBox());
            ci.cancel();
        }
    }

    @Unique
    private boolean anvilcraft$isProjectileMovement(Vec3 movement) {
        double movementLength = movement.length();
        double velocityLength = this.getDeltaMovement().length();
        if (movementLength <= 0.98 || velocityLength < 1.0E-6) return false;
        double lengthRatio = movementLength / velocityLength;
        double directionSimilarity = movement.dot(this.getDeltaMovement()) / (movementLength * velocityLength);
        return lengthRatio >= 0.5 && lengthRatio <= 2.0 && directionSimilarity >= 0.95;
    }

    @Unique
    private Vec3 anvilcraft$getMovementCenter() {
        return AccelerateManager.getMovementCenter((Entity) (Object) this);
    }

    @Unique
    private Vec3 anvilcraft$getDeflectionTarget() {
        Entity entity = (Entity) (Object) this;
        return this.anvil$hitDeflectionRing.getCenter().subtract(AccelerateManager.getMovementOffset(entity));
    }

    @Unique
    private boolean anvilcraft$findDeflectionRing(Vec3 start, Vec3 movement) {
        this.anvil$hitDeflectionRing = DeflectionRingBlockEntity.findFirstRing((Entity) (Object) this, start, movement);
        return this.anvil$hitDeflectionRing != null;
    }

    @Inject(method = "move", at = @At("HEAD"))
    public void anvil$recordMovement(
        MoverType type,
        Vec3 pos,
        CallbackInfo ci
    ) {
        this.anvil$isMovementFixed = false;
        this.anvil$beforeBoundingMovement = this.getDeltaMovement();
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
        BlockPos blockPos = BlockPos.containing(this.position.add(this.anvil$beforeBoundingMovement
            .scale(0.55 / this.anvil$beforeBoundingMovement.length())
            .multiply(1, 0, 1)));
        NeoForge.EVENT_BUS.post(
            new AnvilEvent.CollisionBlock(this.level, blockPos, self, this.anvil$beforeBoundingMovement.length())
        );
    }

    @WrapOperation(
        method = "handlePortal",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;teleport("
                     + "Lnet/minecraft/world/level/portal/TeleportTransition;"
                     + ")"
                     + "Lnet/minecraft/world/entity/Entity;"
        )
    )
    @SuppressWarnings("deprecation")
    private Entity handlePortal(Entity instance, TeleportTransition transition, Operation<Entity> original) {
        if (!(this.portalProcess instanceof PortalProcessorAccessor accessor)) return original.call(instance, transition);
        Block portal = Util.cast(accessor.getPortal());
        EntityThroughPortalEvent event = NeoForge.EVENT_BUS.post(new EntityThroughPortalEvent(
            this.level,
            instance,
            new PortalType(portal.builtInRegistryHolder().key().identifier())
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
