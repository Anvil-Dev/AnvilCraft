package dev.dubhe.anvilcraft.client.renderer.item.decoration;

import dev.dubhe.anvilcraft.item.IonoCraftBackpackItem;
import dev.dubhe.anvilcraft.util.ColorUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

public class IonoCraftBackpackDecoration implements IItemDecorator {
    private static final int FULL_BAR_COLOR = 0xFF5454FF;
    private static final int BAR_COLOR = 0x7087FFFF;

    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int offsetX, int offsetY) {
        int energy = IonoCraftBackpackItem.getEnergyStored(stack);
        float ratio = Math.clamp((float) energy / IonoCraftBackpackItem.MAX_ENERGY, 0, 1);

        int x = offsetX + 2;
        boolean hasDurability = stack.isBarVisible();
        // 原版耐久条可见时放到其上方，否则放到槽位顶部
        int y = hasDurability ? offsetY + 12 : offsetY + 13;
        int bgH = hasDurability ? 1 : 2;
        int fillH = 1;

        // 背景（与武器能量条一致），z=200 确保在物品图标之上
        guiGraphics.fill(x, y, x + 13, y + bgH, 200, 0xFF000000);

        if (ratio > 0) {
            int filledWidth = Math.round(13 * ratio);
            int color = ColorUtil.lerpColor(ratio, BAR_COLOR, FULL_BAR_COLOR);
            guiGraphics.fill(x, y, x + filledWidth, y + fillH, 200, color);
        }

        return true;
    }
}
