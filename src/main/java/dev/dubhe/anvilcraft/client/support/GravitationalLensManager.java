package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GravitationalLensManager {
    private static final int MAX_BLACK_HOLES = 4;
    private static final int MAX_POLY_VERTS = 6;
    private static final int MAX_SEARCH_DISTANCE_SQR = 256 * 256;

    /** Half-extent of the black hole block model: box(4,4,4,12,12,12) → (12-4)/2/16 = 0.25 */
    private static final float CUBE_HALF_EXTENT = 0.25f;

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
     * Full per-hole data passed to the shader.
     */
    public static final class HoleProjection {
        /** Center UV of the black hole on screen. */
        public final float centerU;
        public final float centerV;
        public final boolean onScreen;
        /** Distance from camera to black hole (world units). */
        public final float cameraDistance;

        /** Number of convex-hull polygon vertices (0 when circular mode or degenerate). */
        public final int polyVertCount;
        /** UV coordinates of polygon vertices, tightly packed. Length = polyVertCount. */
        public final float[] polyU;
        public final float[] polyV;

        HoleProjection(float cu, float cv, boolean onScreen, float dist,
                       int vertCount, float[] pu, float[] pv) {
            this.centerU = cu;
            this.centerV = cv;
            this.onScreen = onScreen;
            this.cameraDistance = dist;
            this.polyVertCount = vertCount;
            this.polyU = pu;
            this.polyV = pv;
        }

        static HoleProjection offScreen() {
            return new HoleProjection(0, 0, false, 0, 0, new float[0], new float[0]);
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
     * Transform a world-space point to NDC, then to screen UV.
     * Returns null if the point is behind the camera or off-screen.
     */
    private static Vector2f worldToScreenUV(float wx, float wy, float wz, Matrix4f viewProj) {
        Vector4f clip = viewProj.transform(new Vector4f(wx, wy, wz, 1.0f));
        if (clip.w <= 0.0f) return null;

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        float ndcZ = clip.z / clip.w;

        if (ndcZ < -1.0f || ndcZ > 1.0f) return null;
        if (ndcX < -1.0f || ndcX > 1.0f || ndcY < -1.0f || ndcY > 1.0f) return null;

        return new Vector2f((ndcX + 1.0f) / 2.0f, (ndcY + 1.0f) / 2.0f);
    }

    /**
     * Compute the 2D convex hull of points using Andrew's monotone chain.
     * Returns vertices in CCW order. Max 6 for a cube projection.
     */
    static List<Vector2f> convexHull2D(List<Vector2f> points) {
        if (points.size() <= 2) return new ArrayList<>(points);

        // Remove exact duplicates
        List<Vector2f> unique = new ArrayList<>();
        for (Vector2f p : points) {
            boolean dup = false;
            for (Vector2f u : unique) {
                if (Math.abs(u.x - p.x) < 0.0001f && Math.abs(u.y - p.y) < 0.0001f) {
                    dup = true;
                    break;
                }
            }
            if (!dup) unique.add(p);
        }
        if (unique.size() <= 2) return unique;

        unique.sort(Comparator.<Vector2f>comparingDouble(v -> v.x)
            .thenComparingDouble(v -> v.y));

        List<Vector2f> hull = new ArrayList<>();

        // Lower hull
        for (Vector2f p : unique) {
            while (hull.size() >= 2) {
                Vector2f a = hull.get(hull.size() - 2);
                Vector2f b = hull.get(hull.size() - 1);
                if ((b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x) > 0) break;
                hull.remove(hull.size() - 1);
            }
            hull.add(p);
        }

        // Upper hull
        int lowerSize = hull.size();
        for (int i = unique.size() - 2; i >= 0; i--) {
            Vector2f p = unique.get(i);
            while (hull.size() > lowerSize) {
                Vector2f a = hull.get(hull.size() - 2);
                Vector2f b = hull.get(hull.size() - 1);
                if ((b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x) > 0) break;
                hull.remove(hull.size() - 1);
            }
            hull.add(p);
        }

        hull.remove(hull.size() - 1); // Remove duplicate
        return hull;
    }

    /**
     * Project the 8 vertices of the cube model and compute the 2D convex hull.
     * Returns an empty list if the projection is degenerate.
     */
    static List<Vector2f> computeCubeProjectionHull(
        BlockPos worldPos, Matrix4f viewProj, double polygonScale
    ) {
        float cx = worldPos.getX() + 0.5f;
        float cy = worldPos.getY() + 0.5f;
        float cz = worldPos.getZ() + 0.5f;
        float h = CUBE_HALF_EXTENT * (float) polygonScale;

        List<Vector2f> projected = new ArrayList<>(8);
        for (int ix = 0; ix < 2; ix++) {
            float x = cx + (ix == 0 ? -h : h);
            for (int iy = 0; iy < 2; iy++) {
                float y = cy + (iy == 0 ? -h : h);
                for (int iz = 0; iz < 2; iz++) {
                    float z = cz + (iz == 0 ? -h : h);
                    Vector2f uv = worldToScreenUV(x, y, z, viewProj);
                    if (uv != null) projected.add(uv);
                }
            }
        }

        if (projected.size() < 3) return projected; // degenerate

        List<Vector2f> hull = convexHull2D(projected);
        // Limit to MAX_POLY_VERTS
        if (hull.size() > MAX_POLY_VERTS) {
            hull = hull.subList(0, MAX_POLY_VERTS);
        }
        return hull;
    }

    /**
     * Collect up to {@code maxCount} on-screen black holes with optional polygon hull data.
     *
     * @param shapeMode CIRCULAR or CUBIC — determines whether polygon hulls are computed.
     */
    public static List<HoleProjection> collectVisibleBlackHoles(
        Camera camera,
        Matrix4f projectionMatrix,
        int maxCount,
        AnvilCraftClientConfig.LensingShape shapeMode,
        double polygonScale
    ) {
        List<HoleProjection> result = new ArrayList<>();
        if (CLIENT_BLACK_HOLE_POSITIONS.isEmpty()) return result;

        Matrix4f viewProj = buildViewProj(camera, projectionMatrix);
        Vector3f cameraPos = camera.getPosition().toVector3f();

        for (BlockPos pos : CLIENT_BLACK_HOLE_POSITIONS) {
            double dx = pos.getX() + 0.5 - cameraPos.x;
            double dy = pos.getY() + 0.5 - cameraPos.y;
            double dz = pos.getZ() + 0.5 - cameraPos.z;
            if (dx * dx + dy * dy + dz * dz > MAX_SEARCH_DISTANCE_SQR) continue;

            // Project center
            Vector2f centerUV = worldToScreenUV(
                pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, viewProj
            );
            if (centerUV == null) continue;

            List<Vector2f> hull = Collections.emptyList();
            if (shapeMode == AnvilCraftClientConfig.LensingShape.CUBIC) {
                hull = computeCubeProjectionHull(pos, viewProj, polygonScale);
            }

            float[] pu, pv;
            int vertCount = hull.size();
            if (vertCount > 0) {
                pu = new float[vertCount];
                pv = new float[vertCount];
                for (int i = 0; i < vertCount; i++) {
                    pu[i] = hull.get(i).x;
                    pv[i] = hull.get(i).y;
                }
            } else {
                pu = new float[0];
                pv = new float[0];
            }

            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            result.add(new HoleProjection(centerUV.x, centerUV.y, true, dist, vertCount, pu, pv));
            if (result.size() >= maxCount) break;
        }
        return result;
    }
}
