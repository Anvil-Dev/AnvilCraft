package dev.dubhe.anvilcraft.entity.model;

import dev.dubhe.anvilcraft.client.renderer.entity.state.IonocraftRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class IonocraftModel extends EntityModel<IonocraftRenderState> {
    public IonocraftModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition partDefinition = meshDefinition.getRoot();

        PartDefinition bbMain = partDefinition.addOrReplaceChild(
            "bb_main",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-6.0F, -10.0F, 5.0F, 12.0F, 10.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(0, 10)
                .addBox(-0.5F, -10.0F, -6.2F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 24.0F, 0.0F)
        );

        PartDefinition cubeR1 = bbMain.addOrReplaceChild(
            "cube_r1",
            CubeListBuilder.create()
                .texOffs(0, 10)
                .addBox(-0.5F, -10.0F, -7.2F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0)
                .addBox(-6.0F, -10.0F, 4.0F, 12.0F, 10.0F, 0.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, 0.0F, 2.0944F, 0.0F)
        );

        PartDefinition cubeR2 = bbMain.addOrReplaceChild(
            "cube_r2",
            CubeListBuilder.create()
                .texOffs(0, 10)
                .addBox(-0.5F, -10.0F, -7.2F, 1.0F, 10.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0)
                .addBox(-6.0F, -10.0F, 4.0F, 12.0F, 10.0F, 0.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, 0.0F, -2.0944F, 0.0F)
        );

        return LayerDefinition.create(meshDefinition, 32, 32);
    }

    @Override
    public void setupAnim(IonocraftRenderState state) {
    }
}