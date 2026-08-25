package dev.dubhe.anvilcraft.util;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/// 方块高亮
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BlockHighlightUtil {
    public static final Map<Vector3ic, Long> SUBCHUNKS = new HashMap<>();

    private static final AtomicReference<@Nullable Level> LEVEL_REF = new AtomicReference<>();

    /// 高亮方块
    ///
    /// @param level 维度
    /// @param pos   位置
    public static void highlightBlock(Level level, BlockPos pos) {
        if (BlockHighlightUtil.getLevel() != level) {
            BlockHighlightUtil.setLevel(level);
            BlockHighlightUtil.SUBCHUNKS.clear();
        }
        BlockHighlightUtil.SUBCHUNKS.put(
            new Vector3i(
                Math.floorDiv(pos.getX(), 16),
                Math.floorDiv(pos.getY(), 16),
                Math.floorDiv(pos.getZ(), 16)
            ),
            level.getGameTime()
        );
    }

    public static void render(
        ClientLevel level,
        SubmitNodeCollector submitNodeCollector,
        PoseStack poseStack,
        CameraRenderState cameraRenderState
    ) {
        Vec3 cameraPos = cameraRenderState.pos;
        int color = 0xFF8932B8;
        poseStack.pushPose();
        for (var iterator = BlockHighlightUtil.SUBCHUNKS.entrySet().iterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            Vector3ic subchunk = entry.getKey();
            Long moment = entry.getValue();
            if (level.getGameTime() > moment + 60 * 20) {
                iterator.remove();
                continue;
            }
            Vector3fc pos1 = new Vector3f(subchunk.mul(16, new Vector3i()));
            Vector3fc pos2 = pos1.add(16, 16, 16, new Vector3f());
            submitNodeCollector.submitCustomGeometry(
                poseStack,
                RenderTypes.lines(),
                (pose, buffer) -> {
                    PoseStack poseStack1 = new PoseStack();
                    poseStack.last().set(pose);
                    AABB aabb = new AABB(new Vec3(pos1), new Vec3(pos2));
                    ShapeRenderer.renderShape(
                        poseStack1,
                        buffer,
                        Shapes.create(aabb),
                        -cameraPos.x,
                        -cameraPos.y,
                        -cameraPos.z,
                        color,
                        7f
                    );
                }
            );
        }
        poseStack.popPose();
    }

    static void setLevel(Level level) {
        BlockHighlightUtil.LEVEL_REF.set(level);
    }

    @Nullable
    static Level getLevel() {
        return BlockHighlightUtil.LEVEL_REF.get();
    }
}
