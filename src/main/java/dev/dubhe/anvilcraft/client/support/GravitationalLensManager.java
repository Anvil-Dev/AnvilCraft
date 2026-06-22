package dev.dubhe.anvilcraft.client.support;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GravitationalLensManager {
    private static final int MAX_BLACK_HOLES = 4;
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
     * Holds the screen-space projection result for a single black hole.
     */
    public record ScreenProjection(float screenU, float screenV, boolean onScreen) {}

    /**
     * Project a world position to screen UV coordinates.
     *
     * @param worldPos        the block position in world space
     * @param camera          the current camera
     * @param projectionMatrix the projection matrix from renderLevel
     * @return ScreenProjection with UV coordinates and on-screen flag
     */
    public static ScreenProjection projectToScreen(
        BlockPos worldPos,
        Camera camera,
        Matrix4f projectionMatrix
    ) {
        float yaw = camera.getYRot();
        float pitch = camera.getXRot();

        // Build view matrix from camera rotation and position.
        // Minecraft yaw: 0 = south (+Z), 90 = west (-X), 180 = north (-Z), 270 = east (+X).
        // Minecraft pitch: positive = looking down.
        // We invert both rotations to go from world → camera space.
        Quaternionf cameraRotation = new Quaternionf()
            .rotateX((float) Math.toRadians(pitch))
            .rotateY((float) Math.toRadians(yaw + 180.0f));

        Vector3f cameraPos = camera.getPosition().toVector3f();

        Matrix4f viewMatrix = new Matrix4f()
            .rotate(cameraRotation)
            .translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        Matrix4f viewProj = new Matrix4f(projectionMatrix).mul(viewMatrix);

        // Center of block
        Vector4f clip = viewProj.transform(
            new Vector4f(
                worldPos.getX() + 0.5f,
                worldPos.getY() + 0.5f,
                worldPos.getZ() + 0.5f,
                1.0f
            )
        );

        if (clip.w <= 0.0f) {
            return new ScreenProjection(0, 0, false);
        }

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        float ndcZ = clip.z / clip.w;

        // Check if within clip space bounds
        if (ndcZ < -1.0f || ndcZ > 1.0f) {
            return new ScreenProjection(0, 0, false);
        }
        if (ndcX < -1.0f || ndcX > 1.0f || ndcY < -1.0f || ndcY > 1.0f) {
            return new ScreenProjection(0, 0, false);
        }

        float screenU = (ndcX + 1.0f) / 2.0f;
        float screenV = (ndcY + 1.0f) / 2.0f;

        return new ScreenProjection(screenU, screenV, true);
    }

    /**
     * Collect up to {@code maxCount} on-screen black hole projections,
     * sorted by distance to camera (nearest first).
     */
    public static List<ScreenProjection> collectVisibleBlackHoles(
        Camera camera,
        Matrix4f projectionMatrix,
        int maxCount
    ) {
        List<ScreenProjection> result = new ArrayList<>();
        if (CLIENT_BLACK_HOLE_POSITIONS.isEmpty()) return result;

        Vector3f cameraPos = camera.getPosition().toVector3f();

        for (BlockPos pos : CLIENT_BLACK_HOLE_POSITIONS) {
            // Quick distance cull
            double dx = pos.getX() + 0.5 - cameraPos.x;
            double dy = pos.getY() + 0.5 - cameraPos.y;
            double dz = pos.getZ() + 0.5 - cameraPos.z;
            double distSqr = dx * dx + dy * dy + dz * dz;
            if (distSqr > MAX_SEARCH_DISTANCE_SQR) continue;

            ScreenProjection proj = projectToScreen(pos, camera, projectionMatrix);
            if (proj.onScreen) {
                result.add(proj);
                if (result.size() >= maxCount) break;
            }
        }
        return result;
    }
}
