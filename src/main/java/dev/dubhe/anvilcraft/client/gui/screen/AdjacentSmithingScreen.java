package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.AdjacentSmithingMenu;
import dev.dubhe.anvilcraft.network.multiple.SmithingTemplatePackets;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/** 带有相邻容器模板面板的锻造界面。 */
public abstract class AdjacentSmithingScreen<M extends AdjacentSmithingMenu> extends ItemCombinerScreen<M> {
    private static final Identifier TEMPLATE_PANEL = SharedTextures.textureGui("crafting/smithing_template");
    private static final int PANEL_WIDTH = 73;
    private static final int PANEL_HEIGHT = 130;
    private static final int PANEL_RIGHT_BORDER = 67;
    private static final int PANEL_TOP_OFFSET = 30;
    private static final int COLUMN_COUNT = 3;
    private static final int VISIBLE_ROW_COUNT = 6;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_X = 4;
    private static final int SLOT_Y = 18;
    private static final int SLIDER_X = 60;
    private static final int SLIDER_MIN_Y = 18;
    private static final int SLIDER_MAX_Y = 117;

    @Nullable
    private EditBox searchBox;

    private int scrollRow;
    private boolean draggingSlider;

    protected AdjacentSmithingScreen(
        M menu,
        Inventory playerInventory,
        Component title,
        Identifier background
    ) {
        super(menu, playerInventory, title, background);
    }

    @Override
    protected void init() {
        super.init();
        this.searchBox = new EditBox(
            this.font,
            this.panelX() + 5,
            this.panelY() + 5,
            60,
            10,
            Component.translatable("gui.recipebook.search_hint")
        );
        this.searchBox.setBordered(false);
        this.searchBox.setMaxLength(50);
        this.searchBox.setTextColor(0xFFFFFFFF);
        this.searchBox.setTextColorUneditable(0xFFFFFFFF);
        this.searchBox.setTextShadow(true);
        this.searchBox.setResponder(value -> this.scrollRow = 0);
        this.searchBox.setFocused(false);
        this.searchBox.visible = this.isTemplatePanelVisible();
        this.addRenderableWidget(this.searchBox);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.searchBox == null) return;
        this.searchBox.visible = this.isTemplatePanelVisible();
        if (!this.searchBox.visible) {
            this.searchBox.setFocused(false);
            this.draggingSlider = false;
        }
        this.clampScrollRow();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        if (!this.isTemplatePanelVisible()) return;
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            AdjacentSmithingScreen.TEMPLATE_PANEL,
            this.panelX(),
            this.panelY(),
            0,
            0,
            AdjacentSmithingScreen.PANEL_WIDTH,
            AdjacentSmithingScreen.PANEL_HEIGHT,
            AdjacentSmithingScreen.PANEL_WIDTH,
            AdjacentSmithingScreen.PANEL_HEIGHT
        );
        int maxScrollRow = this.maxScrollRow();
        if (maxScrollRow > 0) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                SharedTextures.SWITCH_TABLE_SLIDER,
                this.panelX() + AdjacentSmithingScreen.SLIDER_X,
                this.sliderY(maxScrollRow),
                0,
                0,
                8,
                12,
                8,
                12
            );
        }
        this.renderTemplateItems(graphics);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack hovered = this.templateAt(mouseX, mouseY);
        if (hovered.isEmpty()) {
            super.extractTooltip(graphics, mouseX, mouseY);
            return;
        }
        graphics.setTooltipForNextFrame(
            this.font,
            this.getTooltipFromContainerItem(hovered),
            hovered.getTooltipImage(),
            hovered,
            mouseX,
            mouseY
        );
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (!this.isTemplatePanelVisible()) return super.mouseClicked(event, handled);
        int maxScrollRow = this.maxScrollRow();
        if (button == 0 && maxScrollRow > 0 && this.isOverSlider(mouseX, mouseY)) {
            this.draggingSlider = true;
            this.updateScrollFromSlider(mouseY, maxScrollRow);
            return true;
        }
        ItemStack template = this.templateAt(mouseX, mouseY);
        if (!template.isEmpty() && (button == 0 || button == 1)) {
            Identifier templateId = BuiltInRegistries.ITEM.getKey(template.getItem());
            this.playTemplateClickSound();
            ClientPacketDistributor.sendToServer(new SmithingTemplatePackets.Action(
                this.menu.containerId,
                templateId,
                button == 1
            ));
            if (this.searchBox != null) this.searchBox.setFocused(false);
            return true;
        }
        if (this.isOverTemplateGrid(mouseX, mouseY)) return true;
        if (button == 0 && this.searchBox != null && this.searchBox.isMouseOver(mouseX, mouseY)) {
            return super.mouseClicked(event, handled);
        }
        if (this.isOverTemplatePanel(mouseX, mouseY)) return true;
        return super.mouseClicked(event, handled);
    }

    private void playTemplateClickSound() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.draggingSlider = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.draggingSlider && event.button() == 0) {
            this.updateScrollFromSlider(event.y(), this.maxScrollRow());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.isTemplatePanelVisible() || !this.isOverTemplateGrid(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int maxScrollRow = this.maxScrollRow();
        if (maxScrollRow > 0 && scrollY != 0) {
            this.scrollRow = Mth.clamp(this.scrollRow - (int) Math.signum(scrollY), 0, maxScrollRow);
        }
        return true;
    }

    private void renderTemplateItems(GuiGraphicsExtractor graphics) {
        List<ItemStack> templates = this.filteredTemplates();
        int start = this.scrollRow * AdjacentSmithingScreen.COLUMN_COUNT;
        int end = Math.min(start + AdjacentSmithingScreen.COLUMN_COUNT * AdjacentSmithingScreen.VISIBLE_ROW_COUNT, templates.size());
        for (int index = start; index < end; index++) {
            int visibleIndex = index - start;
            int x = this.panelX() + AdjacentSmithingScreen.SLOT_X
                    + visibleIndex % AdjacentSmithingScreen.COLUMN_COUNT * AdjacentSmithingScreen.SLOT_SIZE;
            int y = this.panelY() + AdjacentSmithingScreen.SLOT_Y
                    + visibleIndex / AdjacentSmithingScreen.COLUMN_COUNT * AdjacentSmithingScreen.SLOT_SIZE;
            ItemStack template = templates.get(index);
            if (this.isFavorite(template)) {
                graphics.fill(RenderPipelines.GUI, x, y, x + 16, y + 16, 0x66FFFF00);
            }
            graphics.item(template, x, y, index);
            if (this.menu.isBorrowedTemplate(template)) {
                graphics.fill(RenderPipelines.GUI, x, y, x + 16, y + 16, 0x80909090);
            }
        }
    }

    private List<ItemStack> filteredTemplates() {
        String search = this.searchBox == null ? "" : this.searchBox.getValue().strip().toLowerCase(Locale.ROOT);
        if (search.isEmpty()) return this.menu.getAdjacentTemplates();
        return this.menu.getAdjacentTemplates().stream().filter(stack -> {
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
            return name.contains(search) || id.contains(search);
        }).toList();
    }

    private ItemStack templateAt(double mouseX, double mouseY) {
        int relativeX = (int) mouseX - this.panelX() - AdjacentSmithingScreen.SLOT_X;
        int relativeY = (int) mouseY - this.panelY() - AdjacentSmithingScreen.SLOT_Y;
        if (relativeX < 0 || relativeY < 0) return ItemStack.EMPTY;
        int column = relativeX / AdjacentSmithingScreen.SLOT_SIZE;
        int row = relativeY / AdjacentSmithingScreen.SLOT_SIZE;
        if (column >= AdjacentSmithingScreen.COLUMN_COUNT || row >= AdjacentSmithingScreen.VISIBLE_ROW_COUNT) return ItemStack.EMPTY;
        if (relativeX % AdjacentSmithingScreen.SLOT_SIZE >= 16 || relativeY % AdjacentSmithingScreen.SLOT_SIZE >= 16) {
            return ItemStack.EMPTY;
        }
        int index = (this.scrollRow + row) * AdjacentSmithingScreen.COLUMN_COUNT + column;
        List<ItemStack> templates = this.filteredTemplates();
        return index < templates.size() ? templates.get(index) : ItemStack.EMPTY;
    }

    private void clampScrollRow() {
        this.scrollRow = Mth.clamp(this.scrollRow, 0, this.maxScrollRow());
    }

    private int maxScrollRow() {
        int rowCount = (this.filteredTemplates().size() + AdjacentSmithingScreen.COLUMN_COUNT - 1) / AdjacentSmithingScreen.COLUMN_COUNT;
        return Math.max(0, rowCount - AdjacentSmithingScreen.VISIBLE_ROW_COUNT);
    }

    private int sliderY(int maxScrollRow) {
        if (maxScrollRow <= 0) return this.panelY() + AdjacentSmithingScreen.SLIDER_MIN_Y;
        int travel = AdjacentSmithingScreen.SLIDER_MAX_Y - AdjacentSmithingScreen.SLIDER_MIN_Y;
        return this.panelY() + AdjacentSmithingScreen.SLIDER_MIN_Y + Math.round((float) this.scrollRow / maxScrollRow * travel);
    }

    private void updateScrollFromSlider(double mouseY, int maxScrollRow) {
        if (maxScrollRow <= 0) {
            this.scrollRow = 0;
            return;
        }
        double sliderCenter = mouseY - this.panelY() - AdjacentSmithingScreen.SLIDER_MIN_Y - 6;
        double progress = Mth.clamp(sliderCenter / (AdjacentSmithingScreen.SLIDER_MAX_Y - AdjacentSmithingScreen.SLIDER_MIN_Y), 0.0, 1.0);
        this.scrollRow = Mth.clamp((int) Math.round(progress * maxScrollRow), 0, maxScrollRow);
    }

    private boolean isFavorite(ItemStack template) {
        Identifier id = BuiltInRegistries.ITEM.getKey(template.getItem());
        return this.menu.getFavoriteTemplates().contains(id);
    }

    private boolean isTemplatePanelVisible() {
        return !this.menu.getAdjacentTemplates().isEmpty();
    }

    private boolean isOverTemplateGrid(double mouseX, double mouseY) {
        return mouseX >= this.panelX() + 3
               && mouseX < this.panelX() + 66
               && mouseY >= this.panelY() + 17
               && mouseY < this.panelY() + 129;
    }

    private boolean isOverTemplatePanel(double mouseX, double mouseY) {
        return mouseX >= this.panelX()
               && mouseX < this.panelX() + AdjacentSmithingScreen.PANEL_WIDTH
               && mouseY >= this.panelY()
               && mouseY < this.panelY() + AdjacentSmithingScreen.PANEL_HEIGHT;
    }

    private boolean isOverSlider(double mouseX, double mouseY) {
        return mouseX >= this.panelX() + AdjacentSmithingScreen.SLIDER_X
               && mouseX < this.panelX() + AdjacentSmithingScreen.SLIDER_X + 8
               && mouseY >= this.panelY() + AdjacentSmithingScreen.SLIDER_MIN_Y
               && mouseY < this.panelY() + AdjacentSmithingScreen.SLIDER_MAX_Y + 12;
    }

    private int panelX() {
        return this.leftPos - AdjacentSmithingScreen.PANEL_RIGHT_BORDER;
    }

    private int panelY() {
        return this.topPos + AdjacentSmithingScreen.PANEL_TOP_OFFSET;
    }
}
