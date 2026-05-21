package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;

public class HeliostatsRenderer implements BlockEntityRenderer<HeliostatsBlockEntity> {
    private static final ModelResourceLocation HELIOSTATS_HEAD = ModelResourceLocation.standalone(AnvilCraft.of("block/heliostats_head"));
    private static final ModelResourceLocation HELIOSTATS_HEAD_SUNFLOWER = ModelResourceLocation.standalone(AnvilCraft.of(
        "block/heliostats_head_sunflower"));

    @SuppressWarnings("unused")
    public HeliostatsRenderer(BlockEntityRendererProvider.Context context) {
    }

    private float getHorizontalAngle(float x, float z) {
        float angle = (float) Math.atan(x / z);
        return z < 0 ? (float) (angle + Math.PI) : angle;
    }

    private ModelResourceLocation getHeadModel(HeliostatsBlockEntity blockEntity) {
        return Optional.of(blockEntity)
                   .filter(ignore -> AnvilCraftClient.CONFIG.heliostatsSunflowerModel)
                   .filter(be -> be.getLevel() != null)
                   .map(be -> be.getLevel().getBiome(be.getBlockPos()))
                   .map(biome -> biome.is(Biomes.SUNFLOWER_PLAINS))
                   .orElse(false) ? HELIOSTATS_HEAD_SUNFLOWER : HELIOSTATS_HEAD;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void render(
        HeliostatsBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5, 1.3, 0.5);
        if (
            blockEntity.getWorkResult() != HeliostatsBlockEntity.WorkResult.NO_ROTATION_ANGLE
            && !blockEntity.getNormalVector3f().equals(new Vector3f())
            && !blockEntity.getNormalVector3f().equals(new Vector3f(Float.NaN))
        ) {
            poseStack.mulPose(new Quaternionf().rotateY(getHorizontalAngle(
                blockEntity.getNormalVector3f().x,
                blockEntity.getNormalVector3f().z
            )));
            poseStack.mulPose(new Quaternionf().rotateX((float) (
                Math.atan(Math.hypot(
                    blockEntity.getNormalVector3f().z,
                    blockEntity.getNormalVector3f().x
                ) / blockEntity.getNormalVector3f().y)
            )));
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getBlockRenderer().getModelRenderer().renderModel(
            poseStack.last(),
            buffer.getBuffer(RenderType.cutout()),
            null,
            minecraft.getModelManager().getModel(this.getHeadModel(blockEntity)),
            0,
            0,
            0,
            packedLight,
            packedOverlay
        );
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HeliostatsBlockEntity blockEntity) {
        return AABB.ofSize(blockEntity.getBlockPos().getCenter().add(0, 0.5f, 0), 3, 2, 3);
    }

    @Override
    public int getViewDistance() {
        return AnvilCraft.CLIENT_CONFIG.heliostatsRenderDistance;
    }
}
