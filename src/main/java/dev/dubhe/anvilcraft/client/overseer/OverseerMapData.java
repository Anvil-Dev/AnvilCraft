package dev.dubhe.anvilcraft.client.overseer;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.xaero.OverseerMinimapHighlighter;
import dev.dubhe.anvilcraft.integration.xaero.OverseerWorldMapHighlighter;
import dev.dubhe.anvilcraft.network.OverseerSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashMap;
import java.util.Map;

public class OverseerMapData {
    private static final Map<ResourceKey<Level>, Map<BlockPos, Map<Long, Integer>>> SOURCES = new HashMap<>();
    // Xaero reads highlights on its map worker as well as the client thread.
    private static volatile Map<ResourceKey<Level>, DimensionData> dimensions = Map.of();

    private static boolean lastVisible = true;

    public static boolean isVisible() {
        return AnvilCraft.CLIENT_CONFIG.ui.showOverseerMapHighlights;
    }

    public static void setVisible(boolean visible) {
        AnvilCraft.CLIENT_CONFIG.ui.showOverseerMapHighlights = visible;
        refreshVisibility();
        for (ModConfig config : ModConfigs.getModConfigs(AnvilCraft.MOD_ID)) {
            if (config.getType() != ModConfig.Type.CLIENT || !(config.getSpec() instanceof ModConfigSpec spec)) continue;
            ModConfigSpec.BooleanValue value = spec.getValues().get("ui.show_overseer_map_highlights");
            if (value == null || config.getLoadedConfig() == null) continue;
            value.set(visible);
            spec.save();
        }
    }

    public static void refreshVisibility() {
        boolean visible = isVisible();
        if (lastVisible == visible) return;
        lastVisible = visible;
        if (ModList.get().isLoaded("xaeroworldmap")) dimensions.keySet().forEach(OverseerWorldMapHighlighter::refresh);
        if (ModList.get().isLoaded("xaerominimap")) OverseerMinimapHighlighter.refresh();
    }

    public static void update(OverseerSyncPacket packet) {
        Map<BlockPos, Map<Long, Integer>> sources = SOURCES.computeIfAbsent(packet.dimension(), key -> new HashMap<>());
        if (packet.chunks().isEmpty()) sources.remove(packet.source());
        else sources.put(packet.source(), packet.chunks());

        Map<Long, Map<Long, Integer>> dimensionRegions = new HashMap<>();
        for (Map<Long, Integer> chunks : sources.values()) {
            chunks.forEach((chunk, flags) -> {
                long region = ChunkPos.pack(ChunkPos.getX(chunk) >> 5, ChunkPos.getZ(chunk) >> 5);
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
        if (!isVisible()) return Map.of();
        return dimensions.getOrDefault(dimension, DimensionData.EMPTY).regions().getOrDefault(ChunkPos.pack(regionX, regionZ), Map.of());
    }

    public static int flags(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return region(dimension, chunkX >> 5, chunkZ >> 5).getOrDefault(ChunkPos.pack(chunkX, chunkZ), -1);
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
