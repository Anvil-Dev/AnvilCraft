package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;

@EventBusSubscriber
public class PlayerTrackingChunkEventListener {
    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        LevelChunk chunk = event.getChunk();
        for (BlockEntity value : chunk.getBlockEntities().values()) {
            if (value instanceof BaseLaserBlockEntity laserBE) {
                laserBE.syncTo(event.getPlayer());
            }
        }
    }
}
