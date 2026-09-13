package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 为放大图标保留真实像素密度；同一分辨率仍复用原生物品图集。 */
public final class ScaledGuiItemAtlases implements AutoCloseable {
    public static final Object OWNED_ITEM = new Object();
    private static final int IDLE_ATLASES = 2;
    private final Map<Integer, Set<Object>> requested = new HashMap<>();
    private final Map<Integer, GuiItemAtlas> atlases = new LinkedHashMap<>(8, 0.75F, true);
    private final SubmitNodeCollector collector;
    private final FeatureRenderDispatcher dispatcher;
    private final MultiBufferSource.BufferSource buffers;
    private int guiScale;
    private long allocations;

    public ScaledGuiItemAtlases(SubmitNodeCollector collector, FeatureRenderDispatcher dispatcher, MultiBufferSource.BufferSource buffers) {
        this.collector = collector;
        this.dispatcher = dispatcher;
        this.buffers = buffers;
    }

    public static int resolution(Matrix3x2fc pose, int guiScale, int maximum) {
        double a = pose.m00();
        double b = pose.m01();
        double c = pose.m10();
        double d = pose.m11();
        double sum = a * a + b * b + c * c + d * d;
        double determinant = a * d - b * c;
        double stretch = Math.sqrt((sum + Math.sqrt(Math.max(0, sum * sum - 4 * determinant * determinant))) / 2);
        if (!Double.isFinite(stretch) || stretch <= 1.000001) return 0;
        return Math.min(maximum, (int) Math.ceil(16.0 * guiScale * stretch - 0.00001));
    }

    private int resolution(GuiItemRenderState item) {
        if (item.oversizedItemBounds() != null) return 0;
        Object identity = item.itemStackRenderState().getModelIdentity();
        if (!(identity instanceof List<?> elements) || !elements.contains(OWNED_ITEM)) return 0;
        return resolution(item.pose(), this.guiScale, RenderSystem.getDevice().getMaxTextureSize());
    }

    public void prepare(GuiRenderState state, int guiScale) {
        this.guiScale = guiScale;
        this.requested.clear();
        state.forEachItem(item -> {
            int resolution = this.resolution(item);
            if (resolution > 0) {
                this.requested.computeIfAbsent(resolution, key -> new HashSet<>()).add(item.itemStackRenderState().getModelIdentity());
            }
        });
        int idle = (int) this.atlases.keySet().stream().filter(size -> !this.requested.containsKey(size)).count();
        var iterator = this.atlases.entrySet().iterator();
        while (iterator.hasNext() && idle > IDLE_ATLASES) {
            var entry = iterator.next();
            if (!this.requested.containsKey(entry.getKey())) {
                entry.getValue().close();
                iterator.remove();
                idle--;
            }
        }
    }

    public GuiItemAtlas.@Nullable SlotView get(GuiItemRenderState item) {
        int resolution = this.resolution(item);
        if (resolution <= 0) return null;
        Set<Object> identities = this.requested.get(resolution);
        if (identities == null) return null;
        GuiItemAtlas atlas = this.atlases.get(resolution);
        if (atlas == null || !atlas.tryPrepareFor(identities)) {
            int textureSize = GuiItemAtlas.computeTextureSizeFor(resolution, identities.size());
            if (atlas != null && atlas.textureSize() == textureSize) return null;
            if (atlas != null) atlas.close();
            atlas = new GuiItemAtlas(this.collector, this.dispatcher, this.buffers, textureSize, resolution);
            this.atlases.put(resolution, atlas);
            this.allocations++;
        }
        return atlas.getOrUpdate(item.itemStackRenderState());
    }

    public void endFrame() {
        this.atlases.values().forEach(GuiItemAtlas::endFrame);
    }

    public long allocations() {
        return this.allocations;
    }

    public int cachedAtlases() {
        return this.atlases.size();
    }

    public Set<Integer> requestedResolutions() {
        return Set.copyOf(this.requested.keySet());
    }

    @Override
    public void close() {
        this.atlases.values().forEach(GuiItemAtlas::close);
        this.atlases.clear();
    }
}
