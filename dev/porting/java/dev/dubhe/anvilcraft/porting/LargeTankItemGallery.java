package dev.dubhe.anvilcraft.porting;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class LargeTankItemGallery extends Screen {
    private final List<ItemStack> items;

    public LargeTankItemGallery(List<ItemStack> items) {
        super(Component.literal("Large tank item layers"));
        this.items = List.copyOf(items);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF202530);
        graphics.text(this.font, this.title, 20, 12, -1, false);
        for (int index = 0; index < this.items.size(); index++) {
            int x = this.width / 2 - 135 + index % 3 * 100;
            int y = 45 + index / 3 * 96;
            graphics.text(this.font, Integer.toString(index), x, y - 12, -1, false);
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(4, 4);
            graphics.item(this.items.get(index), 0, 0);
            graphics.pose().popMatrix();
        }
    }
}
