package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.dubhe.anvilcraft.item.EquipmentAbilities;
import dev.dubhe.anvilcraft.util.AtmosphereManager;
import dev.dubhe.anvilcraft.util.GravityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class AtmosphereLivingMixin {
    @Shadow
    protected abstract int decreaseAirSupply(int currentSupply);

    @Shadow
    protected abstract boolean shouldTakeDrowningDamage();

    @ModifyExpressionValue(method = "baseTick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;increaseAirSupply(I)I"))
    private int anvilcraft$preventVacuumRefill(int airSupply) {
        LivingEntity entity = (LivingEntity) (Object) this;
        return AtmosphereManager.isSuffocating(entity) ? entity.getAirSupply() : airSupply;
    }

    // 26.1 暂未调用 LivingBreatheEvent，水外窒息需补入实体的实际呼吸流程。
    @Inject(method = "baseTick", at = @At("TAIL"))
    private void anvilcraft$vacuumBreathing(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) return;
        if (EquipmentAbilities.canBreathe(entity)) {
            entity.setAirSupply(entity.getMaxAirSupply());
            return;
        }
        if (!AtmosphereManager.isSuffocating(entity)) return;
        if (entity.isEyeInFluid(FluidTags.WATER)
            && !level.getBlockState(BlockPos.containing(entity.getEyePosition())).is(Blocks.BUBBLE_COLUMN)) return;
        entity.setAirSupply(this.decreaseAirSupply(entity.getAirSupply()));
        if (this.shouldTakeDrowningDamage()) {
            entity.setAirSupply(0);
            level.broadcastEntityEvent(entity, (byte) 67);
            entity.hurtServer(level, entity.damageSources().drown(), 2.0F);
        }
    }

    @ModifyExpressionValue(method = "travelInAir", at = @At(value = "CONSTANT", args = "floatValue=0.91"))
    private float anvilcraft$horizontalDrag(float drag) {
        LivingEntity entity = (LivingEntity) (Object) this;
        return GravityManager.hasFloorSupport(entity) ? drag : AtmosphereManager.drag(entity, drag);
    }

    @ModifyExpressionValue(method = "travelInAir", at = @At(value = "CONSTANT", args = "floatValue=0.98"))
    private float anvilcraft$verticalDrag(float drag) {
        return AtmosphereManager.drag((LivingEntity) (Object) this, drag);
    }

    @ModifyConstant(method = "updateFallFlyingMovement", constant = {
        @Constant(doubleValue = 0.9800000190734863), @Constant(doubleValue = 0.9900000095367432)
    })
    private double anvilcraft$glidingDrag(double drag) {
        return AtmosphereManager.drag((LivingEntity) (Object) this, drag);
    }

    @ModifyConstant(method = "updateFallFlyingMovement", constant = @Constant(doubleValue = 0.75))
    private double anvilcraft$glidingLift(double lift) {
        return AtmosphereManager.elytraLift((LivingEntity) (Object) this, lift);
    }

    @ModifyConstant(method = "updateFallFlyingMovement",
        constant = {@Constant(doubleValue = -0.1), @Constant(doubleValue = 0.04)})
    private double anvilcraft$glidingResponse(double response) {
        return AtmosphereManager.elytraResponse((LivingEntity) (Object) this, response);
    }

    @ModifyExpressionValue(method = "travelInAir",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;onGround()Z"))
    private boolean anvilcraft$directionalFriction(boolean onGround) {
        return onGround && !GravityManager.hasCustomSurfaceFriction((LivingEntity) (Object) this);
    }

    @ModifyExpressionValue(method = {"getFrictionInfluencedSpeed", "aiStep"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;onGround()Z"))
    private boolean anvilcraft$floorSupport(boolean onGround) {
        return onGround || GravityManager.hasFloorSupport((LivingEntity) (Object) this);
    }
}
