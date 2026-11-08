package dev.dubhe.anvilcraft.client.gui.tooltip;

import dev.dubhe.anvilcraft.util.FormattingUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 端口浮窗中的分组图标网格，数量独立于物品堆上限并始终绘制为角标。 */
public final class StoragePortTooltipComponent implements ClientTooltipComponent {
    private static final int COLUMNS = 8;
    private static final int ROW_HEIGHT = 18;
    private static final int HEADER_HEIGHT = 12;
    private final Component title;
    private final List<Entry> entries;
    private final boolean exact;
    private int minimumWidth;

    public StoragePortTooltipComponent(Component title, List<Entry> entries, boolean exact) {
        this.title = title;
        this.entries = List.copyOf(entries);
        this.exact = exact;
    }

    @Override
    public int getHeight() {
        return HEADER_HEIGHT + Math.ceilDiv(this.entries.size(), COLUMNS) * ROW_HEIGHT;
    }

    private String amount(Entry entry) {
        return entry.count() < 0 ? "…" : this.exact ? Long.toString(entry.count()) : FormattingUtil.toAbbrNum(entry.count());
    }

    private int cellWidth(Font font) {
        return Math.max(ROW_HEIGHT, this.entries.stream().mapToInt(entry -> font.width(this.amount(entry)) + 2).max().orElse(0));
    }

    public void setMinimumWidth(int width) {
        this.minimumWidth = width;
    }

    @Override
    public int getWidth(Font font) {
        return Math.max(this.minimumWidth,
            Math.max(font.width(this.title) + 16, Math.min(COLUMNS, this.entries.size()) * this.cellWidth(font)));
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        int sectionWidth = this.getWidth(font);
        int titleX = x + (sectionWidth - font.width(this.title)) / 2;
        graphics.fill(x, y + 4, titleX - 3, y + 5, 0xFF808080);
        graphics.fill(titleX + font.width(this.title) + 3, y + 4, x + sectionWidth, y + 5, 0xFF808080);
        graphics.drawString(font, this.title, titleX, y, 0xFFAAAAAA, true);
        int cellWidth = this.cellWidth(font);
        for (int index = 0; index < this.entries.size(); index++) {
            Entry entry = this.entries.get(index);
            int itemX = x + index % COLUMNS * cellWidth + cellWidth - ROW_HEIGHT;
            int itemY = y + HEADER_HEIGHT + index / COLUMNS * ROW_HEIGHT;
            ItemStack icon = entry.stack().copyWithCount(1);
            graphics.renderFakeItem(icon, itemX, itemY);
            graphics.renderItemDecorations(font, icon, itemX, itemY, this.amount(entry));
        }
    }

    /** 图标与独立数量，数量为负时表示等待服务端同步。 */
    public record Entry(ItemStack stack, long count) {
    }
}
