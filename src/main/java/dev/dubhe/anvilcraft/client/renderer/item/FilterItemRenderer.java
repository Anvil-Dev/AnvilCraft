package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.client.support.RenderModelSupport;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;

public class FilterItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ItemStack BARRIER = Items.BARRIER.getDefaultInstance();

    @Nullable
    private static FilterItemRenderer instance;

    private FilterItemRenderer(
        BlockEntityRenderDispatcher blockEntityRenderDispatcher,
        EntityModelSet entityModelSet
    ) {
        super(blockEntityRenderDispatcher, entityModelSet);
    }

    public static FilterItemRenderer getInstance() {
        if (instance == null) {
            Minecraft minecraft = Minecraft.getInstance();
            instance = new FilterItemRenderer(
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
        if (!stack.is(ModItems.FILTER)) {
            return;
        }

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getItemModelShaper().getItemModel(stack);
        FilterItemRenderer.renderModel(itemRenderer, stack, poseStack, buffer, packedLight, packedOverlay, model);

        Displaying displaying = FilterItemRenderer.readDisplaying(stack);
        ItemStack displayed = displaying.stack();
        if (displayed.isEmpty()) {
            return;
        }

        final double z = RenderModelSupport.getSize(model).maxZ;
        model = itemRenderer.getItemModelShaper().getItemModel(displayed);
        poseStack.pushPose();
        poseStack.translate(0, 0, z + 0.0005F);
        poseStack.scale(0.5F, 0.5F, 0.0005F);
        poseStack.translate(1, 1, 0);
        displayContext = ItemDisplayContext.FIXED;
        if (model.isGui3d()) {
            displayContext = ItemDisplayContext.GUI;
        } else {
            poseStack.scale(-1, 1, -1);
        }
        itemRenderer.renderStatic(
            displayed,
            displayContext,
            packedLight,
            packedOverlay,
            poseStack,
            buffer,
            null,
            0
        );
        poseStack.popPose();

        if (displaying.blackList()) {
            poseStack.pushPose();
            poseStack.translate(0, 0, z + 0.001F);
            poseStack.scale(0.35F, 0.35F, 0.0005F);
            poseStack.translate(0.8, 2.05, 0);
            poseStack.scale(-1, 1, -1);
            itemRenderer.renderStatic(
                FilterItemRenderer.BARRIER,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                buffer,
                null,
                0
            );
            poseStack.popPose();
        }
    }

    private static Displaying readDisplaying(ItemStack stack) {
        FilterContent content = stack.get(ModComponents.FILTER_CONTENT);
        if (content == null) return new Displaying();
        for (ItemStack filter : content.list()) {
            if (!filter.isEmpty()) {
                return new Displaying(filter, content.blackList());
            }
        }
        return new Displaying();
    }

    record Displaying(ItemStack stack, boolean blackList) {
        public Displaying() {
            this(ItemStack.EMPTY, false);
        }
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
