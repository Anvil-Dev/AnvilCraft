package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class CelestialAnvilItemExtensions implements IClientItemExtensions {
    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProgress, float swingProgress) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float swing = Mth.sin(Mth.sqrt(swingProgress) * Mth.PI);
        pose.translate(side * (0.42F - swing * 0.08F), -0.5F - equipProgress * 0.6F, -0.8F - swing * 0.15F);
        if (!player.isInvisible()) {
            var client = Minecraft.getInstance();
            final var renderer = client.getEntityRenderDispatcher().getPlayerRenderer(player);
            final var collector = client.gameRenderer.getFeatureRenderDispatcher().getSubmitNodeStorage();
            final var skin = player.getSkin().body().texturePath();
            final int light = LevelRenderer.getLightCoords(LevelRenderer.BrightnessGetter.DEFAULT, player.level(),
                player.level().getBlockState(player.blockPosition()), player.blockPosition());
            pose.pushPose();
            pose.translate(side * -0.4F, 0.07F, 0.75F);
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.mulPose(Axis.YP.rotationDegrees(180));
            if (arm == HumanoidArm.RIGHT) {
                renderer.renderRightHand(pose, collector, light, skin, player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE), player);
            } else {
                renderer.renderLeftHand(pose, collector, light, skin, player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE), player);
            }
            pose.popPose();
        }
        return true;
    }
}
