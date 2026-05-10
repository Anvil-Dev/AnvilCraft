package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.JewelCraftingMenu;
import dev.dubhe.anvilcraft.inventory.component.jewel.JewelInputSlot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class JewelCraftingScreen extends AbstractContainerScreen<JewelCraftingMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("crafting", "jewelcrafting_table");

    public JewelCraftingScreen(JewelCraftingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.getImageWidth() - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
    }

    @Override
    protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.getImageWidth()) / 2;
        int j = (this.height - this.getImageHeight()) / 2;
        graphics.blit(BACKGROUND, i, j, 0, 0, this.getImageWidth(), this.getImageHeight());
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderHintItemSlot(graphics);
    }

    private void renderHintItemSlot(GuiGraphicsExtractor graphics) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(leftPos, topPos, 0);
        for (int i = JewelCraftingMenu.CRAFT_SLOT_START; i <= JewelCraftingMenu.CRAFT_SLOT_END; i++) {
            Slot slot = menu.getSlot(i);
            if (!slot.hasItem() && slot instanceof JewelInputSlot inputSlot) {
                int count = inputSlot.getHintCount();
                ItemStack @Nullable [] ingredientItems = inputSlot.getIngredientItems();
                if (ingredientItems != null) {
                    int index = (int) ((System.currentTimeMillis() / 1000) % ingredientItems.length);
                    ItemStack stack = ingredientItems[index];
                    RenderSupport.renderItemWithTransparency(stack, poseStack, slot.x, slot.y, 0.52F);
                    graphics.renderItemDecorations(font, stack.copyWithCount(count), slot.x, slot.y);
                }
            }
        }
        poseStack.popPose();
    }

    @Override
    protected void renderSlotHighlight(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, float partialTick) {
        super.renderSlotHighlight(graphics, slot, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderTooltip(GuiGraphicsExtractor graphics, int x, int y) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null) {
            ItemStack itemstack = null;
            if (this.hoveredSlot.hasItem()) {
                itemstack = this.hoveredSlot.getItem();
            } else if (this.hoveredSlot instanceof JewelInputSlot inputSlot) {
                ItemStack @Nullable [] ingredientItems = inputSlot.getIngredientItems();
                if (ingredientItems != null) {
                    int index = (int) ((System.currentTimeMillis() / 1000) % ingredientItems.length);
                    itemstack = ingredientItems[index];
                }
            }
            if (itemstack != null) {
                graphics.renderTooltip(
                    this.font,
                    this.getTooltipFromContainerItem(itemstack),
                    itemstack.getTooltipImage(),
                    itemstack,
                    x,
                    y
                );
            }
        }
    }

    @Override
    protected void renderSlotContents(GuiGraphicsExtractor graphics, ItemStack itemstack, Slot slot, @Nullable String countString) {
        if (slot instanceof JewelInputSlot inputSlot) {
            if (itemstack.getCount() < inputSlot.getHintCount()) {
                int seed = slot.x + slot.y * imageWidth;
                if (slot.isFake()) {
                    graphics.renderFakeItem(itemstack, slot.x, slot.y, seed);
                } else {
                    graphics.renderItem(itemstack, slot.x, slot.y, seed);
                }
                if (!itemstack.isEmpty()) {
                    graphics.pose().pushPose();
                    String s = String.valueOf(itemstack.getCount());
                    graphics.pose().translate(0.0F, 0.0F, 200.0F);
                    graphics.drawString(font, s, slot.x + 19 - 2 - font.width(s), slot.y + 6 + 3, 0xFFFF5555, true);
                    graphics.pose().popPose();
                }
                return;
            }
        }
        super.renderSlotContents(graphics, itemstack, slot, countString);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (key == GLFW.GLFW_KEY_SPACE) {
            // 处理空格键快速填充配方逻辑
            this.menu.autoFill();
            return true;
        }
        return super.keyPressed(event);
    }
}
