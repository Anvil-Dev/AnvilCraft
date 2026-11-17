package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jspecify.annotations.Nullable;

/** 统一天体在世界、界面和物品中的模型边界与归一化变换。 */
public final class CelestialModelGeometry {
    public static final AABB UNIT = new AABB(0, 0, 0, 1, 1, 1);
    public static final AABB HEAD = new AABB(0.234375, 0.234375, 0.234375, 0.765625, 0.765625, 0.765625);
    private static final Cache<BakedModel, AABB> BOUNDS = CacheBuilder.newBuilder().maximumSize(64).build();

    private CelestialModelGeometry() {
    }

    /** 资源重载或切换世界时释放旧烘焙模型的边界缓存。 */
    public static void clear() {
        BOUNDS.invalidateAll();
    }

    /** 将完整方块模型等比缩放并居中到头颅轮廓，供世界、界面和物品共用。 */
    public static void fitToHead(PoseStack pose, AABB bounds) {
        float scale = (float) (HEAD.getXsize() / Math.max(bounds.getXsize(), Math.max(bounds.getYsize(), bounds.getZsize())));
        var sourceCenter = bounds.getCenter();
        var targetCenter = HEAD.getCenter();
        pose.translate(targetCenter.x, targetCenter.y, targetCenter.z);
        pose.scale(scale, scale, scale);
        pose.translate(-sourceCenter.x, -sourceCenter.y, -sourceCenter.z);
    }

    /** 根据实际烘焙顶点计算边界，兼容模型中的旋转和超出单格的几何。 */
    public static AABB modelBounds(@Nullable BakedModel model) {
        if (model == null) {
            return UNIT;
        }
        AABB cached = BOUNDS.getIfPresent(model);
        if (cached != null) {
            return cached;
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        RandomSource random = RandomSource.create(42);
        Direction[] directions = Direction.values();
        for (int face = 0; face <= directions.length; face++) {
            @Nullable Direction direction = face == directions.length ? null : directions[face];
            random.setSeed(42);
            for (BakedQuad quad : model.getQuads(null, direction, random, ModelData.EMPTY, null)) {
                int[] vertices = quad.getVertices();
                int stride = vertices.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) {
                    int offset = vertex * stride;
                    float x = Float.intBitsToFloat(vertices[offset]);
                    float y = Float.intBitsToFloat(vertices[offset + 1]);
                    float z = Float.intBitsToFloat(vertices[offset + 2]);
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    maxZ = Math.max(maxZ, z);
                }
            }
        }
        AABB bounds = Double.isFinite(minX + minY + minZ + maxX + maxY + maxZ)
            && Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ)) > 0.0001
            ? new AABB(minX, minY, minZ, maxX, maxY, maxZ) : UNIT;
        BOUNDS.put(model, bounds);
        return bounds;
    }

}
