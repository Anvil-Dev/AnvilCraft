package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Matrix4f;

import java.util.EnumSet;

public final class EnergyWeaponArmRenderer {
    private static final ArmMesh[] MESHES = {
        new ArmMesh(false, false), new ArmMesh(true, false), new ArmMesh(false, true), new ArmMesh(true, true)
    };

    private EnergyWeaponArmRenderer() {
    }

    public static void render(
        PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player, HumanoidArm arm, Matrix4f transform
    ) {
        boolean left = arm == HumanoidArm.LEFT;
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        ArmMesh mesh = MESHES[(slim ? 2 : 0) + (left ? 1 : 0)];
        boolean sleeve = player.isModelPartShown(left ? PlayerModelPart.LEFT_SLEEVE : PlayerModelPart.RIGHT_SLEEVE);
        renderArm(pose, buffer, light, player, transform, mesh.skin, mesh.sleeve, sleeve);
    }

    private static void renderArm(
        PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player, Matrix4f transform,
        ModelPart.Cube skin, ModelPart.Cube sleeve, boolean showSleeve
    ) {
        pose.pushPose();
        pose.mulPose(transform);
        skin.compile(pose.last(), buffer.getBuffer(RenderType.entitySolid(player.getSkin().texture())),
            light, OverlayTexture.NO_OVERLAY, -1);
        if (showSleeve) {
            sleeve.compile(pose.last(), buffer.getBuffer(RenderType.entityTranslucent(player.getSkin().texture())),
                light, OverlayTexture.NO_OVERLAY, -1);
        }
        pose.popPose();
    }

    private static ModelPart.Cube arm(int u, int v, float width, float inflate) {
        return new ModelPart.Cube(u, v, -width / 2, -10, -2,
            width, 12, 4, inflate, inflate, inflate, false, 64, 64, EnumSet.allOf(Direction.class));
    }

    private static final class ArmMesh {
        private final ModelPart.Cube skin;
        private final ModelPart.Cube sleeve;

        private ArmMesh(boolean left, boolean slim) {
            float width = slim ? 3 : 4;
            this.skin = arm(left ? 32 : 40, left ? 48 : 16, width, 0);
            this.sleeve = arm(left ? 48 : 40, left ? 48 : 32, width, 0.25F);
        }
    }
}
