package dev.dubhe.anvilcraft.util.algo;

import dev.dubhe.anvilcraft.util.Line;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MinimumSpanningTree3D {

    private static class Edge implements Comparable<Edge> {
        int u, v;
        double weight;
        Edge(int u, int v, double weight) { this.u = u; this.v = v; this.weight = weight; }
        @Override
        public int compareTo(Edge o) { return Double.compare(this.weight, o.weight); }
    }

    private static class UnionFind {
        int[] parent, rank;
        UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            for (int i = 0; i < n; i++) parent[i] = i;
        }
        int find(int x) {
            if (parent[x] != x) parent[x] = find(parent[x]);
            return parent[x];
        }
        void union(int x, int y) {
            int xr = find(x), yr = find(y);
            if (xr == yr) return;
            if (rank[xr] < rank[yr]) parent[xr] = yr;
            else {
                parent[yr] = xr;
                if (rank[xr] == rank[yr]) rank[xr]++;
            }
        }
    }

    public static List<Line> kruskalMST(List<Vec3> points) {
        int n = points.size();
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                edges.add(new Edge(i, j, distance(points.get(i), points.get(j))));
            }
        }
        Collections.sort(edges);
        UnionFind uf = new UnionFind(n);
        List<Line> result = new ArrayList<>();
        for (Edge e : edges) {
            int pu = uf.find(e.u), pv = uf.find(e.v);
            if (pu != pv) {
                uf.union(pu, pv);
                result.add(new Line(points.get(e.u), points.get(e.v), (float) e.weight));
                if (result.size() == n - 1) break;
            }
        }
        return result;
    }

    private static double distance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dy = a.y - b.y, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}