package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.SpacetimeSupercomputerBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.SpacetimeSupercomputerRenderState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SpacetimeSupercomputerBlockEntityRenderer
    implements BlockEntityRenderer<SpacetimeSupercomputerBlockEntity, SpacetimeSupercomputerRenderState> {
    public SpacetimeSupercomputerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public SpacetimeSupercomputerRenderState createRenderState() {
        return new SpacetimeSupercomputerRenderState();
    }

    @Override
    public void extractRenderState(
        SpacetimeSupercomputerBlockEntity entity, SpacetimeSupercomputerRenderState state, float partialTick,
        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPosition, breakProgress);
        state.text = null;
        int total = entity.getProcessingTotal();
        if (entity.getProcessingRecipe() == null || total <= 0 || entity.getProcessingSize() <= 0) return;
        Component text = Component.translatable("gui.anvilcraft.multiblock_4d.progress", entity.getProcessingProgress(), total)
            .withStyle(ChatFormatting.BOLD);
        Font font = Minecraft.getInstance().font;
        state.text = text.getVisualOrderText();
        state.width = font.width(text);
        state.lineHeight = font.lineHeight;
        state.size = (entity.getProcessingSize() - 1) / 2;
    }

    @Override
    public void submit(
        SpacetimeSupercomputerRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera
    ) {
        if (state.text == null || state.width <= 0) return;
        float south = state.size + 1.001F;
        float north = -state.size - 0.001F;
        renderFace(state, pose, collector, 0.5F, north, 0);
        renderFace(state, pose, collector, north, 0.5F, 90);
        renderFace(state, pose, collector, 0.5F, south, 180);
        renderFace(state, pose, collector, south, 0.5F, 270);
    }

    private static void renderFace(
        SpacetimeSupercomputerRenderState state, PoseStack pose, SubmitNodeCollector collector, float x, float z, int rotation
    ) {
        float scale = 53F / (20 * state.width);
        pose.pushPose();
        pose.translate(x, 0.5F, z);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        // Native font backgrounds use negative depth; keep them behind the outward-facing glyphs.
        pose.scale(-scale, -scale, -scale);
        collector.submitText(pose, -state.width / 2F, -state.lineHeight / 2F, state.text, false,
            Font.DisplayMode.NORMAL, state.lightCoords, 0xFFFF95FF, 0x75400040, 0);
        pose.popPose();
    }
}
