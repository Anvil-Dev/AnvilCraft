package dev.dubhe.anvilcraft.mixin.projectile;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import dev.dubhe.anvilcraft.util.AirResistanceManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * 修正箭矢自带的方块射线和实体查询。
 *
 * <p>箭矢没有完全复用通用投掷物碰撞工具，因此需要同时裁剪
 * {@link AbstractArrow#tick} 中的方块射线、实体射线和实体查询包围盒。</p>
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin extends Projectile {
    protected AbstractArrowMixin(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    @ModifyArg(
        method = "tick", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"
    ), index = 0
    )
    private ClipContext anvilcraft$clipBlockTraceAtDeflectionRing(ClipContext context) {
        Vec3 end = anvilcraft$clipEndAtDeflectionRing(context.getFrom(), context.getTo());
        return end == context.getTo()
               ? context
               : new ClipContext(context.getFrom(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this);
    }

    @ModifyArg(
        method = "tick", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;"
                 + "findHitEntity(Lnet/minecraft/world/phys/Vec3;"
                 + "Lnet/minecraft/world/phys/Vec3;)"
                 + "Lnet/minecraft/world/phys/EntityHitResult;"
    ), index = 1
    )
    private Vec3 anvilcraft$clipEntityTraceAtDeflectionRing(Vec3 end) {
        return anvilcraft$clipEndAtDeflectionRing(position(), end);
    }

    @WrapOperation(
        method = "findHitEntity", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/phys/AABB;expandTowards(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;"
    )
    )
    private AABB anvilcraft$clipEntityQueryAtDeflectionRing(AABB box, Vec3 movement, Operation<AABB> original) {
        Vec3 end = anvilcraft$clipEndAtDeflectionRing(position(), position().add(movement));
        return original.call(box, end.subtract(position()));
    }

    @Unique
    private Vec3 anvilcraft$clipEndAtDeflectionRing(Vec3 start, Vec3 end) {
        BlockPos ring = DeflectionRingBlockEntity.findFirstRing(this, start, end.subtract(start));
        return ring == null ? end : ring.getCenter();
    }

    /** Air resistance only; the water inertia branch overwrites this value afterwards. */
    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.99f))
    private float anvilcraft$scaleAirDrag(float vanillaDrag) {
        return AirResistanceManager.drag(this.level(), vanillaDrag);
    }
}
