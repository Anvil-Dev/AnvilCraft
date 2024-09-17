package dev.dubhe.anvilcraft.integration.jei.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.common.gui.elements.DrawableText;

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
            IRecipeLayoutBuilder builder, int slotX, int slotY, Object2IntMap.Entry<Ingredient> entry) {
        IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, slotX, slotY);
        slot.addIngredients(entry.getKey());
        if (entry.getIntValue() > 1) {
            slot.setOverlay(new DrawableText("" + entry.getIntValue(), 2, 2, 0xFFFFFFFF), 12, 12);
        }
    }

    public static void addInputSlots(
            IRecipeLayoutBuilder builder, List<Object2IntMap.Entry<Ingredient>> mergedIngredients) {
        int inputSize = mergedIngredients.size();
        if (inputSize == 0) return;
        if (inputSize == 1) {
            Object2IntMap.Entry<Ingredient> entry = mergedIngredients.getFirst();
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, 21, 24);
            slot.addIngredients(entry.getKey());
            if (entry.getIntValue() > 1) {
                slot.setOverlay(new DrawableText("" + entry.getIntValue(), 2, 2, 0xFFFFFFFF), 12, 12);
            }
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
                int row = index / 2;
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

    public static void addOutputSlots(IRecipeLayoutBuilder builder, List<ItemStack> results) {
        int outputSize = results.size();
        if (outputSize == 0) return;
        if (outputSize == 1) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 125, 24).addItemStack(results.getFirst());
        } else if (outputSize <= 4) {
            int startX = 117;
            int startY = 15;
            for (int index = 0; index < outputSize; index++) {
                int row = index / 2;
                int col = index % 2;
                builder.addSlot(RecipeIngredientRole.OUTPUT, startX + 18 * col, startY + 18 * row)
                        .addItemStack(results.get(index));
            }
        } else if (outputSize <= 6) {
            int startX = 108;
            int startY = 15;
            for (int index = 0; index < outputSize; index++) {
                int row = index / 2;
                int col = index % 3;
                builder.addSlot(RecipeIngredientRole.OUTPUT, startX + 18 * col, startY + 18 * row)
                        .addItemStack(results.get(index));
            }
        } else {
            int startX = 108;
            int startY = 6;
            for (int index = 0; index < outputSize; index++) {
                if (index > 9) break;
                int row = index / 3;
                int col = index % 3;
                builder.addSlot(RecipeIngredientRole.OUTPUT, startX + 18 * col, startY + 18 * row)
                        .addItemStack(results.get(index));
            }
        }
    }
}
