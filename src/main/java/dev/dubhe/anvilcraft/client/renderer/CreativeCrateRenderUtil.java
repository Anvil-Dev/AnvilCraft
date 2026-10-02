package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public final class CreativeCrateRenderUtil {
    private CreativeCrateRenderUtil() {
    }

    public static void submit(
        ItemStackRenderState item, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outline
    ) {
        for (int side = 0; side < 6; side++) {
            pose.pushPose();
            applyFace(pose, side);
            item.submit(pose, collector, light, overlay, outline);
            pose.popPose();
        }
    }

    public static void applyFace(PoseStack pose, int side) {
        switch (side) {
            case 0 -> pose.translate(0.5, 0.5, 0.9);
            case 1 -> pose.translate(0.5, 0.5, 0.1);
            case 2 -> pose.translate(0.9, 0.5, 0.5);
            case 3 -> pose.translate(0.1, 0.5, 0.5);
            case 4 -> pose.translate(0.5, 0.1, 0.5);
            case 5 -> pose.translate(0.5, 0.9, 0.5);
            default -> throw new IllegalArgumentException("Invalid crate face: " + side);
        }
        pose.scale(0.8F, 0.8F, 0.8F);
        if (side == 2 || side == 3) pose.mulPose(Axis.YP.rotationDegrees(90));
        if (side == 4 || side == 5) pose.mulPose(Axis.XP.rotationDegrees(90));
        if (side == 5) pose.mulPose(Axis.ZP.rotationDegrees(180));
    }
}
