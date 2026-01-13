package dev.dubhe.anvilcraft.client.gui.tooltip;

import dev.dubhe.anvilcraft.inventory.tooltip.FilterTooltip;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ClientFilterTooltip implements ClientTooltipComponent {
    public static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/bundle.png");
    private final FilterContent content;

    public ClientFilterTooltip(FilterTooltip tooltip) {
        this.content = tooltip.content();
    }

    @Override
    public int getHeight() {
        return 18 * 3 + 4;
    }

    @Override
    public int getWidth(Font font) {
        return 18 * 6;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        int i = 0;
        for (ItemStack stack : content.list()) {
            if (i >= 18) break;
            int row = i / 6;
            int col = i % 6;
            int itemX = x + col * 18;
            int itemY = y + row * 18;
            this.renderSlot(itemX, itemY, guiGraphics);
            if (!stack.isEmpty()) {
                guiGraphics.renderItem(stack, itemX + 1, itemY + 1);
                guiGraphics.renderItemDecorations(font, stack, itemX + 1, itemY + 1);
            }
            i++;
        }
    }

    private void renderSlot(int x, int y, GuiGraphics guiGraphics) {
        guiGraphics.blitSprite(
            ResourceLocation.withDefaultNamespace("container/bundle/slot"),
            x,
            y,
            18,
            18
        );
    }
}
