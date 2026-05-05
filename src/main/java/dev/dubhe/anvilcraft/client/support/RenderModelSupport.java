package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.anvilcraft.lib.v2.util.ListUtil;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 工具类，用于处理模型渲染相关操作，包括获取模型的包围盒大小以及解析模型顶点数据。
 */
public class RenderModelSupport {
    /**
     * 根据给定的方块状态和模型，计算该模型在世界中的包围盒（AABB）。
     *
     * @param state 方块状态，用于获取模型的特定状态下的几何信息
     * @param model 渲染模型对象，包含模型的几何数据
     * @return 返回模型的轴对齐包围盒（AABB）
     */
    @SuppressWarnings("deprecation")
    public static AABB getSize(
        BlockState state,
        BakedModel model
    ) {
        RandomSource rand = RandomSource.create();
        List<Vector3f> vec3f = new ArrayList<>();
        // 遍历所有方向的面，收集模型的顶点信息
        for (Direction side : Direction.values()) {
            for (BakedQuad quad : model.getQuads(state, side, rand)) {
                vec3f.addAll(RenderModelSupport.getVertices(quad));
            }
        }
        // 处理无方向的面
        for (BakedQuad quad : model.getQuads(state, null, rand)) {
            vec3f.addAll(RenderModelSupport.getVertices(quad));
        }
        return RenderModelSupport.getSize(vec3f);
    }

    /**
     * 根据给定的模型，计算该模型在世界中的包围盒（AABB），不依赖于方块状态。
     *
     * @param model 渲染模型对象，包含模型的几何数据
     * @return 返回模型的轴对齐包围盒（AABB）
     */
    @SuppressWarnings("deprecation")
    public static AABB getSize(
        BakedModel model
    ) {
        RandomSource rand = RandomSource.create();
        List<Vector3f> vec3f = new ArrayList<>();
        // 遍历所有方向的面，收集模型的顶点信息
        for (Direction side : Direction.values()) {
            for (BakedQuad quad : model.getQuads(null, side, rand)) {
                vec3f.addAll(RenderModelSupport.getVertices(quad));
            }
        }
        // 处理无方向的面
        for (BakedQuad quad : model.getQuads(null, null, rand)) {
            vec3f.addAll(RenderModelSupport.getVertices(quad));
        }
        return RenderModelSupport.getSize(vec3f);
    }

    /**
     * 根据顶点列表计算包围盒（AABB）。
     *
     * @param vec3f 包含模型顶点坐标的列表
     * @return 返回由顶点确定的轴对齐包围盒（AABB）
     */
    public static AABB getSize(List<Vector3f> vec3f) {
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        // 遍历所有顶点，找出最大和最小坐标
        vec3f.forEach(f -> {
            max.x = Math.max(max.x, f.x);
            max.y = Math.max(max.y, f.y);
            max.z = Math.max(max.z, f.z);
            min.x = Math.min(min.x, f.x);
            min.y = Math.min(min.y, f.y);
            min.z = Math.min(min.z, f.z);
        });
        // 对坐标进行限制，确保在合理范围内
        max.x = Math.clamp(max.x, -1.0f, 2.0f);
        max.y = Math.clamp(max.y, -1.0f, 2.0f);
        max.z = Math.clamp(max.z, -1.0f, 2.0f);
        min.x = Math.clamp(min.x, -1.0f, 2.0f);
        min.y = Math.clamp(min.y, -1.0f, 2.0f);
        min.z = Math.clamp(min.z, -1.0f, 2.0f);
        return new AABB(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    /**
     * 从给定的 BakedQuad 中提取顶点坐标。
     *
     * @param quad 包含顶点数据的 BakedQuad 对象
     * @return 返回包含该 quad 所有顶点坐标的列表
     */
    public static List<Vector3f> getVertices(BakedQuad quad) {
        int[] aint = quad.getVertices();
        int j = aint.length / 8;
        List<Vector3f> vec3f;
        // 使用 MemoryStack 来安全地分配本地内存并解析顶点数据
        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            ByteBuffer bytebuffer = memorystack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
            IntBuffer intbuffer = bytebuffer.asIntBuffer();
            vec3f = ListUtil.createWithValues(
                j, i -> {
                    intbuffer.clear();
                    intbuffer.put(aint, i * 8, 8);
                    float f = bytebuffer.getFloat(0);
                    float f1 = bytebuffer.getFloat(4);
                    float f2 = bytebuffer.getFloat(8);
                    return new Vector3f(f, f1, f2);
                }
            );
        }
        return vec3f;
    }
}
