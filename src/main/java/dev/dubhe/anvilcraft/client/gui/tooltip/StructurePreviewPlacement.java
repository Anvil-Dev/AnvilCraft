package dev.dubhe.anvilcraft.client.gui.tooltip;

import org.jspecify.annotations.Nullable;

/** 根据 tooltip 的最终位置选择鼠标另一侧的预览区域，并适应屏幕边缘空间。 */
public record StructurePreviewPlacement(int x, int y, int size) {
    public static @Nullable StructurePreviewPlacement opposite(
        int screenWidth, int screenHeight, int mouseX, int tooltipX, int tooltipY, int tooltipWidth, int preferredSize
    ) {
        int padding = 4;
        int gap = 12;
        boolean tooltipRight = tooltipX >= mouseX;
        int boundary = tooltipRight ? Math.min(mouseX - gap, tooltipX - gap)
            : Math.max(mouseX + gap, tooltipX + tooltipWidth + gap);
        int available = tooltipRight ? boundary - padding : screenWidth - padding - boundary;
        int size = Math.min(preferredSize, Math.min(available, screenHeight - padding * 2));
        if (size <= 0) {
            return null;
        }
        int x = tooltipRight ? boundary - size : boundary;
        int y = Math.clamp(tooltipY, padding, screenHeight - padding - size);
        return new StructurePreviewPlacement(x, y, size);
    }
}
