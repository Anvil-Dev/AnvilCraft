package dev.dubhe.anvilcraft.client.gui.tooltip;

import dev.dubhe.anvilcraft.inventory.tooltip.ConfinementChamberTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * 约束仓物品 tooltip：以 16×16 图标 + 名称/数量 的形式渲染存储的内容物。
 */
public class ClientConfinementChamberTooltip implements ClientTooltipComponent {
    private static final int ICON_SIZE = 16;
    private static final int LINE_HEIGHT = 10;
    private static final int ICON_GAP = 1;

    private final ItemStack item;

    public ClientConfinementChamberTooltip(ConfinementChamberTooltip tooltip) {
        this.item = tooltip.item();
    }

    @Override
    public int getHeight() {
        return item.isEmpty() ? 0 : LINE_HEIGHT + ICON_SIZE;
    }

    @Override
    public int getWidth(Font font) {
        if (item.isEmpty()) return 0;
        int width = font.width(Component.translatable("tooltip.anvilcraft.creative_crate.item"));
        return Math.max(width, ICON_SIZE + ICON_GAP + font.width(itemLine()));
    }

    @Override
    public void renderText(
        Font font,
        int mouseX,
        int mouseY,
        Matrix4f matrix,
        MultiBufferSource.BufferSource bufferSource
    ) {
        if (item.isEmpty()) return;
        font.drawInBatch(
            Component.translatable("tooltip.anvilcraft.creative_crate.item").withStyle(ChatFormatting.BLUE),
            mouseX, mouseY, -1, true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880
        );
        font.drawInBatch(
            itemLine(),
            mouseX + ICON_SIZE + ICON_GAP, mouseY + LINE_HEIGHT + (float) (ICON_SIZE - LINE_HEIGHT) / 2,
            -1, true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880
        );
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        if (item.isEmpty()) return;
        guiGraphics.renderItem(item, x, y + LINE_HEIGHT);
    }

    /** 内容物行：名称 + 数量。 */
    private Component itemLine() {
        return Component.literal("")
            .append(item.getHoverName())
            .append(Component.literal(" x" + item.getCount()))
            .withStyle(ChatFormatting.GRAY);
    }
}