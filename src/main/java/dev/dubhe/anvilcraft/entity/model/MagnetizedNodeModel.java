package dev.dubhe.anvilcraft.entity.model;

import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class MagnetizedNodeModel extends HierarchicalModel<MagnetizedNodeEntity> {
    public static final AnimationDefinition ROTATING = AnimationDefinition.Builder.withLength(6F).looping()
        .addAnimation("rotating",
            new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F),
                    AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2F, KeyframeAnimations.degreeVec(0F, 360F, 0F),
                    AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4F, KeyframeAnimations.degreeVec(0F, 720F, 0F),
                    AnimationChannel.Interpolations.LINEAR),
                new Keyframe(6F, KeyframeAnimations.degreeVec(0F, 1080F, 0F),
                    AnimationChannel.Interpolations.LINEAR)))
        .addAnimation("main",
            new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F),
                    AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3F, KeyframeAnimations.degreeVec(0F, -360F, 0F),
                    AnimationChannel.Interpolations.LINEAR),
                new Keyframe(6F, KeyframeAnimations.degreeVec(0F, -720F, 0F),
                    AnimationChannel.Interpolations.LINEAR))).build();
    private final ModelPart root;

    public MagnetizedNodeModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition rotating = partdefinition.addOrReplaceChild(
            "rotating",
            CubeListBuilder.create()
                .texOffs(0, 4)
                .addBox(-3.0F, -3.0F, -1.0F, 1.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 24.0F, 0.0F)
        );
        rotating.addOrReplaceChild(
            "cube_r1",
            CubeListBuilder.create()
                .texOffs(0, 4)
                .addBox(-3.0F, -3.0F, -1.0F, 1.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 2.0944F, 0.0F)
        );
        rotating.addOrReplaceChild(
            "cube_r2",
            CubeListBuilder.create()
                .texOffs(0, 4)
                .addBox(-3.0F, -3.0F, -1.0F, 1.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.0944F, 0.0F)
        );
        partdefinition.addOrReplaceChild(
            "main",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
            PartPose.offset(0.0F, 24.0F, 0.0F)
        );

        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    @Override
    public void setupAnim(
        MagnetizedNodeEntity entity,
        float limbSwing,
        float limbSwingAmount,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
    ) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        animate(entity.rotatingState, ROTATING, ageInTicks, 1.0F);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}