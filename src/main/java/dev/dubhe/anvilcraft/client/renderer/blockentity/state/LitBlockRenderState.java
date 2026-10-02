package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import lombok.Getter;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class LitBlockRenderState extends BlockEntityRenderState {
    @Getter
    private final Map<ChunkSectionLayer, List<LitQuad>> layers = new EnumMap<>(ChunkSectionLayer.class);

    public void clearQuads() {
        this.layers.values().forEach(List::clear);
    }

    public void addQuad(float x, float y, float z, BakedQuad quad, QuadInstance instance) {
        QuadInstance copy = new QuadInstance();
        for (int vertex = 0; vertex < 4; vertex++) {
            copy.setColor(vertex, instance.getColor(vertex));
            copy.setLightCoords(vertex, instance.getLightCoords(vertex));
        }
        copy.setOverlayCoords(instance.overlayCoords());
        this.layers.computeIfAbsent(quad.materialInfo().layer(), ignored -> new ArrayList<>())
            .add(new LitQuad(x, y, z, quad, copy));
    }

    public void submitGeometry(PoseStack pose, SubmitNodeCollector collector) {
        this.layers.forEach((layer, quads) -> {
            if (quads.isEmpty()) return;
            var type = switch (layer) {
                case SOLID -> RenderTypes.solidMovingBlock();
                case CUTOUT -> RenderTypes.cutoutMovingBlock();
                case TRANSLUCENT -> RenderTypes.translucentMovingBlock();
            };
            var snapshot = List.copyOf(quads);
            collector.submitCustomGeometry(pose, type, (submittedPose, buffer) -> {
                var translated = submittedPose.copy();
                for (var quad : snapshot) {
                    translated.set(submittedPose);
                    translated.translate(quad.x(), quad.y(), quad.z());
                    buffer.putBakedQuad(translated, quad.quad(), quad.lighting());
                }
            });
        });
    }

    public record LitQuad(float x, float y, float z, BakedQuad quad, QuadInstance lighting) {
    }
}
