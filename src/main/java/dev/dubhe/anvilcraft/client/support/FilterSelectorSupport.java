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
    private static final int BG_WIDTH = FilterSelectorSupport.COLS * FilterSelectorSupport.SLOT_SIZE + FilterSelectorSupport.PADDING * 2;
    private static final int BG_HEIGHT = 3 * FilterSelectorSupport.SLOT_SIZE + FilterSelectorSupport.PADDING * 2 + 2;

    private static ItemStack currentFilter = ItemStack.EMPTY;
    @Nullable
    private static FilterContent content = null;

    public static void setCurrentFilterStack(ItemStack filter) {
        if (ItemStack.isSameItemSameComponents(FilterSelectorSupport.currentFilter, filter)) return;
        FilterSelectorSupport.currentFilter = filter;
        if (filter.isEmpty()) {
            FilterSelectorSupport.content = null;
            return;
        }
        FilterSelectorSupport.content = filter.getOrDefault(ModComponents.FILTER_CONTENT, new FilterContent());
    }

    private static final int CROP_U = 58;
    private static final int CROP_V = 14;
    private static final int SLOT_OFFSET_X = 62 - FilterSelectorSupport.CROP_U;
    private static final int SLOT_OFFSET_Y = 17 - FilterSelectorSupport.CROP_V + 1;

    public static void render(GuiGraphicsExtractor graphics, int x, int y) {
        if (FilterSelectorSupport.content == null) return;
        int left = x - FilterSelectorSupport.BG_WIDTH / 2;
        int top = y - FilterSelectorSupport.BG_HEIGHT - 3;

        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            SharedTextures.bg("misc", "filter"),
            left, top,
            FilterSelectorSupport.CROP_U, FilterSelectorSupport.CROP_V,
            FilterSelectorSupport.BG_WIDTH, FilterSelectorSupport.BG_HEIGHT,
            FilterSelectorSupport.BG_WIDTH, FilterSelectorSupport.BG_HEIGHT,
            256, 256
        );

        for (int i = 0; i < FilterSelectorSupport.content.list().size(); i++) {
            ItemStack stack = FilterSelectorSupport.content.list().get(i);
            int row = i / FilterSelectorSupport.COLS;
            int col = i % FilterSelectorSupport.COLS;
            int itemX = left + FilterSelectorSupport.SLOT_OFFSET_X + col * FilterSelectorSupport.SLOT_SIZE;
            int itemY = top + FilterSelectorSupport.SLOT_OFFSET_Y + row * FilterSelectorSupport.SLOT_SIZE;

            if (!stack.isEmpty()) {
                graphics.fakeItem(stack, itemX, itemY);
                graphics.itemDecorations(Minecraft.getInstance().font, stack, itemX, itemY);
            }
        }
    }
}
