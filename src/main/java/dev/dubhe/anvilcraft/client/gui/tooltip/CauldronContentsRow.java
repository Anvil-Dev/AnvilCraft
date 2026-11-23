package dev.dubhe.anvilcraft.client.gui.tooltip;

import dev.dubhe.anvilcraft.api.tooltip.impl.CauldronProcessingTooltip.Row;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** 让材料名和配方预览在各自的列内换行，并把每行的预览对齐到整个浮窗的右沿。 */
public final class CauldronContentsRow implements ClientTooltipComponent {
    private static final int GAP = 8;
    private static final int PADDING = 2;
    private final Row row;
    private final int width;
    private final List<FormattedCharSequence> labels;
    private final List<FormattedCharSequence> previews;

    public CauldronContentsRow(Row row, Font font, int maxWidth) {
        this.row = row;
        int left = font.width(row.label());
        int right = row.preview() == null ? 0 : font.width(row.preview());
        int gap = right == 0 ? 0 : GAP;
        this.width = Math.min(maxWidth, left + right + gap + 2 * PADDING);
        int space = Math.max(2, this.width - gap - 2 * PADDING);
        int rightWidth = Math.min(right, Math.max(space / 2, space - left));
        this.labels = font.split(row.label(), Math.max(1, space - rightWidth));
        this.previews = row.preview() == null ? List.of() : font.split(row.preview(), Math.max(1, rightWidth));
    }

    @Override
    public int getWidth(Font font) {
        return this.width;
    }

    @Override
    public int getHeight(Font font) {
        return Math.max(this.labels.size(), this.previews.size()) * font.lineHeight + (this.row.fluid().isEmpty() ? 1 : 4);
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        if (!this.row.fluid().isEmpty()) {
            this.drawFluidBar(graphics, x, y, w, this.getHeight(font));
            y += 2;
        }
        for (int i = 0; i < this.labels.size(); i++) {
            graphics.text(font, this.labels.get(i), x + PADDING, y + i * font.lineHeight, -1, true);
        }
        for (int i = 0; i < this.previews.size(); i++) {
            var text = this.previews.get(i);
            graphics.text(font, text, x + w - PADDING - font.width(text), y + i * font.lineHeight, -1, true);
        }
    }

    private void drawFluidBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        FluidStack fluid = this.row.fluid();
        int filled = Math.clamp((long) fluid.getAmount() * (width - 2) / Math.max(1, this.row.capacity()), 1, width - 2);
        var models = Minecraft.getInstance().getModelManager().getFluidStateModelSet();
        var model = FluidRenderHelper.getModel(models, fluid.getFluid());
        graphics.fill(x, y, x + width, y + height - 1, 0xFF505050);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 2, 0xFF242424);
        graphics.enableScissor(x + 1, y + 1, x + 1 + filled, y + height - 2);
        int color = model.fluidTintSource() == null ? -1 : ARGB.opaque(model.fluidTintSource().colorAsStack(fluid));
        for (int dy = 0; dy < height - 3; dy += 16) {
            for (int dx = 0; dx < filled; dx += 16) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, model.stillMaterial().sprite(),
                    x + 1 + dx, y + 1 + dy, 16, 16, color);
            }
        }
        graphics.disableScissor();
        graphics.fill(x + 1, y + 1, x + 1 + filled, y + height - 2, 0x70000000);
    }
}
