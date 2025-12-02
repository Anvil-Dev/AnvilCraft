package dev.dubhe.anvilcraft.client.renderer.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public class CauldronOutletModel {
    private final ModelPart outlet;

    public CauldronOutletModel(ModelPart root) {
        this.outlet = root.getChild("outlet");
    }

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(
            "anvilcraft",
            "cauldron_outlet"
        ), "main"
    );

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Create a model that exactly matches the cauldron_outlet.json model
        PartDefinition outlet = partdefinition.addOrReplaceChild(
            "outlet",
            CubeListBuilder.create().texOffs(0, 8).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
            PartPose.ZERO
        );

        outlet.addOrReplaceChild(
            "cube_inverted",
            CubeListBuilder.create()
                .texOffs(7, 8)
                .addBox(-3.99F, -3.99F, -3.99F, 0.99F, 7.98F, 7.98F)
                .texOffs(14, 0)
                .addBox(-3.99F, -3.99F, -3.99F, 7.98F, 7.98F, 0.99F)
                .texOffs(0, 8)
                .addBox(2.99F, -3.99F, -3.99F, 0.99F, 7.98F, 7.98F)
                .texOffs(7, 0)
                .addBox(-3.99F, -3.99F, 2.99F, 7.98F, 7.98F, 0.99F),
            PartPose.ZERO
        );

        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.outlet.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}