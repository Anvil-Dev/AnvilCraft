package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

public final class ItemUseAnimationTransform {
    private ItemUseAnimationTransform() {
    }

    public static boolean applyCrossbowCharge(
        PoseStack poseStack,
        LocalPlayer player,
        HumanoidArm arm,
        ItemStack stack,
        float partialTick,
        float equipProgress,
        int chargeTicks
    ) {
        if (!ItemUseAnimationTransform.isUsingArm(player, arm)) return false;

        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        // 1.21.1 只对原版弩应用装填矩阵，此处为普通物品复用同一套变换。
        ItemUseAnimationTransform.applyItemArmTransform(poseStack, direction, equipProgress);
        poseStack.translate(direction * -0.4785682F, -0.094387F, 0.05731531F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-11.935F));
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * 65.3F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * -9.785F));

        float elapsedTicks = stack.getUseDuration(player)
                             - (player.getUseItemRemainingTicks() - partialTick + 1.0F);
        float progress = Mth.clamp(elapsedTicks / Math.max(1, chargeTicks), 0.0F, 1.0F);
        if (progress > 0.1F) {
            float shake = Mth.sin((elapsedTicks - 0.1F) * 1.3F) * (progress - 0.1F);
            poseStack.translate(0.0F, shake * 0.004F, 0.0F);
        }

        poseStack.translate(0.0F, 0.0F, progress * 0.04F);
        poseStack.scale(1.0F, 1.0F, 1.0F + progress * 0.2F);
        poseStack.mulPose(Axis.YN.rotationDegrees(direction * 45.0F));
        return true;
    }

    public static boolean applySwordBlock(
        PoseStack poseStack,
        LocalPlayer player,
        HumanoidArm arm,
        float equipProgress
    ) {
        if (!ItemUseAnimationTransform.isUsingArm(player, arm)) return false;

        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        // 复用旧版举剑格挡的第一人称矩阵。
        ItemUseAnimationTransform.applyItemArmTransform(poseStack, direction, equipProgress);
        poseStack.translate(direction * -0.14142136F, 0.08F, 0.14142136F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-102.25F));
        poseStack.mulPose(Axis.YP.rotationDegrees(direction * 13.365F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * 78.05F));
        return true;
    }

    private static boolean isUsingArm(LocalPlayer player, HumanoidArm arm) {
        if (!player.isUsingItem() || player.getUseItemRemainingTicks() <= 0) return false;
        HumanoidArm usedArm = player.getUsedItemHand() == InteractionHand.MAIN_HAND
            ? player.getMainArm()
            : player.getMainArm().getOpposite();
        return arm == usedArm;
    }

    private static void applyItemArmTransform(PoseStack poseStack, int direction, float equipProgress) {
        poseStack.translate(direction * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
    }
}
