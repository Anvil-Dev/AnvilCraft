package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.entity.model.CauldronOutletModel;
import dev.dubhe.anvilcraft.entity.CauldronOutletEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class CauldronOutletRenderer extends EntityRenderer<CauldronOutletEntity> {
    private final CauldronOutletModel model;

    public CauldronOutletRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CauldronOutletModel(context.bakeLayer(CauldronOutletModel.LAYER_LOCATION));
    }

    @Override
    public void render(
        CauldronOutletEntity entity,
        float entityYaw,
        float partialTicks,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight
    ) {
        poseStack.pushPose();

        // 视觉平滑移动处理
        BlockPos currentPos = entity.getCauldronPos();
        PistonMovingBlockEntity targetPiston = null;
        BlockPos pistonPos = null;

        // 搜索脚下和四周的活塞
        List<BlockPos> searchPositions = new ArrayList<>();
        searchPositions.add(currentPos);
        for (Direction dir : Direction.values()) {
            searchPositions.add(currentPos.relative(dir));
        }

        for (BlockPos pos : searchPositions) {
            if (entity.level().getBlockState(pos).is(Blocks.MOVING_PISTON)) {
                BlockEntity be = entity.level().getBlockEntity(pos);
                if (be instanceof PistonMovingBlockEntity pbe) {
                    // 计算这个活塞是从哪儿推过来的
                    Direction moveDir = pbe.isExtending() ? pbe.getDirection() : pbe.getDirection().getOpposite();
                    BlockPos origin = pos.relative(moveDir.getOpposite());

                    // 如果来源位置就是我的脚下，那它就是我的锅
                    if (origin.equals(currentPos)) {
                        targetPiston = pbe;
                        pistonPos = pos;
                        break;
                    }
                }
            }
        }

        // 找到了关联活塞，进行视觉修正
        if (targetPiston != null) {
            // 获取平滑移动进度
            float xoff = targetPiston.getXOff(partialTicks);
            float yoff = targetPiston.getYOff(partialTicks);
            float zoff = targetPiston.getZOff(partialTicks);

            // 计算位移差值：目标位置 - 当前位置 + 动画偏移
            double dx = (pistonPos.getX() - currentPos.getX()) + xoff;
            double dy = (pistonPos.getY() - currentPos.getY()) + yoff;
            double dz = (pistonPos.getZ() - currentPos.getZ()) + zoff;

            poseStack.translate(dx, dy, dz);
        }

        // 不同方向的模型渲染
        Direction direction = entity.getAttachedDirection();
        switch (direction) {
            case DOWN -> {
                poseStack.translate(0.0, 0.125, 0.0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            }
            case SOUTH -> {
                poseStack.translate(0.0, 0.18375, 0.0);
                poseStack.mulPose(Axis.YN.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-120));
            }
            case WEST -> {
                poseStack.translate(0.0, 0.18375, 0.0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(120));
            }
            case EAST -> {
                poseStack.translate(0.0, 0.18375, 0.0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-120));
            }
            default -> {
                poseStack.translate(0.0, 0.18375, 0.0);
                poseStack.mulPose(Axis.YN.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(120));
            }
        }
        poseStack.scale(0.73f, 0.73f, 0.73f);

        var consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CauldronOutletEntity entity) {
        return AnvilCraft.of("textures/block/cauldron_outlet.png");
    }
}