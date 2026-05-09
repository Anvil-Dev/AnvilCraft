package dev.dubhe.anvilcraft.client.renderer.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Unit;

public class ThrownHeavyHalberdModel extends Model<Unit> {

    public ThrownHeavyHalberdModel(ModelPart root) {
        super(root, RenderTypes::entitySolid);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // region group2
        PartDefinition group2 = partdefinition.addOrReplaceChild(
            "group2",
            CubeListBuilder.create(),
            PartPose.offset(0.0F, 16.0F, 0.0F)
        );
        group2.addOrReplaceChild(
            "cube_r1", CubeListBuilder.create().texOffs(16, 8).addBox(0.5F, 0.5F, -2.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 8).addBox(-2.5F, -2.5F, -2.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(8, 8).addBox(0.5F, -2.5F, -2.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 3.1416F, 0.0F, 2.3562F)
        );
        group2.addOrReplaceChild(
            "cube_r2", CubeListBuilder.create().texOffs(8, 8).mirror()
                .addBox(-2.5F, -2.5F, -2.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
            PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, -3.1416F, 0.0F, -2.3562F)
        );
        group2.addOrReplaceChild(
            "cube_r3", CubeListBuilder.create().texOffs(16, 8).addBox(0.5F, 0.5F, -2.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(8, 8).addBox(0.5F, -2.5F, -2.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 8).addBox(-2.5F, -2.5F, -2.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 12).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.7854F)
        );
        group2.addOrReplaceChild(
            "cube_r4", CubeListBuilder.create().texOffs(8, 8).mirror()
                .addBox(-2.5F, -2.5F, -2.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
            PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, -0.7854F)
        );
        // endregion

        // region group
        PartDefinition group = partdefinition.addOrReplaceChild(
            "group", CubeListBuilder.create().texOffs(12, 0).addBox(1.5F, -6.5F, -0.5F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(1.5F, -10.5F, 0.0F, 3.0F, 4.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(20, 1).addBox(2.5F, -8.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.0F, 13.0F, 0.0F)
        );
        group.addOrReplaceChild(
            "cube_r5", CubeListBuilder.create().texOffs(20, 1).mirror().addBox(
                    -2.3F, -4.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(6, 0).mirror().addBox(-4.3F, -6.5F, 0.0F, 3.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(24, 0).mirror().addBox(-3.3F, -2.5F, -0.5F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false),
            PartPose.offsetAndRotation(3.0F, -3.0F, 0.0F, 0.0F, 0.0F, -0.3927F)
        );
        group.addOrReplaceChild(
            "cube_r6", CubeListBuilder.create().texOffs(6, 0).addBox(1.3F, -6.5F, 0.0F, 3.0F, 8.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(20, 1).addBox(1.3F, -4.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(24, 0).addBox(1.3F, -2.5F, -0.5F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(3.0F, -3.0F, 0.0F, 0.0F, 0.0F, 0.3927F)
        );
        // endregion

        partdefinition.addOrReplaceChild(
            "bone", CubeListBuilder.create().texOffs(8, 26).addBox(-10.0F, 0.2426F, 6.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(24, 14).addBox(-9.0F, -12.7574F, 7.0F, 2.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offset(8.0F, 24.0F, -8.0F)
        );

        return LayerDefinition.create(meshdefinition, 32, 32);
    }
}
