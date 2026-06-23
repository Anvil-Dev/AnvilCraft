package dev.dubhe.anvilcraft.client.support;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
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
     * Build the combined view-projection matrix from CameraRenderState + projection.
     */
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
     * Build the combined view-projection matrix from camera position/rotation + projection.
     */
    private static Matrix4f buildViewProj(Camera camera, Matrix4f projectionMatrix) {
        Vector3f cameraPos = camera.position().toVector3f();

        Matrix4f viewMatrix = new Matrix4f()
            .rotate(camera.rotation())
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

        Matrix4f viewProj = buildViewProj(cameraState, projectionMatrix);
        Vector3f cameraPos = cameraState.pos.toVector3f();

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
     * Pre-allocated FloatBuffer for UBO upload.
     * Layout: vec4 LensParams + 256 vec4 BlackHole = 257 × 4 floats = 4112 bytes.
     */
    private static final FloatBuffer LENS_UBO_BUF =
        ByteBuffer.allocateDirect(257 * 4 * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
    /**
     * UBO handle — created on first frame, reset on shader reload.
     */
    private static int lensUbo = 0;
    /**
     * Set to true each frame before the lens post-chain processes, cleared after binding.
     */
    private static volatile boolean needsBind = false;

    /**
     * Upload hole data + lens params to the UBO and bind it.
     *
     * @param holes              collected hole projections (≤ 256)
     * @param count              actual number of holes
     * @param lensStrength       lens distortion strength
     * @param eventHorizonRadius event horizon radius in UV units
     * @param perspectiveScale   perspective scaling reference distance
     */
    public static void uploadLensUbo(
        List<HoleProjection> holes, int count,
        float lensStrength, float eventHorizonRadius, float perspectiveScale
    ) {
        FloatBuffer buf = LENS_UBO_BUF;
        buf.clear();
        // First vec4: LensParams (count, lensStrength, eventHorizonRadius, perspectiveScale)
        buf.put((float) count).put(lensStrength).put(eventHorizonRadius).put(perspectiveScale);
        // Remaining 256 vec4s: BlackHole data
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
        // Mark that the UBO needs to be bound before the next draw
        needsBind = true;
    }

    /**
     * Called before every render pass draw. If the lens UBO has been uploaded,
     * binds it to the "BlackHoles" uniform block of the current shader program.
     * Uses {@code glGetUniformBlockIndex} + {@code glUniformBlockBinding} to
     * ensure correct binding without requiring {@code binding = 0} in the shader.
     */
    public static void bindLensUboIfNeeded() {
        if (!needsBind || lensUbo == 0) return;

        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if (program == 0) return;

        int blockIndex = GL31.glGetUniformBlockIndex(program, "BlackHoles");
        if (blockIndex == GL31.GL_INVALID_INDEX) return;

        // Use binding point 8 to avoid conflicts with SamplerInfo (binding 0) and other post-pass UBOs
        GL31.glUniformBlockBinding(program, blockIndex, 8);
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, 8, lensUbo);
    }

    /**
     * Clear the UBO bind flag — call after the lens post-chain has finished processing.
     */
    public static void clearLensUboFlag() {
        needsBind = false;
    }

    /**
     * Free the UBO and reset state. Call on shader reload / GL context recreation.
     */
    public static void resetLensUbo() {
        if (lensUbo != 0) {
            GL15.glDeleteBuffers(lensUbo);
            lensUbo = 0;
        }
        needsBind = false;
    }
}
