package dev.dubhe.anvilcraft.client.gui.tooltip;

import dev.dubhe.anvilcraft.inventory.tooltip.StoragePortTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * 仓储端口物品 tooltip：以 16×16 图标 + 名称/缓存数量 的形式渲染标记物品。
 * 没有标记时回退到缓存中的第一个物品。
 */
public class ClientStoragePortTooltip implements ClientTooltipComponent {
    private static final String TAG_MARKED = "marked_item";
    private static final String TAG_BUFFER = "buffer";
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_STACK = "Stack";
    private static final int ICON_SIZE = 16;
    private static final int LINE_HEIGHT = 10;
    private static final int ICON_GAP = 1;

    private final ItemStack item;
    private final int count;

    public ClientStoragePortTooltip(StoragePortTooltip tooltip) {
        CompoundTag tag = tooltip.blockEntityTag();
        HolderLookup.Provider registries = ClientStoragePortTooltip.registries();
        this.item = ClientStoragePortTooltip.readItem(tag, registries);
        this.count = ClientStoragePortTooltip.readCount(tag, this.item, registries);
    }

    private static ItemStack readItem(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        if (registries == null) {
            return ItemStack.EMPTY;
        }
        if (tag.contains(ClientStoragePortTooltip.TAG_MARKED, Tag.TAG_COMPOUND)) {
            ItemStack marked = ItemStack.parseOptional(registries, tag.getCompound(ClientStoragePortTooltip.TAG_MARKED));
            if (!marked.isEmpty()) {
                return marked;
            }
        }
        ListTag items = tag.getCompound(ClientStoragePortTooltip.TAG_BUFFER).getList(
            ClientStoragePortTooltip.TAG_ITEMS, Tag.TAG_COMPOUND
        );
        for (int i = 0; i < items.size(); i++) {
            CompoundTag entry = items.getCompound(i);
            ItemStack stack = ItemStack.parseOptional(registries, entry);
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int readCount(CompoundTag tag, ItemStack item, @Nullable HolderLookup.Provider registries) {
        if (item.isEmpty() || registries == null) {
            return 0;
        }
        int total = 0;
        ListTag items = tag.getCompound(ClientStoragePortTooltip.TAG_BUFFER).getList(
            ClientStoragePortTooltip.TAG_ITEMS, Tag.TAG_COMPOUND
        );
        for (int i = 0; i < items.size(); i++) {
            CompoundTag entry = items.getCompound(i);
            ItemStack stack = ItemStack.parseOptional(registries, entry);
            if (ItemStack.isSameItemSameComponents(item, stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @Override
    public int getHeight() {
        return item.isEmpty() ? 0 : ClientStoragePortTooltip.LINE_HEIGHT + ClientStoragePortTooltip.ICON_SIZE;
    }

    @Override
    public int getWidth(Font font) {
        if (item.isEmpty()) {
            return 0;
        }
        int labelWidth = font.width(Component.translatable("tooltip.anvilcraft.storage_port.item"));
        return Math.max(labelWidth, ClientStoragePortTooltip.ICON_SIZE + ClientStoragePortTooltip.ICON_GAP
            + font.width(this.itemLine()));
    }

    @Override
    public void renderText(
        Font font,
        int mouseX,
        int mouseY,
        Matrix4f matrix,
        MultiBufferSource.BufferSource bufferSource
    ) {
        if (item.isEmpty()) {
            return;
        }
        font.drawInBatch(
            Component.translatable("tooltip.anvilcraft.storage_port.item").withStyle(ChatFormatting.BLUE),
            mouseX,
            mouseY,
            -1,
            true,
            matrix,
            bufferSource,
            Font.DisplayMode.NORMAL,
            0,
            15728880
        );
        font.drawInBatch(
            this.itemLine(),
            mouseX + ClientStoragePortTooltip.ICON_SIZE + ClientStoragePortTooltip.ICON_GAP,
            mouseY + ClientStoragePortTooltip.LINE_HEIGHT
                + (float) (ClientStoragePortTooltip.ICON_SIZE - ClientStoragePortTooltip.LINE_HEIGHT) / 2,
            -1,
            true,
            matrix,
            bufferSource,
            Font.DisplayMode.NORMAL,
            0,
            15728880
        );
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        if (item.isEmpty()) {
            return;
        }
        guiGraphics.renderItem(item.copyWithCount(1), x, y + ClientStoragePortTooltip.LINE_HEIGHT);
    }

    /** 物品行：数量 + 物品名称（灰色），如「64x Iron Ingot」。 */
    private Component itemLine() {
        return Component.literal(this.count + "x ").append(item.getHoverName()).withStyle(ChatFormatting.GRAY);
    }

    @Nullable
    private static HolderLookup.Provider registries() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            return minecraft.level.registryAccess();
        }
        if (minecraft.getConnection() != null) {
            return minecraft.getConnection().registryAccess();
        }
        return null;
    }
}
