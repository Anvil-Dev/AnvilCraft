package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.api.world.load.ChunkFeatureManager;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mob.class)
public abstract class MobMixin {

    @WrapOperation(
        method = "checkDespawn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/MobCategory;getDespawnDistance()I"
        )
    )
    private int anvilcraft$modifyDespawnDistance(MobCategory category, Operation<Integer> original) {
        // noinspection ConstantConditions
        Mob self = (Mob) (Object) this;
        ChunkPos currentChunk = new ChunkPos(self.blockPosition().getX() >> 4, self.blockPosition().getZ() >> 4);

        if (ChunkFeatureManager.shouldAllowNaturalSpawn(self.level().dimension(), currentChunk)) {
            return ChunkFeatureManager.TRANSCENDIUM_DESPAWN_DISTANCE;
        }
        return original.call(category);
    }
}