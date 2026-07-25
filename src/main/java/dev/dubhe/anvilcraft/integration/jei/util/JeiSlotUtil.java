package dev.dubhe.anvilcraft.integration.jei.util;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;

public class JeiSlotUtil {
    public static final int OFFSET = 19;

    public static final int INPUT_X = 21;
    public static final int OUTPUT_X = 125;
    public static final int ITEM_Y = 15;
    public static final int FLUID_Y = 46;
    public static final int DEFAULT_Y = 22;

    /**
     * 默认的居中位置
     */
    public static void drawDefaultInputSlots(GuiGraphics guiGraphics, IDrawable slot, int inputSize) {
        drawSlots(guiGraphics, slot, inputSize, INPUT_X - 1, DEFAULT_Y - 1);
    }

    /**
     * 存在流体时物品位置向上偏移
     */
    public static void drawItemInputSlots(GuiGraphics guiGraphics, IDrawable slot, int inputSize) {
        drawSlots(guiGraphics, slot, inputSize, INPUT_X - 1, ITEM_Y - 1);
    }

    /**
     * 存在物品时流体位置向下偏移
     */
    public static void drawFluidInputSlots(GuiGraphics guiGraphics, IDrawable slot, int inputSize) {
        drawSlots(guiGraphics, slot, inputSize, INPUT_X - 1, FLUID_Y - 1);
    }

    /**
     * 默认的居中位置
     */
    public static void drawDefaultOutputSlots(GuiGraphics guiGraphics, IDrawable slot, int outputSize) {
        drawSlots(guiGraphics, slot, outputSize, OUTPUT_X - 1, DEFAULT_Y - 1);
    }

    /**
     * 存在流体时物品位置向上偏移
     */
    public static void drawItemOutputSlots(GuiGraphics guiGraphics, IDrawable slot, int outputSize) {
        drawSlots(guiGraphics, slot, outputSize, OUTPUT_X - 1, ITEM_Y - 1);
    }

    /**
     * 存在物品时流体位置向下偏移
     */
    public static void drawFluidOutputSlots(GuiGraphics guiGraphics, IDrawable slot, int inputSize) {
        drawSlots(guiGraphics, slot, inputSize, OUTPUT_X - 1, FLUID_Y - 1);
    }

    public static void drawSlots(GuiGraphics guiGraphics, IDrawable slot, int size, int centerX, int centerY) {
        if (size == 0) return;
        int cols = (int) Math.ceil(Math.sqrt(size));
        int rows = Math.ceilDiv(size, cols);
        int startX = centerX - (cols - 1) * OFFSET / 2;
        int startY = centerY - (rows - 1) * OFFSET / 2;
        for (int i = 0; i < size; i++) {
            slot.draw(guiGraphics, startX + (i % cols) * OFFSET, startY + (i / cols) * OFFSET);
        }
    }

}
