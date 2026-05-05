package dev.dubhe.anvilcraft.util;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.anvilcraft.lib.v2.util.predicate.WeightedChanceBlockStates;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.util.RecipeUtil;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.joml.Matrix3x2fStack;

import java.util.List;
import java.util.Optional;

public class AgeratumUtil {
    public static final int SLOT_SIZE = 19;
    public static final int BLOCK_SIZE = 20;
    public static final int BLOCK_TOOLTIP_SIZE = 22;
    public static final Identifier SLOT = Ageratum.location("textures/gui/component/slot.png");
    public static final Identifier ARROW = Ageratum.location("textures/gui/component/arrow.png");

    public static void renderText(GuiGraphicsExtractor g, Component text, int x, int y) {
        renderText(g, text, x, y, 0.8f);
    }

    public static void renderText(GuiGraphicsExtractor g, Component text, int x, int y, float scale) {
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        g.text(
            Minecraft.getInstance().font,
            text,
            0, 0, 0xFF000000, false
        );

        pose.popMatrix();
    }

    public static void renderArrow(GuiGraphicsExtractor g, int x, int y) {
        renderArrow(g, x, y, 0);
    }

    public static void renderArrow(GuiGraphicsExtractor g, int x, int y, float rotation) {
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x + 16, y + 16);
        pose.rotate(rotation);
        g.blit(ARROW, -16, -16, 0, 0, 32, 32, 32, 32);
        pose.popMatrix();
    }

    public static void renderBlock(
        MDRenderContext context,
        BlockStatePredicate blockStatePredicate,
        float mouseX,
        float mouseY,
        int x,
        int y
    ) {
        List<BlockState> inputBlockState = blockStatePredicate.constructStatesForRender();
        BlockState inputBlockRenderedState = inputBlockState.get(
            RecipeUtil.getDisplayIndex(inputBlockState.size())
        );
        renderBlock(context, inputBlockRenderedState, mouseX, mouseY, x, y);
    }

    public static void renderBlock(
        MDRenderContext context,
        WeightedChanceBlockStates chanceBlockState,
        float mouseX,
        float mouseY,
        int x,
        int y
    ) {
        List<WeightedChanceBlockStates.Entry> results = chanceBlockState.states();
        WeightedChanceBlockStates.Entry result = results.get(RecipeUtil.getDisplayIndex(results.size()));
        renderBlock(context, result.state().state(), mouseX, mouseY, x, y);
    }

    public static void renderBlock(
        MDRenderContext context,
        ChanceBlockState chanceBlockState,
        float mouseX,
        float mouseY,
        int x,
        int y
    ) {
        renderBlock(context, chanceBlockState.state(), mouseX, mouseY, x, y);
    }

    public static void renderBlock(MDRenderContext context, BlockState blockState, float mouseX, float mouseY, int x, int y) {
        RenderSupport.renderBlock(context.graphics(), blockState, x, y, BLOCK_SIZE);
        AgeratumUtil.renderTooltip(context, blockState, mouseX, mouseY, x, y);
    }

    public static <T> void renderItems(
        MDRenderContext context,
        List<T> displaying,
        float mouseX,
        float mouseY,
        int startX,
        int startY
    ) {
        if (displaying.isEmpty()) return;

        int len = Math.max(1, (int) Math.ceil(Math.sqrt(displaying.size())));

        int offsetX = startX - (Math.min(len, displaying.size()) - 1) * SLOT_SIZE / 2;
        int offsetY = startY - (displaying.size() - 1) / len * SLOT_SIZE / 2;

        for (int i = 0; i < displaying.size(); i++) {
            T stack = displaying.get(i);
            int x = offsetX + (i % len) * SLOT_SIZE;
            int y = offsetY + (i / len) * SLOT_SIZE;
            switch (stack) {
                case ItemStack stack1 -> renderItem(context, stack1, mouseX, mouseY, x, y);
                case Ingredient stack1 -> renderItem(context, stack1, mouseX, mouseY, x, y);
                case ItemIngredientPredicate stack1 -> renderItem(context, stack1, mouseX, mouseY, x, y);
                case ChanceItemStack stack1 -> renderItem(context, stack1, mouseX, mouseY, x, y);
                case ItemStackTemplate stack1 -> renderItem(context, stack1, mouseX, mouseY, x, y);
                default -> {
                }
            }
        }
    }

    public static void renderItem(MDRenderContext context, ItemStackTemplate displaying, float mouseX, float mouseY, int x, int y) {
        ItemStack stack = displaying.create();
        renderItem(context, stack, mouseX, mouseY, x, y);
    }

    public static void renderItem(MDRenderContext context, ChanceItemStack displaying, float mouseX, float mouseY, int x, int y) {
        ItemStack stack = getStack(displaying);
        renderItem(context, stack, mouseX, mouseY, x, y);
    }

    public static void renderItem(MDRenderContext context, ItemIngredientPredicate displaying, float mouseX, float mouseY, int x, int y) {
        renderSlot(context, x, y);
        renderItemWithoutSlot(context, displaying, mouseX, mouseY, x, y);
    }

    public static void renderItem(MDRenderContext context, Ingredient displaying, float mouseX, float mouseY, int x, int y) {
        renderItem(context, RecipeUtil.getDisplayItem(displaying), mouseX, mouseY, x, y);
    }

    public static void renderItem(MDRenderContext context, ItemStack displaying, float mouseX, float mouseY, int x, int y) {
        renderSlot(context, x, y);
        renderItemWithoutSlot(context, displaying, mouseX, mouseY, x, y);
    }

    private static void renderSlot(MDRenderContext context, int x, int y) {
        context.graphics().blit(SLOT, x - 8, y - 8, 0, 0, 32, 32, 32, 32);
    }

    public static void renderItemWithoutSlot(
        MDRenderContext context,
        ItemStackTemplate displaying,
        float mouseX,
        float mouseY,
        int x,
        int y
    ) {
        renderItemWithoutSlot(context, displaying.create(), mouseX, mouseY, x, y);
    }

    public static void renderItemWithoutSlot(MDRenderContext context, Ingredient displaying, float mouseX, float mouseY, int x, int y) {
        renderItemWithoutSlot(context, RecipeUtil.getDisplayItem(displaying), mouseX, mouseY, x, y);
    }

    public static void renderItemWithoutSlot(
        MDRenderContext context,
        ItemIngredientPredicate displaying,
        float mouseX,
        float mouseY,
        int x,
        int y
    ) {
        ItemStack stack = displaying.getItems()[RecipeUtil.getDisplayIndex(displaying.getItems().length)].create();
        renderItemWithoutSlot(context, stack, mouseX, mouseY, x, y);
    }

    public static void renderItemWithoutSlot(MDRenderContext context, ItemStack displaying, float mouseX, float mouseY, int x, int y) {
        GuiGraphicsExtractor g = context.graphics();
        g.item(displaying, x, y);
        g.itemDecorations(Minecraft.getInstance().font, displaying, x, y);
        AgeratumUtil.renderTooltip(context, displaying, x, y, mouseX, mouseY);
    }

    public static void renderTooltip(MDRenderContext context, BlockState state, float mouseX, float mouseY, int startX, int startY) {
        if (isHoverBlock(startX, startY, mouseX, mouseY)) {
            context.tooltips().add(new MDRenderContext.Tooltip(TooltipUtil.tooltip(state.getBlock()), Optional.empty()));
        }
    }

    public static void renderTooltip(MDRenderContext context, ItemStack stack, int startX, int startY, float mouseX, float mouseY) {
        if (isHoverItem(startX, startY, mouseX, mouseY)) {
            context.addTooltip(stack);
        }
    }

    public static boolean isHoverBlock(int startX, int startY, float mouseX, float mouseY) {
        return isHover(
            startX - BLOCK_TOOLTIP_SIZE / 2,
            startY - BLOCK_TOOLTIP_SIZE / 3,
            BLOCK_TOOLTIP_SIZE,
            BLOCK_TOOLTIP_SIZE,
            mouseX,
            mouseY
        );
    }

    public static boolean isHoverItem(int startX, int startY, float mouseX, float mouseY) {
        return isHover(startX, startY, 16, 16, mouseX, mouseY);
    }

    public static boolean isHover(int startX, int startY, int width, int height, float mouseX, float mouseY) {
        return mouseX >= startX && mouseX <= startX + width && mouseY >= startY && mouseY <= startY + height;
    }

    public static ItemStack getStack(ChanceItemStack stack) {
        ItemStack itemStack = stack.stack().create();
        if (stack.count() instanceof ConstantValue) {
            itemStack.setCount(stack.getMaxCount());
        } else if (stack.count() instanceof BinomialDistributionGenerator count) {
            if (count.p() instanceof ConstantValue(float value) && value == 1) itemStack.setCount(stack.getMaxCount());
        }
        return itemStack;
    }
}
