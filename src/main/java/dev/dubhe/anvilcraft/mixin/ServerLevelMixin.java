package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.worldgen.OverworldLikeGenerationBootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Unique
    ItemEntity anvilcraft$addedItem;
    @Unique
    ExperienceOrb anvilcraft$addedExperienceOrb;
    @Unique
    boolean anvilcraft$shouldCheckDiscarded;

    /** Give the built-in overworld-like dimension a deterministic seed of its own. */
    @Inject(method = "getSeed", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$useOverworldLikeSeed(CallbackInfoReturnable<Long> cir) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (!CelestialTravelManager.isOverworldLike(level.dimension())) return;
        cir.setReturnValue(OverworldLikeGenerationBootstrap.getActiveSeed(level.getServer()));
    }

    /** Keep structure-presence checks on the same seed as terrain generation. */
    @ModifyArg(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/StructureCheck;<init>("
                + "Lnet/minecraft/world/level/chunk/storage/ChunkScanAccess;"
                + "Lnet/minecraft/core/RegistryAccess;"
                + "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;"
                + "Lnet/minecraft/resources/ResourceKey;"
                + "Lnet/minecraft/world/level/chunk/ChunkGenerator;"
                + "Lnet/minecraft/world/level/levelgen/RandomState;"
                + "Lnet/minecraft/world/level/LevelHeightAccessor;"
                + "Lnet/minecraft/world/level/biome/BiomeSource;"
                + "JLcom/mojang/datafixers/DataFixer;)V"
        ),
        index = 8
    )
    private long anvilcraft$useOverworldLikeStructureSeed(long seed) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (!CelestialTravelManager.isOverworldLike(level.dimension())) return seed;
        return OverworldLikeGenerationBootstrap.getActiveSeed(level.getServer());
    }

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
            this.anvilcraft$addedItem = e1;
            this.anvilcraft$shouldCheckDiscarded = true;
        } else if (entity instanceof ExperienceOrb e1) {
            this.anvilcraft$addedExperienceOrb = e1;
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
        if (this.anvilcraft$shouldCheckDiscarded) {
            this.anvilcraft$shouldCheckDiscarded = false;
            if (
                this.anvilcraft$addedItem != null && this.anvilcraft$addedItem.anvilcraft$getDiscarded()
                || this.anvilcraft$addedExperienceOrb != null && this.anvilcraft$addedExperienceOrb.anvilcraft$getDiscarded()
            ) {
                return;
            }
        }

        original.call(instance, string, o);
    }
}
