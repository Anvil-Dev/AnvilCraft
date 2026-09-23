package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.mixin.accessor.BuildingRodItemLayerAccessor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BuildingRodModelParts {
    private static final int[] GREEN_TINT = {0xFFD8FFBF};
    private static final Map<List<BakedQuad>, BuildingRodModelParts> CACHE = new LinkedHashMap<>();
    private final List<BakedQuad> body = new ArrayList<>();
    private final List<BakedQuad> core = new ArrayList<>();
    private final List<BakedQuad> greenCore = new ArrayList<>();

    private BuildingRodModelParts(List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            if (!isCore(quad)) {
                this.body.add(quad);
                continue;
            }
            this.core.add(quad);
            var info = quad.materialInfo();
            var tinted = new BakedQuad.MaterialInfo(info.sprite(), info.layer(), info.itemRenderType(), 0,
                info.shade(), info.lightEmission(), info.ambientOcclusion());
            this.greenCore.add(new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(),
                quad.packedUV0(), quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(), tinted));
        }
    }

    static boolean isCore(BakedQuad quad) {
        for (int vertex = 0; vertex < 4; vertex++) {
            var point = quad.position(vertex);
            if (point.x() < 7f / 16 - 0.001f || point.x() > 9f / 16 + 0.001f
                || point.y() < 13f / 16 - 0.001f || point.y() > 15f / 16 + 0.001f
                || point.z() < 7f / 16 - 0.001f || point.z() > 9f / 16 + 0.001f) return false;
        }
        return true;
    }

    static void clearCache() {
        CACHE.clear();
    }

    static void render(
        ModelState model, ItemStack stack, ItemDisplayContext context, PoseStack pose, SubmitNodeCollector collector,
        int light, int overlay, int outline, float angle, boolean green, boolean powered
    ) {
        var foil = stack.hasFoil() ? ItemStackRenderState.FoilType.STANDARD : ItemStackRenderState.FoilType.NONE;
        if (model.layers == null) return;
        for (var layer : model.layers) {
            var quads = layer.prepareQuadList();
            var parts = CACHE.get(quads);
            if (parts == null) {
                if (CACHE.size() >= 128) CACHE.clear();
                parts = new BuildingRodModelParts(quads);
                CACHE.put(List.copyOf(quads), parts);
            }
            int[] tints = layer.tintLayers().isEmpty()
                ? ItemStackRenderState.LayerRenderState.EMPTY_TINTS : layer.tintLayers().toIntArray();
            collector.submitItem(pose, context, light, overlay, outline, tints, parts.body, foil);
            pose.pushPose();
            pose.translate(0.5, 14.0 / 16, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            pose.translate(-0.5, -14.0 / 16, -0.5);
            collector.submitItem(pose, context, powered ? LightCoordsUtil.FULL_BRIGHT : light, overlay, outline,
                green ? GREEN_TINT : tints, green ? parts.greenCore : parts.core, foil);
            pose.popPose();
        }
    }

    static class ModelState extends ItemStackRenderState {
        protected boolean capture = true;
        private @Nullable List<LayerRenderState> layers;
        private @Nullable List<Object> identity;

        @Override
        public LayerRenderState newLayer() {
            var layer = super.newLayer();
            if (this.capture) {
                if (this.layers == null) this.layers = new ArrayList<>();
                this.layers.add(layer);
            }
            return layer;
        }

        @Override
        public void clear() {
            super.clear();
            if (this.layers != null) this.layers.clear();
            if (this.identity != null) this.identity.clear();
        }

        @Override
        public void appendModelIdentityElement(Object element) {
            if (this.capture) {
                if (this.identity == null) this.identity = new ArrayList<>();
                this.identity.add(element);
            }
        }

        List<Object> identity() {
            return this.identity == null ? List.of() : List.copyOf(this.identity);
        }

        void applyTransform(PoseStack pose) {
            if (this.layers != null && !this.layers.isEmpty()) {
                ((BuildingRodItemLayerAccessor) this.layers.getFirst()).anvilcraft$applyTransform(pose.last());
            }
        }
    }
}
