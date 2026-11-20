package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.MountedRedstone;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class MountedRedstonePose {
    private MountedRedstonePose() {
    }

    static void apply(PoseStack pose, BlockState state) {
        Direction attachment = state.getValue(MountedRedstone.ATTACHMENT);
        pose.translate(0.5f, 0.5f, 0.5f);
        if (attachment.getAxis().isHorizontal()) {
            pose.mulPose(Axis.YP.rotationDegrees(-attachment.getOpposite().toYRot()));
            pose.mulPose(Axis.XP.rotationDegrees(90));
        } else if (attachment == Direction.UP) {
            pose.mulPose(Axis.XP.rotationDegrees(180));
        }
        pose.mulPose(Axis.YP.rotationDegrees(-state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()));
        pose.translate(-0.5f, -0.5f, -0.5f);
    }
}
