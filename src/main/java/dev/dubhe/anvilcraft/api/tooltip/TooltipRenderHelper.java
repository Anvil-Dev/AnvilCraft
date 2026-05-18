package dev.dubhe.anvilcraft.api.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;
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
        BlockPos offsetPos,
        VoxelShape shape,
        int color
    ) {
        renderOutline(
            poseStack.last(),
            consumer,
            camX,
            camY,
            camZ,
            offsetPos,
            shape,
            color
        );
    }

    public static void renderOutline(
        PoseStack.Pose poseStack,
        VertexConsumer consumer,
        double camX,
        double camY,
        double camZ,
        BlockPos offsetPos,
        VoxelShape shape,
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
        PoseStack.Pose pose,
        VertexConsumer consumer,
        VoxelShape shape,
        double x,
        double y,
        double z,
        int color
    ) {
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
     * @param graphics      GuiGraphicsExtractor
     * @param font      字体
     * @param itemStack 图标物品
     * @param lines     Tooltip内容
     * @param x         x坐标
     * @param y         y坐标
     */
    public static void renderTooltipWithItemIcon(
        GuiGraphicsExtractor graphics,
        Font font,
        ItemStack itemStack,
        List<Component> lines,
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
            height += component.getHeight(font);
        }

        Vector2ic vector2ic = tooltipPositioner.positionTooltip(graphics.guiWidth(), graphics.guiHeight(), x, y, width, height);
        int vx = vector2ic.x();
        int vy = vector2ic.y();
        graphics.pose().pushMatrix();

        int finalVy = vy;
        int finalWidth = width;
        int finalHeight = height + 16;
        renderTooltipBackground(graphics, vx, finalVy, finalWidth, finalHeight, backgroundColor, borderTopColor, borderBottomColor);

        graphics.item(itemStack, vx, vy);

        vy += 16;

        ClientTooltipComponent component;
        for (int i = 0, q = vy; i < components.size(); ++i) {
            component = components.get(i);
            component.extractText(graphics, font, vx, q);
            q += component.getHeight(font) + (i == 0 ? 2 : 0);
        }

        for (int i = 0, q = vy; i < components.size(); ++i) {
            component = components.get(i);
            component.extractImage(font, vx, q, finalWidth, finalHeight, graphics);
            q += component.getHeight(font) + (i == 0 ? 2 : 0);
        }

        graphics.pose().popMatrix();
    }

    private static void renderTooltipBackground(
        GuiGraphicsExtractor graphics,
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
        renderHorizontalLine(graphics, i, j - 1, k, backgroundColor);
        renderHorizontalLine(graphics, i, j + l, k, backgroundColor);
        renderRectangle(graphics, i, j, k, l, backgroundColor);
        renderVerticalLine(graphics, i - 1, j, l, backgroundColor);
        renderVerticalLine(graphics, i + k, j, l, backgroundColor);
        renderFrameGradient(graphics, i, j + 1, k, l, borderTopColor, borderBottomColor);
    }

    private static void renderFrameGradient(
        GuiGraphicsExtractor graphics,
        int x,
        int y,
        int width,
        int height,
        int topColor,
        int bottomColor
    ) {
        renderVerticalLineGradient(graphics, x, y, height - 2, topColor, bottomColor);
        renderVerticalLineGradient(graphics, x + width - 1, y, height - 2, topColor, bottomColor);
        renderHorizontalLine(graphics, x, y - 1, width, topColor);
        renderHorizontalLine(graphics, x, y - 1 + height - 1, width, bottomColor);
    }

    private static void renderVerticalLine(GuiGraphicsExtractor graphics, int x, int y, int length, int color) {
        graphics.fill(x, y, x + 1, y + length, color);
    }

    private static void renderVerticalLineGradient(GuiGraphicsExtractor graphics, int x, int y, int length, int topColor, int bottomColor) {
        graphics.fillGradient(x, y, x + 1, y + length, topColor, bottomColor);
    }

    private static void renderHorizontalLine(GuiGraphicsExtractor graphics, int x, int y, int length, int color) {
        graphics.fill(x, y, x + length, y + 1, color);
    }

    private static void renderRectangle(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, color);
    }
}
