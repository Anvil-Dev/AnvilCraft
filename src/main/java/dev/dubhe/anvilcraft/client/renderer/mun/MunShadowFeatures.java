package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.LeashFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.LinkedHashMap;

/** Replays native submissions into capture consumers without drawing world render passes. */
final class MunShadowFeatures implements AutoCloseable {
    final SubmitNodeStorage submits = new SubmitNodeStorage();
    private final CaptureBuffers buffers;
    private final CaptureBuffers discardBuffers;
    private final OutlineBufferSource outlines;
    private final ModelFeatureRenderer models = new ModelFeatureRenderer();
    private final ModelPartFeatureRenderer parts = new ModelPartFeatureRenderer();
    private final ItemFeatureRenderer items = new ItemFeatureRenderer();
    private final BlockFeatureRenderer blocks = new BlockFeatureRenderer();
    private final CustomFeatureRenderer custom = new CustomFeatureRenderer();
    private final LeashFeatureRenderer leashes = new LeashFeatureRenderer();

    MunShadowFeatures(MultiBufferSource destination, VertexConsumer discard) {
        this.buffers = new CaptureBuffers(destination);
        this.discardBuffers = new CaptureBuffers(type -> discard);
        this.outlines = new OutlineBufferSource() {
            @Override
            public VertexConsumer getBuffer(RenderType type) {
                return discard;
            }
        };
    }

    void render() {
        var client = Minecraft.getInstance();
        var models = client.getModelManager().getBlockStateModelSet();
        var options = client.gameRenderer.getGameRenderState().optionsRenderState;
        try {
            for (var collection : this.submits.getSubmitsPerOrder().values()) {
                this.models.renderSolid(collection, this.buffers, this.outlines, this.discardBuffers);
                this.parts.renderSolid(collection, this.buffers, this.outlines, this.discardBuffers);
                this.items.renderSolid(collection, this.buffers, this.outlines);
                this.blocks.renderSolid(collection, this.buffers, models, this.outlines, options);
                this.custom.renderSolid(collection, this.buffers);
                this.leashes.renderSolid(collection, this.buffers);
                this.models.renderTranslucent(collection, this.buffers, this.outlines, this.discardBuffers);
                this.parts.renderTranslucent(collection, this.buffers, this.outlines, this.discardBuffers);
                this.items.renderTranslucent(collection, this.buffers, this.outlines);
                this.blocks.renderTranslucent(collection, this.buffers, models, this.outlines, this.discardBuffers, options);
                this.custom.renderTranslucent(collection, this.buffers);
            }
        } finally {
            this.submits.clear();
        }
    }

    void endFrame() {
        this.submits.endFrame();
    }

    @Override
    public void close() {
        this.submits.clear();
        this.submits.endFrame();
        this.buffers.close();
        this.discardBuffers.close();
        this.outlines.outlineBufferSource.sharedBuffer.close();
    }

    private static final class CaptureBuffers extends MultiBufferSource.BufferSource implements AutoCloseable {
        private final MultiBufferSource destination;

        private CaptureBuffers(MultiBufferSource destination) {
            super(new ByteBufferBuilder(256), new LinkedHashMap<>());
            this.destination = destination;
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            return this.destination.getBuffer(type);
        }

        @Override
        public void close() {
            this.sharedBuffer.close();
        }
    }
}
