package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.AdvancedComparatorBlock;
import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.AdvancedComparatorRenderState;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class AdvancedComparatorRenderer implements BlockEntityRenderer<AdvancedComparatorBlockEntity, AdvancedComparatorRenderState> {
    public static final ModelLayerLocation INDICATOR_LAYER = new ModelLayerLocation(
        AnvilCraft.of("block/advanced_comparator_indicator"),
        "main"
    );
    public static final Identifier INDICATOR_TEXTURE = SharedTextures.texture("block/advanced_comparator_indicator");
    private final Model.Simple indicator;

    public AdvancedComparatorRenderer(BlockEntityRendererProvider.Context context) {
        this(context.entityModelSet());
    }

    public AdvancedComparatorRenderer(SpecialModelRenderer.BakingContext context) {
        this(context.entityModelSet());
    }

    public AdvancedComparatorRenderer(EntityModelSet set) {
        this.indicator = new Model.Simple(set.bakeLayer(AdvancedComparatorRenderer.INDICATOR_LAYER), RenderTypes::entitySolid);
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
        submitNodeCollector.submitModel(
            this.indicator,
            Unit.INSTANCE,
            poseStack,
            AdvancedComparatorRenderer.INDICATOR_TEXTURE,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            0,
            state.breakProgress
        );
        poseStack.popPose();
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "main",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(7.5F, 3.2625F, 7.5F, 1F, 1F, 1F, CubeDeformation.NONE.extend(3F, 2.2625F, 0.5F))
                .addBox(7.5F, 3.2625F, 7.5F, 1F, 1F, 1F, CubeDeformation.NONE.extend(0.5F, 2.2625F, 3F)),
            PartPose.ZERO
        );
        return LayerDefinition.create(mesh, 64, 64);
    }
}
