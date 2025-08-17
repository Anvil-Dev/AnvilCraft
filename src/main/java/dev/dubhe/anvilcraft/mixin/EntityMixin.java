package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.dubhe.anvilcraft.api.event.FallingBlockCollisionEvent;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import dev.dubhe.anvilcraft.util.DeflectionEntity;
import dev.dubhe.anvilcraft.util.Util;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Optional;

@Mixin(Entity.class)
public abstract class EntityMixin implements DeflectionEntity {
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

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public boolean isDeflected() {
        return anvil$isDeflected;
    }

    @SuppressWarnings("AddedMixinMembersNamePattern")
    @Override
    public Vec3 getFixedDeltaMovement() {
        return anvil$fixedDeltaMovement;
    }

    @WrapOperation(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V", ordinal = 1))
    public void anvilcraft$fixFallingBlockEntity(Entity instance, double x, double y, double z, Operation<Void> original, @Share("isFixed") LocalBooleanRef isFixed) {
        isFixed.set(false);
        Vec3 vec3 = new Vec3(x - getX(), y - getY(), z - getZ());
        if (Util.instanceOfAny(this, Projectile.class, FallingBlockEntity.class, Player.class) && vec3.length() > 0.98) {
            Vec3 s = position();
            Vec3 e = vec3.add(s);
            ArrayList<Pair<BlockPos, Double>> blockPosList = new ArrayList<>();
            for (BlockPos blockPos : DeflectionRingBlockEntity.getAllBlocks(level)) {
                Vec3 q = blockPos.getCenter();
                double a = s.distanceTo(q);
                double b = e.distanceTo(q);
                double c = s.distanceTo(e);
                double d = -(b * b - c * c - a * a) / (2 * c);
                double distance = Math.sqrt(a * a - d * d);
                if (distance <= 0.56747 && d > 0)
                    blockPosList.add(Pair.of(blockPos, d));
            }
            double distance = Double.MAX_VALUE;
            BlockPos blockPos = null;
            for (Pair<BlockPos, Double> pos : blockPosList) {
                if (distance > pos.right()) {
                    distance = pos.right();
                    blockPos = pos.left();
                }
            }
            if (blockPos == null) {
                anvil$isDeflected = false;
                setPos(e);
                return;
            }
            double a = distance / vec3.length();

            if (a > 1) {
                anvil$isDeflected = false;
                setPos(e);
                return;
            }
            setPos(vec3.multiply(a, a, a).add(s));
            isFixed.set(true);
            anvil$fixedDeltaMovement = vec3.multiply(a, a, a);
            anvil$isDeflected = true;
            return;
        }
        anvil$isDeflected = false;
        original.call(instance, x, y, z);
    }

    @WrapOperation(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;equal(DD)Z", ordinal = 0))
    public boolean anvilcraft$cancelCollision1(double x, double y, Operation<Boolean> original, @Share("isFixed") LocalBooleanRef isFixed) {
        return isFixed.get() || original.call(x, y);
    }

    @WrapOperation(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;equal(DD)Z", ordinal = 1))
    public boolean anvilcraft$cancelCollision2(double x, double y, Operation<Boolean> original, @Share("isFixed") LocalBooleanRef isFixed) {
        return isFixed.get() || original.call(x, y);
    }

    @Inject(method = "setPos(DDD)V", at = @At("HEAD"), cancellable = true)
    public void anvilcraft$changeProjectilePosSetResult(double x, double y, double z, CallbackInfo ci) {
        if (!Util.instanceOfAny(this, Projectile.class)) return;
        Vec3 vec3 = new Vec3(x - getX(), y - getY(), z - getZ());
        if (vec3.add(getDeltaMovement().scale(-1)).length() > 0.5) return;
        if (Util.instanceOfAny(this, Projectile.class, FallingBlockEntity.class) && vec3.length() > 0.98) {
            Vec3 s = position();
            Vec3 e = vec3.add(s);
            ArrayList<Pair<BlockPos, Double>> blockPosList = new ArrayList<>();
            for (BlockPos blockPos : DeflectionRingBlockEntity.getAllBlocks(level)) {
                Vec3 q = blockPos.getCenter();
                double a = s.distanceTo(q);
                double b = e.distanceTo(q);
                double c = s.distanceTo(e);
                double d = -(b * b - c * c - a * a) / (2 * c);
                double distance = Math.sqrt(a * a - d * d);
                if (distance <= 0.56747 && d > 0)
                    blockPosList.add(Pair.of(blockPos, d));
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
            setPosRaw(pos.x, pos.y, pos.z);
            setBoundingBox(makeBoundingBox());
            ci.cancel();
        }
    }

    @Inject(method = "move", at = @At("HEAD"))
    public void anvil$recordMovement(
        MoverType type, Vec3 pos, CallbackInfo ci, @Share("beforeBoundingMovement") LocalRef<Vec3> beforeBoundingMovement
    ) {
        beforeBoundingMovement.set(this.getDeltaMovement());
    }

    @Inject(method = "move", at = @At("RETURN"))
    public void anvil$collisionCraft(
        MoverType type, Vec3 pos, CallbackInfo ci, @Share("beforeBoundingMovement") LocalRef<Vec3> beforeBoundingMovement
    ) {
        Optional<FallingBlockEntity> entityOp = Util.castSafely(this, FallingBlockEntity.class);
        if (entityOp.isEmpty() || !this.horizontalCollision) return;
        FallingBlockEntity thiS = entityOp.get();
        BlockPos blockPos = BlockPos.containing(
            this.position.add(beforeBoundingMovement.get().scale(0.55 / beforeBoundingMovement.get().length()).multiply(1, 0, 1))
        );
        NeoForge.EVENT_BUS.post(new FallingBlockCollisionEvent(thiS, blockPos, level, beforeBoundingMovement.get().length()));
    }
}
