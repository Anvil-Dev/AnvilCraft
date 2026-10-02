package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.RedstoneDiceBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.RedstoneDiceRenderState;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

public class RedstoneDiceBlockEntityRenderer
    implements BlockEntityRenderer<RedstoneDiceBlockEntity, RedstoneDiceRenderState>, ModelSelectionRenderer<RedstoneDiceBlockEntity> {
    public static final StandaloneModelKey<BlockStateModel> DICE = new StandaloneModelKey<>(() -> "AnvilCraft: RedstoneDice");
    private static final float[] X = {5.75f, 8, 10.25f};
    private static final float[] Z = {5.75f, 10.25f, 5.75f};

    public RedstoneDiceBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public RedstoneDiceRenderState createRenderState() {
        return new RedstoneDiceRenderState();
    }

    @Override
    public void extractRenderState(
        RedstoneDiceBlockEntity blockEntity, RedstoneDiceRenderState state, float partialTick,
        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        extractAnimation(blockEntity, state, partialTick);
        state.model = FeatureRendererSupport.initialize(DICE, blockEntity);
    }

    private static void extractAnimation(RedstoneDiceBlockEntity blockEntity, RedstoneDiceRenderState state, float partialTick) {
        state.progress = blockEntity.getRollProgress(partialTick);
        state.scenario = blockEntity.getScenario(false);
        state.previousScenario = blockEntity.getScenario(true);
        for (int index = 0; index < 3; index++) {
            state.faces[index] = blockEntity.getFace(index, false);
            state.previousFaces[index] = blockEntity.getFace(index, true);
        }
    }

    @Override
    public void collectSelectionModels(RedstoneDiceBlockEntity blockEntity, float partialTick, PoseStack pose, ModelConsumer consumer) {
        RedstoneDiceRenderState state = new RedstoneDiceRenderState();
        extractAnimation(blockEntity, state, partialTick);
        collectModels(state, pose, consumer);
    }

    @Override
    public void submit(RedstoneDiceRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        collectModels(state, pose, (model, modelPose) ->
            state.model.submit(modelPose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0));
    }

    private static void collectModels(RedstoneDiceRenderState state, PoseStack pose, ModelConsumer consumer) {
        float progress = state.progress;
        float eased = progress * progress * progress * (progress * (progress * 6 - 15) + 10);
        float envelope = Mth.sin(progress * Mth.PI);
        envelope *= envelope;
        int scenario = state.scenario;
        for (int index = 0; index < 3; index++) {
            Quaternionf rotation = orientation(state.previousFaces[index], state.previousScenario, index)
                .slerp(orientation(state.faces[index], scenario, index), eased);
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
