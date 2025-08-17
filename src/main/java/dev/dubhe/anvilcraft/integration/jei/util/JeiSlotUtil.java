package dev.dubhe.anvilcraft.integration.jei.util;

import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.common.gui.elements.DrawableText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.List;

public class JeiSlotUtil {
    public static void drawInputSlots(GuiGraphics guiGraphics, IDrawable slot, int inputSize) {
        if (inputSize == 0) return;
        if (inputSize == 1) {
            slot.draw(guiGraphics, 20, 23);
        } else if (inputSize <= 4) {
            int startX = 10;
            int startY = 14;
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    slot.draw(guiGraphics, startX + j * 18, startY + i * 18);
                }
            }
        } else if (inputSize <= 6) {
            int startX = 1;
            int startY = 14;
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    slot.draw(guiGraphics, startX + j * 18, startY + i * 18);
                }
            }
        } else {
            int startX = 1;
            int startY = 5;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    slot.draw(guiGraphics, startX + j * 18, startY + i * 18);
                }
            }
        }
    }

    public static void drawOutputSlots(GuiGraphics guiGraphics, IDrawable slot, int outputSize) {
        if (outputSize == 0) return;
        if (outputSize == 1) {
            slot.draw(guiGraphics, 124, 23);
        } else if (outputSize <= 4) {
            int startX = 116;
            int startY = 14;
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    slot.draw(guiGraphics, startX + j * 18, startY + i * 18);
                }
            }
        } else if (outputSize <= 6) {
            int startX = 107;
            int startY = 14;
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    slot.draw(guiGraphics, startX + j * 18, startY + i * 18);
                }
            }
        } else {
            int startX = 107;
            int startY = 5;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    slot.draw(guiGraphics, startX + j * 18, startY + i * 18);
                }
            }
        }
    }

    public static void addSlotWithCount(
        IRecipeLayoutBuilder builder, int slotX, int slotY, ItemIngredientPredicate entry) {
        IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, slotX, slotY);
        slot.addIngredients(Ingredient.of(entry.getItems()));
    }

    public static void addInputSlots(
        IRecipeLayoutBuilder builder, List<ItemIngredientPredicate> mergedIngredients) {
        int inputSize = mergedIngredients.size();
        if (inputSize == 0) return;
        if (inputSize == 1) {
            ItemIngredientPredicate ingredient = mergedIngredients.getFirst();
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, 21, 24);
            slot.addIngredients(Ingredient.of(ingredient.getItems()));
        } else if (inputSize <= 4) {
            int startX = 11;
            int startY = 15;
            for (int index = 0; index < inputSize; index++) {
                int row = index / 2;
                int col = index % 2;
                addSlotWithCount(builder, startX + 18 * col, startY + 18 * row, mergedIngredients.get(index));
            }
        } else if (inputSize <= 6) {
            int startX = 2;
            int startY = 15;
            for (int index = 0; index < inputSize; index++) {
                int row = index / 3;
                int col = index % 3;
                addSlotWithCount(builder, startX + 18 * col, startY + 18 * row, mergedIngredients.get(index));
            }
        } else {
            int startX = 1;
            int startY = 6;
            for (int index = 0; index < inputSize; index++) {
                if (index > 9) break;
                int row = index / 3;
                int col = index % 3;
                addSlotWithCount(builder, startX + 18 * col, startY + 18 * row, mergedIngredients.get(index));
            }
        }
    }

    public static void addOutputSlots(IRecipeLayoutBuilder builder, List<ChanceItemStack> results) {
        int outputSize = results.size();
        if (outputSize == 0) return;
        if (outputSize == 1) {
            ChanceItemStack stack = results.getFirst();
            ItemStack itemStack = stack.getStack().copy();
            if (stack.getCount() instanceof ConstantValue) {
                itemStack.setCount(stack.getMaxCount());
            }
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, 125, 24)
                .addItemStack(itemStack);
            JeiRecipeUtil.addTooltips(slot, stack.getMaxCount(), stack.getCount());
        } else if (outputSize <= 4) {
            int startX = 117;
            int startY = 15;
            for (int index = 0; index < outputSize; index++) {
                int row = index / 2;
                int col = index % 2;
                ChanceItemStack stack = results.get(index);
                ItemStack itemStack = stack.getStack().copy();
                if (stack.getCount() instanceof ConstantValue) {
                    itemStack.setCount(stack.getMaxCount());
                }
                IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, startX + 18 * col, startY + 18 * row)
                    .addItemStack(itemStack);
                JeiRecipeUtil.addTooltips(slot, stack.getMaxCount(), stack.getCount());
            }
        } else if (outputSize <= 6) {
            int startX = 108;
            int startY = 15;
            for (int index = 0; index < outputSize; index++) {
                int row = index / 2;
                int col = index % 3;
                ChanceItemStack stack = results.get(index);
                ItemStack itemStack = stack.getStack().copy();
                if (stack.getCount() instanceof ConstantValue) {
                    itemStack.setCount(stack.getMaxCount());
                }
                IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, startX + 18 * col, startY + 18 * row)
                    .addItemStack(itemStack);
                JeiRecipeUtil.addTooltips(slot, stack.getMaxCount(), stack.getCount());
            }
        } else {
            int startX = 108;
            int startY = 6;
            for (int index = 0; index < outputSize; index++) {
                if (index > 9) break;
                int row = index / 3;
                int col = index % 3;
                ChanceItemStack stack = results.get(index);
                ItemStack itemStack = stack.getStack().copy();
                if (stack.getCount() instanceof ConstantValue) {
                    itemStack.setCount(stack.getMaxCount());
                }
                IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, startX + 18 * col, startY + 18 * row)
                    .addItemStack(itemStack);
                JeiRecipeUtil.addTooltips(slot, stack.getMaxCount(), stack.getCount());
            }
        }
    }
}
