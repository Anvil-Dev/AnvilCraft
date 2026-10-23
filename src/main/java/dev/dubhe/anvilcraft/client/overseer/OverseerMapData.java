package dev.dubhe.anvilcraft.client.overseer;

import dev.dubhe.anvilcraft.integration.xaero.OverseerMinimapHighlighter;
import dev.dubhe.anvilcraft.integration.xaero.OverseerWorldMapHighlighter;
import dev.dubhe.anvilcraft.network.OverseerSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.HashMap;
import java.util.Map;

public class OverseerMapData {
    private static final Map<ResourceKey<Level>, Map<BlockPos, Map<Long, Integer>>> SOURCES = new HashMap<>();
    // Xaero reads highlights on its map worker as well as the client thread.
    private static volatile Map<ResourceKey<Level>, DimensionData> dimensions = Map.of();

    public static void update(OverseerSyncPacket packet) {
        Map<BlockPos, Map<Long, Integer>> sources = SOURCES.computeIfAbsent(packet.dimension(), key -> new HashMap<>());
        if (packet.chunks().isEmpty()) sources.remove(packet.source());
        else sources.put(packet.source(), packet.chunks());

        Map<Long, Map<Long, Integer>> dimensionRegions = new HashMap<>();
        for (Map<Long, Integer> chunks : sources.values()) {
            chunks.forEach((chunk, flags) -> {
                long region = ChunkPos.asLong(ChunkPos.getX(chunk) >> 5, ChunkPos.getZ(chunk) >> 5);
                dimensionRegions.computeIfAbsent(region, key -> new HashMap<>()).merge(chunk, flags, (a, b) -> a | b);
            });
        }
        dimensionRegions.replaceAll((region, chunks) -> Map.copyOf(chunks));
        Map<ResourceKey<Level>, DimensionData> next = new HashMap<>(dimensions);
        next.put(packet.dimension(), new DimensionData(Map.copyOf(dimensionRegions), revision(packet.dimension()) + 1));
        dimensions = Map.copyOf(next);
        if (ModList.get().isLoaded("xaeroworldmap")) OverseerWorldMapHighlighter.refresh(packet.dimension());
        if (ModList.get().isLoaded("xaerominimap")) OverseerMinimapHighlighter.refresh();
    }

    public static Map<Long, Integer> region(ResourceKey<Level> dimension, int regionX, int regionZ) {
        return dimensions.getOrDefault(dimension, DimensionData.EMPTY).regions().getOrDefault(ChunkPos.asLong(regionX, regionZ), Map.of());
    }

    public static int flags(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return region(dimension, chunkX >> 5, chunkZ >> 5).getOrDefault(ChunkPos.asLong(chunkX, chunkZ), -1);
    }

    public static int revision(ResourceKey<Level> dimension) {
        return dimensions.getOrDefault(dimension, DimensionData.EMPTY).revision();
    }

    private record DimensionData(Map<Long, Map<Long, Integer>> regions, int revision) {
        private static final DimensionData EMPTY = new DimensionData(Map.of(), 0);
    }

    public static void clear() {
        SOURCES.clear();
        dimensions = Map.of();
    }
}
