package dev.dubhe.anvilcraft.api.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2ic;

import java.util.List;

public class TooltipRenderHelper {

    /**
     * 渲染外框
     */
    public static void renderOutline(
        PoseStack poseStack,
        VertexConsumer consumer,
        double camX,
        double camY,
        double camZ,
        @NotNull BlockPos offsetPos,
        @NotNull VoxelShape shape,
        int color
    ) {
        renderShape(
            poseStack,
            consumer,
            shape,
            (double) offsetPos.getX() - camX,
            (double) offsetPos.getY() - camY,
            (double) offsetPos.getZ() - camZ,
            color
        );
    }

    private static void renderShape(
        @NotNull PoseStack poseStack,
        VertexConsumer consumer,
        @NotNull VoxelShape shape,
        double x,
        double y,
        double z,
        int color
    ) {
        PoseStack.Pose pose = poseStack.last();
        shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float dx = (float) (maxX - minX);
            float dy = (float) (maxY - minY);
            float dz = (float) (maxZ - minZ);
            float distance = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            consumer.addVertex(pose.pose(), (float) (minX + x), (float) (minY + y), (float) (minZ + z))
                .setColor(color)
                .setNormal(pose.copy(), dx /= distance, dy /= distance, dz /= distance);
            consumer.addVertex(pose.pose(), (float) (maxX + x), (float) (maxY + y), (float) (maxZ + z))
                .setColor(color)
                .setNormal(pose.copy(), dx, dy, dz);
        });
    }

    /**
     * 渲染带图标的Tooltip
     *
     * @param thiz      GuiGraphics
     * @param font      字体
     * @param itemStack 图标物品
     * @param lines     Tooltip内容
     * @param x         x坐标
     * @param y         y坐标
     */
    public static void renderTooltipWithItemIcon(
        GuiGraphics thiz,
        Font font,
        ItemStack itemStack,
        @NotNull List<Component> lines,
        int x,
        int y,
        int backgroundColor,
        int borderTopColor,
        int borderBottomColor
    ) {
        ClientTooltipPositioner tooltipPositioner = DefaultTooltipPositioner.INSTANCE;
        List<ClientTooltipComponent> components = lines.stream()
            .map(Component::getVisualOrderText)
            .map(ClientTooltipComponent::create)
            .toList();
        if (components.isEmpty()) return;
        int width = 0;
        int height = components.size() == 1 ? -2 : 0;

        for (ClientTooltipComponent component : components) {
            width = Math.max(component.getWidth(font), width);
            height += component.getHeight();
        }

        Vector2ic vector2ic = tooltipPositioner.positionTooltip(thiz.guiWidth(), thiz.guiHeight(), x, y, width, height);
        int vx = vector2ic.x();
        int vy = vector2ic.y();
        thiz.pose().pushPose();

        int finalVy = vy;
        int finalWidth = width;
        int finalHeight = height + 16;
        thiz.drawManaged(() -> renderTooltipBackground(
            thiz, vx, finalVy, finalWidth, finalHeight, backgroundColor, borderTopColor, borderBottomColor));
        thiz.pose().translate(0.0F, 0.0F, 400.0F);

        thiz.renderFakeItem(itemStack, vx, vy);

        vy += 16;

        ClientTooltipComponent component;
        for (int i = 0, q = vy; i < components.size(); ++i) {
            component = components.get(i);
            component.renderText(font, vx, q, thiz.pose().last().pose(), thiz.bufferSource());
            q += component.getHeight() + (i == 0 ? 2 : 0);
        }

        for (int i = 0, q = vy; i < components.size(); ++i) {
            component = components.get(i);
            component.renderImage(font, vx, q, thiz);
            q += component.getHeight() + (i == 0 ? 2 : 0);
        }

        thiz.pose().popPose();
    }

    private static void renderTooltipBackground(
        GuiGraphics guiGraphics,
        int x,
        int y,
        int width,
        int height,
        int backgroundColor,
        int borderTopColor,
        int borderBottomColor
    ) {
        int i = x - 3;
        int j = y - 3;
        int k = width + 3 + 3;
        int l = height + 3 + 3;
        renderHorizontalLine(guiGraphics, i, j - 1, k, 400, backgroundColor);
        renderHorizontalLine(guiGraphics, i, j + l, k, 400, backgroundColor);
        renderRectangle(guiGraphics, i, j, k, l, 400, backgroundColor);
        renderVerticalLine(guiGraphics, i - 1, j, l, 400, backgroundColor);
        renderVerticalLine(guiGraphics, i + k, j, l, 400, backgroundColor);
        renderFrameGradient(guiGraphics, i, j + 1, k, l, 400, borderTopColor, borderBottomColor);
    }

    private static void renderFrameGradient(
        GuiGraphics guiGraphics,
        int x,
        int y,
        int width,
        int height,
        int z,
        int topColor,
        int bottomColor
    ) {
        renderVerticalLineGradient(guiGraphics, x, y, height - 2, z, topColor, bottomColor);
        renderVerticalLineGradient(guiGraphics, x + width - 1, y, height - 2, z, topColor, bottomColor);
        renderHorizontalLine(guiGraphics, x, y - 1, width, z, topColor);
        renderHorizontalLine(guiGraphics, x, y - 1 + height - 1, width, z, bottomColor);
    }

    private static void renderVerticalLine(GuiGraphics guiGraphics, int x, int y, int length, int z, int color) {
        guiGraphics.fill(x, y, x + 1, y + length, z, color);
    }

    private static void renderVerticalLineGradient(
        GuiGraphics guiGraphics, int x, int y, int length, int z, int topColor, int bottomColor) {
        guiGraphics.fillGradient(x, y, x + 1, y + length, z, topColor, bottomColor);
    }

    private static void renderHorizontalLine(GuiGraphics guiGraphics, int x, int y, int length, int z, int color) {
        guiGraphics.fill(x, y, x + length, y + 1, z, color);
    }

    private static void renderRectangle(
        GuiGraphics guiGraphics, int x, int y, int width, int height, int z, int color) {
        guiGraphics.fill(x, y, x + width, y + height, z, color);
    }
}
