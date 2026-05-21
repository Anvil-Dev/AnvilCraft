package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.HasMobBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.HasMobBlockRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.NewMinecartBehavior;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

public class HasMobBlockRenderer implements BlockEntityRenderer<HasMobBlockEntity, HasMobBlockRenderState> {
    private final EntityRenderDispatcher dispatcher;

    @SuppressWarnings("unused")
    public HasMobBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.entityRenderer();
    }

    @Override
    public HasMobBlockRenderState createRenderState() {
        return new HasMobBlockRenderState();
    }

    @Override
    public void extractRenderState(
        HasMobBlockEntity be,
        HasMobBlockRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        Entity entity = be.getOrCreateDisplayEntity(be.getLevel());
        if (entity == null) return;
        state.setMob(new EntityRenderState());
        this.extractEntityState(entity, state.getMob(), partialTicks);
    }

    @Override
    public void submit(
        HasMobBlockRenderState state,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        EntityRenderState mob = state.getMob();
        if (mob == null) return;
        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        float size = 0.73125F;
        float max = Math.max(mob.boundingBoxWidth, mob.boundingBoxHeight);
        if ((double) max > 1.0) size /= max;
        pose.translate(0.0F, 0.14F, 0.0F);
        pose.scale(size, size, size);
        this.dispatcher.submit(
            mob,
            camera,
            state.blockPos.getX(),
            state.blockPos.getY(),
            state.blockPos.getZ(),
            pose,
            collector
        );
        pose.popPose();
    }

    private void extractEntityState(Entity entity, EntityRenderState state, float partialTicks) {
        state.entityType = entity.getType();
        state.x = Mth.lerp(partialTicks, entity.xOld, entity.getX());
        state.y = Mth.lerp(partialTicks, entity.yOld, entity.getY());
        state.z = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
        state.isInvisible = entity.isInvisible();
        state.partialTick = partialTicks;
        state.ageInTicks = entity.tickCount + partialTicks;
        state.boundingBoxWidth = entity.getBbWidth();
        state.boundingBoxHeight = entity.getBbHeight();
        state.eyeHeight = entity.getEyeHeight();
        if (entity.isPassenger()
            && entity.getVehicle() instanceof AbstractMinecart minecart
            && minecart.getBehavior() instanceof NewMinecartBehavior behavior
            && behavior.cartHasPosRotLerp()
        ) {
            double cartLerpX = Mth.lerp(partialTicks, minecart.xOld, minecart.getX());
            double cartLerpY = Mth.lerp(partialTicks, minecart.yOld, minecart.getY());
            double cartLerpZ = Mth.lerp(partialTicks, minecart.zOld, minecart.getZ());
            state.passengerOffset = behavior.getCartLerpPosition(partialTicks).subtract(new Vec3(cartLerpX, cartLerpY, cartLerpZ));
        } else {
            state.passengerOffset = null;
        }

        if (this.dispatcher.camera != null) {
            state.distanceToCameraSq = this.dispatcher.distanceToSqr(entity);
            state.nameTag = null;

            if (state.distanceToCameraSq < 100.0) {
                state.scoreText = entity.belowNameDisplay();
            } else {
                state.scoreText = null;
            }
        }

        label77: {
            state.isDiscrete = entity.isDiscrete();
            Level level = entity.level();
            if (entity instanceof Leashable leashable) {
                Entity entityYRot = leashable.getLeashHolder();
                if (entityYRot instanceof Entity) {
                    float entityYRotx = entity.getPreciseBodyRotation(partialTicks) * (float) (Math.PI / 180.0);
                    Vec3 attachOffset = leashable.getLeashOffset(partialTicks);
                    BlockPos entityEyePos = BlockPos.containing(entity.getEyePosition(partialTicks));
                    BlockPos roperEyePos = BlockPos.containing(entityYRot.getEyePosition(partialTicks));
                    int startBlockLight = entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.BLOCK, entityEyePos);
                    int endBlockLight = entityYRot.isOnFire() ? 15 : entityYRot.level().getBrightness(LightLayer.BLOCK, roperEyePos);
                    int startSkyLight = level.getBrightness(LightLayer.SKY, entityEyePos);
                    int endSkyLight = level.getBrightness(LightLayer.SKY, roperEyePos);
                    boolean quadConnection = entityYRot.supportQuadLeashAsHolder() && leashable.supportQuadLeash();
                    int leashCount = quadConnection ? 4 : 1;
                    if (state.leashStates == null || state.leashStates.size() != leashCount) {
                        state.leashStates = new ArrayList<>(leashCount);

                        for (int i = 0; i < leashCount; i++) {
                            state.leashStates.add(new EntityRenderState.LeashState());
                        }
                    }

                    if (quadConnection) {
                        float roperYRot = entityYRot.getPreciseBodyRotation(partialTicks) * (float) (Math.PI / 180.0);
                        Vec3 holderPos = entityYRot.getPosition(partialTicks);
                        Vec3[] leashableAttachmentPoints = leashable.getQuadLeashOffsets();
                        Vec3[] roperAttachmentPoints = entityYRot.getQuadLeashHolderOffsets();
                        int i = 0;

                        while (true) {
                            if (i >= leashCount) {
                                break label77;
                            }

                            EntityRenderState.LeashState leashState = state.leashStates.get(i);
                            leashState.offset = leashableAttachmentPoints[i].yRot(-entityYRotx);
                            leashState.start = entity.getPosition(partialTicks).add(leashState.offset);
                            leashState.end = holderPos.add(roperAttachmentPoints[i].yRot(-roperYRot));
                            leashState.startBlockLight = startBlockLight;
                            leashState.endBlockLight = endBlockLight;
                            leashState.startSkyLight = startSkyLight;
                            leashState.endSkyLight = endSkyLight;
                            leashState.slack = false;
                            i++;
                        }
                    } else {
                        Vec3 rotatedAttachOffset = attachOffset.yRot(-entityYRotx);
                        EntityRenderState.LeashState leashState = state.leashStates.getFirst();
                        leashState.offset = rotatedAttachOffset;
                        leashState.start = entity.getPosition(partialTicks).add(rotatedAttachOffset);
                        leashState.end = entityYRot.getRopeHoldPosition(partialTicks);
                        leashState.startBlockLight = startBlockLight;
                        leashState.endBlockLight = endBlockLight;
                        leashState.startSkyLight = startSkyLight;
                        leashState.endSkyLight = endSkyLight;
                        break label77;
                    }
                }
            }

            state.leashStates = null;
        }

        state.displayFireAnimation = entity.displayFireAnimation();
        Minecraft minecraft = Minecraft.getInstance();
        boolean appearsGlowing = minecraft.shouldEntityAppearGlowing(entity);
        state.outlineColor = appearsGlowing ? ARGB.opaque(entity.getTeamColor()) : 0;
        BlockPos blockPos = BlockPos.containing(entity.getLightProbePosition(partialTicks));
        state.lightCoords = LightCoordsUtil.pack(
            entity.isOnFire() ? 15 : entity.level().getBrightness(LightLayer.BLOCK, blockPos),
            entity.level().getBrightness(LightLayer.SKY, blockPos)
        );
    }
}
