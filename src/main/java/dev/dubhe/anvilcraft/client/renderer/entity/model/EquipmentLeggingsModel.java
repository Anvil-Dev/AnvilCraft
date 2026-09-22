package dev.dubhe.anvilcraft.client.renderer.entity.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class EquipmentLeggingsModel extends HumanoidModel<HumanoidRenderState> {
    public EquipmentLeggingsModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        if (state instanceof ArmorStandRenderState stand) {
            this.body.xRot = (float) (Math.PI / 180.0) * stand.bodyPose.x();
            this.body.yRot = (float) (Math.PI / 180.0) * stand.bodyPose.y();
            this.body.zRot = (float) (Math.PI / 180.0) * stand.bodyPose.z();
            this.rightLeg.xRot = (float) (Math.PI / 180.0) * stand.rightLegPose.x();
            this.rightLeg.yRot = (float) (Math.PI / 180.0) * stand.rightLegPose.y();
            this.rightLeg.zRot = (float) (Math.PI / 180.0) * stand.rightLegPose.z();
            this.leftLeg.xRot = (float) (Math.PI / 180.0) * stand.leftLegPose.x();
            this.leftLeg.yRot = (float) (Math.PI / 180.0) * stand.leftLegPose.y();
            this.leftLeg.zRot = (float) (Math.PI / 180.0) * stand.leftLegPose.z();
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO)
            .addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition part1 = root.addOrReplaceChild("body", CubeListBuilder.create(),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        part1.addOrReplaceChild("cube2", CubeListBuilder.create().texOffs(16, 55)
            .addBox(-4.0F, 7.0F, -2.0F, 8.0F, 5.0F, 4.0F, new CubeDeformation(0.51F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        PartDefinition part3 = root.addOrReplaceChild("right_leg", CubeListBuilder.create(),
            PartPose.offsetAndRotation(-1.9F, 12.0F, 0.0F, 0.191986F, -0.0F, 0.034907F));
        part3.addOrReplaceChild("cube4", CubeListBuilder.create().texOffs(0, 50)
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.5F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        part3.addOrReplaceChild("cube5", CubeListBuilder.create().texOffs(16, 48)
            .addBox(-2.0F, 3.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.75F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        PartDefinition part6 = root.addOrReplaceChild("left_leg", CubeListBuilder.create(),
            PartPose.offsetAndRotation(1.9F, 12.0F, 0.0F, -0.174533F, -0.0F, -0.034907F));
        part6.addOrReplaceChild("cube7", CubeListBuilder.create().texOffs(0, 50).mirror()
            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.5F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        part6.addOrReplaceChild("cube8", CubeListBuilder.create().texOffs(16, 48).mirror()
            .addBox(-2.0F, 3.0F, -2.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.75F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

}
