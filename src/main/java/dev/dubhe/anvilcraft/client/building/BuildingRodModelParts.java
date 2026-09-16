package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

final class BuildingRodModelParts {
    private static final Map<BakedModel, BuildingRodModelParts> CACHE = new WeakHashMap<>();
    private final List<BakedQuad> body = new ArrayList<>();
    private final List<BakedQuad> core = new ArrayList<>();

    private BuildingRodModelParts(BakedModel model) {
        RandomSource random = RandomSource.create(42);
        for (Direction direction : Direction.values()) {
            random.setSeed(42);
            this.add(model.getQuads(null, direction, random, ModelData.EMPTY, null));
        }
        random.setSeed(42);
        this.add(model.getQuads(null, null, random, ModelData.EMPTY, null));
    }

    private void add(List<BakedQuad> quads) {
        for (BakedQuad quad : quads) (isCore(quad) ? this.core : this.body).add(quad);
    }

    private static boolean isCore(BakedQuad quad) {
        int[] vertices = quad.getVertices();
        int stride = DefaultVertexFormat.BLOCK.getVertexSize() / Integer.BYTES;
        for (int vertex = 0; vertex < 4; vertex++) {
            float x = Float.intBitsToFloat(vertices[vertex * stride]);
            float y = Float.intBitsToFloat(vertices[vertex * stride + 1]);
            float z = Float.intBitsToFloat(vertices[vertex * stride + 2]);
            // 只拆分原模型中 [7, 13, 7] 至 [9, 15, 9] 的悬浮核心。
            if (x < 7f / 16 - 0.001f || x > 9f / 16 + 0.001f
                || y < 13f / 16 - 0.001f || y > 15f / 16 + 0.001f
                || z < 7f / 16 - 0.001f || z > 9f / 16 + 0.001f) return false;
        }
        return true;
    }

    static void render(
        BakedModel model, ItemStack stack, PoseStack pose, MultiBufferSource buffers,
        int light, int overlay, float angle, boolean green, boolean powered
    ) {
        for (BakedModel pass : model.getRenderPasses(stack, true)) {
            BuildingRodModelParts parts = CACHE.computeIfAbsent(pass, BuildingRodModelParts::new);
            for (var type : pass.getRenderTypes(stack, true)) {
                VertexConsumer vertices = ItemRenderer.getFoilBuffer(buffers, type, true, stack.hasFoil());
                for (BakedQuad quad : parts.body) vertices.putBulkData(pose.last(), quad, 1, 1, 1, 1, light, overlay);
                pose.pushPose();
                pose.translate(0.5, 14.0 / 16, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(angle));
                pose.translate(-0.5, -14.0 / 16, -0.5);
                for (BakedQuad quad : parts.core) {
                    vertices.putBulkData(pose.last(), quad, green ? 0.85f : 1, 1, green ? 0.75f : 1, 1,
                        powered ? LightTexture.FULL_BRIGHT : light, overlay);
                }
                pose.popPose();
            }
        }
    }
}
