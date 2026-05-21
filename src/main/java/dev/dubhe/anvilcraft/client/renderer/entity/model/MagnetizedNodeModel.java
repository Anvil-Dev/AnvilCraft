package dev.dubhe.anvilcraft.client.renderer.entity.model;

import dev.dubhe.anvilcraft.client.renderer.entity.state.MagnetizedNodeRenderState;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class MagnetizedNodeModel extends Model<MagnetizedNodeRenderState> {
    public static final AnimationDefinition ROTATING = AnimationDefinition.Builder.withLength(6F).looping()
        .addAnimation(
            "rotating",
            new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F),
                    AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2F, KeyframeAnimations.degreeVec(0F, 360F, 0F),
                    AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4F, KeyframeAnimations.degreeVec(0F, 720F, 0F),
                    AnimationChannel.Interpolations.LINEAR),
                new Keyframe(6F, KeyframeAnimations.degreeVec(0F, 1080F, 0F),
                    AnimationChannel.Interpolations.LINEAR)
            )
        )
        .addAnimation(
            "main",
            new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0F, KeyframeAnimations.degreeVec(0F, 0F, 0F),
                    AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3F, KeyframeAnimations.degreeVec(0F, -360F, 0F),
                    AnimationChannel.Interpolations.LINEAR),
                new Keyframe(6F, KeyframeAnimations.degreeVec(0F, -720F, 0F),
                    AnimationChannel.Interpolations.LINEAR)
            )
        )
        .build();
    private final KeyframeAnimation rotating;

    public MagnetizedNodeModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.rotating = ROTATING.bake(root);
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
    public void setupAnim(MagnetizedNodeRenderState state) {
        super.setupAnim(state);
        this.rotating.apply(state.getRotation(), state.ageInTicks);
    }
}
