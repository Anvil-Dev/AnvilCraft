package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.client.renderer.entity.model.CauldronOutletModel;
import dev.dubhe.anvilcraft.client.renderer.entity.state.CauldronOutletRenderState;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.entity.CauldronOutletEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class CauldronOutletRenderer extends EntityRenderer<CauldronOutletEntity, CauldronOutletRenderState> {
    public static final Identifier TEXTURE = SharedTextures.texture("block/cauldron_outlet");
    private final CauldronOutletModel model;

    public CauldronOutletRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CauldronOutletModel(context.bakeLayer(CauldronOutletModel.LAYER_LOCATION));
    }

    @Override
    public CauldronOutletRenderState createRenderState() {
        return new CauldronOutletRenderState();
    }

    @Override
    public void extractRenderState(CauldronOutletEntity entity, CauldronOutletRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
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
            float xo = targetPiston.getXOff(partialTicks);
            float yo = targetPiston.getYOff(partialTicks);
            float zo = targetPiston.getZOff(partialTicks);

            // 计算位移差值：目标位置 - 当前位置 + 动画偏移
            double dx = (pistonPos.getX() - currentPos.getX()) + xo;
            double dy = (pistonPos.getY() - currentPos.getY()) + yo;
            double dz = (pistonPos.getZ() - currentPos.getZ()) + zo;

            state.setDx(dx);
            state.setDy(dy);
            state.setDz(dz);
        }

        // 不同方向的模型渲染
        Direction direction = entity.getAttachedDirection();
        switch (direction) {
            case DOWN -> {
                state.setDy(state.getDy() + 0.125);
                state.addRotation(Axis.ZP.rotationDegrees(180));
            }
            case SOUTH -> {
                state.setDy(state.getDy() + 0.18375);
                state.addRotation(Axis.YN.rotationDegrees(90));
                state.addRotation(Axis.ZP.rotationDegrees(-120));
            }
            case WEST -> {
                state.setDy(state.getDy() + 0.18375);
                state.addRotation(Axis.ZP.rotationDegrees(120));
            }
            case EAST -> {
                state.setDy(state.getDy() + 0.18375);
                state.addRotation(Axis.ZP.rotationDegrees(-120));
            }
            default -> {
                state.setDy(state.getDy() + 0.18375);
                state.addRotation(Axis.YN.rotationDegrees(90));
                state.addRotation(Axis.ZP.rotationDegrees(120));
            }
        }
    }

    @Override
    public void submit(
        CauldronOutletRenderState state,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        pose.pushPose();
        pose.translate(state.getDx(), state.getDy(), state.getDz());
        for (Quaternionf rotation : state.getRotation()) {
            pose.mulPose(rotation);
        }
        pose.scale(0.73F, 0.73F, 0.73F);
        collector.submitModel(
            this.model,
            Unit.INSTANCE,
            pose,
            TEXTURE,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            state.outlineColor,
            null
        );
        pose.popPose();
    }
}