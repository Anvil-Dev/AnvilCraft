package dev.dubhe.anvilcraft.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.util.RecipeUtil;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.ArrayList;
import java.util.List;

public class AgeratumUtil {
    public static final int SLOT_SIZE = 19;
    public static final int BLOCK_SIZE = 16;
    public static final ResourceLocation SLOT = Ageratum.location("textures/gui/component/slot.png");
    public static final ResourceLocation ARROW = Ageratum.location("textures/gui/component/arrow.png");

    public static void renderArrow(GuiGraphics g, int x, int y) {
        renderArrow(g, x, y, 0);
    }

    public static void renderArrow(GuiGraphics g, int x, int y, float rotation) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x + 16, y + 16, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(rotation));
        g.blit(ARROW, -16, -16, 0, 0, 32, 32, 32, 32);
        pose.popPose();
    }

    public static void renderBlock(GuiGraphics g, BlockState blockState, int x, int y, int z) {
        RenderSupport.renderBlock(g, blockState, x, y, z, BLOCK_SIZE, RenderSupport.SINGLE_BLOCK);
    }

    public static void renderItemPredicates(
        MDRenderContext context,
        List<ItemIngredientPredicate> displaying,
        float mouseX,
        float mouseY,
        int startX,
        int startY
    ) {
        List<ItemStack> stacks = displaying.stream()
            .map(ingredient -> ingredient.getItems()[RecipeUtil.getDisplayIndex(ingredient.getItems().length)])
            .filter(stack -> !stack.isEmpty())
            .toList();
        renderItems(context, stacks, mouseX, mouseY, startX, startY);
    }

    public static void renderChanceItemStacks(
        MDRenderContext context,
        List<ChanceItemStack> displaying,
        float mouseX,
        float mouseY,
        int startX,
        int startY
    ) {
        List<ChanceItemStack> chanceItemStacks = new ArrayList<>();
        for (ChanceItemStack outputItem : displaying) {
            if (outputItem.count() instanceof BinomialDistributionGenerator(NumberProvider n, NumberProvider p)) {
                if (p instanceof ConstantValue(float value) && value < 1 && n instanceof ConstantValue(float count)) {
                    chanceItemStacks.add(ChanceItemStack.of(outputItem.stack(), (int) count, value));
                } else {
                    chanceItemStacks.add(ChanceItemStack.of(outputItem.stack(), outputItem.getMaxCount()));
                }
            }
        }
        List<ItemStack> stacks = chanceItemStacks.stream()
            .map(AgeratumUtil::getStack)
            .filter(stack -> !stack.isEmpty())
            .toList();
        renderItems(context, stacks, mouseX, mouseY, startX, startY);
    }

    public static void renderItems(
        MDRenderContext context,
        List<ItemStack> displaying,
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
            ItemStack stack = displaying.get(i);
            int x = offsetX + (i % len) * SLOT_SIZE;
            int y = offsetY + (i / len) * SLOT_SIZE;
            renderItem(context, stack, mouseX, mouseY, x, y);
        }
    }

    public static void renderItem(
        MDRenderContext context,
        Object2IntMap.Entry<Ingredient> displaying,
        float mouseX,
        float mouseY,
        int x,
        int y
    ) {
        ItemStack stack = displaying.getKey().getItems()[0];
        stack.setCount(displaying.getIntValue());
        renderItem(context, stack, mouseX, mouseY, x, y);
    }

    public static void renderItem(
        MDRenderContext context,
        ItemIngredientPredicate displaying,
        float mouseX,
        float mouseY,
        int x,
        int y
    ) {
        ItemStack stack = displaying.getItems()[RecipeUtil.getDisplayIndex(displaying.getItems().length)];
        renderItem(context, stack, mouseX, mouseY, x, y);
    }

    public static void renderItem(MDRenderContext context, Ingredient displaying, float mouseX, float mouseY, int x, int y) {
        renderItem(context, RecipeUtil.getDisplayItem(displaying), mouseX, mouseY, x, y);
    }

    public static void renderItem(MDRenderContext context, ItemStack displaying, float mouseX, float mouseY, int x, int y) {
        context.graphics().blit(SLOT, x - 8, y - 8, 0, 0, 32, 32, 32, 32);
        renderItemWithoutSlot(context, displaying, mouseX, mouseY, x, y);
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
        ItemStack stack = displaying.getItems()[RecipeUtil.getDisplayIndex(displaying.getItems().length)];
        renderItemWithoutSlot(context, stack, mouseX, mouseY, x, y);
    }

    public static void renderItemWithoutSlot(
        MDRenderContext context,
        Object2IntMap.Entry<Ingredient> displaying,
        float mouseX,
        float mouseY,
        int x,
        int y
    ) {
        ItemStack stack = displaying.getKey().getItems()[0];
        stack.setCount(displaying.getIntValue());
        renderItemWithoutSlot(context, stack, mouseX, mouseY, x, y);
    }

    public static void renderItemWithoutSlot(MDRenderContext context, ItemStack displaying, float mouseX, float mouseY, int x, int y) {
        GuiGraphics g = context.graphics();
        g.renderItem(displaying, x, y);
        g.renderItemDecorations(Minecraft.getInstance().font, displaying, x, y);
        AgeratumUtil.renderTooltip(context, displaying, x, y, mouseX, mouseY);
    }

    public static void renderTooltip(MDRenderContext context, ItemStack stack, int startX, int startY, float mouseX, float mouseY) {
        if (isHoverItem(startX, startY, mouseX, mouseY)) {
            context.addTooltip(stack);
        }
    }

    public static boolean isHoverItem(int startX, int startY, float mouseX, float mouseY) {
        return isHover(startX, startY, 16, 16, mouseX, mouseY);
    }

    public static boolean isHover(int startX, int startY, int width, int height, float mouseX, float mouseY) {
        return mouseX >= startX && mouseX <= startX + width && mouseY >= startY && mouseY <= startY + height;
    }

    public static ItemStack getStack(ChanceItemStack stack) {
        ItemStack itemStack = stack.stack().copy();
        if (stack.count() instanceof ConstantValue) {
            itemStack.setCount(stack.getMaxCount());
        } else if (stack.count() instanceof BinomialDistributionGenerator count) {
            if (count.p() instanceof ConstantValue(float value) && value == 1) itemStack.setCount(stack.getMaxCount());
        }
        return itemStack;
    }
}
