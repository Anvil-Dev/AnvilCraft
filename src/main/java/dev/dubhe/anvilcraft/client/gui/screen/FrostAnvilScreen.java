package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.FrostAnvilMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class FrostAnvilScreen extends ItemCombinerScreen<FrostAnvilMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("crafting", "frost_anvil");
    private @Nullable EditBox name;
    private final Player player;

    /// 皇家铁砧 GUI
    ///
    /// @param menu            菜单
    /// @param playerInventory 背包
    /// @param title           标题
    public FrostAnvilScreen(FrostAnvilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, FrostAnvilScreen.BACKGROUND);
        this.player = playerInventory.player;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.getImageWidth() - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
    }

    @Override
    protected void subInit() {
        int i = (this.width - this.getImageWidth()) / 2;
        int j = (this.height - this.getImageHeight()) / 2;
        this.name = new EditBox(this.font, i + 62, j + 24, 103, 12, Component.translatable("container.repair"));
        this.name.setCanLoseFocus(false);
        this.name.setTextColor(-1);
        this.name.setTextColorUneditable(-1);
        this.name.setBordered(false);
        this.name.setMaxLength(50);
        this.name.setResponder(this::onNameChanged);
        this.name.setValue("");
        this.addWidget(this.name);
        this.setInitialFocus(this.name);
        this.name.setEditable(false);
    }

    @Override
    public void resize(int width, int height) {
        String string = this.name.getValue();
        this.init(width, height);
        this.name.setValue(string);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256 && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
        if (this.name.keyPressed(event) || this.name.canConsumeInput()) {
            return true;
        }
        return super.keyPressed(event);
    }

    private void onNameChanged(String name) {
        Slot slot = this.menu.getSlot(0);
        if (!slot.hasItem()) {
            return;
        }
        String string = name;
        if (!slot.getItem().has(DataComponents.CUSTOM_NAME)
            && string.equals(slot.getItem().getHoverName().getString())) {
            string = "";
        }
        if (this.menu.setItemName(string) && this.minecraft.player != null) {
            this.minecraft.player.connection.send(new ServerboundRenameItemPacket(string));
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(
            this.font,
            this.title,
            this.titleLabelX,
            this.titleLabelY,
            0xFF404040,
            false
        );
        int i = this.menu.getCost();
        if (this.menu.result.noCostInRenaming && this.menu.result.onlyRenaming || i > 0) {
            Component component;
            int j = 0xFF80FF20;
            if (!this.menu.getSlot(2).hasItem()) {
                component = null;
            } else {
                component = Component.translatable("container.repair.cost", i);
                if (!this.menu.getSlot(2).mayPickup(this.player)) {
                    j = 0xFFFF6060;
                }
            }
            if (component != null) {
                int k = this.getImageWidth() - 8 - this.font.width(component) - 2;
                graphics.fill(k - 2, 67, this.getImageWidth() - 8, 79, 0x4F000000);
                graphics.text(this.font, component, k, 69, j);
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        Identifier texture = this.menu.getSlot(0).getItem().isEmpty()
                             ? SharedTextures.TEXT_FIELD_DISABLE
                             : SharedTextures.TEXT_FIELD;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, this.leftPos + 59, this.topPos + 20, 0, 0, 110, 16, 256, 256);
        this.name.extractWidgetRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    protected void extractErrorIcon(GuiGraphicsExtractor graphics, int x, int y) {
        if (
            (this.menu.getSlot(0).hasItem() || this.menu.getSlot(1).hasItem())
            && !this.menu.getSlot(this.menu.getResultSlot()).hasItem()
        ) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, SharedTextures.ERROR_SPRITE, x + 103, y + 47, 0, 0, 16, 16, 16, 16);
        }
    }

    @Override
    public void slotChanged(AbstractContainerMenu containerToSend, int dataSlotIndex, ItemStack stack) {
        if (dataSlotIndex == 0) {
            this.name.setValue(stack.isEmpty() ? "" : stack.getHoverName().getString());
            this.name.setEditable(!stack.isEmpty());
            this.setFocused(this.name);
        }
    }
}
