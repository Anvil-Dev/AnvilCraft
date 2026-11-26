package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.utility.redstone.ButtonMount;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

final class MountedModelFootprint {
    private final AABB.Builder bounds = new AABB.Builder();
    private double floor = Double.POSITIVE_INFINITY;
    private double minX;
    private double maxX;
    private double minZ;
    private double maxZ;

    void add(BakedQuad quad, Vec3 offset) {
        var corners = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            Vector3fc p = quad.position(i);
            corners[i] = offset.add(p.x(), p.y(), p.z());
            this.bounds.include(corners[i].toVector3f());
        }
        double low = Math.min(Math.min(corners[0].y, corners[1].y), Math.min(corners[2].y, corners[3].y));
        if (low > this.floor + 1E-6) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            var a = corners[i];
            var b = corners[(i + 1) % 4];
            if (Math.abs(a.y - low) < 1E-6 && Math.abs(b.y - low) < 1E-6
                && Math.abs(a.x - b.x) > 1E-6 && Math.abs(a.z - b.z) > 1E-6) {
                var top = corners[(i + 3) % 4];
                if (top.y > low && Math.abs(top.x - a.x) < 1E-6 && Math.abs(top.z - a.z) < 1E-6) {
                    this.addCrossedFace(quad, i, a, b, top);
                    return;
                }
            }
        }
        for (Vec3 p : corners) {
            this.include(p);
        }
    }

    // 花草的交叉面片往往远宽于根部，只以第一行可见纹素的边缘作为脚部。
    private void addCrossedFace(BakedQuad quad, int i, Vec3 a, Vec3 b, Vec3 top) {
        var sprite = quad.materialInfo().sprite();
        var contents = sprite.contents();
        int w = contents.width();
        int h = contents.height();
        double[] u = new double[3];
        double[] v = new double[3];
        for (int j = 0; j < 3; j++) {
            long uv = quad.packedUV((i + (j == 2 ? 3 : j)) % 4);
            u[j] = (UVPair.unpackU(uv) - sprite.getU0()) / (sprite.getU1() - sprite.getU0()) * w;
            v[j] = (UVPair.unpackV(uv) - sprite.getV0()) / (sprite.getV1() - sprite.getV0()) * h;
        }
        int columns = Math.max(1, (int) Math.round(Math.hypot(u[1] - u[0], v[1] - v[0])));
        int rows = Math.max(1, (int) Math.round(Math.hypot(u[2] - u[0], v[2] - v[0])));
        for (int y = 0; y < rows; y++) {
            int first = columns;
            int last = -1;
            for (int x = 0; x < columns; x++) {
                double s = (x + 0.5) / columns;
                double t = (y + 0.5) / rows;
                int px = Mth.clamp(Mth.floor(u[0] + s * (u[1] - u[0]) + t * (u[2] - u[0])), 0, w - 1);
                int py = Mth.clamp(Mth.floor(v[0] + s * (v[1] - v[0]) + t * (v[2] - v[0])), 0, h - 1);
                if (!contents.isTransparent(0, px, py)) {
                    first = Math.min(first, x);
                    last = x;
                }
            }
            if (last >= first) {
                Vec3 start = a.lerp(top, (double) y / rows);
                this.include(start.add(b.subtract(a).scale((double) first / columns)));
                this.include(start.add(b.subtract(a).scale((double) (last + 1) / columns)));
                return;
            }
        }
    }

    private void include(Vec3 p) {
        if (p.y < this.floor - 1E-6) {
            this.floor = p.y;
            this.minX = this.maxX = p.x;
            this.minZ = this.maxZ = p.z;
        } else if (p.y <= this.floor + 1E-6) {
            this.minX = Math.min(this.minX, p.x);
            this.minZ = Math.min(this.minZ, p.z);
            this.maxX = Math.max(this.maxX, p.x);
            this.maxZ = Math.max(this.maxZ, p.z);
        }
    }

    MountedBlockProjection.Fit fit(ButtonMount fallback) {
        double width = Math.max(this.maxX - this.minX, this.maxZ - this.minZ);
        if (!Double.isFinite(this.floor) || width < 1E-6) {
            return MountedBlockProjection.Fit.fromMount(fallback);
        }
        Vec3 anchor = new Vec3((this.minX + this.maxX) / 2, this.floor, (this.minZ + this.maxZ) / 2);
        double scale = 0.5 / width;
        AABB box = this.bounds.build().move(anchor.scale(-1));
        AABB fitted = new AABB(box.minX * scale, box.minY * scale, box.minZ * scale,
            box.maxX * scale, box.maxY * scale, box.maxZ * scale).move(0.5, ButtonMount.FLOOR, 0.5);
        return new MountedBlockProjection.Fit(anchor, scale, fitted);
    }
}
