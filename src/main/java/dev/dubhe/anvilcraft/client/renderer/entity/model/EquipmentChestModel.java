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

public class EquipmentChestModel extends HumanoidModel<HumanoidRenderState> {
    public EquipmentChestModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        if (state instanceof ArmorStandRenderState stand) {
            this.body.xRot = (float) (Math.PI / 180.0) * stand.bodyPose.x();
            this.body.yRot = (float) (Math.PI / 180.0) * stand.bodyPose.y();
            this.body.zRot = (float) (Math.PI / 180.0) * stand.bodyPose.z();
            this.rightArm.xRot = (float) (Math.PI / 180.0) * stand.rightArmPose.x();
            this.rightArm.yRot = (float) (Math.PI / 180.0) * stand.rightArmPose.y();
            this.rightArm.zRot = (float) (Math.PI / 180.0) * stand.rightArmPose.z();
            this.leftArm.xRot = (float) (Math.PI / 180.0) * stand.leftArmPose.x();
            this.leftArm.yRot = (float) (Math.PI / 180.0) * stand.leftArmPose.y();
            this.leftArm.zRot = (float) (Math.PI / 180.0) * stand.leftArmPose.z();
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
        part1.addOrReplaceChild("cube2", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(1.01F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        part1.addOrReplaceChild("cube3", CubeListBuilder.create().texOffs(48, 56)
            .addBox(-3.0F, -3.0F, -1.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 8.0F, 4.0F, -0.0F, -0.0F, -0.785398F));
        part1.addOrReplaceChild("cube4", CubeListBuilder.create().texOffs(48, 56)
            .addBox(-3.0F, -3.0F, -1.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 8.0F, -4.0F, -0.0F, -0.0F, -0.785398F));
        PartDefinition part5 = part1.addOrReplaceChild("part5", CubeListBuilder.create(),
            PartPose.offsetAndRotation(-5.0F, 3.0F, 7.0F, -0.0F, -0.0F, 0.0F));
        part5.addOrReplaceChild("cube6", CubeListBuilder.create().texOffs(32, 25)
            .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.022339F, -0.217701F, 0.042619F));
        part5.addOrReplaceChild("cube7", CubeListBuilder.create().texOffs(27, 40)
            .addBox(-4.0F, -1.0F, 4.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.022339F, -0.217701F, 0.042619F));
        part5.addOrReplaceChild("cube8", CubeListBuilder.create().texOffs(0, 40)
            .addBox(-11.0F, -1.0F, -4.0F, 7.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.022339F, -0.217701F, 0.042619F));
        part5.addOrReplaceChild("cube9", CubeListBuilder.create().texOffs(48, 42)
            .addBox(-13.0F, -1.0F, -4.0F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.022339F, -0.217701F, 0.042619F));
        part5.addOrReplaceChild("cube10", CubeListBuilder.create().texOffs(10, 32)
            .addBox(-5.5F, -5.0F, 0.0F, 11.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.040553F, -1.002779F, 0.071983F));
        part5.addOrReplaceChild("cube11", CubeListBuilder.create().texOffs(10, 32)
            .addBox(-5.5F, -5.0F, 0.0F, 11.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.115726F, -0.567493F, -3.117705F));
        PartDefinition part12 = part1.addOrReplaceChild("part12", CubeListBuilder.create(),
            PartPose.offsetAndRotation(5.0F, 3.0F, 7.0F, -0.0F, -0.0F, 0.0F));
        part12.addOrReplaceChild("cube13", CubeListBuilder.create().texOffs(10, 32).mirror()
            .addBox(-5.5F, -5.0F, 0.0F, 11.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.115726F, 0.567493F, 3.117705F));
        part12.addOrReplaceChild("cube14", CubeListBuilder.create().texOffs(10, 32).mirror()
            .addBox(-5.5F, -5.0F, 0.0F, 11.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.040553F, 1.002779F, -0.071983F));
        part12.addOrReplaceChild("cube15", CubeListBuilder.create().texOffs(48, 42).mirror()
            .addBox(11.0F, -1.0F, -4.0F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.022339F, 0.217701F, -0.042619F));
        part12.addOrReplaceChild("cube16", CubeListBuilder.create().texOffs(32, 25).mirror()
            .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.022339F, 0.217701F, -0.042619F));
        part12.addOrReplaceChild("cube17", CubeListBuilder.create().texOffs(27, 40).mirror()
            .addBox(0.0F, -1.0F, 4.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.022339F, 0.217701F, -0.042619F));
        part12.addOrReplaceChild("cube18", CubeListBuilder.create().texOffs(0, 40).mirror()
            .addBox(4.0F, -1.0F, -4.0F, 7.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.022339F, 0.217701F, -0.042619F));
        PartDefinition part19 = root.addOrReplaceChild("right_arm", CubeListBuilder.create(),
            PartPose.offsetAndRotation(-5.0F, 2.0F, 0.0F, -0.174533F, -0.0F, 0.0F));
        part19.addOrReplaceChild("cube20", CubeListBuilder.create().texOffs(40, 16)
            .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(1.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        PartDefinition part21 = root.addOrReplaceChild("left_arm", CubeListBuilder.create(),
            PartPose.offsetAndRotation(5.0F, 2.0F, 0.0F, 0.20944F, -0.0F, 0.0F));
        part21.addOrReplaceChild("cube22", CubeListBuilder.create().texOffs(40, 16).mirror()
            .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(1.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0F, -0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

}
