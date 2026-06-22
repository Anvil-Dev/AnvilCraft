package dev.dubhe.anvilcraft.client.support;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

public class GravitationalLensManager {
    private static final int MAX_SEARCH_DISTANCE_SQR = 256 * 256;

    /** Client-side cache of loaded black hole block positions. */
    public static final Set<BlockPos> CLIENT_BLACK_HOLE_POSITIONS =
        Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void register(BlockPos pos) {
        CLIENT_BLACK_HOLE_POSITIONS.add(pos.immutable());
    }

    public static void unregister(BlockPos pos) {
        CLIENT_BLACK_HOLE_POSITIONS.remove(pos);
    }

    /**
     * Per-hole data passed to the shader.
     */
    public static final class HoleProjection {
        /** Center UV of the black hole on screen. */
        public final float centerU;
        public final float centerV;
        /** Distance from camera to black hole (world units). */
        public final float cameraDistance;
        /** Lens direction: > 0 = convex (pull), < 0 = concave (push). */
        public final float lensDirection;

        HoleProjection(float cu, float cv, float dist, float dir) {
            this.centerU = cu;
            this.centerV = cv;
            this.cameraDistance = dist;
            this.lensDirection = dir;
        }
    }

    /**
     * Build the combined view-projection matrix from camera position/rotation + projection.
     */
    private static Matrix4f buildViewProj(Camera camera, Matrix4f projectionMatrix) {
        float yaw = camera.getYRot();
        float pitch = camera.getXRot();

        Quaternionf cameraRotation = new Quaternionf()
            .rotateX((float) Math.toRadians(pitch))
            .rotateY((float) Math.toRadians(yaw + 180.0f));

        Vector3f cameraPos = camera.getPosition().toVector3f();

        Matrix4f viewMatrix = new Matrix4f()
            .rotate(cameraRotation)
            .translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        return new Matrix4f(projectionMatrix).mul(viewMatrix);
    }

    /**
     * Transform a world-space point to screen UV via view-projection.
     * Returns {@code null} when the point is behind the camera (clip.w ≤ 0).
     */
    private static @Nullable Vector2f worldToScreenUV(float wx, float wy, float wz, Matrix4f viewProj) {
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
     * Collect up to {@code maxCount} on-screen black holes, sorted nearest first.
     */
    public static List<HoleProjection> collectVisibleBlackHoles(
        Camera camera,
        Matrix4f projectionMatrix,
        int maxCount,
        float lensDirection
    ) {
        List<HoleProjection> result = new ArrayList<>();
        if (CLIENT_BLACK_HOLE_POSITIONS.isEmpty()) return result;

        Matrix4f viewProj = buildViewProj(camera, projectionMatrix);
        Vector3f cameraPos = camera.getPosition().toVector3f();

        for (BlockPos pos : CLIENT_BLACK_HOLE_POSITIONS) {
            double dx = pos.getX() + 0.5 - cameraPos.x;
            double dy = pos.getY() + 0.5 - cameraPos.y;
            double dz = pos.getZ() + 0.5 - cameraPos.z;
            double distanceSqr = dx * dx + dy * dy + dz * dz;
            if (distanceSqr > MAX_SEARCH_DISTANCE_SQR) continue;

            Vector2f centerUV = worldToScreenUV(
                pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, viewProj
            );
            if (centerUV == null) continue;

            // Skip black holes whose center is far off-screen
            if (centerUV.x < -0.2f || centerUV.x > 1.2f
                || centerUV.y < -0.2f || centerUV.y > 1.2f) continue;

            float dist = (float) Math.sqrt(distanceSqr);
            result.add(new HoleProjection(centerUV.x, centerUV.y, dist, lensDirection));
        }

        // Sort nearest first, then take the closest maxCount
        result.sort((a, b) -> Float.compare(a.cameraDistance, b.cameraDistance));
        if (result.size() > maxCount) {
            result = result.subList(0, maxCount);
        }
        return result;
    }
}
