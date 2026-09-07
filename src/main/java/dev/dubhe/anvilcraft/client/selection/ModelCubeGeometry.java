package dev.dubhe.anvilcraft.client.selection;

import dev.anvilcraft.lib.v2.cube.client.model.CubeModelDecoder;
import dev.anvilcraft.lib.v2.cube.geometry.ConvexShape;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

public final class ModelCubeGeometry {
    // 520 使用 Vec3.normalize 计算面法线；放大局部坐标以保留薄面，再由 SelectionPart 还原。
    public static final float SCALE = 128.0F;
    private static final float MIN_THICKNESS = 0.0016F;
    private static final double EPSILON = 1.0E-7;

    private ModelCubeGeometry() {
    }

    public static List<ConvexShape> decode(List<BlockElement> elements) {
        if (elements.isEmpty()) return List.of();
        boolean hasVolume = elements.stream().anyMatch(element -> !isPlane(element));
        List<BlockElement> scaled = new ArrayList<>(elements.size());
        for (BlockElement element : elements) {
            // 混合模型中的透明特效面不应产生矩形选区；纯平面的指针等部件仍须能够选取。
            if (hasVolume && isPlane(element)) continue;
            Vector3f from = new Vector3f(element.from);
            Vector3f to = new Vector3f(element.to);
            for (int axis = 0; axis < 3; axis++) {
                if (Math.abs(to.get(axis) - from.get(axis)) < MIN_THICKNESS) {
                    float center = (to.get(axis) + from.get(axis)) * 0.5F;
                    from.setComponent(axis, center - MIN_THICKNESS * 0.5F);
                    to.setComponent(axis, center + MIN_THICKNESS * 0.5F);
                }
            }
            BlockElementRotation rotation = element.rotation;
            if (rotation != null) {
                rotation = new BlockElementRotation(
                    new Vector3f(rotation.origin()).mul(SCALE), rotation.axis(), rotation.angle(), rotation.rescale()
                );
            }
            scaled.add(new BlockElement(from.mul(SCALE), to.mul(SCALE), element.faces, rotation, element.shade));
        }
        return CubeModelDecoder.decode(scaled, BlockModelRotation.X0_Y0);
    }

    private static boolean isPlane(BlockElement element) {
        return element.from.x == element.to.x || element.from.y == element.to.y || element.from.z == element.to.z;
    }

    @Nullable
    public static ConvexShape clip(ConvexShape shape, AABB cell) {
        if (!shape.bounds().intersects(cell)) return null;
        if (cell.contains(shape.bounds().minX, shape.bounds().minY, shape.bounds().minZ)
            && cell.contains(shape.bounds().maxX, shape.bounds().maxY, shape.bounds().maxZ)) return shape;
        List<List<Vec3>> faces = new ArrayList<>();
        for (ConvexShape.Face face : shape.faces()) {
            List<Vec3> vertices = new ArrayList<>();
            for (Vec3 vertex : shape.vertices()) {
                if (Math.abs(face.signedDistance(vertex)) < EPSILON) vertices.add(vertex);
            }
            faces.add(order(vertices, face.normal()));
        }
        for (int axis = 0; axis < 3; axis++) {
            Vec3 normal = new Vec3(axis == 0 ? 1 : 0, axis == 1 ? 1 : 0, axis == 2 ? 1 : 0);
            double min = axis == 0 ? cell.minX : axis == 1 ? cell.minY : cell.minZ;
            double max = axis == 0 ? cell.maxX : axis == 1 ? cell.maxY : cell.maxZ;
            faces = clipFaces(faces, normal, max);
            faces = clipFaces(faces, normal.scale(-1), -min);
            if (faces.size() < 4) return null;
        }
        List<Vec3> vertices = new ArrayList<>();
        List<int[]> indices = new ArrayList<>();
        for (List<Vec3> face : faces) {
            // 格子边界与旋转面相切时会产生浮点精度级的小三角形，不能交给 520 的法线归一化。
            double areaSquared = 0;
            for (int i = 1; i + 1 < face.size(); i++) {
                areaSquared = Math.max(areaSquared, face.get(i).subtract(face.getFirst())
                    .cross(face.get(i + 1).subtract(face.getFirst())).lengthSqr());
            }
            if (areaSquared < 1.0E-8) continue;
            int[] polygon = face.stream().mapToInt(vertex -> index(vertices, vertex)).distinct().toArray();
            if (polygon.length >= 3) indices.add(polygon);
        }
        if (vertices.size() < 4 || indices.size() < 4) return null;
        try {
            return new ConvexShape(vertices, indices.toArray(int[][]::new));
        } catch (IllegalArgumentException exception) {
            AABB bounds = new AABB(vertices.getFirst(), vertices.getFirst());
            for (Vec3 vertex : vertices) bounds = bounds.minmax(new AABB(vertex, vertex));
            if (Math.min(bounds.getXsize(), Math.min(bounds.getYsize(), bounds.getZsize())) < EPSILON) return null;
            throw exception;
        }
    }

    private static List<List<Vec3>> clipFaces(List<List<Vec3>> faces, Vec3 normal, double offset) {
        List<List<Vec3>> result = new ArrayList<>();
        List<Vec3> cap = new ArrayList<>();
        for (List<Vec3> face : faces) {
            List<Vec3> clipped = new ArrayList<>();
            for (int i = 0; i < face.size(); i++) {
                Vec3 start = face.get(i);
                Vec3 end = face.get((i + 1) % face.size());
                double first = start.dot(normal) - offset;
                double second = end.dot(normal) - offset;
                if (first <= EPSILON) index(clipped, start);
                if ((first < -EPSILON && second > EPSILON) || (first > EPSILON && second < -EPSILON)) {
                    Vec3 intersection = start.lerp(end, first / (first - second));
                    index(clipped, intersection);
                    index(cap, intersection);
                } else if (Math.abs(first) <= EPSILON) {
                    index(cap, start);
                }
            }
            if (clipped.size() >= 3) result.add(clipped);
        }
        if (cap.size() >= 3) {
            List<Vec3> ordered = order(cap, normal);
            if (result.stream().noneMatch(face -> face.size() == cap.size() && face.containsAll(cap))) result.add(ordered);
        }
        return result;
    }

    private static List<Vec3> order(List<Vec3> vertices, Vec3 normal) {
        if (vertices.size() < 3) return vertices;
        Vec3 center = Vec3.ZERO;
        for (Vec3 vertex : vertices) center = center.add(vertex);
        Vec3 origin = center.scale(1.0 / vertices.size());
        Vec3 first = vertices.getFirst().subtract(origin);
        Vec3 second = normal.cross(first);
        vertices.sort(Comparator.comparingDouble(vertex -> {
            Vec3 delta = vertex.subtract(origin);
            return Math.atan2(delta.dot(second), delta.dot(first));
        }));
        return vertices;
    }

    private static int index(List<Vec3> vertices, Vec3 vertex) {
        for (int i = 0; i < vertices.size(); i++) {
            if (vertices.get(i).distanceToSqr(vertex) < EPSILON * EPSILON) return i;
        }
        vertices.add(vertex);
        return vertices.size() - 1;
    }
}
