package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.world.load.ChunkFeatureManager;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobCategory.class)
public class MobCategoryMixin {

    @Inject(
        method = "getDespawnDistance",
        at = @At("HEAD"),
        cancellable = true
    )
    private void anvilcraft$onGetDespawnDistance(CallbackInfoReturnable<Integer> cir) {
        ChunkPos currentChunk = ChunkFeatureManager.CURRENT_SPAWNING_CHUNK.get();

        if (ChunkFeatureManager.shouldAllowNaturalSpawn(currentChunk)) {
            cir.setReturnValue(ChunkFeatureManager.TRANSCENDIUM_DESPAWN_DISTANCE);
        }
    }
}