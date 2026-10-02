package dev.dubhe.anvilcraft.client.gui.tooltip;

import dev.dubhe.anvilcraft.inventory.tooltip.StorageTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ClientStorageTooltip implements ClientTooltipComponent {
    private static final int LINE_HEIGHT = 10;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 2;
    private static final int MAX_VISIBLE_TYPES = 9;

    private final int usedTypes;
    private final int typeLimit;
    private final List<ItemStack> types;

    public ClientStorageTooltip(StorageTooltip tooltip) {
        this.usedTypes = tooltip.usedTypes();
        this.typeLimit = tooltip.typeLimit();
        this.types = tooltip.types();
    }

    private boolean hasMore() {
        return this.usedTypes > this.types.size();
    }

    @Override
    public int getHeight(Font font) {
        int height = ClientStorageTooltip.LINE_HEIGHT * 2;
        if (!this.types.isEmpty()) {
            height += ClientStorageTooltip.ICON_SIZE;
        }
        return height;
    }

    @Override
    public int getWidth(Font font) {
        int width = Math.max(
            font.width(Component.translatable("tooltip.anvilcraft.storage.types")),
            font.width(this.typesLine())
        );
        if (!this.types.isEmpty()) {
            int visible = Math.min(this.types.size(), ClientStorageTooltip.MAX_VISIBLE_TYPES);
            int iconsWidth = visible * (ClientStorageTooltip.ICON_SIZE + ClientStorageTooltip.ICON_GAP)
                - ClientStorageTooltip.ICON_GAP;
            if (this.hasMore()) {
                iconsWidth += ClientStorageTooltip.ICON_GAP + font.width(Component.literal("..."));
            }
            width = Math.max(width, iconsWidth);
        }
        return width;
    }

    private MutableComponent typesLine() {
        if (this.typeLimit == 0) {
            return Component.translatable("tooltip.anvilcraft.storage.types.value.infinite", this.usedTypes);
        }
        return Component.translatable("tooltip.anvilcraft.storage.types.value", this.usedTypes, this.typeLimit);
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor guiGraphics) {
        guiGraphics.text(font, Component.translatable("tooltip.anvilcraft.storage.types").withStyle(ChatFormatting.BLUE),
            x, y, -1, true);
        guiGraphics.text(font, this.typesLine().withStyle(ChatFormatting.GRAY), x, y + ClientStorageTooltip.LINE_HEIGHT, -1, true);
        if (this.types.isEmpty()) {
            return;
        }
        int iconY = y + ClientStorageTooltip.LINE_HEIGHT * 2;
        int visible = Math.min(this.types.size(), ClientStorageTooltip.MAX_VISIBLE_TYPES);
        for (int i = 0; i < visible; i++) {
            guiGraphics.item(
                this.types.get(i),
                x + i * (ClientStorageTooltip.ICON_SIZE + ClientStorageTooltip.ICON_GAP),
                iconY
            );
        }
        if (this.hasMore()) {
            guiGraphics.text(
                font,
                Component.literal("...").withStyle(ChatFormatting.GRAY),
                x + visible * (ClientStorageTooltip.ICON_SIZE + ClientStorageTooltip.ICON_GAP),
                iconY + (ClientStorageTooltip.ICON_SIZE - ClientStorageTooltip.LINE_HEIGHT) / 2,
                -1,
                true
            );
        }
    }
}
