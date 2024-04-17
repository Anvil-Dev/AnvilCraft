package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.RoyalAnvilMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RoyalAnvilScreen extends ItemCombinerScreen<RoyalAnvilMenu> {
    private static final ResourceLocation ANVIL_LOCATION =
        AnvilCraft.of("textures/gui/container/smithing/background/royal_anvil.png");
    private static final ResourceLocation TEXT_LOCATION =
        AnvilCraft.of("textures/gui/container/smithing/text_field.png");
    private static final ResourceLocation TEXT_DISABLE_LOCATION =
        AnvilCraft.of("textures/gui/container/smithing/text_field_disabled.png");
    private EditBox name;
    private final Player player;

    /**
     * 皇家铁砧 GUI
     *
     * @param menu            菜单
     * @param playerInventory 背包
     * @param title           标题
     */
    public RoyalAnvilScreen(RoyalAnvilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, ANVIL_LOCATION);
        this.player = playerInventory.player;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.name.tick();
    }

    @Override
    protected void subInit() {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
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
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        String string = this.name.getValue();
        this.init(minecraft, width, height);
        this.name.setValue(string);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
        if (this.name.keyPressed(keyCode, scanCode, modifiers) || this.name.canConsumeInput()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void onNameChanged(String name) {
        Slot slot = this.menu.getSlot(0);
        if (!slot.hasItem()) {
            return;
        }
        String string = name;
        if (!slot.getItem().hasCustomHoverName() && string.equals(slot.getItem().getHoverName().getString())) {
            string = "";
        }
        if (this.menu.setItemName(string) && this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.connection.send(new ServerboundRenameItemPacket(string));
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        int i = this.menu.getCost();
        if (i > 0) {
            Component component;
            int j = 8453920;
            if (!this.menu.getSlot(2).hasItem()) {
                component = null;
            } else {
                component = Component.translatable("container.repair.cost", i);
                if (!this.menu.getSlot(2).mayPickup(this.player)) {
                    j = 0xFF6060;
                }
            }
            if (component != null) {
                int k = this.imageWidth - 8 - this.font.width(component) - 2;
                guiGraphics.fill(k - 2, 67, this.imageWidth - 8, 79, 0x4F000000);
                guiGraphics.drawString(this.font, component, k, 69, j);
            }
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        ResourceLocation location = this.menu.getSlot(0).getItem().isEmpty() ? TEXT_DISABLE_LOCATION : TEXT_LOCATION;
        guiGraphics.blit(location, this.leftPos + 59, this.topPos + 20, 0, 0, 110, 16, 110, 16);
    }

    @Override
    public void renderFg(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.name.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderErrorIcon(@NotNull GuiGraphics guiGraphics, int x, int y) {
    }

    @Override
    public void slotChanged(
        @NotNull AbstractContainerMenu containerToSend, int dataSlotIndex, @NotNull ItemStack stack
    ) {
        if (dataSlotIndex == 0) {
            this.name.setValue(stack.isEmpty() ? "" : stack.getHoverName().getString());
            this.name.setEditable(!stack.isEmpty());
            this.setFocused(this.name);
        }
    }
}
