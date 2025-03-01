package dev.dubhe.anvilcraft.entity.model;


import lombok.Getter;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@ParametersAreNonnullByDefault
public class IonocraftBackpackModel extends HumanoidModel<LivingEntity> {
    private final ModelPart root;

    public IonocraftBackpackModel(ModelPart root) {
        super(root);
        this.root = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0f);
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(40, 0).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(1.01F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("capacitor_r1", CubeListBuilder.create().texOffs(48, 24).addBox(-3.0F, -3.0F, -1.0F, 6.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 8.0F, 4.0F, 0.0F, 0.0F, -0.7854F));

        PartDefinition rightArm1 = body.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 3.0F, 7.0F));

        rightArm1.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 14).addBox(-5.5F, -5.0F, 0.0F, 11.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.1157F, -0.5675F, -3.1177F));

        rightArm1.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 14).addBox(-5.5F, -5.0F, 0.0F, 11.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0406F, -1.0028F, 0.072F));

        rightArm1.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(26, 21).addBox(-13.0F, -1.0F, -4.0F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
            .texOffs(0, 21).addBox(-11.0F, -1.0F, -4.0F, 7.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(22, 14).addBox(-4.0F, -1.0F, 4.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
            .texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0223F, -0.2177F, 0.0426F));

        PartDefinition leftArm1 = body.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 3.0F, 7.0F));

        leftArm1.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 21).mirror().addBox(4.0F, -1.0F, -4.0F, 7.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false)
            .texOffs(22, 14).mirror().addBox(0.0F, -1.0F, 4.0F, 4.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false)
            .texOffs(0, 0).mirror().addBox(-4.0F, -4.0F, -4.0F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false)
            .texOffs(26, 21).mirror().addBox(11.0F, -1.0F, -4.0F, 2.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0223F, 0.2177F, -0.0426F));

        leftArm1.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(0, 14).mirror().addBox(-5.5F, -5.0F, 0.0F, 11.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0406F, 1.0028F, -0.072F));

        leftArm1.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 14).mirror().addBox(-5.5F, -5.0F, 0.0F, 11.0F, 7.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -3.1157F, 0.5675F, 3.1177F));

        return LayerDefinition.create(meshdefinition, 64, 32);
    }
}