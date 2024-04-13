package dev.dubhe.anvilcraft.util;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.Data;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * 方块高亮
 */
public interface IBlockHighlightUtil {
    @Environment(EnvType.CLIENT)
    RenderType NO_DEPTH =
        RenderType.create(AnvilCraft.MOD_ID + "_no_depth", DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES, 256, true, true, RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                .setCullState(RenderStateShard.NO_CULL)
                .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2)))
                .createCompositeState(true));
    Map<Vector3ic, Long> SUBCHUNKS = new HashMap<>();

    LevelData level = new LevelData();

    /**
     * 高亮方块
     *
     * @param level 维度
     * @param pos   位置
     */
    static void highlightBlock(Level level, BlockPos pos) {
        if (IBlockHighlightUtil.getLevel() != level) {
            IBlockHighlightUtil.setLevel(level);
            SUBCHUNKS.clear();
        }
        if (level == null) return;
        SUBCHUNKS.put(new Vector3i(Math.floorDiv(pos.getX(), 16), Math.floorDiv(pos.getY(), 16),
            Math.floorDiv(pos.getZ(), 16)), level.getGameTime());
    }

    /**
     * 渲染
     *
     * @param level     世界
     * @param consumers 消耗
     * @param poseStack 渲染空间
     * @param camera    相机
     */
    @Environment(EnvType.CLIENT)
    static void render(
        Level level, @NotNull MultiBufferSource consumers, @NotNull PoseStack poseStack, @NotNull Camera camera
    ) {
        VertexConsumer consumer = consumers.getBuffer(IBlockHighlightUtil.NO_DEPTH);
        Vec3 cameraPos = camera.getPosition();
        int color = 0xFF8932B8;
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        for (var iterator = IBlockHighlightUtil.SUBCHUNKS.entrySet().iterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            Vector3ic subchunk = entry.getKey();
            Long moment = entry.getValue();
            if (level.getGameTime() > moment + 60 * 20) {
                iterator.remove();
                continue;
            }
            Vector3ic pos1 = subchunk.mul(16, new Vector3i());
            Vector3ic pos2 = pos1.add(16, 16, 16, new Vector3i());
            LevelRenderer.renderLineBox(
                poseStack,
                consumer,
                pos1.x(),
                pos1.y(),
                pos1.z(),
                pos2.x(),
                pos2.y(),
                pos2.z(),
                (color >> 16 & 0xFF) / 255f,
                (color >> 8 & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                (color >> 24) / 255f
            );
        }
        poseStack.popPose();
    }

    static void setLevel(Level level) {
        IBlockHighlightUtil.level.setLevel(level);
    }

    static Level getLevel() {
        return IBlockHighlightUtil.level.getLevel();
    }

    @Data
    class LevelData {
        Level level;
    }
}
