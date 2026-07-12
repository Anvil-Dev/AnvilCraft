package dev.dubhe.anvilcraft.api.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

public record BlockStateModelTessellateState(
    StandaloneModelKey<BlockStateModel> key,
    RenderType renderType,
    boolean translucent,
    boolean lighting
) {

    public void submit(SubmitNodeCollector collector, PoseStack poseStack, int overlayCoords, int packedLight, int tint) {
        WrappedBlockStateModel model = BlockStateModelRenderer.INSTANCE.getModel(this);
        if (model == null) return;
        collector.submitModel(
            model,
            this,
            poseStack,
            this.renderType,
            packedLight,
            overlayCoords,
            tint,
            null,
            0,
            null
        );
    }
}
