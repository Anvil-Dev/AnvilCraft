package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public class CrabClawItemInHandRenderer extends AbstractItemInHandRenderer {
    public static final StandaloneModelKey<BlockStateModel> HOLDING_ITEM =
        new StandaloneModelKey<>(() -> "AnvilCraft: Crab Claw Holding Item Model");
    public static final StandaloneModelKey<BlockStateModel> HOLDING_BLOCK =
        new StandaloneModelKey<>(() -> "AnvilCraft: Crab Claw Holding Block Model");

    private final ItemStackRenderState itemRenderState = new ItemStackRenderState();
    private final BlockModelRenderState blockModelRenderState = new BlockModelRenderState();

    protected CrabClawItemInHandRenderer(ItemModelResolver itemModelResolver, IItemRenderer renderer) {
        super(itemModelResolver, renderer);
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
        SubmitNodeCollector collector,
        int lightCoords
    ) {
        if (hand == InteractionHand.OFF_HAND) {
            return;
        }
        boolean flag = hand == InteractionHand.MAIN_HAND;
        HumanoidArm humanoidarm = flag ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean flag2 = humanoidarm == HumanoidArm.LEFT;
        final int i = flag2 ? -1 : 1;
        if (this.mainHandItem.isEmpty()) {
            this.renderItem(
                player,
                this.offHandItem,
                ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                poseStack,
                collector,
                lightCoords
            );
            return;
        }
        boolean isBlockItem = this.mainHandItem.getItem() instanceof BlockItem;
        switch (stack.getUseAnimation()) {
            case EAT:
            case DRINK:
                if (
                    player.isUsingItem()
                        && player.getUseItemRemainingTicks() > 0
                        && player.getUsedItemHand() == hand
                ) {
                    poseStack.translate(0, -0.25F, 0.05F);
                }
                break;
            case NONE:
                break;
            default:
                return;
        }
        if (stack.getItem() instanceof FishingRodItem) return;

        // Render the crab claw holding model (standalone block model)
        Minecraft mc = Minecraft.getInstance();
        mc.getModelManager().getStandaloneModel(isBlockItem ? HOLDING_BLOCK : HOLDING_ITEM).collectParts(
            mc.level,
            player.blockPosition(),
            player.level().getBlockState(player.blockPosition()),
            net.minecraft.util.RandomSource.create(),
            this.blockModelRenderState.setupModel(new org.joml.Matrix4f(), false)
        );
        this.blockModelRenderState.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, 0);

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

        // Render the off-hand item on top of the claw
        this.itemModelResolver.updateForTopItem(
            this.itemRenderState,
            this.offHandItem,
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
            player.level(),
            player,
            player.getId() + ItemDisplayContext.FIRST_PERSON_RIGHT_HAND.ordinal()
        );
        this.itemRenderState.submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }
}
