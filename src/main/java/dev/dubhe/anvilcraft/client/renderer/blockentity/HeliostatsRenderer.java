package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.HeliostatsRenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class HeliostatsRenderer implements BlockEntityRenderer<HeliostatsBlockEntity, HeliostatsRenderState> {
    public static final StandaloneModelKey<BlockStateModel> HEAD = new StandaloneModelKey<>(
        () -> "AnvilCraft: Heliostats Head Model"
    );
    public static final StandaloneModelKey<BlockStateModel> HEAD_SUNFLOWER = new StandaloneModelKey<>(
        () -> "AnvilCraft: Heliostats Sunflower Head Model"
    );

    @SuppressWarnings("unused")
    public HeliostatsRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public HeliostatsRenderState createRenderState() {
        return new HeliostatsRenderState();
    }

    @Override
    public void extractRenderState(
        HeliostatsBlockEntity be,
        HeliostatsRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.setHead(FeatureRendererSupport.initialize(this.getHeadModel(be), be));
        if (
            be.getWorkResult() != HeliostatsBlockEntity.WorkResult.NO_ROTATION_ANGLE
            && !be.getNormalVector3f().equals(new Vector3f())
            && !be.getNormalVector3f().equals(new Vector3f(Float.NaN))
        ) {
            state.addRotation(new Quaternionf().rotateY(
                this.getHorizontalAngle(be.getNormalVector3f().x, be.getNormalVector3f().z)
            ));
            state.addRotation(new Quaternionf().rotateX(
                (float) Math.atan(Math.hypot(be.getNormalVector3f().z, be.getNormalVector3f().x) / be.getNormalVector3f().y)
            ));
        }
    }

    private float getHorizontalAngle(float x, float z) {
        float angle = (float) Math.atan(x / z);
        return z < 0 ? (float) (angle + Math.PI) : angle;
    }

    private StandaloneModelKey<BlockStateModel> getHeadModel(HeliostatsBlockEntity blockEntity) {
        return Optional.of(blockEntity)
                   .filter(ignore -> AnvilCraftClient.CONFIG.heliostatsSunflowerModel)
                   .filter(be -> be.getLevel() != null)
                   .map(be -> be.getLevel().getBiome(be.getBlockPos()))
                   .map(biome -> biome.is(Biomes.SUNFLOWER_PLAINS))
                   .orElse(false) ? HeliostatsRenderer.HEAD_SUNFLOWER : HeliostatsRenderer.HEAD;
    }

    @Override
    public void submit(
        HeliostatsRenderState state,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        pose.pushPose();
        pose.translate(0.5, 1.3, 0.5);
        for (Quaternionf rotation : state.getRotation()) {
            pose.mulPose(rotation);
        }
        state.getHead().submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HeliostatsBlockEntity blockEntity) {
        return AABB.ofSize(blockEntity.getBlockPos().getCenter().add(0, 0.5F, 0), 3, 2, 3);
    }

    @Override
    public int getViewDistance() {
        return AnvilCraft.CLIENT_CONFIG.heliostatsRenderDistance;
    }
}
