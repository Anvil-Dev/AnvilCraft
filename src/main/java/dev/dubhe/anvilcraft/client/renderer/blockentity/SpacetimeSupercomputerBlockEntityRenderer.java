package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.SpacetimeSupercomputerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;

public class SpacetimeSupercomputerBlockEntityRenderer implements BlockEntityRenderer<SpacetimeSupercomputerBlockEntity> {
    public SpacetimeSupercomputerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        SpacetimeSupercomputerBlockEntity blockEntity,
        float partialTick,
        PoseStack pose,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        if (blockEntity.getProcessingRecipe() == null) {
            return;
        }
        int total = blockEntity.getProcessingTotal();
        if (total <= 0) {
            return;
        }
        int size = blockEntity.getProcessingSize();
        if (size <= 0) {
            return;
        }
        size = (size - 1) / 2;
        Component text = Component.translatable("gui.anvilcraft.multiblock_4d.progress", blockEntity.getProcessingProgress(), total)
            .withStyle(ChatFormatting.BOLD);
        Font font = Minecraft.getInstance().font;
        float width = font.width(text);
        float scale = 53F / (20 * width); // width / (width * (width / 2.65F))
        float south = size + 1.001F;
        float north = -size - 0.001F;
        this.renderFaceText(pose, buffer, font, text, width, scale, packedLight, 0.5F, north, 0);
        this.renderFaceText(pose, buffer, font, text, width, scale, packedLight, north, 0.5F, 90);
        this.renderFaceText(pose, buffer, font, text, width, scale, packedLight, 0.5F, south, 180);
        this.renderFaceText(pose, buffer, font, text, width, scale, packedLight, south, 0.5F, 270);
    }

    private void renderFaceText(
        PoseStack pose,
        MultiBufferSource buffer,
        Font font,
        Component text,
        float width,
        float scale,
        int packedLight,
        float x,
        float z,
        int rotation
    ) {
        pose.pushPose();
        pose.translate(x, 0.5F, z);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.scale(-scale, -scale, scale);
        font.drawInBatch(
            text,
            -width / 2F,
            -font.lineHeight / 2F,
            0xFFFF95FF,
            false,
            pose.last().pose(),
            buffer,
            Font.DisplayMode.NORMAL,
            0x75400040,
            packedLight
        );
        pose.popPose();
    }
}
