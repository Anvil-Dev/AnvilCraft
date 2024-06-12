package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.AnvilCraft;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
abstract class FallingBlockEntityMixin extends Entity {
    public FallingBlockEntityMixin(EntityType<? extends FallingBlockEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(
            method = "tick",
            at =
                    @At(
                            value = "INVOKE",
                            shift = At.Shift.AFTER,
                            target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;"
                                    + "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"),
            cancellable = true)
    private void afterMove(CallbackInfo ci) {
        if (AnvilCraft.config.sandDupingFix && this.isRemoved()) ci.cancel();
    }
}
