package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.HasMobBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.HasMobBlockRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class HasMobBlockRenderer implements BlockEntityRenderer<HasMobBlockEntity, HasMobBlockRenderState> {
    private final EntityRenderDispatcher dispatcher;

    @SuppressWarnings("unused")
    public HasMobBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.dispatcher = context.entityRenderer();
    }

    @Override
    public HasMobBlockRenderState createRenderState() {
        return new HasMobBlockRenderState();
    }

    @Override
    public void extractRenderState(
        HasMobBlockEntity be,
        HasMobBlockRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        Level level = be.getLevel();
        if (level == null) {
            state.setMob(null);
            return;
        }
        Entity entity = be.getOrCreateDisplayEntity(level);
        if (entity == null) {
            state.setMob(null);
            return;
        }
        EntityRenderState mob = this.dispatcher.extractEntity(entity, partialTicks);
        state.setMob(mob);
    }

    @Override
    public void submit(
        HasMobBlockRenderState state,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        EntityRenderState mob = state.getMob();
        if (mob == null) return;
        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        float size = 0.73125F;
        float max = Math.max(mob.boundingBoxWidth, mob.boundingBoxHeight);
        if ((double) max > 1.0) size /= max;
        pose.translate(0.0F, 0.14F, 0.0F);
        pose.scale(size, size, size);
        this.dispatcher.submit(
            mob,
            camera,
            state.blockPos.getX(),
            state.blockPos.getY(),
            state.blockPos.getZ(),
            pose,
            collector
        );
        pose.popPose();
    }
}
