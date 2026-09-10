package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.item.CheckValveItem;
import dev.dubhe.anvilcraft.block.item.PipeBlockItem;
import dev.dubhe.anvilcraft.client.support.RenderModelSupport;
import dev.dubhe.anvilcraft.item.HeavyHalberdItem;
import dev.dubhe.anvilcraft.item.weapon.EnergyWeaponItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

public class CrabClawItemInHandRenderer extends AbstractItemInHandRenderer {
    private static final ModelResourceLocation HOLDING_ITEM =
        ModelResourceLocation.standalone(AnvilCraft.of("item/crab_claw_holding_item"));
    private static final ModelResourceLocation HOLDING_BLOCK =
        ModelResourceLocation.standalone(AnvilCraft.of("item/crab_claw_holding_block"));
    private static final ModelResourceLocation HOLDING_BLOCK_SLAB =
        ModelResourceLocation.standalone(AnvilCraft.of("item/crab_claw_holding_block_slab"));
    private static final ModelResourceLocation HOLDING_BLOCK_PANEL =
        ModelResourceLocation.standalone(AnvilCraft.of("item/crab_claw_holding_block_panel"));
    /**
     * 扁平方块高度上限（模型空间 0~1 的方块坐标）：压力板、地毯、活板门、阳光探测器等
     * 不高过该值的模型用 panel 档，改用闭合开口并下调握持高度、放大外移量。
     */
    private static final float PANEL_MAX_HEIGHT = 0.4f;
    /**
     * 台阶档高度上限：台阶（0.5）与管道（0.625）等不高过该值的模型用 slab 档
     * （张开开口、钳口更靠内），再高则用方块档。
     */
    private static final float SLAB_MAX_HEIGHT = 0.625f;
    /** 方块档与台阶档握持时物品的竖直偏移（缩放前） */
    private static final float BLOCK_HOLD_Y = 0.4f;
    /**
     * 薄片档握持时物品的竖直偏移（缩放前）。<br>
     * 薄模型比整方块矮得多，沿用方块档高度会顶到蟹钳上颚而被遮挡，故单独下调。
     */
    private static final float PANEL_HOLD_Y = 0.2f;
    /** 握持方块时的水平旋转（缩放前） */
    private static final float BLOCK_HOLD_YAW = 60f;
    /** 方块档与台阶档的前后倾角，使方块贴合张开钳口 */
    private static final float BLOCK_HOLD_PITCH = 25f;
    /** 薄片档的前后倾角，比方块档更陡以贴合闭合钳口 */
    private static final float PANEL_HOLD_PITCH = 45f;
    /** 方块档与台阶档向外的偏移（缩放前） */
    private static final float BLOCK_HOLD_Z = -0.1f;
    /**
     * 薄片档向外的偏移（缩放前）。<br>
     * 手部变换位于 z = -0.72，即 -Z 指向屏幕内、远离玩家，故更小的值让薄片从钳口往外突出。
     */
    private static final float PANEL_HOLD_Z = -0.35f;
    /** 长柄武器向左的偏移（缩放前） */
    private static final float SPEAR_HOLD_X = -0.23f;
    /**
     * 长柄武器向后的偏移（缩放前）。<br>
     * 手部变换位于 z = -0.72，即 -Z 指向屏幕内（远离玩家），故 +Z 为向后、靠近玩家。
     */
    private static final float SPEAR_HOLD_Z = 0.07f;
    /**
     * 投掷蓄力时蟹钳绕 X 轴的偏转角，使钳口与戟柄成直角。<br>
     * 取反可翻转倾倒方向。
     */
    private static final float TRIDENT_THROW_CLAW_PITCH = 90f;

    private @Nullable BakedModel cachedHeightModel;
    private @Nullable BlockState cachedHeightState;
    private float cachedHeight;

    protected CrabClawItemInHandRenderer(ItemRenderer itemRenderer, IItemRenderer renderer) {
        super(itemRenderer, renderer);
    }

    /**
     * 实测模型包围盒的高度（Y 尺寸），整方块为 1.0、台阶为 0.5、压力板为 0.0625。<br>
     * 用高度而非最薄维度分档：栅栏这类多元素模型的最薄维度等于其立柱宽度 0.25，
     * 会与薄片混淆，而其高度 1.0 能正确反映它是个立体方块。<br>
     * 必须带上方块状态：部分多方块模型在状态为 null 时不会返回任何 quad，会量出退化包围盒。
     * 测不出几何数据时返回 0，由调用方退回方块档。
     */
    private float getModelHeight(BakedModel model, @Nullable BlockState state) {
        if (model != this.cachedHeightModel || state != this.cachedHeightState) {
            AABB size = state == null
                ? RenderModelSupport.getSize(model)
                : RenderModelSupport.getSize(state, model);
            float height = (float) size.getYsize();
            // 无几何数据时包围盒会退化成 min>max，尺寸为负；此时视为不可测量
            this.cachedHeight = Math.max(height, 0.0f);
            this.cachedHeightModel = model;
            this.cachedHeightState = state;
        }
        return this.cachedHeight;
    }

    /**
     * 是否为按方块握持的物品。<br>
     * 管道、止逆阀这类物品是自定义 {@link Item}（{@link PipeBlockItem}、{@link CheckValveItem}）
     * 而非 {@link BlockItem}，若只判断 {@link BlockItem} 会被当成普通物品渲染。
     */
    private static boolean isBlockLikeItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem
            || stack.getItem() instanceof PipeBlockItem
            || stack.getItem() instanceof CheckValveItem;
    }

    /**
     * 是否为需要向左偏移的长柄武器。<br>
     * 原版三叉戟等使用 {@link UseAnim#SPEAR} 手持动画的物品由动画类型识别；
     * 重戟的长矛模式不使用该动画（{@link UseAnim#NONE}），需按模式单独识别。
     */
    private static boolean isSpearLike(ItemStack stack) {
        if (stack.getUseAnimation() == UseAnim.SPEAR) {
            return true;
        }
        return stack.getItem() instanceof HeavyHalberdItem
            && HeavyHalberdItem.getMode(stack) == HeavyHalberdItem.SPEAR_MODE;
    }

    /**
     * 是否为正在蓄力投掷的长柄武器（原版三叉戟、重戟的三叉戟模式）。<br>
     * 这两类物品都以 {@link UseAnim#SPEAR} 作为手持动画，与重戟长矛模式的 {@link UseAnim#NONE} 区分。
     */
    private static boolean isThrowingSpear(AbstractClientPlayer player, ItemStack stack, InteractionHand hand) {
        return stack.getUseAnimation() == UseAnim.SPEAR
            && player.isUsingItem()
            && player.getUseItemRemainingTicks() > 0
            && player.getUsedItemHand() == hand;
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
        final int i = flag2 ? -1 : 1;
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
        if (stack.getItem() instanceof EnergyWeaponItem) return;
        BakedModel mainHandModel = this.itemRenderer.getModel(
            this.mainHandItem, player.level(), player, combinedLight
        );
        boolean isBlockItem = mainHandModel.isGui3d()
            && CrabClawItemInHandRenderer.isBlockLikeItem(this.mainHandItem);
        BlockState mainHandState = this.mainHandItem.getItem() instanceof BlockItem blockItem
            ? blockItem.getBlock().defaultBlockState()
            : null;
        // 按实测高度分档：薄片用闭合开口的 panel，台阶用钳口更靠内的 slab，更高的用 block。
        // 测不出几何数据（高度为 0）时一律按方块档处理，避免被误判成薄片
        float height = this.getModelHeight(mainHandModel, mainHandState);
        boolean measurable = height > 0.0f;
        boolean isPanel = isBlockItem && measurable && height <= PANEL_MAX_HEIGHT;
        boolean isSlab = isBlockItem && measurable && !isPanel && height <= SLAB_MAX_HEIGHT;
        ModelResourceLocation holdingModel;
        if (!isBlockItem) {
            holdingModel = HOLDING_ITEM;
        } else if (isPanel) {
            holdingModel = HOLDING_BLOCK_PANEL;
        } else if (isSlab) {
            holdingModel = HOLDING_BLOCK_SLAB;
        } else {
            holdingModel = HOLDING_BLOCK;
        }
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
            case SPEAR:
                break;
            default:
                return;
        }
        if (stack.getItem() instanceof FishingRodItem) return;
        // 投掷蓄力时蟹钳单独偏转，使钳口与戟柄成直角；只作用于蟹钳，主手物品不受影响
        poseStack.pushPose();
        if (CrabClawItemInHandRenderer.isThrowingSpear(player, stack, hand)) {
            poseStack.mulPose(Axis.XP.rotationDegrees(TRIDENT_THROW_CLAW_PITCH * i));
        }
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
                .getModel(holdingModel)
        );
        poseStack.popPose();
        if (isBlockItem) {
            poseStack.mulPose(Axis.YP.rotationDegrees(BLOCK_HOLD_YAW * i));
            poseStack.mulPose(Axis.XP.rotationDegrees(isPanel ? PANEL_HOLD_PITCH : BLOCK_HOLD_PITCH));
            poseStack.scale(0.5f, 0.5f, 0.5f);
            poseStack.translate(
                0.25f * i,
                isPanel ? PANEL_HOLD_Y : BLOCK_HOLD_Y,
                isPanel ? PANEL_HOLD_Z : BLOCK_HOLD_Z
            );
        } else {
            poseStack.mulPose(Axis.ZP.rotationDegrees(5f * i));
            poseStack.scale(0.75f, 0.75f, 0.75f);
            poseStack.translate(0, 0.45f, 0.02f);
            if (stack.getItem() instanceof MaceItem) {
                poseStack.mulPose(Axis.YP.rotationDegrees(-10f * i));
                poseStack.translate(0.08f * i, -0.1f, 0);
            }
            // 长柄武器向左、向后偏移，避免与蟹钳持握姿态重叠
            if (CrabClawItemInHandRenderer.isSpearLike(stack)) {
                poseStack.translate(SPEAR_HOLD_X * i, 0, SPEAR_HOLD_Z);
            }
        }
    }
}
