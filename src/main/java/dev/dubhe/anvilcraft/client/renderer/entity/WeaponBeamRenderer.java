package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CorruptedBeaconRenderer;
import dev.dubhe.anvilcraft.client.renderer.laser.LaserCompiler;
import dev.dubhe.anvilcraft.entity.WeaponBeamEntity;
import dev.dubhe.anvilcraft.item.weapon.CorruptedBeaconActivatorItem;
import dev.dubhe.anvilcraft.item.weapon.LaserGunItem;
import dev.dubhe.anvilcraft.util.WeaponRaycastUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class WeaponBeamRenderer extends EntityRenderer<WeaponBeamEntity> {
    private static final float VIEW_BOB_COMPENSATION = 0.65f;

    public WeaponBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(
        WeaponBeamEntity entity,
        float yaw,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int light
    ) {
        if (isObsoleteContinuousBeam(entity)) return;
        if (!isOwnerFiringContinuousBeam(entity)) return;
        Vec3 end = entity.getEndOffset();
        Vec3 originOffset = Vec3.ZERO;
        if (entity.getStyle() == WeaponBeamEntity.CORRUPTED || entity.getStyle() == WeaponBeamEntity.LASER) {
            LiveBeam liveBeam = resolveLiveBeam(entity, partialTick);
            if (liveBeam != null) {
                originOffset = liveBeam.start().subtract(entity.getPosition(partialTick));
                end = liveBeam.end().subtract(liveBeam.start());
            }
        }
        if (end.lengthSqr() < 1.0E-6) return;
        Entity owner = entity.getOwner();
        Minecraft minecraft = Minecraft.getInstance();
        poseStack.pushPose();
        if (owner == minecraft.player
            && minecraft.options.getCameraType().isFirstPerson()
            && minecraft.options.bobView().get()
            && owner instanceof Player player) {
            counterViewBob(poseStack, player, partialTick);
        }
        poseStack.translate(originOffset.x, originOffset.y, originOffset.z);
        switch (entity.getStyle()) {
            case WeaponBeamEntity.CORRUPTED -> renderCorruptedBeam(end, poseStack, buffers);
            case WeaponBeamEntity.LASER -> renderLaserBeam(end, entity.getStrength(), poseStack, buffers);
            default -> renderTeslaArc(entity, end, poseStack, buffers);
        }
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    private static boolean isObsoleteContinuousBeam(WeaponBeamEntity beam) {
        if (beam.getStyle() != WeaponBeamEntity.CORRUPTED && beam.getStyle() != WeaponBeamEntity.LASER) return false;
        Entity owner = beam.getOwner();
        var searchArea = owner == null ? beam.getBoundingBox().inflate(3.0) : owner.getBoundingBox().inflate(3.0);
        return !beam.level().getEntitiesOfClass(
            WeaponBeamEntity.class,
            searchArea,
            other -> other != beam
                && other.getId() > beam.getId()
                && other.getStyle() == beam.getStyle()
                && other.getOwnerId() == beam.getOwnerId()
        ).isEmpty();
    }

    private static boolean isOwnerFiringContinuousBeam(WeaponBeamEntity beam) {
        if (beam.getStyle() != WeaponBeamEntity.CORRUPTED && beam.getStyle() != WeaponBeamEntity.LASER) return true;
        Entity owner = beam.getOwner();
        if (!(owner instanceof Player player) || !player.isUsingItem()) return false;
        return beam.getStyle() == WeaponBeamEntity.LASER
            ? player.getUseItem().getItem() instanceof LaserGunItem
            : player.getUseItem().getItem() instanceof CorruptedBeaconActivatorItem;
    }

    private static void counterViewBob(PoseStack poseStack, Player player, float partialTick) {
        ViewBobCompensation compensation = createViewBobCompensation(player, partialTick);
        poseStack.last().pose().set(compensation.pose.mul(poseStack.last().pose(), new Matrix4f()));
        poseStack.last().normal().set(compensation.normal.mul(poseStack.last().normal(), new Matrix3f()));
    }

    private static ViewBobCompensation createViewBobCompensation(Player player, float partialTick) {
        float walkDelta = player.walkDist - player.walkDistO;
        float phase = -(player.walkDist + walkDelta * partialTick);
        float bob = Mth.lerp(partialTick, player.oBob, player.bob) * VIEW_BOB_COMPENSATION;
        float translateX = Mth.sin(phase * (float) Math.PI) * bob * 0.5f;
        float translateY = -Math.abs(Mth.cos(phase * (float) Math.PI) * bob);
        float rotateZ = Mth.sin(phase * (float) Math.PI) * bob * 3.0f;
        float rotateX = Math.abs(Mth.cos(phase * (float) Math.PI - 0.2f) * bob) * 5.0f;

        Quaternionf cameraToWorld = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        Quaternionf worldToCamera = cameraToWorld.conjugate(new Quaternionf());
        Matrix4f inverseCameraBob = new Matrix4f()
            .rotateX((float) Math.toRadians(-rotateX))
            .rotateZ((float) Math.toRadians(-rotateZ))
            .translate(-translateX, -translateY, 0.0f);
        Matrix4f worldCompensation = new Matrix4f()
            .rotate(cameraToWorld)
            .mul(inverseCameraBob)
            .rotate(worldToCamera);

        Matrix3f inverseCameraRotation = new Matrix3f()
            .rotateX((float) Math.toRadians(-rotateX))
            .rotateZ((float) Math.toRadians(-rotateZ));
        Matrix3f worldRotation = new Matrix3f()
            .rotate(cameraToWorld)
            .mul(inverseCameraRotation)
            .rotate(worldToCamera);
        return new ViewBobCompensation(worldCompensation, worldRotation);
    }

    private static LiveBeam resolveLiveBeam(WeaponBeamEntity beam, float partialTick) {
        Entity owner = beam.getOwner();
        if (!(owner instanceof Player player)) return null;
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 eye = owner == minecraft.player && minecraft.options.getCameraType().isFirstPerson()
            ? minecraft.gameRenderer.getMainCamera().getPosition()
            : player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        Vec3 start = WeaponRaycastUtil.visualStart(
            player, eye, partialTick, WeaponRaycastUtil.MUZZLE_RIGHT_OFFSET);
        double range = beam.getStyle() == WeaponBeamEntity.CORRUPTED ? 64.0 : 48.0;
        Vec3 rayEnd = eye.add(look.scale(range));
        WeaponRaycastUtil.Ray ray = new WeaponRaycastUtil.Ray(eye, rayEnd);
        Vec3 end = WeaponRaycastUtil.laserBlockHit(player.level(), player, ray).getLocation();
        return new LiveBeam(start, end);
    }

    private static void renderLaserBeam(
        Vec3 end,
        int laserLevel,
        PoseStack poseStack,
        MultiBufferSource buffers
    ) {
        poseStack.pushPose();
        rotateLocalYTo(end, poseStack);
        LaserCompiler.compileWeaponBeam(
            poseStack.last(),
            (float) end.length(),
            laserLevel,
            buffers::getBuffer
        );
        poseStack.popPose();
    }

    private static void renderCorruptedBeam(Vec3 end, PoseStack poseStack, MultiBufferSource buffers) {
        poseStack.pushPose();
        rotateLocalYTo(end, poseStack);
        poseStack.scale(0.5f, 1.0f, 0.5f);
        CorruptedBeaconRenderer.renderBeam(
            buffers.getBuffer(ModRenderTypes.CORRUPTED_BEACON_BEAM),
            poseStack.last(),
            0.0f,
            0.0f,
            0.0f,
            (float) end.length(),
            0.5f
        );
        poseStack.popPose();
    }

    private static void rotateLocalYTo(Vec3 end, PoseStack poseStack) {
        Vector3f direction = new Vector3f((float) end.x, (float) end.y, (float) end.z).normalize();
        poseStack.mulPose(new Quaternionf().rotationTo(new Vector3f(0.0f, 1.0f, 0.0f), direction));
    }

    private static void renderTeslaArc(
        WeaponBeamEntity entity,
        Vec3 end,
        PoseStack poseStack,
        MultiBufferSource buffers
    ) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(entity.position());
        Vec3 direction = end.normalize();
        Vec3 side = direction.cross(camera.subtract(end.scale(0.5)).normalize()).normalize().scale(0.12F);
        float[] color = new float[]{0.6F, 0.7F, 1.0F, 0.85F};
        VertexConsumer consumer = buffers.getBuffer(ModRenderTypes.LIGHTNING);
        Matrix4f matrix = poseStack.last().pose();
        vertex(consumer, matrix, side.reverse(), color, 0, 0);
        vertex(consumer, matrix, end.subtract(side), color, 1, 0);
        vertex(consumer, matrix, end.add(side), color, 1, 1);
        vertex(consumer, matrix, side, color, 0, 1);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 pos, float[] color, float u, float v) {
        consumer.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
            .setColor(color[0], color[1], color[2], color[3])
            .setUv(u, v)
            .setUv1(0, 0)
            .setUv2(240, 240)
            .setNormal(0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(WeaponBeamEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private record LiveBeam(Vec3 start, Vec3 end) {
    }

    private record ViewBobCompensation(Matrix4f pose, Matrix3f normal) {
    }
}
