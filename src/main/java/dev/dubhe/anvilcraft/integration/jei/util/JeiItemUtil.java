package dev.dubhe.anvilcraft.integration.jei.util;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.List;

public class JeiItemUtil {
    /**
     * 默认的居中位置
     */
    public static void addDefaultInputSlots(IRecipeLayoutBuilder builder, List<ItemIngredientPredicate> mergedIngredients) {
        JeiItemUtil.addInputSlots(builder, mergedIngredients, JeiSlotUtil.INPUT_X, JeiSlotUtil.DEFAULT_Y);
    }

    /**
     * 存在流体时物品位置向上偏移
     */
    public static void addItemInputSlots(IRecipeLayoutBuilder builder, List<ItemIngredientPredicate> mergedIngredients) {
        JeiItemUtil.addInputSlots(builder, mergedIngredients, JeiSlotUtil.INPUT_X, JeiSlotUtil.ITEM_Y);
    }

    public static void addInputSlots(
        IRecipeLayoutBuilder builder,
        List<ItemIngredientPredicate> mergedIngredients,
        int centerX,
        int centerY
    ) {
        JeiItemUtil.addSlots(
            mergedIngredients.size(), centerX, centerY,
            (x, y, i) -> JeiItemUtil.addSlotWithCount(builder, x, y, mergedIngredients.get(i))
        );
    }

    /**
     * 默认的居中位置
     */
    public static void addDefaultOutputSlots(IRecipeLayoutBuilder builder, List<ChanceItemStack> results) {
        JeiItemUtil.addOutputSlots(builder, results, JeiSlotUtil.OUTPUT_X, JeiSlotUtil.DEFAULT_Y);
    }

    /**
     * 存在流体时物品位置向上偏移
     */
    public static void addItemOutputSlots(IRecipeLayoutBuilder builder, List<ChanceItemStack> results) {
        JeiItemUtil.addOutputSlots(builder, results, JeiSlotUtil.OUTPUT_X, JeiSlotUtil.ITEM_Y);
    }

    public static void addOutputSlots(IRecipeLayoutBuilder builder, List<ChanceItemStack> results, int centerX, int centerY) {
        JeiItemUtil.addSlots(
            results.size(), centerX, centerY,
            (x, y, i) -> JeiItemUtil.addOutputSlot(builder, x, y, results.get(i))
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
        int startX = centerX - (cols - 1) * JeiSlotUtil.OFFSET / 2;
        int startY = centerY - (rows - 1) * JeiSlotUtil.OFFSET / 2;
        for (int i = 0; i < size; i++) {
            placer.place(startX + (i % cols) * JeiSlotUtil.OFFSET, startY + (i / cols) * JeiSlotUtil.OFFSET, i);
        }
    }

    public static void addSlotWithCount(IRecipeLayoutBuilder builder, int x, int y, ItemIngredientPredicate entry) {
        IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, x, y);
        for (ItemStackTemplate template : entry.getItems()) {
            slot.add(template);
        }
    }

    public static void addOutputSlot(IRecipeLayoutBuilder builder, int x, int y, ChanceItemStack stack) {
        ItemStackTemplate template = stack.stack();
        if (stack.count() instanceof ConstantValue) {
            template = template.withCount(stack.getMaxCount());
        }
        IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).add(template);
        JeiRecipeUtil.addTooltips(slot, stack.getMaxCount(), stack.count());
    }
}
