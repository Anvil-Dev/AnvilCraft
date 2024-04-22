package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.item.CrabClawItem;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    void onLivingEntityTick(CallbackInfo ci) {
        CrabClawItem.holdingCrabClawIncreasesRange((LivingEntity) (Object) this);
    }
}
