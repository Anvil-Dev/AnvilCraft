package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.client.init.ModAtlasIds;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CorruptedBeaconRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.LaserRenderState;
import dev.dubhe.anvilcraft.client.renderer.entity.state.WeaponBeamRenderState;
import dev.dubhe.anvilcraft.client.renderer.laser.LaserCompiler;
import dev.dubhe.anvilcraft.entity.WeaponBeamEntity;
import dev.dubhe.anvilcraft.item.weapon.CorruptedBeaconActivatorItem;
import dev.dubhe.anvilcraft.item.weapon.LaserGunItem;
import dev.dubhe.anvilcraft.util.WeaponRaycastUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class WeaponBeamRenderer extends EntityRenderer<WeaponBeamEntity, WeaponBeamRenderState> {
    private static final float VIEW_BOB_COMPENSATION = 0.65F;

    public WeaponBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected boolean affectedByCulling(WeaponBeamEntity entity) {
        return false;
    }

    @Override
    public WeaponBeamRenderState createRenderState() {
        return new WeaponBeamRenderState();
    }

    @Override
    public void extractRenderState(WeaponBeamEntity entity, WeaponBeamRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.setVisible(false);
        state.setCompensateViewBob(false);
        if (isObsoleteContinuousBeam(entity) || !isOwnerFiringContinuousBeam(entity)) return;

        Vec3 origin = entity.getPosition(partialTick);
        Vec3 originOffset = Vec3.ZERO;
        Vec3 endOffset = entity.getEndOffset();
        if (entity.getStyle() == WeaponBeamEntity.CORRUPTED || entity.getStyle() == WeaponBeamEntity.LASER) {
            LiveBeam liveBeam = resolveLiveBeam(entity, partialTick);
            if (liveBeam != null) {
                originOffset = liveBeam.start().subtract(origin);
                endOffset = liveBeam.end().subtract(liveBeam.start());
            }
        }
        if (endOffset.lengthSqr() < 1.0E-6) return;

        state.setVisible(true);
        state.setStyle(entity.getStyle());
        state.setOrigin(origin);
        state.setOriginOffset(originOffset);
        state.setEndOffset(endOffset);
        Minecraft minecraft = Minecraft.getInstance();
        state.setCompensateViewBob(
            entity.getOwner() == minecraft.player
                && minecraft.options.getCameraType().isFirstPerson()
                && minecraft.options.bobView().get()
        );
        if (entity.getStyle() == WeaponBeamEntity.LASER) {
            state.setLaser(createLaserState(endOffset, entity.getStrength()));
        } else {
            state.setLaser(null);
        }
    }

    private static LaserRenderState createLaserState(Vec3 end, int strength) {
        LaserRenderState laser = new LaserRenderState();
        final TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(ModAtlasIds.LASER);
        laser.length = (float) end.length();
        laser.offset = 0.0F;
        laser.color = 0x00FF0D0D;
        laser.laserLevel = strength;
        laser.laserAtlasSprite = atlas.getSprite(LaserRenderState.LASER_TEXTURE);
        laser.solidAtlasSprite = atlas.getSprite(LaserRenderState.SOLID_TEXTURE);
        return laser;
    }

    @Override
    public void submit(
        WeaponBeamRenderState state,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        if (!state.isVisible()) return;
        Vec3 end = state.getEndOffset();
        ViewBobCompensation compensation = state.isCompensateViewBob()
            ? createViewBobCompensation(camera)
            : null;
        if (state.getStyle() == WeaponBeamEntity.CORRUPTED) {
            Vec3 start = state.getOrigin().add(state.getOriginOffset());
            CorruptedBeaconRenderer.deferWeaponBeam(
                start,
                start.add(end),
                compensation == null ? null : compensation.pose()
            );
            return;
        }

        pose.pushPose();
        if (compensation != null) {
            pose.last().pose().set(compensation.pose().mul(pose.last().pose(), new Matrix4f()));
            pose.last().normal().set(compensation.normal().mul(pose.last().normal(), new Matrix3f()));
        }
        pose.translate(state.getOriginOffset().x, state.getOriginOffset().y, state.getOriginOffset().z);
        if (state.getStyle() == WeaponBeamEntity.LASER) {
            rotateLocalYTo(end, pose);
            if (state.getLaser() != null) LaserCompiler.submit(pose, state.getLaser(), collector, false);
        } else {
            submitTeslaArc(state, end, pose, collector, camera);
        }
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    private static ViewBobCompensation createViewBobCompensation(CameraRenderState camera) {
        float phase = camera.entityRenderState.backwardsInterpolatedWalkDistance;
        float bob = camera.entityRenderState.bob * VIEW_BOB_COMPENSATION;
        float translateX = Mth.sin(phase * (float) Math.PI) * bob * 0.5F;
        float translateY = -Math.abs(Mth.cos(phase * (float) Math.PI) * bob);
        float rotateZ = Mth.sin(phase * (float) Math.PI) * bob * 3.0F;
        float rotateX = Math.abs(Mth.cos(phase * (float) Math.PI - 0.2F) * bob) * 5.0F;

        Quaternionf cameraToWorld = camera.orientation;
        Quaternionf worldToCamera = cameraToWorld.conjugate(new Quaternionf());
        Matrix4f inverseCameraBob = new Matrix4f()
            .rotateX((float) Math.toRadians(-rotateX))
            .rotateZ((float) Math.toRadians(-rotateZ))
            .translate(-translateX, -translateY, 0.0F);
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

    private static void rotateLocalYTo(Vec3 end, PoseStack pose) {
        Vector3f direction = end.toVector3f().normalize();
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), direction));
    }

    private static void submitTeslaArc(
        WeaponBeamRenderState state,
        Vec3 end,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        Vec3 cameraOffset = camera.pos.subtract(state.getOrigin().add(state.getOriginOffset()));
        Vec3 direction = end.normalize();
        Vec3 side = direction.cross(cameraOffset.subtract(end.scale(0.5)).normalize()).normalize().scale(0.12F);
        collector.submitCustomGeometry(
            pose,
            ModRenderTypes.LIGHTNING,
            (last, consumer) -> {
                Matrix4f matrix = last.pose();
                vertex(consumer, matrix, side.reverse(), 0.0F, 0.0F);
                vertex(consumer, matrix, end.subtract(side), 1.0F, 0.0F);
                vertex(consumer, matrix, end.add(side), 1.0F, 1.0F);
                vertex(consumer, matrix, side, 0.0F, 1.0F);
            }
        );
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 pos, float u, float v) {
        consumer.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
            .setColor(0.6F, 0.7F, 1.0F, 0.85F)
            .setUv(u, v)
            .setUv1(0, 0)
            .setUv2(240, 240)
            .setNormal(0, 1, 0);
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

    private static LiveBeam resolveLiveBeam(WeaponBeamEntity beam, float partialTick) {
        Entity owner = beam.getOwner();
        if (!(owner instanceof Player player)) return null;
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 eye = owner == minecraft.player && minecraft.options.getCameraType().isFirstPerson()
            ? minecraft.gameRenderer.getMainCamera().position()
            : player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        Vec3 start = WeaponRaycastUtil.visualStart(
            player, eye, partialTick, WeaponRaycastUtil.MUZZLE_RIGHT_OFFSET);
        double range = beam.getStyle() == WeaponBeamEntity.CORRUPTED ? 64.0 : 48.0;
        WeaponRaycastUtil.Ray ray = new WeaponRaycastUtil.Ray(eye, eye.add(look.scale(range)));
        Vec3 end = WeaponRaycastUtil.laserBlockHit(player.level(), player, ray).getLocation();
        return new LiveBeam(start, end);
    }

    private record LiveBeam(Vec3 start, Vec3 end) {
    }

    private record ViewBobCompensation(Matrix4f pose, Matrix3f normal) {
    }
}
