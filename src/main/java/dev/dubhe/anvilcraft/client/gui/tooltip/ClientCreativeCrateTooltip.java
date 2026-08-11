package dev.dubhe.anvilcraft.client.gui.tooltip;

import dev.dubhe.anvilcraft.inventory.tooltip.CreativeCrateTooltip;
import dev.dubhe.anvilcraft.util.UnitUtil;
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
 * 创造板条箱物品 tooltip：以 16×16 图标 + 名称/无限标记 的形式渲染存储物品。
 */
public class ClientCreativeCrateTooltip implements ClientTooltipComponent {
    private static final String TAG_ITEMS = "Items";
    private static final int ICON_SIZE = 16;
    private static final int LINE_HEIGHT = 10;
    private static final int ICON_GAP = 1;

    private final ItemStack item;

    public ClientCreativeCrateTooltip(CreativeCrateTooltip tooltip) {
        this.item = readStoredItem(tooltip.itemTag());
    }

    private static ItemStack readStoredItem(CompoundTag itemTag) {
        ListTag items = itemTag.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        if (items.isEmpty()) return ItemStack.EMPTY;
        HolderLookup.Provider registries = registries();
        if (registries == null) return ItemStack.EMPTY;
        return ItemStack.parseOptional(registries, items.getCompound(0));
    }

    private static HolderLookup.@Nullable Provider registries() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            return minecraft.level.registryAccess();
        }
        if (minecraft.getConnection() != null) {
            return minecraft.getConnection().registryAccess();
        }
        return null;
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
        guiGraphics.renderItem(item.copyWithCount(1), x, y + LINE_HEIGHT);
    }

    /** 存储物品行：名称 + 无限标记（灰色）。 */
    private Component itemLine() {
        return Component.literal("")
            .append(item.getHoverName())
            .append(Component.literal(" " + UnitUtil.INFINITE_POWER))
            .withStyle(ChatFormatting.GRAY);
    }
}
