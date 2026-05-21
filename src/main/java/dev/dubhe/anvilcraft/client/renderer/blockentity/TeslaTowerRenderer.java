package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.block.entity.TeslaTowerBlockEntity;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.TeslaTowerRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public class TeslaTowerRenderer implements BlockEntityRenderer<TeslaTowerBlockEntity, TeslaTowerRenderState> {
    private static final float LIGHTNING_WIDTH = 1F;
    private static final AABB BASE_RENDER_BBOX = new AABB(BlockPos.ZERO).inflate(17, 17, 17);

    @SuppressWarnings("unused")
    public TeslaTowerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public TeslaTowerRenderState createRenderState() {
        return new TeslaTowerRenderState();
    }

    @Override
    public void extractRenderState(
        TeslaTowerBlockEntity be,
        TeslaTowerRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.setStart(new Vec3(0.5, 3.5, 0.5));
        if (be.getTargetEntityUUID() != null) {
            Entity entity = be.getLevel().getEntities().get(be.getTargetEntityUUID());
            if (entity == null) return;
            state.setEnd(entity.getEyePosition());
        } else if (be.getTargetLightningRod() != null) {
            state.setEnd(be.getTargetLightningRod().getCenter().add(0.0, 0.3, 0.0));
        } else {
            return;
        }
        BlockPos pos = be.getBlockPos();
        state.setEnd(state.getEnd().subtract(pos.getCenter().subtract(0.5, 0.5, 0.5)));
        state.setCamera(cameraPosition.subtract(pos.getX(), pos.getY(), pos.getZ()));
    }

    @Override
    public void submit(
        TeslaTowerRenderState state,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        if (state.getStart() == null || state.getEnd() == null || state.getCamera() == null) {
            return;
        }
        collector.submitCustomGeometry(
            pose,
            ModRenderTypes.LIGHTNING,
            (last, consumer) -> this.submitLightning(
                last.pose(),
                consumer,
                state.getStart(),
                state.getEnd(),
                state.getCamera(),
                LIGHTNING_WIDTH,
                0.7F
            )
        );
    }

    @SuppressWarnings("SameParameterValue")
    public void submitLightning(
        Matrix4f matrix,
        VertexConsumer consumer,
        Vec3 start,
        Vec3 end,
        Vec3 localCamera,
        float width,
        float alpha
    ) {
        Vec3 dir = end.subtract(start).normalize();
        Vec3 mid = start.add(end).scale(0.5);
        Vec3 toCamera = localCamera.subtract(mid).normalize();
        Vec3 perp = dir.cross(toCamera).normalize().scale(width);

        float sx = (float) start.x;
        float sy = (float) start.y;
        float sz = (float) start.z;
        float ex = (float) end.x;
        float ey = (float) end.y;
        float ez = (float) end.z;
        float px = (float) perp.x;
        float py = (float) perp.y;
        float pz = (float) perp.z;

        consumer.addVertex(matrix, sx - px, sy - py, sz - pz)
            .setColor(0.6F, 0.7F, 1.0F, alpha)
            .setUv(0.0F, 0.0F)
            .setUv1(0, 0)
            .setUv2(240, 240)
            .setNormal(0, 1, 0);

        consumer.addVertex(matrix, ex - px, ey - py, ez - pz)
            .setColor(0.6F, 0.7F, 1.0F, alpha)
            .setUv(1.0F, 0.0F)
            .setUv1(0, 0)
            .setUv2(240, 240)
            .setNormal(0, 1, 0);

        consumer.addVertex(matrix, ex + px, ey + py, ez + pz)
            .setColor(0.6F, 0.7F, 1.0F, alpha)
            .setUv(1.0F, 1.0F)
            .setUv1(0, 0)
            .setUv2(240, 240)
            .setNormal(0, 1, 0);

        consumer.addVertex(matrix, sx + px, sy + py, sz + pz)
            .setColor(0.6F, 0.7F, 1.0F, alpha)
            .setUv(0.0F, 1.0F)
            .setUv1(0, 0)
            .setUv2(240, 240)
            .setNormal(0, 1, 0);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(TeslaTowerBlockEntity blockEntity) {
        return TeslaTowerRenderer.BASE_RENDER_BBOX
            .move(blockEntity.getBlockPos())
            .move(0, 4, 0);
    }
}
