package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.event.EntityThroughPortalEvent;
import dev.dubhe.anvilcraft.api.injection.entity.IEntityExtension;
import dev.dubhe.anvilcraft.api.portal.PortalType;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import dev.dubhe.anvilcraft.mixin.accessor.PortalProcessorAccessor;
import dev.dubhe.anvilcraft.util.AccelerateManager;
import dev.dubhe.anvilcraft.util.GravityManager;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Optional;

@Mixin(Entity.class)
public abstract class EntityMixin implements IEntityExtension {
    @Unique
    public Vec3 anvil$fixedDeltaMovement;
    @Unique
    public Boolean anvil$isDeflected;

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
        return this.anvil$isDeflected;
    }

    @Override
    public Vec3 anvilcraft$getFixedDeltaMovement() {
        return this.anvil$fixedDeltaMovement;
    }

    @WrapOperation(
        method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;setPos(Lnet/minecraft/world/phys/Vec3;)V"
        )
    )
    public void anvilcraft$fixFallingBlockEntity(
        Entity instance,
        Vec3 pos,
        Operation<Void> original,
        @Share("isFixed") LocalBooleanRef isFixed
    ) {
        isFixed.set(false);
        Vec3 vec3 = new Vec3(pos.x - this.getX(), pos.y - this.getY(), pos.z - this.getZ());
        if (Util.instanceOfAny(this, Projectile.class, FallingBlockEntity.class, Player.class) && vec3.length() > 0.98) {
            Vec3 s = this.position();
            Vec3 e = vec3.add(s);
            ArrayList<Pair<BlockPos, Double>> blockPosList = new ArrayList<>();
            for (BlockPos blockPos : DeflectionRingBlockEntity.getAllBlocks(this.level)) {
                Vec3 q = blockPos.getCenter();
                double a = s.distanceTo(q);
                double b = e.distanceTo(q);
                double c = s.distanceTo(e);
                double d = -(b * b - c * c - a * a) / (2 * c);
                double distance = Math.sqrt(a * a - d * d);
                if (distance <= 0.56747 && d > 0) {
                    blockPosList.add(Pair.of(blockPos, d));
                }
            }
            double distance = Double.MAX_VALUE;
            BlockPos blockPos = null;
            for (Pair<BlockPos, Double> blockPosDoublePair : blockPosList) {
                if (distance > blockPosDoublePair.right()) {
                    distance = blockPosDoublePair.right();
                    blockPos = blockPosDoublePair.left();
                }
            }
            if (blockPos == null) {
                this.anvil$isDeflected = false;
                this.setPos(e);
                return;
            }
            double a = distance / vec3.length();

            if (a > 1) {
                this.anvil$isDeflected = false;
                this.setPos(e);
                return;
            }
            this.setPos(vec3.multiply(a, a, a).add(s));
            isFixed.set(true);
            this.anvil$fixedDeltaMovement = vec3.multiply(a, a, a);
            this.anvil$isDeflected = true;

            return;
        }
        this.anvil$isDeflected = false;
        original.call(instance, pos);
    }

    @WrapOperation(
        method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(
            value = "INVOKE", target = "Lnet/minecraft/util/Mth;equal(DD)Z", ordinal = 0
        )
    )
    public boolean anvilcraft$cancelCollision1(double a, double b, Operation<Boolean> original, @Share("isFixed") LocalBooleanRef isFixed) {
        return isFixed.get() || original.call(a, b);
    }

    @WrapOperation(
        method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(
            value = "INVOKE", target = "Lnet/minecraft/util/Mth;equal(DD)Z", ordinal = 1
        )
    )
    public boolean anvilcraft$cancelCollision2(double a, double b, Operation<Boolean> original, @Share("isFixed") LocalBooleanRef isFixed) {
        return isFixed.get() || original.call(a, b);
    }

    @Inject(method = "setPos(DDD)V", at = @At("HEAD"), cancellable = true)
    public void anvilcraft$changeProjectilePosSetResult(double x, double y, double z, CallbackInfo ci) {
        if (!Util.instanceOfAny(this, Projectile.class)) return;
        Vec3 vec3 = new Vec3(x - this.getX(), y - this.getY(), z - this.getZ());
        if (vec3.add(this.getDeltaMovement().scale(-1)).length() > 0.5) return;
        if (Util.instanceOfAny(this, Projectile.class, FallingBlockEntity.class) && vec3.length() > 0.98) {
            Vec3 s = this.position();
            Vec3 e = vec3.add(s);
            ArrayList<Pair<BlockPos, Double>> blockPosList = new ArrayList<>();
            for (BlockPos blockPos : DeflectionRingBlockEntity.getAllBlocks(this.level)) {
                Vec3 q = blockPos.getCenter();
                double a = s.distanceTo(q);
                double b = e.distanceTo(q);
                double c = s.distanceTo(e);
                double d = -(b * b - c * c - a * a) / (2 * c);
                double distance = Math.sqrt(a * a - d * d);
                if (distance <= 0.56747 && d > 0) {
                    blockPosList.add(Pair.of(blockPos, d));
                }
            }
            double distance = Double.MAX_VALUE;
            BlockPos blockPos = null;
            for (Pair<BlockPos, Double> pos : blockPosList) {
                if (distance > pos.right()) {
                    distance = pos.right();
                    blockPos = pos.left();
                }
            }
            if (blockPos == null) return;
            double a = distance / vec3.length();

            if (a > 1) return;
            Vec3 pos = vec3.multiply(a, a, a).add(s);
            this.setPosRaw(pos.x, pos.y, pos.z);
            this.setBoundingBox(this.makeBoundingBox());
            ci.cancel();
        }
    }

    @Inject(method = "move", at = @At("HEAD"))
    public void anvil$recordMovement(
        MoverType moverType,
        Vec3 delta,
        CallbackInfo ci,
        @Share("beforeBoundingMovement") LocalRef<Vec3> beforeBoundingMovement
    ) {
        beforeBoundingMovement.set(this.getDeltaMovement());
    }

    @Inject(method = "move", at = @At("RETURN"))
    public void anvil$collisionCraft(
        MoverType moverType,
        Vec3 delta,
        CallbackInfo ci,
        @Share("beforeBoundingMovement") LocalRef<Vec3> beforeBoundingMovement
    ) {
        Optional<FallingBlockEntity> entityOp = Util.castSafely(this, FallingBlockEntity.class);
        if (entityOp.isEmpty() || !this.horizontalCollision) return;
        FallingBlockEntity self = entityOp.get();
        BlockPos blockPos = BlockPos.containing(this.position.add(beforeBoundingMovement.get()
            .scale(0.55 / beforeBoundingMovement.get().length())
            .multiply(1, 0, 1)));
        NeoForge.EVENT_BUS.post(new AnvilEvent.CollisionBlock(this.level, blockPos, self, beforeBoundingMovement.get().length()));
    }

    @WrapOperation(
        method = "handlePortal",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;teleport("
                     + "Lnet/minecraft/world/level/portal/TeleportTransition;)"
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
        Level level = entity.level();

        // 获取基础重力
        double baseGravity = cir.getReturnValue() * GravityManager.getGravityType(entity).getScalar();

        // 维度重力 = 基础重力 * 维度系数
        double dimensionGravity = baseGravity * GravityManager.getDimensionGravity(level);

        // 实际重力 = 维度重力 - 引力向量.y
        double newGravity = dimensionGravity - GravityManager.getGravityVector(entity).y;

        // 返回实际重力
        cir.setReturnValue(newGravity);
    }

    @Inject(
        method = "tick", at = @At("TAIL")
    )
    private void anvilcraft$ApplyHorizontalGravity(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        // 排除无重力实体和创造飞行玩家
        if (entity.isNoGravity() || (entity instanceof Player player && player.getAbilities().flying)) {
            return;
        }

        // 应用引力向量的水平分量
        Vec3 finalForce = GravityManager.getGravityVector(entity);
        if (finalForce.x != 0 || finalForce.z != 0) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(finalForce.x, 0, finalForce.z));
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void anvilcraft$handleAcceleration(CallbackInfo ci) {
        AccelerateManager.handleAcceleration((Entity) (Object) this);
    }
}
