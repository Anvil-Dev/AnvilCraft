package dev.dubhe.anvilcraft.api.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2ic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HudTooltipManager {
    public static final HudTooltipManager INSTANCE = new HudTooltipManager();
    private static final int BACKGROUND_COLOR = 0xCC100010;
    private static final int BORDER_COLOR_TOP = 0x505000ff;
    private static final int BORDER_COLOR_BOTTOM = 0x5028007f;
    private final List<TooltipProvider> providers = new ArrayList<>();
    private final List<AffectRangeProvider> affectRangeProviders = new ArrayList<>();

    static {
        INSTANCE.registerTooltip(new PowerComponentTooltipProvider());
        INSTANCE.registerAffectRange(new AffectRangeProviderImpl());
        INSTANCE.registerTooltip(new RubyPrismTooltipProvider());
    }

    private void registerAffectRange(AffectRangeProviderImpl affectRangeProvider) {
        affectRangeProviders.add(affectRangeProvider);
    }

    /**
     * 渲染
     */
    public void renderTooltip(
            GuiGraphics guiGraphics,
            BlockEntity entity,
            float partialTick,
            int screenWidth,
            int screenHeight
    ) {
        if (entity == null) return;
        final int tooltipPosX = screenWidth / 2 + 10;
        final int tooltipPosY = screenHeight / 2 + 10;
        Font font = Minecraft.getInstance().font;
        TooltipProvider currentProvider = determineTooltipProvider(entity);
        if (currentProvider == null) return;
        renderTooltipWithItemIcon(
                guiGraphics,
                font,
                currentProvider.icon(entity),
                currentProvider.tooltip(entity),
                tooltipPosX,
                tooltipPosY,
                BACKGROUND_COLOR,
                BORDER_COLOR_TOP,
                BORDER_COLOR_BOTTOM
        );
    }

    /**
     * 渲染作用范围
     */
    public void renderAffectRange(
            BlockEntity entity,
            PoseStack poseStack,
            VertexConsumer consumer,
            double camX, double camY, double camZ
    ) {
        AffectRangeProvider currentProvider = determineAffectRangeProvider(entity);
        if (currentProvider == null) return;
        VoxelShape shape = currentProvider.affectRange(entity);
        if (shape == null) return;
        renderOutline(
                poseStack,
                consumer,
                camX,
                camY,
                camZ,
                BlockPos.ZERO,
                shape,
                0xffffffff
        );
    }

    private static void renderOutline(
            PoseStack poseStack, VertexConsumer consumer,
            double camX, double camY, double camZ, @NotNull BlockPos pos, @NotNull VoxelShape shape,
            int color
    ) {
        renderShape(
                poseStack, consumer, shape,
                (double) pos.getX() - camX, (double) pos.getY() - camY, (double) pos.getZ() - camZ,
                color
        );
    }

    private static void renderShape(
            @NotNull PoseStack poseStack, VertexConsumer consumer, @NotNull VoxelShape shape,
            double x, double y, double z, int color
    ) {
        PoseStack.Pose pose = poseStack.last();
        shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float k = (float) (maxX - minX);
            float l = (float) (maxY - minY);
            float m = (float) (maxZ - minZ);
            float n = Mth.sqrt(k * k + l * l + m * m);
            consumer.vertex(pose.pose(), (float) (minX + x), (float) (minY + y), (float) (minZ + z))
                    .color(color)
                    .normal(pose.normal(), k /= n, l /= n, m /= n)
                    .endVertex();
            consumer.vertex(pose.pose(), (float) (maxX + x), (float) (maxY + y), (float) (maxZ + z))
                    .color(color)
                    .normal(pose.normal(), k, l, m)
                    .endVertex();
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

        Vector2ic vector2ic = tooltipPositioner.positionTooltip(
                thiz.guiWidth(),
                thiz.guiHeight(),
                x,
                y,
                width,
                height
        );
        int vx = vector2ic.x();
        int vy = vector2ic.y();
        thiz.pose().pushPose();

        int finalVy = vy;
        int finalWidth = width;
        int finalHeight = height + 16;
        thiz.drawManaged(() -> renderTooltipBackground(
                thiz,
                vx,
                finalVy,
                finalWidth,
                finalHeight,
                backgroundColor,
                borderTopColor,
                borderBottomColor
        ));
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

    private TooltipProvider determineTooltipProvider(BlockEntity entity) {
        if (entity == null) return null;
        ArrayList<TooltipProvider> tooltipProviders = providers.stream()
                .filter(it -> it.accepts(entity))
                .sorted(Comparator.comparingInt(TooltipProvider::priority))
                .collect(Collectors.toCollection(ArrayList::new));
        if (tooltipProviders.isEmpty()) return null;
        return tooltipProviders.get(0);
    }

    private AffectRangeProvider determineAffectRangeProvider(BlockEntity entity) {
        if (entity == null) return null;
        ArrayList<AffectRangeProvider> pv = affectRangeProviders.stream()
                .filter(it -> it.accepts(entity))
                .sorted(Comparator.comparingInt(AffectRangeProvider::priority))
                .collect(Collectors.toCollection(ArrayList::new));
        if (pv.isEmpty()) return null;
        return pv.get(0);
    }

    private void registerTooltip(TooltipProvider provider) {
        providers.add(provider);
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
            GuiGraphics guiGraphics,
            int x,
            int y,
            int length,
            int z,
            int topColor,
            int bottomColor
    ) {
        guiGraphics.fillGradient(x, y, x + 1, y + length, z, topColor, bottomColor);
    }

    private static void renderHorizontalLine(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int length,
            int z,
            int color
    ) {
        guiGraphics.fill(x, y, x + length, y + 1, z, color);
    }

    private static void renderRectangle(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            int z,
            int color
    ) {
        guiGraphics.fill(x, y, x + width, y + height, z, color);
    }

}
