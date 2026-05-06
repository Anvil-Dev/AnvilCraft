package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.AdvancedComparatorBlock;
import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.AdvancedComparatorRenderState;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AdvancedComparatorRenderer implements BlockEntityRenderer<AdvancedComparatorBlockEntity, AdvancedComparatorRenderState> {
    public static final StandaloneModelKey<BlockStateModel> INDICATOR = new StandaloneModelKey<>(
        () -> "AnvilCraft: Advanced Comparator Indicator Model"
    );

    public AdvancedComparatorRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public AdvancedComparatorRenderState createRenderState() {
        return new AdvancedComparatorRenderState();
    }

    @Override
    public void extractRenderState(
        AdvancedComparatorBlockEntity be,
        AdvancedComparatorRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.setSignal(be.getBlockState().getValue(AdvancedComparatorBlock.POWER));
        state.setIndicator(new BlockModelRenderState());
        Minecraft mc = Minecraft.getInstance();
        mc.getModelManager().getStandaloneModel(AdvancedComparatorRenderer.INDICATOR).collectParts(
            mc.level,
            be.getBlockPos(),
            be.getBlockState(),
            RandomSource.create(),
            state.getIndicator().setupModel(new Matrix4f(), false)
        );
    }

    @Override
    public void submit(
        AdvancedComparatorRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera
    ) {
        poseStack.pushPose();
        float height = state.getSignal() / 3F * .0625F;
        poseStack.translate(0, height, 0);
        state.getIndicator().submit(
            poseStack,
            submitNodeCollector,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            0
        );
        poseStack.popPose();
    }
}
