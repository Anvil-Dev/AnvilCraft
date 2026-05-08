package dev.dubhe.anvilcraft.api.tooltip.providers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * 手持物品的tooltip
 */
public interface IHandHeldItemTooltipProvider {
    boolean accepts(ItemStack itemStack);

    void render(
        PoseStack poseStack,
        VertexConsumer consumer,
        ItemStack itemStack,
        double camX,
        double camY,
        double camZ
    );

    void renderTooltip(GuiGraphicsExtractor guiGraphics, int screenWidth, int screenHeight);

    int priority();
}
