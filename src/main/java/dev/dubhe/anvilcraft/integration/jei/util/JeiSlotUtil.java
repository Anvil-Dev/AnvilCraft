package dev.dubhe.anvilcraft.integration.jei.util;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.List;

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

    /**
     * 默认的居中位置
     */
    public static void addDefaultInputSlots(IRecipeLayoutBuilder builder, List<ItemIngredientPredicate> mergedIngredients) {
        JeiSlotUtil.addInputSlots(builder, mergedIngredients, INPUT_X, DEFAULT_Y);
    }

    /**
     * 存在流体时物品位置向上偏移
     */
    public static void addItemInputSlots(IRecipeLayoutBuilder builder, List<ItemIngredientPredicate> mergedIngredients) {
        JeiSlotUtil.addInputSlots(builder, mergedIngredients, INPUT_X, ITEM_Y);
    }

    public static void addInputSlots(
        IRecipeLayoutBuilder builder,
        List<ItemIngredientPredicate> mergedIngredients,
        int centerX,
        int centerY
    ) {
        addSlots(
            mergedIngredients.size(), centerX, centerY,
            (x, y, i) -> addSlotWithCount(builder, x, y, mergedIngredients.get(i))
        );
    }

    /**
     * 默认的居中位置
     */
    public static void addDefaultOutputSlots(IRecipeLayoutBuilder builder, List<ChanceItemStack> results) {
        JeiSlotUtil.addOutputSlots(builder, results, OUTPUT_X, DEFAULT_Y);
    }

    /**
     * 存在流体时物品位置向上偏移
     */
    public static void addItemOutputSlots(IRecipeLayoutBuilder builder, List<ChanceItemStack> results) {
        JeiSlotUtil.addOutputSlots(builder, results, OUTPUT_X, ITEM_Y);
    }

    public static void addOutputSlots(IRecipeLayoutBuilder builder, List<ChanceItemStack> results, int centerX, int centerY) {
        addSlots(
            results.size(), centerX, centerY,
            (x, y, i) -> addOutputSlot(builder, x, y, results.get(i))
        );
    }

    @FunctionalInterface
    private interface SlotPlacer {
        void place(int x, int y, int index);
    }

    private static void addSlots(int size, int centerX, int centerY, SlotPlacer placer) {
        if (size == 0) return;
        int cols = (int) Math.ceil(Math.sqrt(size));
        int rows = Math.ceilDiv(size, cols);
        int startX = centerX - (cols - 1) * OFFSET / 2;
        int startY = centerY - (rows - 1) * OFFSET / 2;
        for (int i = 0; i < size; i++) {
            placer.place(startX + (i % cols) * OFFSET, startY + (i / cols) * OFFSET, i);
        }
    }

    public static void addSlotWithCount(IRecipeLayoutBuilder builder, int x, int y, ItemIngredientPredicate entry) {
        IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, x, y);
        slot.addIngredients(Ingredient.of(entry.getItems()));
    }

    public static void addOutputSlot(IRecipeLayoutBuilder builder, int x, int y, ChanceItemStack stack) {
        ItemStack itemStack = stack.stack().copy();
        if (stack.count() instanceof ConstantValue) {
            itemStack.setCount(stack.getMaxCount());
        }
        IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).addItemStack(itemStack);
        JeiRecipeUtil.addTooltips(slot, stack.getMaxCount(), stack.count());
    }
}
