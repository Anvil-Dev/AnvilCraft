package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.block.TeslaTowerBlock;
import dev.dubhe.anvilcraft.block.entity.TeslaTowerBlockEntity;
import dev.dubhe.anvilcraft.block.state.Vertical4PartHalf;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class TeslaTowerRenderer implements BlockEntityRenderer<TeslaTowerBlockEntity> {
    private static final float LIGHTNING_WIDTH = 1f;
    private static final AABB BASE_RENDER_BBOX = new AABB(BlockPos.ZERO).inflate(17, 17, 17);

    @SuppressWarnings("unused")
    public TeslaTowerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        TeslaTowerBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        if (blockEntity.getBlockState().getValue(TeslaTowerBlock.HALF) != Vertical4PartHalf.BOTTOM) {
            return;
        }
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        if (level.getGameTime() - blockEntity.getLastStrikeTime() > 5) {
            return;
        }
        Vec3 end;
        if (blockEntity.getTargetEntityUUID() != null) {
            Entity entity = level.getEntities().get(blockEntity.getTargetEntityUUID());
            if (entity == null) {
                return;
            }
            end = entity.getEyePosition();
        } else if (blockEntity.getTargetLightningRod() != null) {
            end = blockEntity.getTargetLightningRod().getCenter().add(0.0, 0.3, 0.0);
        } else {
            return;
        }
        end = end.subtract(blockEntity.getBlockPos().getCenter().subtract(0.5, 0.5, 0.5));
        poseStack.pushPose();
        Vec3 start = new Vec3(0.5, 3.5, 0.5);
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        BlockPos pos = blockEntity.getBlockPos();
        Vec3 localCamera = cameraPos.subtract(pos.getX(), pos.getY(), pos.getZ());

        renderLightning(poseStack, bufferSource, start, end, localCamera, LIGHTNING_WIDTH, 0.7f);

        poseStack.popPose();
    }

    @SuppressWarnings("SameParameterValue")
    public void renderLightning(
        PoseStack poseStack,
        MultiBufferSource buffer,
        Vec3 start,
        Vec3 end,
        Vec3 localCamera,
        float width,
        float alpha
    ) {
        VertexConsumer consumer = buffer.getBuffer(ModRenderTypes.LIGHTNING);
        Matrix4f matrix = poseStack.last().pose();

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
            .setColor(0.6f, 0.7f, 1.0f, alpha)
            .setUv(0.0f, 0.0f)
            .setUv1(0, 0)
            .setUv2(240, 240)
            .setNormal(0, 1, 0);

        consumer.addVertex(matrix, ex - px, ey - py, ez - pz)
            .setColor(0.6f, 0.7f, 1.0f, alpha)
            .setUv(1.0f, 0.0f)
            .setUv1(0, 0)
            .setUv2(240, 240)
            .setNormal(0, 1, 0);

        consumer.addVertex(matrix, ex + px, ey + py, ez + pz)
            .setColor(0.6f, 0.7f, 1.0f, alpha)
            .setUv(1.0f, 1.0f)
            .setUv1(0, 0)
            .setUv2(240, 240)
            .setNormal(0, 1, 0);

        consumer.addVertex(matrix, sx + px, sy + py, sz + pz)
            .setColor(0.6f, 0.7f, 1.0f, alpha)
            .setUv(0.0f, 1.0f)
            .setUv1(0, 0)
            .setUv2(240, 240)
            .setNormal(0, 1, 0);
    }

    @Override
    public boolean shouldRenderOffScreen(TeslaTowerBlockEntity blockEntity) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(TeslaTowerBlockEntity blockEntity) {
        return TeslaTowerRenderer.BASE_RENDER_BBOX
            .move(blockEntity.getPos())
            .move(0, 4, 0);
    }
}
