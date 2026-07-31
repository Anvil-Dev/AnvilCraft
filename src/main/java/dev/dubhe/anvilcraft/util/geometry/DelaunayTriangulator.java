package dev.dubhe.anvilcraft.util.geometry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

public final class DelaunayTriangulator {
    private static final double EPSILON = 1.0E-9;

    private DelaunayTriangulator() {
    }

    public static Set<Edge> triangulate(Collection<Point> input) {
        return DelaunayTriangulator.triangulate(new ArrayList<>(input));
    }

    public static Set<Edge> triangulate(int pointCount, IntFunction<Point> pointFactory) {
        List<Point> points = new ArrayList<>(pointCount);
        for (int index = 0; index < pointCount; index++) {
            points.add(pointFactory.apply(index));
        }
        return DelaunayTriangulator.triangulate(points);
    }

    private static Set<Edge> triangulate(List<Point> input) {
        List<Point> points = DelaunayTriangulator.deduplicate(input);
        if (points.size() < 2) return Set.of();
        if (points.size() == 2) {
            return Set.of(new Edge(points.get(0).id(), points.get(1).id()));
        }

        SuperTriangle superTriangle = DelaunayTriangulator.createSuperTriangle(points);
        int superA = points.size();
        int superB = points.size() + 1;
        int superC = points.size() + 2;
        points.add(new Point(superA, superTriangle.ax(), superTriangle.ay()));
        points.add(new Point(superB, superTriangle.bx(), superTriangle.by()));
        points.add(new Point(superC, superTriangle.cx(), superTriangle.cy()));
        TriangleBuffer triangles = new TriangleBuffer(points.size() * 4);
        triangles.add(superA, superB, superC, points);

        HashMap<Long, EdgeReference> localEdges = new HashMap<>();
        ArrayList<BoundaryEdge> boundaryEdges = new ArrayList<>();
        ArrayList<Integer> badTriangles = new ArrayList<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        int lastHitTriangle = 0;

        int inputSize = points.size() - 3;
        for (int pointIndex = 0; pointIndex < inputSize; pointIndex++) {
            Point point = points.get(pointIndex);
            badTriangles.clear();
            int seedTriangle = DelaunayTriangulator.findSeedTriangle(triangles, point, lastHitTriangle);
            if (seedTriangle == -1) continue;
            DelaunayTriangulator.collectBadTriangles(triangles, point, seedTriangle, queue, badTriangles);
            if (badTriangles.isEmpty()) continue;
            lastHitTriangle = seedTriangle;

            int badTriangleStamp = triangles.nextVisitStamp();
            for (int triangleIndex : badTriangles) {
                triangles.setVisitStamp(triangleIndex, badTriangleStamp);
            }
            boundaryEdges.clear();
            DelaunayTriangulator.collectBoundaryEdges(triangles, badTriangles, badTriangleStamp, boundaryEdges);
            for (int triangleIndex : badTriangles) {
                triangles.remove(triangleIndex);
            }
            localEdges.clear();
            for (BoundaryEdge edge : boundaryEdges) {
                int triangleIndex = triangles.add(edge.a(), edge.b(), point.id(), points);
                if (triangleIndex == -1) continue;
                triangles.setNeighbor(triangleIndex, 0, edge.outsideNeighbor());
                if (edge.outsideNeighbor() != -1) {
                    triangles.replaceNeighbor(edge.outsideNeighbor(), edge.a(), edge.b(), triangleIndex);
                }
                DelaunayTriangulator.registerLocalEdge(localEdges, triangles, triangleIndex, edge.b(), point.id(), 1);
                DelaunayTriangulator.registerLocalEdge(localEdges, triangles, triangleIndex, point.id(), edge.a(), 2);
            }
        }

        HashSet<Edge> edges = HashSet.newHashSet(triangles.size() * 2);
        for (int triangleIndex = 0; triangleIndex < triangles.size(); triangleIndex++) {
            if (!triangles.isAlive(triangleIndex)) continue;
            int a = triangles.getVertexA(triangleIndex);
            int b = triangles.getVertexB(triangleIndex);
            int c = triangles.getVertexC(triangleIndex);
            if (a >= inputSize || b >= inputSize || c >= inputSize) continue;
            edges.add(new Edge(a, b));
            edges.add(new Edge(b, c));
            edges.add(new Edge(c, a));
        }
        return edges;
    }

    private static int findSeedTriangle(TriangleBuffer triangles, Point point, int lastHitTriangle) {
        if (lastHitTriangle >= 0 && lastHitTriangle < triangles.size()
            && triangles.isAlive(lastHitTriangle)
            && triangles.containsInCircumcircle(lastHitTriangle, point)) {
            return lastHitTriangle;
        }
        int walkedTriangle = DelaunayTriangulator.walkSeedTriangle(triangles, point, lastHitTriangle);
        if (walkedTriangle != -1) return walkedTriangle;
        for (int triangleIndex = 0; triangleIndex < triangles.size(); triangleIndex++) {
            if (!triangles.isAlive(triangleIndex)) continue;
            if (triangles.containsInCircumcircle(triangleIndex, point)) {
                return triangleIndex;
            }
        }
        return -1;
    }

    private static int walkSeedTriangle(TriangleBuffer triangles, Point point, int startTriangle) {
        int triangleIndex = startTriangle;
        if (triangleIndex < 0 || triangleIndex >= triangles.size() || !triangles.isAlive(triangleIndex)) {
            triangleIndex = triangles.firstAliveTriangle();
        }
        if (triangleIndex == -1) return -1;

        int stamp = triangles.nextVisitStamp();
        // noinspection ConstantValue
        while (triangleIndex != -1) {
            if (triangles.visitStamp(triangleIndex) == stamp) return -1;
            triangles.setVisitStamp(triangleIndex, stamp);
            if (triangles.containsInCircumcircle(triangleIndex, point)) {
                return triangleIndex;
            }
            int nextTriangle = triangles.closestNeighbor(triangleIndex, point);
            if (nextTriangle == -1) return -1;
            triangleIndex = nextTriangle;
        }
        return -1;
    }

    private static void collectBadTriangles(
        TriangleBuffer triangles,
        Point point,
        int seedTriangle,
        ArrayDeque<Integer> queue,
        ArrayList<Integer> badTriangles
    ) {
        queue.clear();
        int stamp = triangles.nextVisitStamp();
        queue.add(seedTriangle);
        while (!queue.isEmpty()) {
            int triangleIndex = queue.removeFirst();
            if (!triangles.isAlive(triangleIndex)) continue;
            if (triangles.visitStamp(triangleIndex) == stamp) continue;
            triangles.setVisitStamp(triangleIndex, stamp);
            if (!triangles.containsInCircumcircle(triangleIndex, point)) continue;
            badTriangles.add(triangleIndex);
            int neighborAB = triangles.neighborAB(triangleIndex);
            int neighborBC = triangles.neighborBC(triangleIndex);
            int neighborCA = triangles.neighborCA(triangleIndex);
            if (neighborAB != -1) queue.addLast(neighborAB);
            if (neighborBC != -1) queue.addLast(neighborBC);
            if (neighborCA != -1) queue.addLast(neighborCA);
        }
    }

    private static List<Point> deduplicate(List<Point> input) {
        List<Point> points = new ArrayList<>(input.size());
        Set<Long> seen = HashSet.newHashSet(input.size());
        for (Point point : input) {
            long key = DelaunayTriangulator.quantizedKey(point.x(), point.y());
            if (seen.add(key)) points.add(point);
        }
        return points;
    }

    private static long quantizedKey(double x, double y) {
        long qx = Math.round(x / DelaunayTriangulator.EPSILON);
        long qy = Math.round(y / DelaunayTriangulator.EPSILON);
        return (qx * 31) ^ qy;
    }

    private static void collectBoundaryEdges(
        TriangleBuffer triangles,
        List<Integer> badTriangles,
        int badTriangleStamp,
        List<BoundaryEdge> boundaryEdges
    ) {
        for (int triangleIndex : badTriangles) {
            DelaunayTriangulator.addBoundaryEdge(
                boundaryEdges,
                triangles,
                badTriangleStamp,
                triangleIndex,
                triangles.getVertexA(triangleIndex),
                triangles.getVertexB(triangleIndex),
                0
            );
            DelaunayTriangulator.addBoundaryEdge(
                boundaryEdges,
                triangles,
                badTriangleStamp,
                triangleIndex,
                triangles.getVertexB(triangleIndex),
                triangles.getVertexC(triangleIndex),
                1
            );
            DelaunayTriangulator.addBoundaryEdge(
                boundaryEdges,
                triangles,
                badTriangleStamp,
                triangleIndex,
                triangles.getVertexC(triangleIndex),
                triangles.getVertexA(triangleIndex),
                2
            );
        }
    }

    private static void addBoundaryEdge(
        List<BoundaryEdge> boundaryEdges,
        TriangleBuffer triangles,
        int badTriangleStamp,
        int triangleIndex,
        int a,
        int b,
        int side
    ) {
        int outsideNeighbor = triangles.neighbor(triangleIndex, side);
        if (outsideNeighbor != -1 && triangles.visitStamp(outsideNeighbor) == badTriangleStamp) return;
        boundaryEdges.add(new BoundaryEdge(a, b, outsideNeighbor));
    }

    private static void registerLocalEdge(
        Map<Long, EdgeReference> localEdges,
        TriangleBuffer triangles,
        int triangleIndex,
        int a,
        int b,
        int side
    ) {
        long key = DelaunayTriangulator.packEdge(a, b);
        EdgeReference other = localEdges.putIfAbsent(key, new EdgeReference(triangleIndex, side));
        if (other == null) return;
        triangles.setNeighbor(triangleIndex, side, other.triangleIndex());
        triangles.setNeighbor(other.triangleIndex(), other.side(), triangleIndex);
    }

    private static long packEdge(int a, int b) {
        int min = Math.min(a, b);
        int max = Math.max(a, b);
        return ((long) min << 32) | (max & 0xffffffffL);
    }

    private static SuperTriangle createSuperTriangle(List<Point> points) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Point point : points) {
            if (point.x() < minX) minX = point.x();
            if (point.y() < minY) minY = point.y();
            if (point.x() > maxX) maxX = point.x();
            if (point.y() > maxY) maxY = point.y();
        }
        double delta = Math.max(maxX - minX, maxY - minY);
        if (delta < DelaunayTriangulator.EPSILON) delta = 1.0;
        double midX = (minX + maxX) * 0.5;
        double midY = (minY + maxY) * 0.5;
        return new SuperTriangle(
            midX - 20 * delta, midY - delta,
            midX, midY + 20 * delta,
            midX + 20 * delta, midY - delta
        );
    }

    public record Point(int id, double x, double y) {
    }

    public record Edge(int a, int b) {
        public Edge {
            if (a > b) {
                int temp = a;
                a = b;
                b = temp;
            }
        }
    }

    private record BoundaryEdge(int a, int b, int outsideNeighbor) {
    }

    private record EdgeReference(int triangleIndex, int side) {
    }

    private record SuperTriangle(double ax, double ay, double bx, double by, double cx, double cy) {
    }

    private static final class TriangleBuffer {
        private final ArrayList<Integer> vertexA;
        private final ArrayList<Integer> vertexB;
        private final ArrayList<Integer> vertexC;
        private final ArrayList<Double> circumcenterX;
        private final ArrayList<Double> circumcenterY;
        private final ArrayList<Double> circumradiusSquared;
        private final ArrayList<Boolean> alive;
        private final ArrayList<Integer> neighborAB;
        private final ArrayList<Integer> neighborBC;
        private final ArrayList<Integer> neighborCA;
        private final ArrayList<Integer> visitStamp;
        private int nextVisitStamp = 1;

        private TriangleBuffer(int initialCapacity) {
            this.vertexA = new ArrayList<>(initialCapacity);
            this.vertexB = new ArrayList<>(initialCapacity);
            this.vertexC = new ArrayList<>(initialCapacity);
            this.circumcenterX = new ArrayList<>(initialCapacity);
            this.circumcenterY = new ArrayList<>(initialCapacity);
            this.circumradiusSquared = new ArrayList<>(initialCapacity);
            this.alive = new ArrayList<>(initialCapacity);
            this.neighborAB = new ArrayList<>(initialCapacity);
            this.neighborBC = new ArrayList<>(initialCapacity);
            this.neighborCA = new ArrayList<>(initialCapacity);
            this.visitStamp = new ArrayList<>(initialCapacity);
        }

        private int add(int a, int b, int c, List<Point> points) {
            Point p1 = points.get(a);
            Point p2 = points.get(b);
            Point p3 = points.get(c);
            double determinant = TriangleBuffer.determinant(p1, p2, p3);
            if (Math.abs(determinant) <= DelaunayTriangulator.EPSILON) return -1;
            this.vertexA.add(a);
            this.vertexB.add(b);
            this.vertexC.add(c);
            double centerX = (
                                 (TriangleBuffer.square(p1.x()) + TriangleBuffer.square(p1.y())) * (p2.y() - p3.y())
                                 + (TriangleBuffer.square(p2.x()) + TriangleBuffer.square(p2.y())) * (p3.y() - p1.y())
                                 + (TriangleBuffer.square(p3.x()) + TriangleBuffer.square(p3.y())) * (p1.y() - p2.y())
                             ) / determinant;
            double centerY = (
                                 (TriangleBuffer.square(p1.x()) + TriangleBuffer.square(p1.y())) * (p3.x() - p2.x())
                                 + (TriangleBuffer.square(p2.x()) + TriangleBuffer.square(p2.y())) * (p1.x() - p3.x())
                                 + (TriangleBuffer.square(p3.x()) + TriangleBuffer.square(p3.y())) * (p2.x() - p1.x())
                             ) / determinant;
            this.circumcenterX.add(centerX);
            this.circumcenterY.add(centerY);
            double dx = p1.x() - centerX;
            double dy = p1.y() - centerY;
            this.circumradiusSquared.add(dx * dx + dy * dy);
            this.alive.add(true);
            this.neighborAB.add(-1);
            this.neighborBC.add(-1);
            this.neighborCA.add(-1);
            this.visitStamp.add(0);
            return this.vertexA.size() - 1;
        }

        private void rebuildAdjacency() {
            HashMap<Long, EdgeReference> edges = new HashMap<>();
            for (int triangleIndex = 0; triangleIndex < this.size(); triangleIndex++) {
                if (!this.isAlive(triangleIndex)) continue;
                this.clearNeighbors(triangleIndex);
            }
            for (int triangleIndex = 0; triangleIndex < this.size(); triangleIndex++) {
                if (!this.isAlive(triangleIndex)) continue;
                this.registerEdge(edges, triangleIndex, this.getVertexA(triangleIndex), this.getVertexB(triangleIndex), 0);
                this.registerEdge(edges, triangleIndex, this.getVertexB(triangleIndex), this.getVertexC(triangleIndex), 1);
                this.registerEdge(edges, triangleIndex, this.getVertexC(triangleIndex), this.getVertexA(triangleIndex), 2);
            }
        }

        private void registerEdge(HashMap<Long, EdgeReference> edges, int triangleIndex, int a, int b, int side) {
            long key = DelaunayTriangulator.packEdge(a, b);
            EdgeReference other = edges.putIfAbsent(key, new EdgeReference(triangleIndex, side));
            if (other == null) return;
            this.setNeighbor(triangleIndex, side, other.triangleIndex());
            this.setNeighbor(other.triangleIndex(), other.side(), triangleIndex);
        }

        private void clearNeighbors(int triangleIndex) {
            this.neighborAB.set(triangleIndex, -1);
            this.neighborBC.set(triangleIndex, -1);
            this.neighborCA.set(triangleIndex, -1);
        }

        private void setNeighbor(int triangleIndex, int side, int neighbor) {
            if (side == 0) {
                this.neighborAB.set(triangleIndex, neighbor);
            } else if (side == 1) {
                this.neighborBC.set(triangleIndex, neighbor);
            } else {
                this.neighborCA.set(triangleIndex, neighbor);
            }
        }

        private int neighbor(int triangleIndex, int side) {
            if (side == 0) return this.neighborAB(triangleIndex);
            if (side == 1) return this.neighborBC(triangleIndex);
            return this.neighborCA(triangleIndex);
        }

        private void replaceNeighbor(int triangleIndex, int a, int b, int neighbor) {
            if (TriangleBuffer.matchesEdge(this.getVertexA(triangleIndex), this.getVertexB(triangleIndex), a, b)) {
                this.neighborAB.set(triangleIndex, neighbor);
                return;
            }
            if (TriangleBuffer.matchesEdge(this.getVertexB(triangleIndex), this.getVertexC(triangleIndex), a, b)) {
                this.neighborBC.set(triangleIndex, neighbor);
                return;
            }
            if (TriangleBuffer.matchesEdge(this.getVertexC(triangleIndex), this.getVertexA(triangleIndex), a, b)) {
                this.neighborCA.set(triangleIndex, neighbor);
            }
        }

        private static boolean matchesEdge(int edgeA, int edgeB, int targetA, int targetB) {
            return (edgeA == targetA && edgeB == targetB) || (edgeA == targetB && edgeB == targetA);
        }

        private int firstAliveTriangle() {
            for (int triangleIndex = 0; triangleIndex < this.size(); triangleIndex++) {
                if (this.isAlive(triangleIndex)) return triangleIndex;
            }
            return -1;
        }

        private int closestNeighbor(int triangleIndex, Point point) {
            int nextTriangle = -1;
            double bestDistanceSquared = this.squaredDistanceToCircumcenter(triangleIndex, point);
            int neighborAB = this.neighborAB(triangleIndex);
            if (neighborAB != -1 && this.isAlive(neighborAB)) {
                double distanceSquared = this.squaredDistanceToCircumcenter(neighborAB, point);
                if (distanceSquared + DelaunayTriangulator.EPSILON < bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared;
                    nextTriangle = neighborAB;
                }
            }
            int neighborBC = this.neighborBC(triangleIndex);
            if (neighborBC != -1 && this.isAlive(neighborBC)) {
                double distanceSquared = this.squaredDistanceToCircumcenter(neighborBC, point);
                if (distanceSquared + DelaunayTriangulator.EPSILON < bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared;
                    nextTriangle = neighborBC;
                }
            }
            int neighborCA = this.neighborCA(triangleIndex);
            if (neighborCA != -1 && this.isAlive(neighborCA)) {
                double distanceSquared = this.squaredDistanceToCircumcenter(neighborCA, point);
                if (distanceSquared + DelaunayTriangulator.EPSILON < bestDistanceSquared) {
                    nextTriangle = neighborCA;
                }
            }
            return nextTriangle;
        }

        private double squaredDistanceToCircumcenter(int triangleIndex, Point point) {
            double dx = point.x() - this.circumcenterX.get(triangleIndex);
            double dy = point.y() - this.circumcenterY.get(triangleIndex);
            return dx * dx + dy * dy;
        }

        private boolean containsInCircumcircle(int triangleIndex, Point point) {
            double dx = point.x() - this.circumcenterX.get(triangleIndex);
            double dy = point.y() - this.circumcenterY.get(triangleIndex);
            return dx * dx + dy * dy <= this.circumradiusSquared.get(triangleIndex) + DelaunayTriangulator.EPSILON;
        }

        private void remove(int triangleIndex) {
            this.alive.set(triangleIndex, false);
            this.clearNeighbors(triangleIndex);
        }

        private boolean isAlive(int triangleIndex) {
            return this.alive.get(triangleIndex);
        }

        private int getVertexA(int triangleIndex) {
            return this.vertexA.get(triangleIndex);
        }

        private int getVertexB(int triangleIndex) {
            return this.vertexB.get(triangleIndex);
        }

        private int getVertexC(int triangleIndex) {
            return this.vertexC.get(triangleIndex);
        }

        private int neighborAB(int triangleIndex) {
            return this.neighborAB.get(triangleIndex);
        }

        private int neighborBC(int triangleIndex) {
            return this.neighborBC.get(triangleIndex);
        }

        private int neighborCA(int triangleIndex) {
            return this.neighborCA.get(triangleIndex);
        }

        private int visitStamp(int triangleIndex) {
            return this.visitStamp.get(triangleIndex);
        }

        private void setVisitStamp(int triangleIndex, int stamp) {
            this.visitStamp.set(triangleIndex, stamp);
        }

        private int nextVisitStamp() {
            return this.nextVisitStamp++;
        }

        private int size() {
            return this.vertexA.size();
        }

        private static double determinant(Point p1, Point p2, Point p3) {
            return 2.0 * (p1.x() * (p2.y() - p3.y()) + p2.x() * (p3.y() - p1.y()) + p3.x() * (p1.y() - p2.y()));
        }

        private static double square(double value) {
            return value * value;
        }
    }
}
