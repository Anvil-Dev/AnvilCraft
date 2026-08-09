package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.world.load.ChunkFeatureManager;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.util.TriState;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DistanceManager.class)
public abstract class DistanceManagerMixin {

    @Inject(
        method = "hasPlayersNearby",
        at = @At("RETURN"),
        cancellable = true
    )
    private void anvilcraft$onHasPlayersNearby(long pos, CallbackInfoReturnable<TriState> cir) {

        if (cir.getReturnValue() == TriState.FALSE) {
            ChunkPos chunkPos = ChunkPos.unpack(pos);
            if (ChunkFeatureManager.shouldAllowNaturalSpawnAnyDimension(chunkPos)) {
                cir.setReturnValue(TriState.DEFAULT);
            }
        }
    }

    @Inject(
        method = "getSpawnCandidateChunks",
        at = @At("RETURN"),
        cancellable = true
    )
    private void anvilcraft$onGetSpawnCandidateChunks(CallbackInfoReturnable<LongIterator> cir) {

        LongOpenHashSet extended = new LongOpenHashSet();
        LongIterator original = cir.getReturnValue();
        while (original.hasNext()) {
            extended.add(original.nextLong());
        }
        for (ChunkPos cp : ChunkFeatureManager.getAllNaturalSpawnChunks()) {
            extended.add(cp.pack());
        }
        cir.setReturnValue(extended.iterator());
    }
}