package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.RedstoneDiceBlockEntity;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

public class RedstoneDiceBlockEntityRenderer
    implements BlockEntityRenderer<RedstoneDiceBlockEntity>, ModelSelectionRenderer<RedstoneDiceBlockEntity> {
    private static final ModelResourceLocation DICE = ModelResourceLocation.standalone(AnvilCraft.of("block/redstone_dice_dice"));
    private static final float[] X = {5.75f, 8, 10.25f};
    private static final float[] Z = {5.75f, 10.25f, 5.75f};

    public RedstoneDiceBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        RedstoneDiceBlockEntity dice, float partialTick, PoseStack poseStack,
        MultiBufferSource bufferSource, int packedLight, int packedOverlay
    ) {
        this.collectSelectionModels(dice, partialTick, poseStack, (model, pose) -> Minecraft.getInstance()
            .getBlockRenderer().getModelRenderer().renderModel(
                pose.last(), bufferSource.getBuffer(RenderType.cutout()), null,
                Minecraft.getInstance().getModelManager().getModel(model), 1, 1, 1, packedLight, packedOverlay
            ));
    }

    @Override
    public void collectSelectionModels(RedstoneDiceBlockEntity dice, float partialTick, PoseStack pose, ModelConsumer consumer) {
        float progress = dice.getRollProgress(partialTick);
        float eased = progress * progress * progress * (progress * (progress * 6 - 15) + 10);
        float envelope = Mth.sin(progress * Mth.PI);
        envelope *= envelope;
        int scenario = dice.getScenario(false);
        for (int index = 0; index < 3; index++) {
            Quaternionf rotation = orientation(dice.getFace(index, true), dice.getScenario(true), index)
                .slerp(orientation(dice.getFace(index, false), scenario, index), eased);
            if (progress > 0 && progress < 1) {
                float turns = Mth.TWO_PI * eased;
                rotation.rotateX(turns * (2 + (scenario + index) % 2))
                    .rotateZ(turns * (1 + (scenario >> index & 1)));
            }
            Matrix3f matrix = new Matrix3f().rotation(rotation);
            float extentX = 1.5f * (Math.abs(matrix.m00()) + Math.abs(matrix.m10()) + Math.abs(matrix.m20()));
            float extentY = 1.5f * (Math.abs(matrix.m01()) + Math.abs(matrix.m11()) + Math.abs(matrix.m21()));
            float extentZ = 1.5f * (Math.abs(matrix.m02()) + Math.abs(matrix.m12()) + Math.abs(matrix.m22()));
            float phase = (scenario + index * 5) * Mth.PI / 8;
            float x = Mth.clamp(X[index] + 0.25f * envelope * Mth.sin(phase + progress * Mth.TWO_PI), 3 + extentX, 13 - extentX);
            float z = Mth.clamp(Z[index] + 0.25f * envelope * Mth.cos(phase + progress * Mth.TWO_PI), 3 + extentZ, 13 - extentZ);
            pose.pushPose();
            pose.translate(x / 16, (4 + extentY + 0.65f * envelope) / 16, z / 16);
            pose.mulPose(rotation);
            consumer.accept(DICE, pose);
            pose.popPose();
        }
    }

    private static Quaternionf orientation(int face, int scenario, int index) {
        Quaternionf rotation = new Quaternionf().rotationY((scenario * 3 + index * 5 + 1) * Mth.PI / 8);
        return switch (face) {
            case 2 -> rotation.rotateX(Mth.HALF_PI);
            case 3 -> rotation.rotateZ(-Mth.HALF_PI);
            case 4 -> rotation.rotateZ(Mth.HALF_PI);
            case 5 -> rotation.rotateX(-Mth.HALF_PI);
            case 6 -> rotation.rotateX(Mth.PI);
            default -> rotation;
        };
    }
}
