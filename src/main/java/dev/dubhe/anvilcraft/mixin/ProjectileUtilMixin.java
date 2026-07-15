package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import dev.dubhe.anvilcraft.util.AccelerateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * ???? {@link ProjectileUtil} ???????????
 *
 * <p>??????? tick ??????????????????????????
 * ?????????????????????????????</p>
 */
@Mixin(ProjectileUtil.class)
public abstract class ProjectileUtilMixin {
    @WrapOperation(
        method = "getHitResult", at = @At(
        value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"
    )
    )
    private static Vec3 anvilcraft$clipAtDeflectionRing(
        Vec3 start,
        Vec3 movement,
        Operation<Vec3> original,
        @Local(argsOnly = true) Entity entity
    ) {
        Vec3 safeMovement = AccelerateManager.clampMovement(entity, movement);
        BlockPos ring = DeflectionRingBlockEntity.findFirstRing(entity, start, safeMovement);
        return ring == null ? original.call(start, safeMovement) : ring.getCenter();
    }

    @WrapOperation(
        method = "getHitResult", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/phys/AABB;expandTowards(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;"
    )
    )
    private static AABB anvilcraft$clipEntityQueryAtDeflectionRing(
        AABB box,
        Vec3 movement,
        Operation<AABB> original,
        @Local(argsOnly = true, ordinal = 0) Vec3 start,
        @Local(argsOnly = true) Entity entity
    ) {
        Vec3 safeMovement = AccelerateManager.clampMovement(entity, movement);
        BlockPos ring = DeflectionRingBlockEntity.findFirstRing(entity, start, safeMovement);
        Vec3 clippedMovement = ring == null ? safeMovement : ring.getCenter().subtract(start);
        return original.call(box, clippedMovement);
    }
}
