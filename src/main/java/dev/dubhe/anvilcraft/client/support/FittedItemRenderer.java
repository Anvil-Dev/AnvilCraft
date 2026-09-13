package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModRenderPipelines;
import dev.dubhe.anvilcraft.mixin.client.ItemStackRenderStateAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 使用独立队列捕获 GUI 姿态，将裁切后的透明图标贴到物品表面。 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class FittedItemRenderer {
    private static final int RESOLUTION = 512;
    private static final int PADDING = 2;
    private static final int MAX_ICONS = 64;
    private static final Map<Key, Icon> ICONS = new LinkedHashMap<>(16, 0.75F, true);
    private static final Map<Key, Request> REQUESTS = new LinkedHashMap<>();
    private static final Set<Key> IN_FLIGHT = new HashSet<>();
    private static @Nullable Capture capture;
    private static int captureDepth;
    private static int generation;
    private static long nextTexture;

    private FittedItemRenderer() {
    }

    public static boolean isRenderingPreview() {
        return captureDepth > 0;
    }

    public static float frontZ(ItemStack stack) {
        var state = new TrackingItemStackRenderState();
        captureDepth++;
        try {
            var client = Minecraft.getInstance();
            client.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.NONE, client.level, null, 0);
        } finally {
            captureDepth--;
        }
        float maximum = Float.NEGATIVE_INFINITY;
        for (var layer : ((ItemStackRenderStateAccessor) state).anvilcraft$getLayers()) {
            for (var quad : layer.prepareQuadList()) {
                for (int vertex = 0; vertex < 4; vertex++) maximum = Math.max(maximum, quad.position(vertex).z());
            }
        }
        return Float.isFinite(maximum) ? maximum : 8.5F / 16;
    }

    public static @Nullable Icon prepare(ItemStack stack) {
        if (stack.isEmpty() || isRenderingPreview()) return null;
        final var key = new Key(stack.copyWithCount(1));
        var state = new TrackingItemStackRenderState();
        var measured = stack.copyWithCount(1);
        measured.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        captureDepth++;
        try {
            var client = Minecraft.getInstance();
            client.getItemModelResolver().updateForTopItem(state, measured, ItemDisplayContext.GUI, client.level, null, 0);
        } finally {
            captureDepth--;
        }
        Icon icon = ICONS.get(key);
        long now = Util.getMillis();
        if (icon == null || !icon.modelIdentity.equals(state.getModelIdentity()) || icon.animated && now - icon.createdAt >= 250) {
            if (!IN_FLIGHT.contains(key)) REQUESTS.put(key, new Request(state, now, animated(state)));
            while (REQUESTS.size() > MAX_ICONS) REQUESTS.remove(REQUESTS.keySet().iterator().next());
        }
        return icon;
    }

    private static boolean animated(TrackingItemStackRenderState state) {
        if (state.isAnimated() || ((List<?>) state.getModelIdentity()).stream().anyMatch(SpecialModelWrapper.class::isInstance)) {
            return true;
        }
        for (var layer : ((ItemStackRenderStateAccessor) state).anvilcraft$getLayers()) {
            for (var quad : layer.prepareQuadList()) {
                if (quad.materialInfo().sprite().contents().getUniqueFrames().size() > 1) return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Pre event) {
        if (REQUESTS.isEmpty() || Minecraft.getInstance().level == null) return;
        if (capture == null) capture = new Capture();
        int remaining = 4 - IN_FLIGHT.size();
        var iterator = REQUESTS.entrySet().iterator();
        while (remaining-- > 0 && iterator.hasNext()) {
            var entry = iterator.next();
            iterator.remove();
            IN_FLIGHT.add(entry.getKey());
            capture.render(entry.getKey(), entry.getValue(), generation);
        }
        capture.dispatcher.endFrame();
    }

    @SubscribeEvent
    public static void reload(ModelEvent.BakingCompleted event) {
        clear();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    public static void clear() {
        generation++;
        REQUESTS.clear();
        IN_FLIGHT.clear();
        ICONS.values().forEach(icon -> Minecraft.getInstance().getTextureManager().release(icon.texture));
        ICONS.clear();
        if (capture != null) capture.close();
        capture = null;
    }

    public static void submit(Icon icon, float size, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean foil) {
        collector.submitCustomGeometry(pose, icon.renderType, (matrix, vertices) -> quad(icon, size, matrix, vertices, light, overlay));
        if (foil) {
            collector.submitCustomGeometry(pose, RenderTypes.entityGlint(),
                (matrix, vertices) -> quad(icon, size, matrix, vertices, light, overlay));
        }
    }

    private static void quad(Icon icon, float size, PoseStack.Pose pose, VertexConsumer vertices, int light, int overlay) {
        float scale = size / Math.max(icon.width, icon.height);
        float x = icon.width * scale / 2;
        float y = icon.height * scale / 2;
        float u = (float) PADDING / (icon.width + PADDING * 2);
        float v = (float) PADDING / (icon.height + PADDING * 2);
        vertex(pose, vertices, -x, -y, u, 1 - v, light, overlay);
        vertex(pose, vertices, x, -y, 1 - u, 1 - v, light, overlay);
        vertex(pose, vertices, x, y, 1 - u, v, light, overlay);
        vertex(pose, vertices, -x, y, u, v, light, overlay);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vertices, float x, float y, float u, float v, int light, int overlay) {
        vertices.addVertex(pose, x, y, 0).setColor(-1).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, 1);
    }

    private static void complete(Key key, Request request, int epoch, NativeImage image) {
        if (epoch != generation) return;
        IN_FLIGHT.remove(key);
        int minX = RESOLUTION;
        int minY = RESOLUTION;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < RESOLUTION; y++) {
            for (int x = 0; x < RESOLUTION; x++) {
                if ((image.getPixel(x, y) >>> 24) == 0) continue;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < minX) {
            minX = 0;
            minY = 0;
            maxX = 0;
            maxY = 0;
        }
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        var cropped = new NativeImage(width + PADDING * 2, height + PADDING * 2, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) cropped.setPixel(x + PADDING, y + PADDING, image.getPixel(minX + x, minY + y));
        }
        var texture = AnvilCraft.of("dynamic/filter_icon/" + nextTexture++);
        Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(texture::toString, cropped));
        var renderType = RenderType.create("anvilcraft_fitted_icon", RenderSetup.builder(ModRenderPipelines.FITTED_ITEM)
            .useLightmap().useOverlay().withTexture("Sampler0", texture).createRenderSetup());
        Icon old = ICONS.put(key, new Icon(texture, renderType, width, height, request.state.getModelIdentity(),
            request.animated, request.createdAt));
        if (old != null) Minecraft.getInstance().getTextureManager().release(old.texture);
        while (ICONS.size() > MAX_ICONS) {
            var iterator = ICONS.values().iterator();
            var removed = iterator.next();
            iterator.remove();
            Minecraft.getInstance().getTextureManager().release(removed.texture);
        }
    }

    public record Icon(
        Identifier texture, RenderType renderType, int width, int height, Object modelIdentity, boolean animated, long createdAt
    ) {
    }

    private record Key(ItemStack stack) {
        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(this.stack);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof Key key && ItemStack.isSameItemSameComponents(this.stack, key.stack);
        }
    }

    private record Request(TrackingItemStackRenderState state, long createdAt, boolean animated) {
    }

    private static final class Capture implements AutoCloseable {
        private final RenderTarget target = new TextureTarget("AnvilCraft fitted item", RESOLUTION, RESOLUTION, true);
        private final ByteBufferBuilder vertexBuffer = new ByteBufferBuilder(1048576);
        private final MultiBufferSource.BufferSource buffers = MultiBufferSource.immediate(this.vertexBuffer);
        private final SubmitNodeStorage nodes = new SubmitNodeStorage();
        private final Projection projection = new Projection();
        private final ProjectionMatrixBuffer projectionBuffer = new ProjectionMatrixBuffer("AnvilCraft fitted item");
        private final FeatureRenderDispatcher dispatcher;

        private Capture() {
            var client = Minecraft.getInstance();
            this.dispatcher = new FeatureRenderDispatcher(this.nodes, client.getModelManager(), this.buffers, client.getAtlasManager(),
                client.renderBuffers().outlineBufferSource(), this.buffers, client.font, client.gameRenderer.getGameRenderState());
            this.projection.setupOrtho(-1000, 1000, 2, 2, true);
        }

        private void render(Key key, Request request, int epoch) {
            final var color = RenderSystem.outputColorTextureOverride;
            final var depth = RenderSystem.outputDepthTextureOverride;
            final var lights = RenderSystem.getShaderLights();
            var scissor = RenderSystem.getScissorStateForRenderTypeDraws();
            final boolean clipped = scissor.enabled();
            final int sx = scissor.x();
            final int sy = scissor.y();
            final int sw = scissor.width();
            final int sh = scissor.height();
            RenderSystem.backupProjectionMatrix();
            RenderSystem.getModelViewStack().pushMatrix().identity();
            captureDepth++;
            try {
                RenderSystem.getDevice().createCommandEncoder()
                    .clearColorAndDepthTextures(this.target.getColorTexture(), 0, this.target.getDepthTexture(), 1);
                RenderSystem.outputColorTextureOverride = this.target.getColorTextureView();
                RenderSystem.outputDepthTextureOverride = this.target.getDepthTextureView();
                RenderSystem.disableScissorForRenderTypeDraws();
                RenderSystem.setProjectionMatrix(this.projectionBuffer.getBuffer(this.projection), ProjectionType.ORTHOGRAPHIC);
                Minecraft.getInstance().gameRenderer.getLighting()
                    .setupFor(request.state.usesBlockLight() ? Lighting.Entry.ITEMS_3D : Lighting.Entry.ITEMS_FLAT);
                var bounds = request.state.getModelBoundingBox();
                float extent = (float) Math.max(bounds.getXsize(), bounds.getYsize());
                float scale = Float.isFinite(extent) && extent > 0.0001F ? 1.8F / extent : 1.8F;
                var pose = new PoseStack();
                pose.translate(1, 1, 0);
                pose.scale(scale, -scale, scale);
                pose.translate(-bounds.getCenter().x, -bounds.getCenter().y, -bounds.getCenter().z);
                request.state.submit(pose, this.nodes, 15728880, OverlayTexture.NO_OVERLAY, 0);
                this.dispatcher.renderAllFeatures();
                this.buffers.endBatch();
                this.readback(key, request, epoch);
            } finally {
                captureDepth--;
                RenderSystem.outputColorTextureOverride = color;
                RenderSystem.outputDepthTextureOverride = depth;
                RenderSystem.restoreProjectionMatrix();
                RenderSystem.getModelViewStack().popMatrix();
                if (lights != null) RenderSystem.setShaderLights(lights);
                if (clipped) RenderSystem.enableScissorForRenderTypeDraws(sx, sy, sw, sh);
                else RenderSystem.disableScissorForRenderTypeDraws();
            }
        }

        private void readback(Key key, Request request, int epoch) {
            var device = RenderSystem.getDevice();
            GpuBuffer staging = device.createBuffer(() -> "AnvilCraft fitted item readback", 9, (long) RESOLUTION * RESOLUTION * 4);
            var encoder = device.createCommandEncoder();
            encoder.copyTextureToBuffer(this.target.getColorTexture(), staging, 0, () -> {
                try (
                    staging;
                    var mapped = encoder.mapBuffer(staging, true, false);
                    var image = new NativeImage(RESOLUTION, RESOLUTION, false)
                ) {
                    for (int y = 0; y < RESOLUTION; y++) {
                        for (int x = 0; x < RESOLUTION; x++) {
                            image.setPixelABGR(x, RESOLUTION - y - 1, mapped.data().getInt((x + y * RESOLUTION) * 4));
                        }
                    }
                    complete(key, request, epoch, image);
                }
            }, 0);
        }

        @Override
        public void close() {
            this.dispatcher.close();
            this.target.destroyBuffers();
            this.projectionBuffer.close();
            this.vertexBuffer.close();
        }
    }
}
