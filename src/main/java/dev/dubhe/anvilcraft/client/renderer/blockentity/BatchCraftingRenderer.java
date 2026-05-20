package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.util.Timer;
import dev.anvilcraft.lib.v2.util.ClientTickRecorder;
import dev.dubhe.anvilcraft.block.entity.batch.BaseBatchCraftingBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.BaseShowItemRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class BatchCraftingRenderer extends BaseShowItemRenderer<BaseBatchCraftingBlockEntity, BaseShowItemRenderState> {
    public BatchCraftingRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BaseShowItemRenderState createRenderState() {
        return new BaseShowItemRenderState();
    }

    @Override
    protected @Nullable ItemStack getDisplayItemStack(BaseBatchCraftingBlockEntity blockEntity) {
        return blockEntity.getDisplayingStack();
    }

    @Override
    public void submit(
        BaseShowItemRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera
    ) {
        ItemClusterRenderState displayState = state.getDisplayState();
        if (displayState == null) {
            return;
        }
        poseStack.pushPose();
        float rotation = (Minecraft.getInstance().level.getGameTime() + Timer.getPartialTick());
        AABB boundingBox = displayState.item.getModelBoundingBox();
        float minOffsetY = -((float) boundingBox.minY) + 0.0625F;
        poseStack.translate(0.5F, minOffsetY + 0.15f, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.popPose();
    }
}
