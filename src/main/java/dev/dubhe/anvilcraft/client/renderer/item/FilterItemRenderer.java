package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.client.support.FittedItemRenderer;
import dev.dubhe.anvilcraft.client.support.RenderModelSupport;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.Util;
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
    private static final long DISPLAY_INTERVAL_MILLIS = 1000;

    private boolean renderingDisplay;

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

        if (this.renderingDisplay || FittedItemRenderer.isRenderingPreview()) return;

        Displaying displaying = FilterItemRenderer.readDisplaying(stack);
        ItemStack displayed = displaying.stack();
        if (displayed.isEmpty()) {
            return;
        }

        final double z = RenderModelSupport.getSize(model).maxZ;
        poseStack.pushPose();
        this.renderingDisplay = true;
        try {
            poseStack.translate(0.5, 0.5, z + 0.01F);
            FittedItemRenderer.render(
                displayed, 8.0F / 16, poseStack, buffer, packedLight, packedOverlay
            );
            if (displaying.denyList()) {
                poseStack.translate(0, 0, 0.02F);
                FittedItemRenderer.render(
                    BARRIER, 4.0F / 16, poseStack, buffer, packedLight, packedOverlay
                );
            }
        } finally {
            this.renderingDisplay = false;
            poseStack.popPose();
        }
    }

    private static Displaying readDisplaying(ItemStack stack) {
        FilterContent content = stack.get(ModComponents.FILTER_CONTENT);
        if (content == null) return new Displaying();
        return new Displaying(selectDisplayed(content, Util.getMillis()), content.denyList());
    }

    static ItemStack selectDisplayed(FilterContent content, long timeMillis) {
        int count = 0;
        for (ItemStack filter : content.list()) {
            if (!filter.isEmpty()) count++;
        }
        if (count == 0) return ItemStack.EMPTY;
        int selected = (int) Math.floorMod(timeMillis / DISPLAY_INTERVAL_MILLIS, count);
        for (ItemStack filter : content.list()) {
            if (!filter.isEmpty() && selected-- == 0) return filter;
        }
        return ItemStack.EMPTY;
    }

    record Displaying(ItemStack stack, boolean denyList) {
        public Displaying() {
            this(ItemStack.EMPTY, false);
        }
    }

    static void renderModel(
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
