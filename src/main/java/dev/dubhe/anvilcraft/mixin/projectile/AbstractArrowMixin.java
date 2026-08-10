package dev.dubhe.anvilcraft.mixin.projectile;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * ?????????????????
 *
 * <p>??????????????????????????
 * {@link AbstractArrow#tick} ????????????????????</p>
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin extends Projectile {
    protected AbstractArrowMixin(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    @ModifyArg(
        method = "tick", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;"
                 + "clipIncludingBorder(Lnet/minecraft/world/level/ClipContext;)"
                 + "Lnet/minecraft/world/phys/BlockHitResult;"
    ), index = 0
    )
    private ClipContext anvilcraft$clipBlockTraceAtDeflectionRing(ClipContext context) {
        Vec3 end = this.anvilcraft$clipEndAtDeflectionRing(context.getFrom(), context.getTo());
        return end == context.getTo()
               ? context
               : new ClipContext(context.getFrom(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this);
    }

    @ModifyArg(
        method = "stepMoveAndHit", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;"
                 + "findHitEntities(Lnet/minecraft/world/phys/Vec3;"
                 + "Lnet/minecraft/world/phys/Vec3;)"
                 + "Ljava/util/Collection;"
    ), index = 1
    )
    private Vec3 anvilcraft$clipEntityTraceAtDeflectionRing(Vec3 end) {
        return this.anvilcraft$clipEndAtDeflectionRing(this.position(), end);
    }

    @WrapOperation(
        method = {"findHitEntity", "findHitEntities"}, at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/phys/AABB;expandTowards(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;"
    )
    )
    private AABB anvilcraft$clipEntityQueryAtDeflectionRing(AABB box, Vec3 movement, Operation<AABB> original) {
        Vec3 end = this.anvilcraft$clipEndAtDeflectionRing(this.position(), this.position().add(movement));
        return original.call(box, end.subtract(this.position()));
    }

    @Unique
    private Vec3 anvilcraft$clipEndAtDeflectionRing(Vec3 start, Vec3 end) {
        BlockPos ring = DeflectionRingBlockEntity.findFirstRing(this, start, end.subtract(start));
        return ring == null ? end : ring.getCenter();
    }
}
