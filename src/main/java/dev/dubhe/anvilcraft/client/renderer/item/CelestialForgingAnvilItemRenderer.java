package dev.dubhe.anvilcraft.client.renderer.item;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilBlock;
import dev.dubhe.anvilcraft.block.cfa.item.CelestialForgingAnvilBlockItem;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.client.renderer.blockentity.CelestialForgingAnvilBlockEntityRenderer;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nullable;

public class CelestialForgingAnvilItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final Cache<CustomData, CelestialForgingAnvilBlockEntity> previews = CacheBuilder.newBuilder()
        .maximumSize(16)
        .build();
    private @Nullable ClientLevel previewLevel;

    private CelestialForgingAnvilItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(
        ItemStack stack,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean head = displayContext == ItemDisplayContext.HEAD;
        poseStack.pushPose();
        if (!head) {
            if (displayContext != ItemDisplayContext.GUI) poseStack.translate(0, 1, 0);
            ItemRenderer itemRenderer = minecraft.getItemRenderer();
            BakedModel model = itemRenderer.getItemModelShaper().getItemModel(stack);
            for (BakedModel pass : model.getRenderPasses(stack, true)) {
                for (RenderType renderType : pass.getRenderTypes(stack, true)) {
                    VertexConsumer vertices = ItemRenderer.getFoilBuffer(buffer, renderType, true, stack.hasFoil());
                    itemRenderer.renderModelLists(pass, stack, packedLight, packedOverlay, poseStack, vertices);
                }
            }
        } else {
            BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
            CelestialForgingAnvilBlock block = ModBlocks.CELESTIAL_FORGING_ANVIL.get();
            for (Cube323PartHalf part : block.getParts()) {
                BlockState state = block.placedState(part, block.defaultBlockState());
                poseStack.pushPose();
                poseStack.translate(part.getOffsetX(), part.getOffsetY(), part.getOffsetZ());
                blockRenderer.renderSingleBlock(state, poseStack, buffer, packedLight, packedOverlay, ModelData.EMPTY, null);
                poseStack.popPose();
            }
        }
        if (minecraft.level == null) {
            poseStack.popPose();
            return;
        }
        if (this.previewLevel != minecraft.level) {
            this.previews.invalidateAll();
            this.previewLevel = minecraft.level;
        }
        CustomData data = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        CelestialForgingAnvilBlockEntity preview = this.previews.getIfPresent(data);
        if (preview == null) {
            preview = new CelestialForgingAnvilBlockEntity(
                ModBlockEntities.CELESTIAL_FORGING_ANVIL.get(), BlockPos.ZERO,
                ModBlocks.CELESTIAL_FORGING_ANVIL.getDefaultState()
            );
            CompoundTag tag = data.copyTag();
            tag.merge(tag.getCompound(CelestialForgingAnvilBlockItem.ITEM_RENDER_DATA));
            // Load before attaching the level so a stored body does not trigger the discovery animation.
            preview.loadWithComponents(tag, minecraft.level.registryAccess());
            preview.setLevel(minecraft.level);
            this.previews.put(data, preview);
        }
        var renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(preview);
        if (renderer instanceof CelestialForgingAnvilBlockEntityRenderer celestialRenderer) {
            if (head) {
                celestialRenderer.renderHeadItem(preview, poseStack, buffer, packedOverlay);
            } else {
                celestialRenderer.renderItemBody(preview, poseStack, buffer, packedOverlay);
            }
        }
        poseStack.popPose();
    }

    public static class ItemExtensions implements IClientItemExtensions {
        private @Nullable CelestialForgingAnvilItemRenderer renderer;

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) this.renderer = new CelestialForgingAnvilItemRenderer();
            return this.renderer;
        }

        @Override
        public boolean applyForgeHandTransform(
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm arm,
            ItemStack stack,
            float partialTick,
            float equipProgress,
            float swingProgress
        ) {
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            float swing = Mth.sin(Mth.sqrt(swingProgress) * Mth.PI);
            poseStack.translate(side * (0.42f - swing * 0.08f), -0.5f - equipProgress * 0.6f, -0.8f - swing * 0.15f);
            if (!player.isInvisible()) {
                Minecraft minecraft = Minecraft.getInstance();
                var entityRenderer = minecraft.getEntityRenderDispatcher().getRenderer(player);
                if (entityRenderer instanceof PlayerRenderer playerRenderer) {
                    poseStack.pushPose();
                    poseStack.translate(side * 5.0f / 16.0f, -0.125f, 0.75f);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
                    int light = LevelRenderer.getLightColor(player.level(), player.blockPosition());
                    MultiBufferSource buffer = minecraft.renderBuffers().bufferSource();
                    if (arm == HumanoidArm.RIGHT) {
                        playerRenderer.renderRightHand(poseStack, buffer, light, player);
                    } else {
                        playerRenderer.renderLeftHand(poseStack, buffer, light, player);
                    }
                    poseStack.popPose();
                }
            }
            return true;
        }
    }
}
