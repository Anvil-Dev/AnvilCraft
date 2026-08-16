package dev.dubhe.anvilcraft.client.renderer.item.decoration;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

public class TerminalInsertionDecoration implements IItemDecorator {
    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int offsetX, int offsetY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        // 仅当处于容器 GUI 中、指针捏着物品、且该终端未被捏起时显示“+”
        if (!(minecraft.screen instanceof AbstractContainerScreen<?>)) {
            return false;
        }
        ItemStack carried = minecraft.player.inventoryMenu.getCarried();
        if (carried.isEmpty() || ItemStack.isSameItemSameComponents(stack, carried)) {
            return false;
        }
        TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
        if (binding == null || binding.id().isEmpty()) {
            return false;
        }
        // 指针捏着物品时在终端图标右上角显示“+”，提示可右键放入存储站。
        // 装饰器调用时 pose 已回到 z=0，需要抬高 z 才会绘制在物品之上。
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
        guiGraphics.drawString(font, "+", offsetX + 10, offsetY + 1, 0xFFFFFFFF, true);
        guiGraphics.pose().popPose();
        return true;
    }
}
