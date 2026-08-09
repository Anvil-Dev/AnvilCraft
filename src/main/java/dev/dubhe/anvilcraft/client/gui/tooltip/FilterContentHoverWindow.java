package dev.dubhe.anvilcraft.client.gui.tooltip;

import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * 过滤器内容的"外挂窗口"：鼠标悬停在过滤器物品上时，在 tooltip 上方渲染一个独立的
 * 浮动窗口显示过滤内容（6×3 物品格）。
 *
 * <p>窗口背景直接裁剪过滤器的 GUI 贴图（{@code gui/misc/background/filter.png}）中
 * 的 6×3 槽位区域，与过滤器界面外观一致（26.1 的写法）。在
 * {@code RenderTooltipEvent.Pre} 中调用 {@link #render}，并把 tooltip 向下推 13px。
 */
public final class FilterContentHoverWindow {
    private static final int COLS = 6;
    private static final int SLOT_SIZE = 18;
    private static final int PADDING = 3;
    private static final int BG_WIDTH = COLS * SLOT_SIZE + PADDING * 2;
    private static final int BG_HEIGHT = 3 * SLOT_SIZE + PADDING * 2 + 2;

    /** 裁剪自 GUI 贴图的区域原点（256×256 贴图中的 u/v 偏移）。 */
    private static final int CROP_U = 58;
    private static final int CROP_V = 14;
    /** 槽位左上角在裁剪窗口内的偏移（GUI 中槽位从 (62,18) 开始）。 */
    private static final int SLOT_OFFSET_X = 62 - CROP_U;
    private static final int SLOT_OFFSET_Y = 17 - CROP_V + 1;

    private FilterContentHoverWindow() {
    }

    /**
     * 在 tooltip 位置 {@code (x, y)} 上方渲染过滤器内容窗口（居中于 {@code x}）。
     *
     * @param stack 过滤器物品堆栈
     * @param x     tooltip 左上角 x（绝对屏幕坐标）
     * @param y     tooltip 左上角 y（绝对屏幕坐标）
     */
    public static void render(GuiGraphics guiGraphics, ItemStack stack, int x, int y, Font font) {
        FilterContent content = stack.get(ModComponents.FILTER_CONTENT);
        if (content == null) {
            return;
        }
        int left = x - BG_WIDTH / 2;
        int top = y - BG_HEIGHT - 3;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 400.0F);
        // 直接裁剪过滤器 GUI 贴图上的槽位区域作为窗口背景
        guiGraphics.blit(
            SharedTextures.bg("misc", "filter"),
            left, top,
            CROP_U, CROP_V,
            BG_WIDTH, BG_HEIGHT
        );

        for (int i = 0; i < content.list().size(); i++) {
            ItemStack itemStack = content.list().get(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            int row = i / COLS;
            int col = i % COLS;
            int itemX = left + SLOT_OFFSET_X + col * SLOT_SIZE;
            int itemY = top + SLOT_OFFSET_Y + row * SLOT_SIZE;
            guiGraphics.renderItem(itemStack, itemX, itemY);
            guiGraphics.renderItemDecorations(font, itemStack, itemX, itemY);
        }
        guiGraphics.pose().popPose();
    }
}
