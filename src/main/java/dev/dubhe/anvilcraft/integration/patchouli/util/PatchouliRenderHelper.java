package dev.dubhe.anvilcraft.integration.patchouli.util;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.mixin.accessor.ScreenAccessor;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.util.RenderHelper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.book.gui.GuiBookEntry;

public class PatchouliRenderHelper {
    public static final ResourceLocation CRAFTING = ResourceLocation.fromNamespaceAndPath(PatchouliAPI.MOD_ID, "textures/gui/crafting.png");
    public static final ResourceLocation EXTRA = AnvilCraft.of("textures/gui/patchouli/crafting.png");

    public static void renderCraftingCustomUV(GuiGraphics guiGraphics, int x, int y, float uOffset, float vOffset, int width, int height) {
        RenderSystem.enableBlend();
        guiGraphics.blit(EXTRA, x, y, uOffset, vOffset, width, height, 256, 256);
    }

    public static void renderArray(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableBlend();
        guiGraphics.blit(EXTRA, x, y, 72, 84, 9, 9, 256, 256);
    }

    public static void render1x1(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableBlend();
        guiGraphics.blit(EXTRA, x, y, 84, 77, 24, 24, 256, 256);
    }

    public static void render1x2(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableBlend();
        guiGraphics.blit(EXTRA, x, y, 8, 6, 43, 24, 256, 256);
    }

    public static void render1x3(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableBlend();
        guiGraphics.blit(EXTRA, x, y, 54, 6, 62, 24, 256, 256);
    }

    public static void render1x4(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableBlend();
        guiGraphics.blit(EXTRA, x, y, 117, 6, 81, 24, 256, 256);
    }

    public static void render1x5(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableBlend();
        guiGraphics.blit(EXTRA, x, y, 8, 32, 100, 24, 256, 256);
    }

    public static void render2x1(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableBlend();
        guiGraphics.blit(CRAFTING, x, y, 11, 135, 24, 43, 128, 256);
    }

    public static void render2x2(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableBlend();
        guiGraphics.blit(EXTRA, x, y, 110, 32, 43, 43, 256, 256);
    }

    public static void render2x3(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableBlend();
        guiGraphics.blit(EXTRA, x, y, 155, 32, 62, 43, 256, 256);
    }

    public static void render2x4(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableBlend();
        guiGraphics.blit(EXTRA, x, y, 155, 77, 81, 43, 256, 256);
    }

    public static void render2x5(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableBlend();
        guiGraphics.blit(EXTRA, x, y, 155, 122, 100, 43, 256, 256);
    }

    public static void render3x2(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.enableBlend();
        guiGraphics.blit(EXTRA, x, y, 110, 77, 43, 62, 256, 256);
    }

    public static void renderIngredientWithCount(
        GuiBookEntry parent, GuiGraphics guiGraphics, Object2IntMap.Entry<Ingredient> ingrAndCount, int x, int y, int mouseX, int mouseY
    ) {
        renderIngredientWithCount(parent, guiGraphics, ingrAndCount.getKey(), ingrAndCount.getIntValue(), x, y, mouseX, mouseY);
    }

    public static void renderIngredientWithCount(
        GuiBookEntry parent, GuiGraphics guiGraphics, Ingredient ingr, int count, int x, int y, int mouseX, int mouseY
    ) {
        RenderSystem.enableBlend();
        ItemStack[] stacks = ingr.getItems();
        if (stacks.length == 0) return;
        ItemStack stack = stacks[(parent.ticksInBook / 20) % stacks.length];

        guiGraphics.renderFakeItem(stack, x, y);
        guiGraphics.renderItemDecorations(((ScreenAccessor) parent).anvilcraft$getFont(), stack.copyWithCount(count), x, y);

        if (parent.isMouseInRelativeRange(mouseX, mouseY, x, y, 16, 16)) {
            parent.setTooltipStack(stack);
        }
    }

    public static void renderIngredient(
        GuiBookEntry parent, GuiGraphics guiGraphics, ItemIngredientPredicate ingr, int x, int y, int mouseX, int mouseY
    ) {
        RenderSystem.enableBlend();
        ItemStack[] stacks = ingr.getItems();
        if (stacks.length == 0) return;
        ItemStack stack = stacks[(parent.ticksInBook / 20) % stacks.length];

        guiGraphics.renderFakeItem(stack, x, y);
        guiGraphics.renderItemDecorations(((ScreenAccessor) parent).anvilcraft$getFont(), stack, x, y);

        if (parent.isMouseInRelativeRange(mouseX, mouseY, x, y, 16, 16)) {
            parent.setTooltipStack(stack);
        }
    }

    public static void renderItemStack(
        GuiBookEntry parent, GuiGraphics guiGraphics, ItemStack stack, int x, int y, int mouseX, int mouseY
    ) {
        RenderSystem.enableBlend();
        guiGraphics.renderFakeItem(stack, x, y);
        guiGraphics.renderItemDecorations(((ScreenAccessor) parent).anvilcraft$getFont(), stack, x, y);

        if (parent.isMouseInRelativeRange(mouseX, mouseY, x, y, 16, 16)) {
            parent.setTooltipStack(stack);
        }
    }

    public static void renderAnvilWithAnimation(GuiBookEntry parent, GuiGraphics guiGraphics, int x, int y) {
        int time = 30 - parent.ticksInBook % 30;
        float anvilYOffset = time < 15 ? (float) Math.sin(time / 15d * 2d * Math.PI + Math.PI / 2) * 6 : 6;
        RenderHelper.renderBlock(
            guiGraphics, Blocks.ANVIL.defaultBlockState(), x,
            y + anvilYOffset, 20,
            12,
            RenderHelper.SINGLE_BLOCK
        );
    }
}
