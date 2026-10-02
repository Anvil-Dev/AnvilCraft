package dev.dubhe.anvilcraft.client.gui.tooltip;

import dev.dubhe.anvilcraft.inventory.tooltip.ConfinementChamberTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ClientConfinementChamberTooltip implements ClientTooltipComponent {
    private static final int ICON_SIZE = 16;
    private static final int LINE_HEIGHT = 10;
    private static final int ICON_GAP = 1;
    private final ItemStack item;

    public ClientConfinementChamberTooltip(ConfinementChamberTooltip tooltip) {
        this.item = tooltip.item();
    }

    @Override
    public int getHeight(Font font) {
        return this.item.isEmpty() ? 0 : LINE_HEIGHT + ICON_SIZE;
    }

    @Override
    public int getWidth(Font font) {
        if (this.item.isEmpty()) return 0;
        return Math.max(font.width(Component.translatable("tooltip.anvilcraft.creative_crate.item")),
            ICON_SIZE + ICON_GAP + font.width(this.itemLine()));
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        if (this.item.isEmpty()) return;
        graphics.text(font, Component.translatable("tooltip.anvilcraft.creative_crate.item").withStyle(ChatFormatting.BLUE),
            x, y, -1, true);
        graphics.text(font, this.itemLine(), x + ICON_SIZE + ICON_GAP, y + LINE_HEIGHT + (ICON_SIZE - LINE_HEIGHT) / 2,
            -1, true);
        graphics.item(this.item, x, y + LINE_HEIGHT);
    }

    private Component itemLine() {
        return Component.empty().append(this.item.getHoverName()).append(" x" + this.item.getCount()).withStyle(ChatFormatting.GRAY);
    }
}
