package dev.dubhe.anvilcraft.client.gui.screen;

import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.JewelCraftingMenu;
import dev.dubhe.anvilcraft.inventory.component.jewel.JewelInputSlot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class JewelCraftingScreen extends AbstractContainerScreen<JewelCraftingMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("crafting", "jewelcrafting_table");

    public JewelCraftingScreen(JewelCraftingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.getImageWidth() - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            JewelCraftingScreen.BACKGROUND,
            this.leftPos,
            this.topPos,
            0,
            0,
            this.getImageWidth(),
            this.getImageHeight(),
            256,
            256
        );
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        this.extractHintItemSlot(graphics);
    }

    private void extractHintItemSlot(GuiGraphicsExtractor graphics) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(this.leftPos, this.topPos);
        for (int i = JewelCraftingMenu.CRAFT_SLOT_START; i <= JewelCraftingMenu.CRAFT_SLOT_END; i++) {
            Slot slot = this.menu.getSlot(i);
            if (!slot.hasItem() && slot instanceof JewelInputSlot inputSlot) {
                int count = inputSlot.getHintCount();
                List<ItemStack> ingredientItems = inputSlot.getIngredientItems();
                if (ingredientItems != null) {
                    int index = (int) ((System.currentTimeMillis() / 1000) % ingredientItems.size());
                    ItemStack stack = ingredientItems.get(index);
                    GuiRenderExtras.itemWithTransparency(graphics, stack, slot.x, slot.y, 0.52F);
                    graphics.itemDecorations(this.font, stack.copyWithCount(count), slot.x, slot.y);
                }
            }
        }
        pose.popMatrix();
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null) {
            ItemStack itemstack = null;
            if (this.hoveredSlot.hasItem()) {
                itemstack = this.hoveredSlot.getItem();
            } else if (this.hoveredSlot instanceof JewelInputSlot inputSlot) {
                List<ItemStack> ingredientItems = inputSlot.getIngredientItems();
                if (ingredientItems != null) {
                    int index = (int) ((System.currentTimeMillis() / 1000) % ingredientItems.size());
                    itemstack = ingredientItems.get(index);
                }
            }
            if (itemstack != null) {
                graphics.setTooltipForNextFrame(
                    this.font,
                    this.getTooltipFromContainerItem(itemstack),
                    itemstack.getTooltipImage(),
                    itemstack,
                    mouseX,
                    mouseY
                );
            }
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderSlotContents(GuiGraphicsExtractor graphics, ItemStack itemstack, Slot slot, @Nullable String countString) {
        if (slot instanceof JewelInputSlot inputSlot) {
            if (itemstack.getCount() < inputSlot.getHintCount()) {
                int seed = slot.x + slot.y * this.imageWidth;
                if (slot.isFake()) {
                    graphics.fakeItem(itemstack, slot.x, slot.y, seed);
                } else {
                    graphics.item(itemstack, slot.x, slot.y, seed);
                }
                if (!itemstack.isEmpty()) {
                    graphics.pose().pushMatrix();
                    String s = String.valueOf(itemstack.getCount());
                    graphics.pose().translate(0.0F, 0.0F);
                    graphics.text(this.font, s, slot.x + 19 - 2 - this.font.width(s), slot.y + 6 + 3, 0xFFFF5555, true);
                    graphics.pose().popMatrix();
                }
                return;
            }
        }
        super.renderSlotContents(graphics, itemstack, slot, countString);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_SPACE) {
            // 处理空格键快速填充配方逻辑
            this.menu.autoFill();
            return true;
        }
        return super.keyPressed(event);
    }
}
