package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Quaternionf;

import java.util.List;

public class CrabClawItemInHandRenderer extends AbstractItemInHandRenderer {
    public static final StandaloneModelKey<QuadCollection> HOLDING_ITEM =
        new StandaloneModelKey<>(() -> "AnvilCraft: Crab Claw Holding Item Model");
    public static final StandaloneModelKey<QuadCollection> HOLDING_BLOCK =
        new StandaloneModelKey<>(() -> "AnvilCraft: Crab Claw Holding Block Model");

    protected CrabClawItemInHandRenderer(ItemModelResolver itemModelResolver, IItemRenderer renderer) {
        super(itemModelResolver, renderer);
    }

    @Override
    public boolean render(
        AbstractClientPlayer player,
        float partialTicks,
        float pitch,
        InteractionHand hand,
        float swingProgress,
        ItemStack stack,
        float equippedProgress,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int lightCoords
    ) {
        if (hand == InteractionHand.OFF_HAND) {
            poseStack.popPose();
            return true;
        }
        boolean flag = hand == InteractionHand.MAIN_HAND;
        HumanoidArm humanoidarm = flag ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean isLeftArmMainArm = humanoidarm == HumanoidArm.LEFT;
        final int i = isLeftArmMainArm ? -1 : 1;
        if (this.mainHandItem.isEmpty()) {
            this.renderItem(
                player,
                this.offHandItem,
                isLeftArmMainArm?
                        ItemDisplayContext.FIRST_PERSON_LEFT_HAND:
                        ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                poseStack,
                collector,
                lightCoords
            );
            return false;
        }

        switch (stack.getUseAnimation()) {
            case EAT:
            case DRINK:
                if (player.isUsingItem()
                    && player.getUseItemRemainingTicks() > 0
                    && player.getUsedItemHand() == hand
                ) {
                    poseStack.translate(0, -0.25F, 0.05F);
                }
                break;
            case NONE:
                break;
            default:
                return false;
        }
        if (stack.getItem() instanceof FishingRodItem) {
            return false;
        }

        boolean isBlockItem = this.mainHandItem.getItem() instanceof BlockItem;
        poseStack.pushPose();
        if (isBlockItem) {
            poseStack.translate(-0.32, 0.18, -0.3);
            poseStack.scale(0.68f, 0.68f, 0.68f);
            poseStack.mulPose(Axis.XP.rotationDegrees(30));
            poseStack.mulPose(Axis.YP.rotationDegrees(-2));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-20));

            if (isLeftArmMainArm) {
                poseStack.translate(0.13000008f, -0.33000007f, -2.9802322E-8f);
                poseStack.mulPose(
                        new Quaternionf()
                                .rotateLocalX(-0.01f)
                                .rotateLocalY( 1.4901161E-8f)
                                .rotateLocalZ( 0.7000004f)
                );
                poseStack.scale(1.0f, 1.0f, 1.0f);
            }

        } else {
            poseStack.translate(-0.26, 0.5, -0.16);
            poseStack.scale(0.68f, 0.68f, 0.68f);
            poseStack.mulPose(Axis.XP.rotationDegrees(30));
            poseStack.mulPose(Axis.YP.rotationDegrees(-10));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-70));

            if (isLeftArmMainArm) {
                poseStack.translate(1.0000001f, 0.3f, 0.0f);
                poseStack.mulPose(
                        new Quaternionf()
                                .rotateLocalX(0.0f)
                                .rotateLocalY(0.0f)
                                .rotateLocalZ(-3.7999988f)
                );
                poseStack.scale(1.0f, 1.0f, 1.0f);
            }

        }
        Minecraft mc = Minecraft.getInstance();
        List<BakedQuad> all = mc.getModelManager()
            .getStandaloneModel(isBlockItem ? HOLDING_BLOCK : HOLDING_ITEM)
            .getAll();
        collector.submitItem(
            poseStack,
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
            lightCoords,
            OverlayTexture.NO_OVERLAY,
            0,
            new int[]{},
            all,
            stack.hasFoil() ? ItemStackRenderState.FoilType.STANDARD : ItemStackRenderState.FoilType.NONE
        );

        poseStack.popPose();

        if (isBlockItem) {
            poseStack.mulPose(Axis.YP.rotationDegrees(60F * i));
            poseStack.mulPose(Axis.XP.rotationDegrees(25F));
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.translate(0.25F * i, 0.4F, -0.1F);
        } else {
            poseStack.mulPose(Axis.ZP.rotationDegrees(5F * i));
            poseStack.scale(0.75F, 0.75F, 0.75F);
            poseStack.translate(0, 0.45F, 0.02F);
            if (stack.getItem() instanceof MaceItem) {
                poseStack.mulPose(Axis.YP.rotationDegrees(-10F * i));
                poseStack.translate(0.08F * i, -0.1F, 0);
            }
        }
        return false;
    }
}
