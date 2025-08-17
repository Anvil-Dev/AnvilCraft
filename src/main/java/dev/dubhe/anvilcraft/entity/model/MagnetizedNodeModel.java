package dev.dubhe.anvilcraft.entity.model;

import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;

public class MagnetizedNodeModel extends HierarchicalModel<MagnetizedNodeEntity> {
    public static final AnimationDefinition ROTATING = AnimationDefinition.Builder.withLength(6f).looping()
        .addAnimation("rotating",
            new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0f, KeyframeAnimations.degreeVec(0f, 0f, 0f),
                    AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2f, KeyframeAnimations.degreeVec(0f, 360f, 0f),
                    AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4f, KeyframeAnimations.degreeVec(0f, 720f, 0f),
                    AnimationChannel.Interpolations.LINEAR),
                new Keyframe(6f, KeyframeAnimations.degreeVec(0f, 1080f, 0f),
                    AnimationChannel.Interpolations.LINEAR)))
        .addAnimation("main",
            new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0f, KeyframeAnimations.degreeVec(0f, 0f, 0f),
                    AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3f, KeyframeAnimations.degreeVec(0f, -360f, 0f),
                    AnimationChannel.Interpolations.LINEAR),
                new Keyframe(6f, KeyframeAnimations.degreeVec(0f, -720f, 0f),
                    AnimationChannel.Interpolations.LINEAR))).build();
    private final ModelPart root;

    public MagnetizedNodeModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition rotating = partdefinition.addOrReplaceChild("rotating", CubeListBuilder.create().texOffs(0, 4).addBox(-3.0F, -3.0F, -1.0F, 1.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));
        rotating.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 4).addBox(-3.0F, -3.0F, -1.0F, 1.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 2.0944F, 0.0F));
        rotating.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 4).addBox(-3.0F, -3.0F, -1.0F, 1.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -2.0944F, 0.0F));
        partdefinition.addOrReplaceChild("main", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    @Override
    public void setupAnim(
        @NotNull MagnetizedNodeEntity entity,
        float limbSwing,
        float limbSwingAmount,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
    ) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        animate(entity.rotatingState, ROTATING, ageInTicks, 1.0f);
    }

    @Override
    public @NotNull ModelPart root() {
        return this.root;
    }
}