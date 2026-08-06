package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.world.load.ChunkFeatureManager;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    @Inject(
        method = "playerIsCloseEnoughForSpawning",
        at = @At("HEAD"),
        cancellable = true
    )
    private void anvilcraft$onPlayerIsCloseEnoughForSpawningHead(
        ServerPlayer player, ChunkPos pos,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (player.isSpectator()) return;

        if (ChunkFeatureManager.shouldAllowNaturalSpawn(pos)) {
            double maxDistanceSqr = (double) ChunkFeatureManager.TRANSCENDIUM_DESPAWN_DISTANCE
                                    * ChunkFeatureManager.TRANSCENDIUM_DESPAWN_DISTANCE;

            double xPos = net.minecraft.core.SectionPos.sectionToBlockCoord(pos.x(), 8);
            double zPos = net.minecraft.core.SectionPos.sectionToBlockCoord(pos.z(), 8);
            double xd = xPos - player.getX();
            double zd = zPos - player.getZ();

            cir.setReturnValue(xd * xd + zd * zd < maxDistanceSqr);
        }
    }
}