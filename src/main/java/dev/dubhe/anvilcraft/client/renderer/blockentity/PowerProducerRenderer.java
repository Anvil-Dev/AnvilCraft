package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.PowerGeneratorRenderState;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class PowerProducerRenderer<T extends BlockEntity & IPowerProducer, S extends PowerGeneratorRenderState>
    implements BlockEntityRenderer<T, S>, ModelSelectionRenderer<T> {
    public static final float ROTATION_MAGIC = 0.001220703125F;

    @Override
    public void extractRenderState(
        T be,
        S state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.setElevation(this.elevation());
        state.setRotation(this.rotation(be, partialTicks));
        state.setCube(FeatureRendererSupport.initialize(this.getModel(), be));
    }

    @Override
    public void submit(S state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        applyHeadPose(pose, state.getElevation(), state.getRotation());
        state.getCube().submitModel(
            ModRenderTypes.CUTOUT_BLOCK, pose, collector, LightCoordsUtil.pack(15, 0), OverlayTexture.NO_OVERLAY, 0
        );
        pose.popPose();
    }

    @Override
    public void collectSelectionModels(T be, float partialTick, PoseStack pose, ModelConsumer consumer) {
        pose.pushPose();
        applyHeadPose(pose, this.elevation(), this.rotation(be, partialTick));
        consumer.accept(this.getModel(), pose);
        pose.popPose();
    }

    private static void applyHeadPose(PoseStack pose, float elevation, float rotation) {
        pose.translate(0.5F, elevation, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.mulPose(Axis.ZP.rotationDegrees(rotation));
    }

    protected float rotation(T blockEntity, float partialTick) {
        return ((float) blockEntity.getTime() + partialTick) * (float) Math.log(blockEntity.getServerPower() + 1) * this.magic() * 50.0F;
    }

    protected float elevation() {
        return 0.8F;
    }

    protected float magic() {
        return PowerProducerRenderer.ROTATION_MAGIC;
    }

    protected abstract StandaloneModelKey<BlockStateModel> getModel();
}
