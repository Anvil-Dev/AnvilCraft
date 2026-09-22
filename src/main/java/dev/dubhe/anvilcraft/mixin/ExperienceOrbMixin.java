package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.dubhe.anvilcraft.api.injection.entity.IExperienceOrbExtension;
import dev.dubhe.anvilcraft.block.entity.ExpCollectorBlockEntity;
import dev.dubhe.anvilcraft.util.AtmosphereManager;
import dev.dubhe.anvilcraft.util.GravityManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ExperienceOrb.class)
abstract class ExperienceOrbMixin extends Entity implements IExperienceOrbExtension {
    @Unique
    private boolean anvilcraft$discarded;

    protected ExperienceOrbMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void anvilcraft$poach() {
        if (this.level().isClientSide()) return;
        this.anvilcraft$discarded = ExpCollectorBlockEntity.poachExperienceOrb((ExperienceOrb) (Object) this);
    }

    @Override
    public boolean anvilcraft$getDiscarded() {
        return this.anvilcraft$discarded;
    }

    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.98F))
    private float anvilcraft$atmosphereDrag(float drag) {
        return AtmosphereManager.drag(this.level(), drag);
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/state/BlockState;getFriction("
            + "Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)F"))
    private float anvilcraft$directionalFriction(float friction) {
        return GravityManager.hasCustomSurfaceFriction(this) ? 1.0F : friction;
    }
}
