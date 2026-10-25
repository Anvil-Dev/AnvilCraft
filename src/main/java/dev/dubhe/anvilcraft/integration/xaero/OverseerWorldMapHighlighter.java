package dev.dubhe.anvilcraft.integration.xaero;

import dev.dubhe.anvilcraft.client.overseer.OverseerMapData;
import dev.dubhe.anvilcraft.client.overseer.OverseerMapStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.map.WorldMapSession;
import xaero.map.highlight.ChunkHighlighter;
import xaero.map.world.MapDimension;

import java.util.List;
import javax.annotation.Nullable;

public class OverseerWorldMapHighlighter extends ChunkHighlighter {
    public OverseerWorldMapHighlighter() {
        super(true);
    }

    public static void refresh(ResourceKey<Level> dimension) {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null) return;
        MapDimension mapDimension = session.getMapProcessor().getMapWorld().getDimension(dimension);
        if (mapDimension != null) mapDimension.getHighlightHandler().clearCachedHashes();
    }

    @Override
    public int calculateRegionHash(ResourceKey<Level> dimension, int regionX, int regionZ) {
        return 31 * OverseerMapData.revision(dimension) + (OverseerMapData.isVisible() ? 1 : 0);
    }

    @Override
    public boolean regionHasHighlights(ResourceKey<Level> dimension, int regionX, int regionZ) {
        return !OverseerMapData.region(dimension, regionX, regionZ).isEmpty();
    }

    @Override
    public boolean chunkIsHighlit(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return OverseerMapData.flags(dimension, chunkX, chunkZ) >= 0;
    }

    @Override
    protected @Nullable int[] getColors(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return OverseerMapStyle.colors(dimension, chunkX, chunkZ);
    }

    @Override
    public Component getChunkHighlightSubtleTooltip(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return OverseerMapStyle.tooltip(dimension, chunkX, chunkZ);
    }

    @Override
    public @Nullable Component getChunkHighlightBluntTooltip(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return null;
    }

    @Override
    public void addMinimapBlockHighlightTooltips(List<Component> tooltips, ResourceKey<Level> dimension, int x, int z, int width) {
        if (this.chunkIsHighlit(dimension, x >> 4, z >> 4)) {
            tooltips.addAll(OverseerMapStyle.tooltipLines(dimension, x >> 4, z >> 4, width));
        }
    }
}
