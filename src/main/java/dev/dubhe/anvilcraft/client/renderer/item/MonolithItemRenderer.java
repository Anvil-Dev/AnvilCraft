package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.renderer.MonolithSurfaceRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jspecify.annotations.Nullable;

public class MonolithItemRenderer extends BlockEntityWithoutLevelRenderer {
    private MonolithItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(
        ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay
    ) {
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = renderer.getItemModelShaper().getItemModel(stack);
        for (BakedModel pass : model.getRenderPasses(stack, true)) {
            for (var type : pass.getRenderTypes(stack, true)) {
                var vertices = ItemRenderer.getFoilBufferDirect(buffers, type, true, stack.hasFoil());
                renderer.renderModelLists(pass, stack, light, overlay, pose, vertices);
            }
            MonolithSurfaceRenderer.renderItem(pass, pose, buffers);
        }
    }

    public static class ItemExtensions implements IClientItemExtensions {
        @Nullable
        private MonolithItemRenderer renderer;

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
                this.renderer = new MonolithItemRenderer();
            }
            return this.renderer;
        }
    }
}
