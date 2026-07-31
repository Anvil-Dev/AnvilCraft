package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.systems.CommandEncoder;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.renderer.post.GravitationalLensPostEffect;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GravitationalLensSupport {
    private static final int MAX_SEARCH_DISTANCE_SQR = 256 * 256;

    public static final Set<BlockPos> CLIENT_BLACK_HOLE_POSITIONS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public static final Set<BlockPos> CLIENT_WHITE_HOLE_POSITIONS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void register(BlockPos pos) {
        GravitationalLensSupport.CLIENT_BLACK_HOLE_POSITIONS.add(pos.immutable());
    }

    public static void unregister(BlockPos pos) {
        GravitationalLensSupport.CLIENT_BLACK_HOLE_POSITIONS.remove(pos);
    }

    public static void registerWhiteHole(BlockPos pos) {
        GravitationalLensSupport.CLIENT_WHITE_HOLE_POSITIONS.add(pos.immutable());
    }

    public static void unregisterWhiteHole(BlockPos pos) {
        GravitationalLensSupport.CLIENT_WHITE_HOLE_POSITIONS.remove(pos);
    }

    private static Matrix4f buildViewProj(CameraRenderState cameraState, Matrix4fc projectionMatrix) {
        float yaw = cameraState.yRot;
        float pitch = cameraState.xRot;

        Quaternionf cameraRotation = new Quaternionf()
            .rotateX((float) Math.toRadians(pitch))
            .rotateY((float) Math.toRadians(yaw + 180.0f));

        Vector3f cameraPos = cameraState.pos.toVector3f();

        Matrix4f viewMatrix = new Matrix4f()
            .rotate(cameraRotation)
            .translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        return new Matrix4f(projectionMatrix).mul(viewMatrix);
    }

    /**
     * Transform a world-space point to screen UV via view-projection.
     * Returns {@code null} when the point is behind the camera (clip.w ≤ 0).
     */
    private static @Nullable Vector2f worldToScreenUV(
        float wx,
        float wy,
        float wz,
        Matrix4f viewProj
    ) {
        Vector4f clip = viewProj.transform(new Vector4f(wx, wy, wz, 1.0f));
        if (clip.w <= 0.0f) return null;

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;

        // Clamp to screen edge — still useful for points slightly off-screen
        ndcX = Math.clamp(ndcX, -1.0f, 1.0f);
        ndcY = Math.clamp(ndcY, -1.0f, 1.0f);

        return new Vector2f((ndcX + 1.0f) / 2.0f, (ndcY + 1.0f) / 2.0f);
    }

    /**
     * Collect up to {@code maxCount} on-screen holes from both black and white hole sets,
     * sorted nearest first. Black holes get {@code blackHoleDir} (positive=convex pull),
     * white holes get {@code whiteHoleDir} (negative=concave push).
     * Uses CameraRenderState + projection matrix from the new rendering pipeline.
     */
    public static List<HoleProjection> collectVisibleHoles(
        CameraRenderState cameraState,
        Matrix4fc projectionMatrix,
        int maxCount,
        float blackHoleDir,
        float whiteHoleDir
    ) {
        List<HoleProjection> result = new ArrayList<>();

        Matrix4f viewProj = GravitationalLensSupport.buildViewProj(cameraState, projectionMatrix);
        Vector3f cameraPos = cameraState.pos.toVector3f();

        GravitationalLensSupport.collectFromSet(GravitationalLensSupport.CLIENT_BLACK_HOLE_POSITIONS, cameraPos, viewProj, blackHoleDir, result);
        GravitationalLensSupport.collectFromSet(GravitationalLensSupport.CLIENT_WHITE_HOLE_POSITIONS, cameraPos, viewProj, whiteHoleDir, result);

        // Sort nearest first, then take the closest maxCount
        result.sort((a, b) -> Float.compare(a.cameraDistance, b.cameraDistance));
        if (result.size() > maxCount) {
            result = result.subList(0, maxCount);
        }
        return result;
    }

    public static boolean uploadBlackHoles(
        GravitationalLensPostEffect effect,
        CommandEncoder commandEncoder,
        LevelRenderState levelRenderState
    ) {
        float dir = (float) AnvilCraftClient.CONFIG.gravitationalLens.lensDirection;
        int maxCount = AnvilCraftClient.CONFIG.gravitationalLens.maxHoleCount;
        CameraRenderState cameraState = levelRenderState.cameraRenderState;
        List<HoleProjection> holes = GravitationalLensSupport.collectVisibleHoles(
            cameraState,
            cameraState.projectionMatrix,
            maxCount,
            dir,
            -dir
        );
        if (holes.isEmpty()) {
            return false;
        }
        int count = Math.min(holes.size(), maxCount);
        effect.uploadBlackHoles(
            commandEncoder,
            holes,
            count,
            (float) AnvilCraftClient.CONFIG.gravitationalLens.lensStrength,
            (float) AnvilCraftClient.CONFIG.gravitationalLens.eventHorizonRadius,
            (float) AnvilCraftClient.CONFIG.gravitationalLens.lensPerspectiveScale
        );
        return true;
    }

    private static void collectFromSet(
        Set<BlockPos> positions,
        Vector3f cameraPos,
        Matrix4f viewProj,
        float lensDir,
        List<HoleProjection> out
    ) {
        for (BlockPos pos : positions) {
            double dx = pos.getX() + 0.5 - cameraPos.x;
            double dy = pos.getY() + 0.5 - cameraPos.y;
            double dz = pos.getZ() + 0.5 - cameraPos.z;
            double distanceSqr = dx * dx + dy * dy + dz * dz;
            if (distanceSqr > GravitationalLensSupport.MAX_SEARCH_DISTANCE_SQR) continue;

            Vector2f centerUV = GravitationalLensSupport.worldToScreenUV(
                pos.getX() + 0.5f,
                pos.getY() + 0.5f,
                pos.getZ() + 0.5f,
                viewProj
            );
            if (centerUV == null) continue;

            if (centerUV.x < -0.2f || centerUV.x > 1.2f
                || centerUV.y < -0.2f || centerUV.y > 1.2f) {
                continue;
            }

            float dist = (float) Math.sqrt(distanceSqr);
            out.add(new HoleProjection(centerUV.x, centerUV.y, dist, lensDir));
        }
    }

    public static final class HoleProjection {
        public final float centerU;
        public final float centerV;
        public final float cameraDistance;
        public final float lensDirection;

        HoleProjection(float cu, float cv, float dist, float dir) {
            this.centerU = cu;
            this.centerV = cv;
            this.cameraDistance = dist;
            this.lensDirection = dir;
        }
    }
}
