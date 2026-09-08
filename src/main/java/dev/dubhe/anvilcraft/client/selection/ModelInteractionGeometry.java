package dev.dubhe.anvilcraft.client.selection;

import dev.anvilcraft.lib.v2.cube.geometry.ConvexShape;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

final class ModelInteractionGeometry {
    private static final double EPSILON = 1.0E-6;
    private static final double MAX_GAP = ModelCubeGeometry.SCALE / 16.0;
    private static final int MAX_CELLS = 65_536;
    private static final int MAX_SHAPE_CELLS = 1_048_576;
    private final double[][] coordinates;
    private final int[] sizes;
    private final int[] strides;
    private final BitSet occupied;

    private ModelInteractionGeometry(double[][] coordinates) {
        this.coordinates = coordinates;
        this.sizes = Arrays.stream(coordinates).mapToInt(axis -> axis.length - 1).toArray();
        this.strides = new int[]{this.sizes[1] * this.sizes[2], this.sizes[2], 1};
        this.occupied = new BitSet(this.sizes[0] * this.strides[0]);
    }

    static SelectionGeometry fill(SelectionGeometry source) {
        if (source.shapes().size() < 2) return source;
        double[][] coordinates = new double[3][];
        long cells = 1;
        for (int axis = 0; axis < 3; axis++) {
            int component = axis;
            double[] values = source.shapes().stream().flatMap(shape -> shape.vertices().stream())
                .mapToDouble(vertex -> coordinate(vertex, component)).sorted().toArray();
            int count = 0;
            for (double value : values) {
                if (count == 0 || value - values[count - 1] > EPSILON) values[count++] = value;
            }
            coordinates[axis] = Arrays.copyOf(values, count);
            cells *= count - 1;
            if (cells <= 0 || cells > MAX_CELLS) return source;
        }
        ModelInteractionGeometry grid = new ModelInteractionGeometry(coordinates);
        if (!grid.rasterize(source.shapes())) return source;
        BitSet filled = new BitSet((int) cells);
        // 各轴只读取原始截面，避免补出的面继续围合、最终吞掉铁砧腰部等开放结构。
        for (int axis = 0; axis < 3; axis++) grid.fillSections(axis, filled);
        filled.andNot(grid.occupied);
        if (filled.isEmpty()) return source;
        List<ConvexShape> shapes = new ArrayList<>(source.shapes());
        while (!filled.isEmpty()) {
            if (shapes.size() >= SelectionGeometry.MAX_SHAPES) return source;
            AABB box = grid.takeBox(filled);
            try {
                shapes.add(ConvexShape.box(box));
            } catch (IllegalArgumentException exception) {
                return source;
            }
        }
        return new SelectionGeometry(shapes);
    }

    private boolean rasterize(List<ConvexShape> shapes) {
        long work = 0;
        for (ConvexShape shape : shapes) {
            AABB bounds = shape.bounds();
            int[] min = this.indices(new Vec3(bounds.minX, bounds.minY, bounds.minZ));
            int[] max = this.indices(new Vec3(bounds.maxX, bounds.maxY, bounds.maxZ));
            work += (long) (max[0] - min[0]) * (max[1] - min[1]) * (max[2] - min[2]);
            if (work > MAX_SHAPE_CELLS) return false;
            boolean box = shape.faces().stream().allMatch(face -> {
                Vec3 normal = face.normal();
                double maximum = Math.max(Math.abs(normal.x), Math.max(Math.abs(normal.y), Math.abs(normal.z)));
                return Math.abs(normal.x) + Math.abs(normal.y) + Math.abs(normal.z) - maximum < EPSILON;
            });
            for (int x = min[0]; x < max[0]; x++) {
                for (int y = min[1]; y < max[1]; y++) {
                    for (int z = min[2]; z < max[2]; z++) {
                        if (box || this.containsCell(shape, x, y, z)) this.occupied.set(this.index(x, y, z));
                    }
                }
            }
        }
        return true;
    }

    private int[] indices(Vec3 point) {
        int[] result = new int[3];
        for (int axis = 0; axis < 3; axis++) {
            int index = Arrays.binarySearch(this.coordinates[axis], coordinate(point, axis));
            result[axis] = index >= 0 ? index : Math.max(0, -index - 2);
        }
        return result;
    }

    private boolean containsCell(ConvexShape shape, int x, int y, int z) {
        // 斜面只贡献完全位于凸体内部的格子，不用旋转模型的包围盒冒充实体。
        for (ConvexShape.Face face : shape.faces()) {
            Vec3 normal = face.normal();
            double distance = normal.x * this.coordinates[0][normal.x >= 0 ? x + 1 : x]
                + normal.y * this.coordinates[1][normal.y >= 0 ? y + 1 : y]
                + normal.z * this.coordinates[2][normal.z >= 0 ? z + 1 : z];
            if (distance > face.planeOffset() + EPSILON) return false;
        }
        return true;
    }

    private void fillSections(int axis, BitSet filled) {
        int horizontal = (axis + 1) % 3;
        int vertical = (axis + 2) % 3;
        int width = this.sizes[horizontal];
        int height = this.sizes[vertical];
        for (int layer = 0; layer < this.sizes[axis]; layer++) {
            boolean[] section = new boolean[width * height];
            int base = layer * this.strides[axis];
            for (int u = 0; u < width; u++) {
                for (int v = 0; v < height; v++) {
                    section[u * height + v] = this.occupied.get(base + u * this.strides[horizontal] + v * this.strides[vertical]);
                }
            }
            boolean[] closed = section.clone();
            for (int v = 0; v < height; v++) closeGaps(section, closed, v, height, this.coordinates[horizontal]);
            for (int u = 0; u < width; u++) closeGaps(section, closed, u * height, 1, this.coordinates[vertical]);
            boolean[] exterior = exterior(closed, width, height);
            for (int u = 0; u < width; u++) {
                for (int v = 0; v < height; v++) {
                    int index = u * height + v;
                    if (!section[index] && (closed[index] || !exterior[index])) {
                        filled.set(base + u * this.strides[horizontal] + v * this.strides[vertical]);
                    }
                }
            }
        }
    }

    private static void closeGaps(boolean[] source, boolean[] result, int offset, int stride, double[] coordinates) {
        int previous = -1;
        for (int cell = 0; cell + 1 < coordinates.length; cell++) {
            if (!source[offset + cell * stride]) continue;
            if (previous >= 0 && coordinates[cell] - coordinates[previous + 1] <= MAX_GAP + EPSILON) {
                for (int gap = previous + 1; gap < cell; gap++) result[offset + gap * stride] = true;
            }
            previous = cell;
        }
    }

    private static boolean[] exterior(boolean[] solid, int width, int height) {
        boolean[] visited = new boolean[solid.length];
        int[] queue = new int[solid.length];
        int end = 0;
        for (int u = 0; u < width; u++) {
            end = enqueue(u * height, solid, visited, queue, end);
            end = enqueue(u * height + height - 1, solid, visited, queue, end);
        }
        for (int v = 0; v < height; v++) {
            end = enqueue(v, solid, visited, queue, end);
            end = enqueue((width - 1) * height + v, solid, visited, queue, end);
        }
        for (int cursor = 0; cursor < end; cursor++) {
            int index = queue[cursor];
            int u = index / height;
            int v = index % height;
            if (u > 0) end = enqueue(index - height, solid, visited, queue, end);
            if (u + 1 < width) end = enqueue(index + height, solid, visited, queue, end);
            if (v > 0) end = enqueue(index - 1, solid, visited, queue, end);
            if (v + 1 < height) end = enqueue(index + 1, solid, visited, queue, end);
        }
        return visited;
    }

    private static int enqueue(int index, boolean[] solid, boolean[] visited, int[] queue, int end) {
        if (solid[index] || visited[index]) return end;
        visited[index] = true;
        queue[end] = index;
        return end + 1;
    }

    private AABB takeBox(BitSet cells) {
        int first = cells.nextSetBit(0);
        int x = first / this.strides[0];
        int y = first / this.strides[1] % this.sizes[1];
        int z = first % this.sizes[2];
        int maxZ = z + 1;
        while (maxZ < this.sizes[2] && cells.get(this.index(x, y, maxZ))) maxZ++;
        int maxY = y + 1;
        while (maxY < this.sizes[1] && this.containsRow(cells, x, maxY, z, maxZ)) maxY++;
        int maxX = x + 1;
        while (maxX < this.sizes[0] && this.containsSlice(cells, maxX, y, maxY, z, maxZ)) maxX++;
        for (int a = x; a < maxX; a++) {
            for (int b = y; b < maxY; b++) {
                int start = this.index(a, b, z);
                cells.clear(start, start + maxZ - z);
            }
        }
        return new AABB(this.coordinates[0][x], this.coordinates[1][y], this.coordinates[2][z],
            this.coordinates[0][maxX], this.coordinates[1][maxY], this.coordinates[2][maxZ]);
    }

    private boolean containsSlice(BitSet cells, int x, int minY, int maxY, int minZ, int maxZ) {
        for (int y = minY; y < maxY; y++) {
            if (!this.containsRow(cells, x, y, minZ, maxZ)) return false;
        }
        return true;
    }

    private boolean containsRow(BitSet cells, int x, int y, int minZ, int maxZ) {
        int start = this.index(x, y, minZ);
        return cells.nextClearBit(start) >= start + maxZ - minZ;
    }

    private int index(int x, int y, int z) {
        return x * this.strides[0] + y * this.strides[1] + z;
    }

    private static double coordinate(Vec3 point, int axis) {
        return axis == 0 ? point.x : axis == 1 ? point.y : point.z;
    }
}
