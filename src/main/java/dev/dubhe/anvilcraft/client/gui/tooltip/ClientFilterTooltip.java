package dev.dubhe.anvilcraft.client.gui.tooltip;

import dev.dubhe.anvilcraft.inventory.tooltip.FilterTooltip;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class ClientFilterTooltip implements ClientTooltipComponent {
    private final FilterContent content;

    public ClientFilterTooltip(FilterTooltip tooltip) {
        this.content = tooltip.content();
    }

    @Override
    public int getHeight(Font font) {
        return 18 * 3 + 4;
    }

    @Override
    public int getWidth(Font font) {
        return 18 * 6;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        int i = 0;
        for (ItemStack stack : this.content.list()) {
            if (i >= 18) break;
            int row = i / 6;
            int col = i % 6;
            int itemX = x + col * 18;
            int itemY = y + row * 18;
            this.extractSlot(itemX, itemY, graphics);
            if (!stack.isEmpty()) {
                graphics.item(stack, itemX + 1, itemY + 1);
                graphics.itemDecorations(font, stack, itemX + 1, itemY + 1);
            }
            i++;
        }
    }

    private void extractSlot(int x, int y, GuiGraphicsExtractor graphics) {
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            Identifier.withDefaultNamespace("container/bundle/slot"),
            x,
            y,
            18,
            18
        );
    }
}
