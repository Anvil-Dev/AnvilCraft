package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Unique
    ItemEntity anvilcraft$addedEntity;
    @Unique
    boolean anvilcraft$shouldCheckDiscarded;

    @Inject(
        method = "addFreshEntity",
        at = @At(value = "HEAD")
    )
    public void poachItemEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ItemEntity e1) {
            e1.anvilcraft$poach();
        }
        if (entity instanceof ExperienceOrb experienceOrb) {
            experienceOrb.anvilcraft$poach();
        }
    }

    @Inject(
        method = "addEntity",
        at = @At(
            value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"
        )
    )
    public void recordAddedItemEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ItemEntity e1) {
            anvilcraft$addedEntity = e1;
            anvilcraft$shouldCheckDiscarded = true;
        }
    }

    @WrapOperation(
        method = "addEntity",
        at = @At(
            value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"
        )
    )
    public void cancelItemDiscardedWarn(Logger instance, String string, Object o, Operation<Void> original) {
        if (anvilcraft$shouldCheckDiscarded && anvilcraft$addedEntity.anvilcraft$getDiscarded()) {
            anvilcraft$shouldCheckDiscarded = false;
            return;
        }

        original.call(instance, string, o);
    }
}
