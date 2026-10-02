package dev.dubhe.anvilcraft.client.gui.tooltip;

import dev.dubhe.anvilcraft.api.tooltip.StoragePortItemTooltip;
import dev.dubhe.anvilcraft.inventory.tooltip.StoragePortTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

public class ClientStoragePortTooltip implements ClientTooltipComponent {
    private final StoragePortItemTooltip.Contents contents;

    public ClientStoragePortTooltip(StoragePortTooltip tooltip) {
        this.contents = StoragePortItemTooltip.contents(tooltip.blockEntityTag(), registries());
    }

    public static HolderLookup.@Nullable Provider registries() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null) return client.level.registryAccess();
        return client.getConnection() == null ? null : client.getConnection().registryAccess();
    }

    private Component itemLine() {
        return Component.literal(this.contents.count() + "x ").append(this.contents.item().getHoverName()).withStyle(ChatFormatting.GRAY);
    }

    @Override
    public int getHeight(Font font) {
        return this.contents.item().isEmpty() ? 0 : 26;
    }

    @Override
    public int getWidth(Font font) {
        if (this.contents.item().isEmpty()) return 0;
        return Math.max(font.width(Component.translatable("tooltip.anvilcraft.storage_port.item")), 17 + font.width(this.itemLine()));
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        if (this.contents.item().isEmpty()) return;
        graphics.text(font, Component.translatable("tooltip.anvilcraft.storage_port.item").withStyle(ChatFormatting.BLUE), x, y, -1, true);
        graphics.text(font, this.itemLine(), x + 17, y + 13, -1, true);
        graphics.item(this.contents.item(), x, y + 10);
    }
}
