package dev.dubhe.anvilcraft.client.support;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

public class GravitationalLensManager {
    private static final int MAX_SEARCH_DISTANCE_SQR = 256 * 256;

    /**
     * Client-side cache of loaded black hole block positions.
     */
    public static final Set<BlockPos> CLIENT_BLACK_HOLE_POSITIONS =
        Collections.newSetFromMap(new ConcurrentHashMap<>());
    /**
     * Client-side cache of loaded white hole block positions.
     */
    public static final Set<BlockPos> CLIENT_WHITE_HOLE_POSITIONS =
        Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void register(BlockPos pos) {
        CLIENT_BLACK_HOLE_POSITIONS.add(pos.immutable());
    }

    public static void unregister(BlockPos pos) {
        CLIENT_BLACK_HOLE_POSITIONS.remove(pos);
    }

    public static void registerWhiteHole(BlockPos pos) {
        CLIENT_WHITE_HOLE_POSITIONS.add(pos.immutable());
    }

    public static void unregisterWhiteHole(BlockPos pos) {
        CLIENT_WHITE_HOLE_POSITIONS.remove(pos);
    }

    /**
     * Per-hole data passed to the shader.
     */
    public static final class HoleProjection {
        /**
         * Center UV of the black hole on screen.
         */
        public final float centerU;
        public final float centerV;
        /**
         * Distance from camera to black hole (world units).
         */
        public final float cameraDistance;
        /**
         * Lens direction: > 0 = convex (pull), < 0 = concave (push).
         */
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
     * Collect up to {@code maxCount} on-screen holes from both black and white hole sets,
     * sorted nearest first. Black holes get {@code blackHoleDir} (positive=convex pull),
     * white holes get {@code whiteHoleDir} (negative=concave push).
     */
    public static List<HoleProjection> collectVisibleHoles(
        Camera camera,
        Matrix4f projectionMatrix,
        int maxCount,
        float blackHoleDir,
        float whiteHoleDir
    ) {
        List<HoleProjection> result = new ArrayList<>();

        Matrix4f viewProj = buildViewProj(camera, projectionMatrix);
        Vector3f cameraPos = camera.getPosition().toVector3f();

        collectFromSet(CLIENT_BLACK_HOLE_POSITIONS, cameraPos, viewProj, blackHoleDir, result);
        collectFromSet(CLIENT_WHITE_HOLE_POSITIONS, cameraPos, viewProj, whiteHoleDir, result);

        // Sort nearest first, then take the closest maxCount
        result.sort((a, b) -> Float.compare(a.cameraDistance, b.cameraDistance));
        if (result.size() > maxCount) {
            result = result.subList(0, maxCount);
        }
        return result;
    }

    private static void collectFromSet(
        Set<BlockPos> positions, Vector3f cameraPos, Matrix4f viewProj,
        float lensDir, List<HoleProjection> out
    ) {
        for (BlockPos pos : positions) {
            double dx = pos.getX() + 0.5 - cameraPos.x;
            double dy = pos.getY() + 0.5 - cameraPos.y;
            double dz = pos.getZ() + 0.5 - cameraPos.z;
            double distanceSqr = dx * dx + dy * dy + dz * dz;
            if (distanceSqr > MAX_SEARCH_DISTANCE_SQR) continue;

            Vector2f centerUV = worldToScreenUV(
                pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, viewProj
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

    // ---- UBO management ----

    /**
     * Pre-allocated FloatBuffer for UBO upload (256 vec4s × 4 floats = 4096 bytes).
     */
    private static final FloatBuffer LENS_UBO_BUF =
        ByteBuffer.allocateDirect(256 * 4 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
    /**
     * UBO handle — created on first frame, reset on shader reload.
     */
    private static int lensUbo = 0;
    /**
     * Last program ID for which the UBO block index was bound.
     */
    private static int lensUboBlockBound = 0;

    /**
     * Upload hole data to the UBO and bind it. Call each frame before running the lens post-chain.
     *
     * @param holes     collected hole projections (≤ 256)
     * @param count     actual number of holes to write (remaining slots zeroed)
     * @param programId the shader program ID, for one-time block-index binding
     */
    public static void uploadLensUbo(List<HoleProjection> holes, int count, int programId) {
        FloatBuffer buf = LENS_UBO_BUF;
        buf.clear();
        for (int i = 0; i < 256; i++) {
            if (i < count) {
                HoleProjection h = holes.get(i);
                buf.put(h.centerU).put(h.centerV).put(h.cameraDistance).put(h.lensDirection);
            } else {
                buf.put(0.0f).put(0.0f).put(1.0f).put(1.0f);
            }
        }
        buf.flip();

        if (lensUbo == 0) {
            lensUbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, lensUbo);
            GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, buf, GL15.GL_DYNAMIC_DRAW);
        } else {
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, lensUbo);
            GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, buf);
        }
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, 0, lensUbo);

        // Bind UBO block "BlackHoles" to binding point 0 (once per shader load)
        if (lensUboBlockBound != programId) {
            int blockIndex = GL31.glGetUniformBlockIndex(programId, "BlackHoles");
            if (blockIndex != GL31.GL_INVALID_INDEX) {
                GL31.glUniformBlockBinding(programId, blockIndex, 0);
            }
            lensUboBlockBound = programId;
        }
    }

    /**
     * Free the UBO and reset state. Call on shader reload / GL context recreation.
     */
    public static void resetLensUbo() {
        if (lensUbo != 0) {
            GL15.glDeleteBuffers(lensUbo);
            lensUbo = 0;
        }
        lensUboBlockBound = 0;
    }
}
