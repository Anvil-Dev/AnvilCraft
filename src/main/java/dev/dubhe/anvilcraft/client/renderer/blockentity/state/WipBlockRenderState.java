package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import com.mojang.blaze3d.vertex.QuadInstance;
import lombok.Getter;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class WipBlockRenderState extends BlockEntityRenderState {
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

    public record LitQuad(float x, float y, float z, BakedQuad quad, QuadInstance lighting) {
    }
}
