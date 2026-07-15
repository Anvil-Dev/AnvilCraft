package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Unique
    private @Nullable Entity anvilcraft$addedEntity;
    @Unique
    private boolean anvilcraft$shouldCheckDiscarded;

    @Inject(
        method = "addFreshEntity",
        at = @At(value = "HEAD")
    )
    public void poachItemEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ItemEntity e1) {
            e1.anvilcraft$poach();
        } else if (entity instanceof ExperienceOrb orb) {
            orb.anvilcraft$poach();
        }
    }

    @Inject(
        method = "addEntity",
        at = @At(
            value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"
        )
    )
    public void recordAddedItemEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
            this.anvilcraft$addedEntity = entity;
            this.anvilcraft$shouldCheckDiscarded = true;
        }
    }

    @WrapOperation(
        method = "addEntity",
        at = @At(
            value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"
        )
    )
    public void cancelItemDiscardedWarn(Logger instance, String string, Object o, Operation<Void> original) {
        boolean discarded = false;
        if (this.anvilcraft$addedEntity instanceof ItemEntity item) {
            discarded = item.anvilcraft$getDiscarded();
        } else if (this.anvilcraft$addedEntity instanceof ExperienceOrb orb) {
            discarded = orb.anvilcraft$getDiscarded();
        }
        if (this.anvilcraft$shouldCheckDiscarded && discarded) {
            this.anvilcraft$shouldCheckDiscarded = false;
            this.anvilcraft$addedEntity = null;
            return;
        }
        this.anvilcraft$shouldCheckDiscarded = false;
        this.anvilcraft$addedEntity = null;
        original.call(instance, string, o);
    }
}
