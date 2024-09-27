package dev.dubhe.anvilcraft.api.tooltip.providers;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * 手持物品的tooltip
 */
public interface HandHeldItemTooltipProvider {
    boolean accepts(ItemStack itemStack);

    void render(
            PoseStack poseStack,
            VertexConsumer consumer,
            ItemStack itemStack,
            double camX,
            double camY,
            double camZ
    );

    void renderTooltip(GuiGraphics guiGraphics, int screenWidth, int screenHeight);

    int priority();
}
