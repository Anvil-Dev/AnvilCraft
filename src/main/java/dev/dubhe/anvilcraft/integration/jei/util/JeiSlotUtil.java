package dev.dubhe.anvilcraft.integration.jei.util;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.Arrays;
import java.util.List;

public class JeiSlotUtil {
    public static final int OFFSET = 19;

    public static final int INPUT_X = 21;
    public static final int OUTPUT_X = 125;
    public static final int ITEM_Y = 15;
    public static final int FLUID_Y = 46;
    public static final int DEFAULT_Y = 22;

    /**
     * 使用默认的居中位置绘制输入槽。
     */
    public static void drawDefaultInputSlots(GuiGraphicsExtractor graphics, IDrawable slot, int inputSize) {
        JeiSlotUtil.drawSlots(graphics, slot, inputSize, JeiSlotUtil.INPUT_X - 1, JeiSlotUtil.DEFAULT_Y - 1);
    }

    /**
     * 存在流体时将物品输入槽向上偏移。
     */
    public static void drawItemInputSlots(GuiGraphicsExtractor graphics, IDrawable slot, int inputSize) {
        JeiSlotUtil.drawSlots(graphics, slot, inputSize, JeiSlotUtil.INPUT_X - 1, JeiSlotUtil.ITEM_Y - 1);
    }

    /**
     * 存在物品时将流体输入槽向下偏移。
     */
    public static void drawFluidInputSlots(GuiGraphicsExtractor graphics, IDrawable slot, int inputSize) {
        JeiSlotUtil.drawSlots(graphics, slot, inputSize, JeiSlotUtil.INPUT_X - 1, JeiSlotUtil.FLUID_Y - 1);
    }

    /**
     * 使用默认的居中位置绘制输出槽。
     */
    public static void drawDefaultOutputSlots(GuiGraphicsExtractor graphics, IDrawable slot, int outputSize) {
        JeiSlotUtil.drawSlots(graphics, slot, outputSize, JeiSlotUtil.OUTPUT_X - 1, JeiSlotUtil.DEFAULT_Y - 1);
    }

    /**
     * 存在流体时将物品输出槽向上偏移。
     */
    public static void drawItemOutputSlots(GuiGraphicsExtractor graphics, IDrawable slot, int outputSize) {
        JeiSlotUtil.drawSlots(graphics, slot, outputSize, JeiSlotUtil.OUTPUT_X - 1, JeiSlotUtil.ITEM_Y - 1);
    }

    /**
     * 存在物品时将流体输出槽向下偏移。
     */
    public static void drawFluidOutputSlots(GuiGraphicsExtractor graphics, IDrawable slot, int outputSize) {
        JeiSlotUtil.drawSlots(graphics, slot, outputSize, JeiSlotUtil.OUTPUT_X - 1, JeiSlotUtil.FLUID_Y - 1);
    }

    public static void drawSlots(
        GuiGraphicsExtractor graphics,
        IDrawable slot,
        int size,
        int centerX,
        int centerY
    ) {
        if (size == 0) return;
        int columns = (int) Math.ceil(Math.sqrt(size));
        int rows = Math.ceilDiv(size, columns);
        int startX = centerX - (columns - 1) * JeiSlotUtil.OFFSET / 2;
        int startY = centerY - (rows - 1) * JeiSlotUtil.OFFSET / 2;
        for (int i = 0; i < size; i++) {
            slot.draw(graphics, startX + (i % columns) * JeiSlotUtil.OFFSET, startY + (i / columns) * JeiSlotUtil.OFFSET);
        }
    }

    public static void drawInputSlots(GuiGraphicsExtractor graphics, IDrawable slot, int inputSize) {
        if (inputSize == 0) return;
        if (inputSize == 1) {
            slot.draw(graphics, 20, 23);
        } else if (inputSize <= 4) {
            int startX = 10;
            int startY = 14;
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    slot.draw(graphics, startX + j * 19, startY + i * 19);
                }
            }
        } else if (inputSize <= 6) {
            int startX = 1;
            int startY = 14;
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    slot.draw(graphics, startX + j * 19, startY + i * 19);
                }
            }
        } else {
            int startX = 1;
            int startY = 5;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    slot.draw(graphics, startX + j * 19, startY + i * 19);
                }
            }
        }
    }

    public static void drawOutputSlots(GuiGraphicsExtractor graphics, IDrawable slot, int outputSize) {
        if (outputSize == 0) return;
        if (outputSize == 1) {
            slot.draw(graphics, 124, 23);
        } else if (outputSize <= 4) {
            int startX = 116;
            int startY = 14;
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    slot.draw(graphics, startX + j * 19, startY + i * 19);
                }
            }
        } else if (outputSize <= 6) {
            int startX = 107;
            int startY = 14;
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    slot.draw(graphics, startX + j * 19, startY + i * 19);
                }
            }
        } else {
            int startX = 107;
            int startY = 5;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    slot.draw(graphics, startX + j * 19, startY + i * 19);
                }
            }
        }
    }

    public static void addSlotWithCount(IRecipeLayoutBuilder builder, int slotX, int slotY, ItemIngredientPredicate entry) {
        IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, slotX, slotY);
        Arrays.stream(entry.getItems()).forEach(template -> slot.add(template.withCount(entry.count())));
    }

    public static void addDiffInputSlots(IRecipeLayoutBuilder builder, ItemIngredientPredicate ingredient) {
        int inputSize = ingredient.count();
        if (inputSize == 0) return;
        if (inputSize == 1) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, 21, 24);
            slot.add(Ingredient.of(Arrays.stream(ingredient.getItems()).map(template -> template.item().value())));
        } else if (inputSize <= 4) {
            int startX = 11;
            int startY = 15;
            for (int index = 0; index < inputSize; index++) {
                int row = index / 2;
                int col = index % 2;
                JeiSlotUtil.addSlotWithCount(builder, startX + 19 * col, startY + 19 * row, ingredient.withCount(1));
            }
        } else if (inputSize <= 6) {
            int startX = 2;
            int startY = 15;
            for (int index = 0; index < inputSize; index++) {
                int row = index / 3;
                int col = index % 3;
                JeiSlotUtil.addSlotWithCount(builder, startX + 19 * col, startY + 19 * row, ingredient.withCount(1));
            }
        } else {
            int startX = 1;
            int startY = 6;
            for (int index = 0; index < inputSize; index++) {
                if (index > 9) break;
                int row = index / 3;
                int col = index % 3;
                JeiSlotUtil.addSlotWithCount(builder, startX + 19 * col, startY + 19 * row, ingredient.withCount(1));
            }
        }
    }

    public static void addInputSlots(IRecipeLayoutBuilder builder, List<ItemIngredientPredicate> mergedIngredients) {
        int inputSize = mergedIngredients.size();
        if (inputSize == 0) return;
        if (inputSize == 1) {
            ItemIngredientPredicate ingredient = mergedIngredients.getFirst();
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, 21, 24);
            slot.add(Ingredient.of(Arrays.stream(ingredient.getItems()).map(template -> template.item().value())));
        } else if (inputSize <= 4) {
            int startX = 11;
            int startY = 15;
            for (int index = 0; index < inputSize; index++) {
                int row = index / 2;
                int col = index % 2;
                JeiSlotUtil.addSlotWithCount(builder, startX + 19 * col, startY + 19 * row, mergedIngredients.get(index));
            }
        } else if (inputSize <= 6) {
            int startX = 2;
            int startY = 15;
            for (int index = 0; index < inputSize; index++) {
                int row = index / 3;
                int col = index % 3;
                JeiSlotUtil.addSlotWithCount(builder, startX + 19 * col, startY + 19 * row, mergedIngredients.get(index));
            }
        } else {
            int startX = 1;
            int startY = 6;
            for (int index = 0; index < inputSize; index++) {
                if (index > 9) break;
                int row = index / 3;
                int col = index % 3;
                JeiSlotUtil.addSlotWithCount(builder, startX + 19 * col, startY + 19 * row, mergedIngredients.get(index));
            }
        }
    }

    public static void addOutputSlots(IRecipeLayoutBuilder builder, List<ChanceItemStack> results) {
        int outputSize = results.size();
        if (outputSize == 0) return;
        if (outputSize == 1) {
            ChanceItemStack stack = results.getFirst();
            ItemStackTemplate template = stack.stack();
            if (stack.count() instanceof ConstantValue) {
                template = template.withCount(stack.getMaxCount());
            }
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, 125, 24).add(template);
            JeiRecipeUtil.addTooltips(slot, stack.getMaxCount(), stack.count());
        } else if (outputSize <= 4) {
            int startX = 117;
            int startY = 15;
            for (int index = 0; index < outputSize; index++) {
                int row = index / 2;
                int col = index % 2;
                ChanceItemStack stack = results.get(index);
                ItemStackTemplate template = stack.stack();
                if (stack.count() instanceof ConstantValue) {
                    template = template.withCount(stack.getMaxCount());
                }
                IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, startX + 19 * col, startY + 19 * row).add(template);
                JeiRecipeUtil.addTooltips(slot, stack.getMaxCount(), stack.count());
            }
        } else if (outputSize <= 6) {
            int startX = 108;
            int startY = 15;
            for (int index = 0; index < outputSize; index++) {
                int row = index / 3;
                int col = index % 3;
                ChanceItemStack stack = results.get(index);
                ItemStackTemplate template = stack.stack();
                if (stack.count() instanceof ConstantValue) {
                    template = template.withCount(stack.getMaxCount());
                }
                IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, startX + 19 * col, startY + 19 * row).add(template);
                JeiRecipeUtil.addTooltips(slot, stack.getMaxCount(), stack.count());
            }
        } else {
            int startX = 108;
            int startY = 6;
            for (int index = 0; index < outputSize; index++) {
                if (index > 9) break;
                int row = index / 3;
                int col = index % 3;
                ChanceItemStack stack = results.get(index);
                ItemStackTemplate template = stack.stack();
                if (stack.count() instanceof ConstantValue) {
                    template = template.withCount(stack.getMaxCount());
                }
                IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, startX + 19 * col, startY + 19 * row).add(template);
                JeiRecipeUtil.addTooltips(slot, stack.getMaxCount(), stack.count());
            }
        }
    }
}
