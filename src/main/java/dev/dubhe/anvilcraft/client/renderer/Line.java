package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;

public record Line(Vec3 start, Vec3 end, float length) {

    public void render(PoseStack pose, VertexConsumer vertex, Vec3 camera, int color) {
        render(pose.last(), vertex, camera, color);
    }

    public void render(PoseStack.Pose pose, VertexConsumer vertex, Vec3 camera, int color) {
        float dx = (float) (this.start().x - this.end().x);
        float dy = (float) (this.start().y - this.end().y);
        float dz = (float) (this.start().z - this.end().z);
        vertex.addVertex(
                pose.pose(),
                (float) (this.start().x - camera.x),
                (float) (this.start().y - camera.y),
                (float) (this.start().z - camera.z)
            ).setColor(color)
            .setNormal(pose, dx /= this.length(), dy /= this.length(), dz /= this.length());
        vertex.addVertex(
                pose.pose(),
                (float) (this.end().x - camera.x),
                (float) (this.end().y - camera.y),
                (float) (this.end().z - camera.z)
            ).setColor(color)
            .setNormal(pose, dx, dy, dz);
    }
}