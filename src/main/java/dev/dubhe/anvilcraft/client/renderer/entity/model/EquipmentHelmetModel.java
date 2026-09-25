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

public class EquipmentHelmetModel extends HumanoidModel<HumanoidRenderState> {
    public EquipmentHelmetModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        if (state instanceof ArmorStandRenderState stand) {
            this.head.xRot = (float) (Math.PI / 180.0) * stand.headPose.x();
            this.head.yRot = (float) (Math.PI / 180.0) * stand.headPose.y();
            this.head.zRot = (float) (Math.PI / 180.0) * stand.headPose.z();
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(),
            PartPose.offsetAndRotation(0, 0, 0, -0.10472F, 0.087266F, 0));
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("cube2", CubeListBuilder.create().texOffs(0, 0)
            .addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(1)), PartPose.ZERO);
        head.addOrReplaceChild("cube3", CubeListBuilder.create().texOffs(32, 0)
            .addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(1.5F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
