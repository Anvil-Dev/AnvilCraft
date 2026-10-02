package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.item.CheckValveItem;
import dev.dubhe.anvilcraft.client.support.ProcessingModelShape;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.block.PipeBlockItem;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdItem;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdMode;
import dev.dubhe.anvilcraft.item.weapon.EnergyWeaponItem;
import dev.dubhe.anvilcraft.mixin.client.ItemStackRenderStateAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CrabClawItemInHandRenderer extends AbstractItemInHandRenderer {
    public static final StandaloneModelKey<HoldingModel> HOLDING_ITEM = new StandaloneModelKey<>(() -> "AnvilCraft: Crab Claw Item");
    public static final StandaloneModelKey<HoldingModel> HOLDING_BLOCK = new StandaloneModelKey<>(() -> "AnvilCraft: Crab Claw Block");
    public static final StandaloneModelKey<HoldingModel> HOLDING_BLOCK_SLAB = new StandaloneModelKey<>(() -> "AnvilCraft: Crab Claw Slab");
    public static final StandaloneModelKey<HoldingModel> HOLDING_BLOCK_PANEL =
        new StandaloneModelKey<>(() -> "AnvilCraft: Crab Claw Panel");
    private static final float PANEL_MAX_HEIGHT = 0.4F;
    private static final float SLAB_MAX_HEIGHT = 0.625F;
    private final ModelState modelState = new ModelState();
    private List<Object> cachedModels = List.of();
    private float cachedHeight;
    private @Nullable BlockState cachedHeightState;

    protected CrabClawItemInHandRenderer(ItemModelResolver resolver, IItemRenderer renderer) {
        super(resolver, renderer);
    }

    public static void registerModels(ModelEvent.RegisterStandalone event) {
        registerModel(event, HOLDING_ITEM, "crab_claw_holding_item");
        registerModel(event, HOLDING_BLOCK, "crab_claw_holding_block");
        registerModel(event, HOLDING_BLOCK_SLAB, "crab_claw_holding_block_slab");
        registerModel(event, HOLDING_BLOCK_PANEL, "crab_claw_holding_block_panel");
    }

    private static void registerModel(ModelEvent.RegisterStandalone event, StandaloneModelKey<HoldingModel> key, String name) {
        event.register(key, new SimpleUnbakedStandaloneModel<>(AnvilCraft.of("item/" + name), (model, baker, debugName) ->
            new HoldingModel(model.bakeTopGeometry(model.getTopTextureSlots(), baker, BlockModelRotation.IDENTITY).getAll(),
                model.getTopTransforms())));
    }

    private float modelHeight(@Nullable BlockState state) {
        if (!this.cachedModels.equals(this.modelState.models) || state != this.cachedHeightState) {
            this.cachedModels = List.copyOf(this.modelState.models);
            this.cachedHeightState = state;
            float minimum = Float.POSITIVE_INFINITY;
            float maximum = Float.NEGATIVE_INFINITY;
            ItemStackRenderState renderState = this.modelState;
            for (var layer : ((ItemStackRenderStateAccessor) renderState).anvilcraft$getLayers()) {
                for (var quad : layer.prepareQuadList()) {
                    for (int vertex = 0; vertex < 4; vertex++) {
                        float y = quad.position(vertex).y();
                        minimum = Math.min(minimum, y);
                        maximum = Math.max(maximum, y);
                    }
                }
            }
            this.cachedHeight = Float.isFinite(minimum) ? Math.max(0, maximum - minimum) : 0;
        }
        return this.cachedHeight;
    }

    private static boolean isBlockLikeItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem || stack.getItem() instanceof PipeBlockItem
            || stack.getItem() instanceof CheckValveItem;
    }

    private static boolean isSpearLike(ItemStack stack) {
        return stack.getUseAnimation() == ItemUseAnimation.TRIDENT || stack.getUseAnimation() == ItemUseAnimation.SPEAR
            || stack.getItem() instanceof HeavyHalberdItem && HeavyHalberdItem.getMode(stack) == HeavyHalberdMode.SPEAR;
    }

    private static boolean isThrowingSpear(AbstractClientPlayer player, ItemStack stack, InteractionHand hand) {
        return stack.getUseAnimation() == ItemUseAnimation.TRIDENT && player.isUsingItem()
            && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand;
    }

    @Override
    public boolean render(
        AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress,
        ItemStack stack, float equippedProgress, PoseStack pose, SubmitNodeCollector collector, int light
    ) {
        if (hand == InteractionHand.OFF_HAND) {
            pose.popPose();
            return true;
        }
        boolean left = player.getMainArm() == HumanoidArm.LEFT;
        final int sign = left ? -1 : 1;
        if (this.mainHandItem.isEmpty()) {
            this.renderItem(player, this.offHandItem,
                left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, pose, collector, light);
            return false;
        }
        if (stack.getItem() instanceof EnergyWeaponItem || stack.is(ModBlocks.CELESTIAL_FORGING_ANVIL.asItem())) return false;
        this.itemModelResolver.updateForTopItem(this.modelState, this.mainHandItem, ItemDisplayContext.NONE, player.level(), player, 0);
        boolean block = this.modelState.isThreeDimensional() && isBlockLikeItem(this.mainHandItem);
        BlockState state = this.mainHandItem.getItem() instanceof BlockItem item ? item.getBlock().defaultBlockState() : null;
        float height = this.modelHeight(state);
        boolean panel = block && height > 0 && height <= PANEL_MAX_HEIGHT;
        boolean slab = block && height > 0 && !panel && height <= SLAB_MAX_HEIGHT;
        var key = !block ? HOLDING_ITEM : panel ? HOLDING_BLOCK_PANEL : slab ? HOLDING_BLOCK_SLAB : HOLDING_BLOCK;
        switch (stack.getUseAnimation()) {
            case EAT, DRINK -> {
                if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                    pose.translate(0, -0.25F, 0.05F);
                }
            }
            case NONE, SPEAR, TRIDENT -> {
            }
            default -> {
                return false;
            }
        }
        if (stack.getItem() instanceof FishingRodItem) return false;
        var holding = Minecraft.getInstance().getModelManager().getStandaloneModel(key);
        pose.pushPose();
        if (isThrowingSpear(player, stack, hand)) pose.mulPose(Axis.XP.rotationDegrees(90 * sign));
        holding.transforms().getTransform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).apply(left, pose.last());
        collector.submitItem(pose, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, light, OverlayTexture.NO_OVERLAY, 0,
            new int[0], holding.quads(),
            this.offHandItem.hasFoil() ? ItemStackRenderState.FoilType.STANDARD : ItemStackRenderState.FoilType.NONE);
        pose.popPose();
        if (block) {
            pose.mulPose(Axis.YP.rotationDegrees(60 * sign));
            pose.mulPose(Axis.XP.rotationDegrees(panel ? 45 : 25));
            pose.scale(0.5F, 0.5F, 0.5F);
            pose.translate(0.25F * sign, panel ? 0.2F : 0.4F, panel ? -0.35F : -0.1F);
        } else {
            pose.mulPose(Axis.ZP.rotationDegrees(5 * sign));
            pose.scale(0.75F, 0.75F, 0.75F);
            pose.translate(0, 0.45F, 0.02F);
            if (stack.getItem() instanceof MaceItem) {
                pose.mulPose(Axis.YP.rotationDegrees(-10 * sign));
                pose.translate(0.08F * sign, -0.1F, 0);
            }
            if (isSpearLike(stack)) pose.translate(-0.23F * sign, 0, 0.07F);
        }
        return false;
    }

    public record HoldingModel(List<BakedQuad> quads, ItemTransforms transforms) {
    }

    private static final class ModelState extends ItemStackRenderState {
        private final List<Object> models = new ArrayList<>();
        private boolean knownGeometry;
        private boolean threeDimensional;

        @Override
        public void clear() {
            super.clear();
            this.models.clear();
            this.knownGeometry = false;
            this.threeDimensional = false;
        }

        @Override
        public void appendModelIdentityElement(Object element) {
            this.models.add(element);
            if (!this.knownGeometry && element instanceof ProcessingModelShape shape) {
                this.knownGeometry = true;
                this.threeDimensional = shape.anvilcraft$isThreeDimensional();
            }
        }

        private boolean isThreeDimensional() {
            return !this.isEmpty() && (this.knownGeometry ? this.threeDimensional : this.usesBlockLight());
        }
    }
}
