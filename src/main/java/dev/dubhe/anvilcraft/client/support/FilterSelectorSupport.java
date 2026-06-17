package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class FilterSelectorSupport {
    private static final int COLS = 6;
    private static final int SLOT_SIZE = 18;
    private static final int PADDING = 3;
    private static final int BG_WIDTH = COLS * SLOT_SIZE + PADDING * 2;
    private static final int BG_HEIGHT = 3 * SLOT_SIZE + PADDING * 2 + 2;

    private static ItemStack currentFilter = ItemStack.EMPTY;
    @Nullable
    private static FilterContent content = null;

    public static void setCurrentFilterStack(ItemStack filter) {
        if (ItemStack.isSameItemSameComponents(currentFilter, filter)) return;
        currentFilter = filter;
        if (filter.isEmpty()) {
            content = null;
            return;
        }
        content = filter.getOrDefault(ModComponents.FILTER_CONTENT, new FilterContent());
    }

    private static final int CROP_U = 58;
    private static final int CROP_V = 14;
    private static final int SLOT_OFFSET_X = 62 - CROP_U;
    private static final int SLOT_OFFSET_Y = 17 - CROP_V + 1;

    public static void render(GuiGraphicsExtractor graphics, int x, int y) {
        if (content == null) return;
        int left = x - BG_WIDTH / 2;
        int top = y - BG_HEIGHT - 3;

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            SharedTextures.bg("misc", "filter"),
            left, top,
            CROP_U, CROP_V,
            BG_WIDTH, BG_HEIGHT,
            BG_WIDTH, BG_HEIGHT,
            256, 256
        );

        for (int i = 0; i < content.list().size(); i++) {
            ItemStack stack = content.list().get(i);
            int row = i / COLS;
            int col = i % COLS;
            int itemX = left + SLOT_OFFSET_X + col * SLOT_SIZE;
            int itemY = top + SLOT_OFFSET_Y + row * SLOT_SIZE;

            if (!stack.isEmpty()) {
                graphics.fakeItem(stack, itemX, itemY);
                graphics.itemDecorations(Minecraft.getInstance().font, stack, itemX, itemY);
            }
        }
    }
}
