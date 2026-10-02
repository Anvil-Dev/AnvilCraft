package dev.dubhe.anvilcraft.client.selection;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.anvilcraft.lib.v2.cube.geometry.ConvexShape;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class ModelPartSelection {
    private final Map<ModelPart.Cube, SelectionGeometry> cubes = new IdentityHashMap<>();

    public void collect(ModelPart root, PoseStack pose, List<SelectionPart> output) {
        root.visit(pose, (partPose, path, index, cube) -> output.add(new SelectionPart(
            this.cubes.computeIfAbsent(cube, ModelPartSelection::geometry),
            new Matrix4f(partPose.pose()).scale(1 / ModelCubeGeometry.SCALE)
        )));
    }

    private static SelectionGeometry geometry(ModelPart.Cube cube) {
        double scale = ModelCubeGeometry.SCALE / 16.0;
        AABB bounds = new AABB(cube.minX * scale, cube.minY * scale, cube.minZ * scale,
            cube.maxX * scale, cube.maxY * scale, cube.maxZ * scale);
        bounds = bounds.inflate(Math.max(0, 0.0128 - bounds.getXsize()) * 0.5,
            Math.max(0, 0.0128 - bounds.getYsize()) * 0.5, Math.max(0, 0.0128 - bounds.getZsize()) * 0.5);
        return new SelectionGeometry(List.of(ConvexShape.box(bounds)));
    }
}
