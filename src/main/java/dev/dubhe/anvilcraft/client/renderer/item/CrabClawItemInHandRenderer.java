package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class CrabClawItemInHandRenderer extends AbstractItemInHandRenderer {

    @Unique
    private static final ModelResourceLocation anvilCraft$HOLDING_ITEM =
        ModelResourceLocation.standalone(AnvilCraft.of("item/crab_claw_holding_item"));

    @Unique
    private static final ModelResourceLocation anvilCraft$HOLDING_BLOCK =
        ModelResourceLocation.standalone(AnvilCraft.of("item/crab_claw_holding_block"));

    protected CrabClawItemInHandRenderer(ItemRenderer itemRenderer, IItemRenderer iItemRenderer) {
        super(itemRenderer, iItemRenderer);
    }

    @Override
    public void render(
        AbstractClientPlayer player,
        float partialTicks,
        float pitch,
        InteractionHand hand,
        float swingProgress,
        ItemStack stack,
        float equippedProgress,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int combinedLight,
        CallbackInfo ci
    ) {
        if (hand == InteractionHand.OFF_HAND) {
            poseStack.popPose();
            ci.cancel();
            return;
        }
        boolean flag = hand == InteractionHand.MAIN_HAND;
        HumanoidArm humanoidarm = flag ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean flag2 = humanoidarm == HumanoidArm.LEFT;
        int i = flag2 ? -1 : 1;
        if (this.mainHandItem.isEmpty()) {
            this.renderItem(
                player,
                this.offHandItem,
                ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                flag2,
                poseStack,
                buffer,
                combinedLight
            );
            return;
        }
        boolean isBlockItem = this.itemRenderer.getModel(this.mainHandItem, player.level(), player, combinedLight).isGui3d()
            && this.mainHandItem.getItem() instanceof BlockItem;
        switch (stack.getUseAnimation()) {
            case EAT:
            case DRINK:
                if (
                    player.isUsingItem()
                        && player.getUseItemRemainingTicks() > 0
                        && player.getUsedItemHand() == hand
                ) {
                    poseStack.translate(0, -0.25f, 0.05f);
                }
                break;
            case NONE:
                break;
            default:
                return;
        }
        if (stack.getItem() instanceof FishingRodItem) return;
        this.itemRenderer.render(
            this.offHandItem,
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
            flag2,
            poseStack,
            buffer,
            combinedLight,
            OverlayTexture.NO_OVERLAY,
            this.itemRenderer
                .getItemModelShaper()
                .getModelManager()
                .getModel(isBlockItem ? anvilCraft$HOLDING_BLOCK : anvilCraft$HOLDING_ITEM)
        );
        if (isBlockItem) {
            poseStack.mulPose(Axis.YP.rotationDegrees(60f * i));
            poseStack.mulPose(Axis.XP.rotationDegrees(25f));
            poseStack.scale(0.5f, 0.5f, 0.5f);
            poseStack.translate(0.25f * i, 0.4f, -0.1f);
        } else {
            poseStack.mulPose(Axis.ZP.rotationDegrees(5f * i));
            poseStack.scale(0.75f, 0.75f, 0.75f);
            poseStack.translate(0, 0.45f, 0.02f);
            if (stack.getItem() instanceof MaceItem) {
                poseStack.mulPose(Axis.YP.rotationDegrees(-10f * i));
                poseStack.translate(0.08f * i, -0.1f, 0);
            }
        }
    }
}
