package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.client.init.ModModelLayers;
import dev.dubhe.anvilcraft.client.renderer.entity.model.ThrownHeavyHalberdModel;
import dev.dubhe.anvilcraft.client.renderer.entity.state.ThrownHeavyHalberdRenderState;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import org.joml.Quaternionf;

public class ThrownHeavyHalberdRenderer<T extends ThrownHeavyHalberdEntity> extends EntityRenderer<T, ThrownHeavyHalberdRenderState> {
    private final ThrownHeavyHalberdModel model;

    public ThrownHeavyHalberdRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ThrownHeavyHalberdModel(context.bakeLayer(ModModelLayers.THROWN_HEAVY_HALBERD));
    }

    @Override
    public ThrownHeavyHalberdRenderState createRenderState() {
        return new ThrownHeavyHalberdRenderState();
    }

    @Override
    public void extractRenderState(T entity, ThrownHeavyHalberdRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.getRotation().add(Axis.YP.rotationDegrees(entity.getYRot(partialTicks) - 90.0F));
        state.getRotation().add(Axis.ZP.rotationDegrees(entity.getXRot(partialTicks) + 90.0F));
        state.setTexture(ThrownHeavyHalberdRenderer.getTextureLocation(entity));
        state.setFoil(entity.isFoil());
    }

    @Override
    public void submit(ThrownHeavyHalberdRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        for (Quaternionf rot : state.getRotation()) {
            pose.mulPose(rot);
        }
        pose.translate(0, -0.4, 0);
        collector.order(0).submitModel(
            this.model,
            Unit.INSTANCE,
            pose,
            state.getTexture(),
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            state.outlineColor,
            null
        );
        if (state.isFoil()) {
            collector.order(1).submitModel(
                this.model,
                Unit.INSTANCE,
                pose,
                ItemFeatureRenderer.getFoilRenderType(this.model.renderType(state.getTexture()), false),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor,
                null
            );
        }
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public static Identifier getTextureLocation(ThrownHeavyHalberdEntity entity) {
        return SharedTextures.texture("entity/heavy_halberd/" + entity.getTextureBase());
    }
}
