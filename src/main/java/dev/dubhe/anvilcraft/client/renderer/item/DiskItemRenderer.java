package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.support.DiskDisplaySupport;
import dev.dubhe.anvilcraft.client.support.FittedItemRenderer;
import dev.dubhe.anvilcraft.client.support.RenderModelSupport;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class DiskItemRenderer extends BlockEntityWithoutLevelRenderer {
    private DiskItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static DiskItemRenderer getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public void renderByItem(
        ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay
    ) {
        if (!stack.is(ModItems.DISK) && !stack.is(ModItems.STRUCTURE_DISK)) return;
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = renderer.getItemModelShaper().getItemModel(stack);
        FilterItemRenderer.renderModel(renderer, stack, pose, buffers, light, overlay, model);
        if (FittedItemRenderer.isRenderingPreview()) return;
        ItemStack displayed = DiskDisplaySupport.getDisplay(stack);
        if (displayed.isEmpty()) return;
        pose.pushPose();
        try {
            boolean structure = stack.is(ModItems.STRUCTURE_DISK);
            pose.translate(0.5, structure ? 0.5 : 6.0 / 16, RenderModelSupport.getSize(model).maxZ + 0.01);
            FittedItemRenderer.render(displayed, (structure ? 8.0F : 6.0F) / 16, pose, buffers, light, overlay, structure);
        } finally {
            pose.popPose();
        }
    }

    private static class Holder {
        private static final DiskItemRenderer INSTANCE = new DiskItemRenderer();
    }
}
