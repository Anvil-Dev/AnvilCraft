package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.ConfinementChamberBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.BaseShowItemRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class ConfinementChamberRenderer extends BaseShowItemRenderer<ConfinementChamberBlockEntity, BaseShowItemRenderState> {
    public ConfinementChamberRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public BaseShowItemRenderState createRenderState() {
        return new BaseShowItemRenderState();
    }

    @Override
    protected @Nullable ItemStack getDisplayItemStack(ConfinementChamberBlockEntity blockEntity) {
        return blockEntity.getItemHandler().copyToList().getFirst();
    }

    @Override
    public void submit(BaseShowItemRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.4, 0.5);
        super.submit(state, poseStack, submitNodeCollector, camera);
        poseStack.popPose();
    }
}
