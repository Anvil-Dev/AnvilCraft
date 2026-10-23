package dev.dubhe.anvilcraft.integration.xaero;

import dev.dubhe.anvilcraft.client.overseer.OverseerMapData;
import dev.dubhe.anvilcraft.client.overseer.OverseerMapStyle;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import xaero.common.minimap.highlight.ChunkHighlighter;
import xaero.common.minimap.highlight.DimensionHighlighterHandler;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.info.render.compile.InfoDisplayCompiler;
import xaero.hud.minimap.module.MinimapSession;

public class OverseerMinimapHighlighter extends ChunkHighlighter {
    public OverseerMinimapHighlighter() {
        super(true);
    }

    public static void refresh() {
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null) return;
        DimensionHighlighterHandler handler = session.getProcessor().getMinimapWriter().getDimensionHighlightHandler();
        if (handler != null) handler.requestRefresh();
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
    protected int @Nullable [] getColors(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return OverseerMapStyle.colors(dimension, chunkX, chunkZ);
    }

    @Override
    public void addChunkHighlightTooltips(InfoDisplayCompiler compiler, ResourceKey<Level> dimension, int x, int z, int width) {
        OverseerMapStyle.tooltipLines(dimension, x, z, width).forEach(compiler::addLine);
    }
}
