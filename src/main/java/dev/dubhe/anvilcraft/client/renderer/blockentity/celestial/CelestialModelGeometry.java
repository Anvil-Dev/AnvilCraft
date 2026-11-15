package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.api.rendering.BlockStateModelRenderer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** 统一天体在世界、界面和物品中的模型边界与归一化变换。 */
public final class CelestialModelGeometry {
    public static final AABB UNIT = new AABB(0, 0, 0, 1, 1, 1);
    public static final AABB HEAD = new AABB(0.234375, 0.234375, 0.234375, 0.765625, 0.765625, 0.765625);
    private static final Cache<BlockStateModel, AABB> BOUNDS = CacheBuilder.newBuilder().maximumSize(64).build();

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
    public static AABB modelBounds(@Nullable BlockStateModel model) {
        if (model == null) {
            return UNIT;
        }
        var cached = BOUNDS.getIfPresent(model);
        if (cached != null) {
            return cached;
        }
        double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        BlockStateModelRenderer.INSTANCE.getTessellatorNoLighting().tesselateBlock((x, y, z, quad, instance) -> {
            for (int vertex = 0; vertex < 4; vertex++) {
                var point = quad.position(vertex);
                for (int axis = 0; axis < 3; axis++) {
                    min[axis] = Math.min(min[axis], point.get(axis));
                    max[axis] = Math.max(max[axis], point.get(axis));
                }
            }
        }, 0, 0, 0, BlockAndTintGetter.EMPTY, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), model, 42);
        var result = Double.isFinite(min[0]) && Math.max(max[0] - min[0], Math.max(max[1] - min[1], max[2] - min[2])) > 0.0001
            ? new AABB(min[0], min[1], min[2], max[0], max[1], max[2]) : UNIT;
        BOUNDS.put(model, result);
        return result;
    }

}
