package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * 仓储端口物品渲染器：与创造板条箱一致，在物品模型的六个面上渲染标记物品。
 */
public class StoragePortItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final String TAG_MARKED = "marked_item";

    @Nullable
    private static StoragePortItemRenderer instance;

    private StoragePortItemRenderer(
        BlockEntityRenderDispatcher blockEntityRenderDispatcher,
        EntityModelSet entityModelSet
    ) {
        super(blockEntityRenderDispatcher, entityModelSet);
    }

    public static StoragePortItemRenderer getInstance() {
        if (instance == null) {
            Minecraft minecraft = Minecraft.getInstance();
            instance = new StoragePortItemRenderer(
                minecraft.getBlockEntityRenderDispatcher(),
                minecraft.getEntityModels()
            );
        }
        return instance;
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
        if (!stack.is(ModBlocks.STORAGE_PORT.asItem())) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getItemModelShaper().getItemModel(stack);
        renderModel(itemRenderer, stack, poseStack, buffer, packedLight, packedOverlay, model);

        ItemStack marked = readMarkedItem(stack);
        if (marked.isEmpty()) {
            return;
        }
        renderMarkedOnFace(itemRenderer, marked, poseStack, buffer, packedLight, packedOverlay, pose -> {
            pose.translate(0.5, 0.5, 0.9);
            pose.scale(0.8f, 0.8f, 0.8f);
        });
        renderMarkedOnFace(itemRenderer, marked, poseStack, buffer, packedLight, packedOverlay, pose -> {
            pose.translate(0.5, 0.5, 0.1);
            pose.scale(0.8f, 0.8f, 0.8f);
        });
        renderMarkedOnFace(itemRenderer, marked, poseStack, buffer, packedLight, packedOverlay, pose -> {
            pose.translate(0.9, 0.5, 0.5);
            pose.scale(0.8f, 0.8f, 0.8f);
            pose.mulPose(Axis.YP.rotationDegrees(90));
        });
        renderMarkedOnFace(itemRenderer, marked, poseStack, buffer, packedLight, packedOverlay, pose -> {
            pose.translate(0.1, 0.5, 0.5);
            pose.scale(0.8f, 0.8f, 0.8f);
            pose.mulPose(Axis.YP.rotationDegrees(90));
        });
        renderMarkedOnFace(itemRenderer, marked, poseStack, buffer, packedLight, packedOverlay, pose -> {
            pose.translate(0.5, 0.1, 0.5);
            pose.scale(0.8f, 0.8f, 0.8f);
            pose.mulPose(Axis.XP.rotationDegrees(90));
        });
        renderMarkedOnFace(itemRenderer, marked, poseStack, buffer, packedLight, packedOverlay, pose -> {
            pose.translate(0.5, 0.9, 0.5);
            pose.scale(0.8f, 0.8f, 0.8f);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(180));
        });
    }

    /** 在端口的一个面上渲染标记物品。 */
    private static void renderMarkedOnFace(
        ItemRenderer itemRenderer,
        ItemStack marked,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay,
        Consumer<PoseStack> transform
    ) {
        poseStack.pushPose();
        transform.accept(poseStack);
        itemRenderer.renderStatic(
            marked,
            ItemDisplayContext.FIXED,
            packedLight,
            packedOverlay,
            poseStack,
            buffer,
            Minecraft.getInstance().level,
            0
        );
        poseStack.popPose();
    }

    private static ItemStack readMarkedItem(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || data.isEmpty()) {
            return ItemStack.EMPTY;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(StoragePortItemRenderer.TAG_MARKED, CompoundTag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }
        Minecraft minecraft = Minecraft.getInstance();
        HolderLookup.Provider registries;
        if (minecraft.level != null) {
            registries = minecraft.level.registryAccess();
        } else if (minecraft.getConnection() != null) {
            registries = minecraft.getConnection().registryAccess();
        } else {
            return ItemStack.EMPTY;
        }
        return ItemStack.parseOptional(registries, tag.getCompound(StoragePortItemRenderer.TAG_MARKED));
    }

    private static void renderModel(
        ItemRenderer itemRenderer,
        ItemStack stack,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay,
        BakedModel model
    ) {
        for (BakedModel pass : model.getRenderPasses(stack, true)) {
            for (RenderType renderType : pass.getRenderTypes(stack, true)) {
                VertexConsumer vertices = ItemRenderer.getFoilBuffer(
                    buffer,
                    renderType,
                    true,
                    stack.hasFoil()
                );
                itemRenderer.renderModelLists(
                    pass,
                    stack,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    vertices
                );
            }
        }
    }
}