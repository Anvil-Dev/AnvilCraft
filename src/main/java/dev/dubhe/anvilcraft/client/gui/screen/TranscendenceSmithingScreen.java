package dev.dubhe.anvilcraft.client.gui.screen;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.item.IMultipleMaterial;
import dev.dubhe.anvilcraft.api.item.IPermutationMaterial;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu;
import dev.dubhe.anvilcraft.item.template.frost.DeformationTemplateItem;
import dev.dubhe.anvilcraft.item.template.frost.PermutationTemplateItem;
import dev.dubhe.anvilcraft.item.template.mto.BaseMultipleToOneTemplateItem;
import dev.dubhe.anvilcraft.network.multiple.TranscendenceSmithingPackets;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** 超限锻造台界面。 */
public class TranscendenceSmithingScreen extends AbstractContainerScreen<TranscendenceSmithingMenu>
    implements ContainerListener {
    private static final Identifier BACKGROUND =
        SharedTextures.bg("crafting", "transcendence_smithing_table");
    private static final Identifier ROYAL_OVERLAY =
        SharedTextures.textureGui("crafting/transcendence_smithing_table/royal");
    private static final Identifier EMBER_OVERLAY =
        SharedTextures.textureGui("crafting/transcendence_smithing_table/ember");
    private static final Identifier FROST_OVERLAY =
        SharedTextures.textureGui("crafting/transcendence_smithing_table/frost");
    private static final Identifier TEMPLATE_PANEL =
        SharedTextures.textureGui("crafting/smithing_template");
    private static final Identifier LEFT =
        SharedTextures.textureGui("crafting/frost_smithing_table/button_left");
    private static final Identifier RIGHT =
        SharedTextures.textureGui("crafting/frost_smithing_table/button_right");

    private static final Identifier EMPTY_SLOT_INGOT =
        Identifier.withDefaultNamespace("container/slot/ingot");

    private static final int OVERLAY_HEIGHT = 83;
    private static final int PANEL_WIDTH = 73;
    private static final int PANEL_HEIGHT = 130;
    private static final int PANEL_RIGHT_BORDER = 67;
    private static final int PANEL_TOP_OFFSET = 30;
    private static final int COLUMN_COUNT = 3;
    private static final int VISIBLE_ROW_COUNT = 6;
    private static final int TEMPLATE_SLOT_SIZE = 18;
    private static final int TEMPLATE_GRID_X = 4;
    private static final int TEMPLATE_GRID_Y = 18;
    private static final int SLIDER_X = 60;
    private static final int SLIDER_MIN_Y = 18;
    private static final int SLIDER_MAX_Y = 117;
    private static final int VIRTUAL_TEMPLATE_X = 8;
    private static final int VIRTUAL_TEMPLATE_Y = 48;

    private static final Component MISSING_TEMPLATE_TOOLTIP =
        Component.translatable("container.upgrade.missing_template_tooltip");
    private static final Component ERROR_TOOLTIP = Component.translatable("container.upgrade.error_tooltip");
    private static final List<Identifier> EMPTY_SLOT_DEFORMATION_MATERIAL = List.of(TranscendenceSmithingScreen.EMPTY_SLOT_INGOT);
    private static final Quaternionf ARMOR_STAND_ANGLE =
        new Quaternionf().rotationXYZ(0.43633232f, 0.0f, (float) Math.PI);
    private static final Vector3f ARMOR_STAND_TRANSLATION = new Vector3f(0.0F, 1.0F, 0.0F);

    private final CyclingSlotBackground firstInputIcon =
        new CyclingSlotBackground(TranscendenceSmithingMenu.ROYAL_FROST_FIRST_INPUT_SLOT);
    private final CyclingSlotBackground secondInputIcon =
        new CyclingSlotBackground(TranscendenceSmithingMenu.ROYAL_FROST_SECOND_INPUT_SLOT);
    private final CyclingSlotBackground emberMaterialIcon =
        new CyclingSlotBackground(TranscendenceSmithingMenu.EMBER_MATERIAL_SLOT);
    private final List<CyclingSlotBackground> emberInputIcons = List.of(
        new CyclingSlotBackground(3),
        new CyclingSlotBackground(4),
        new CyclingSlotBackground(5),
        new CyclingSlotBackground(6),
        new CyclingSlotBackground(7),
        new CyclingSlotBackground(8),
        new CyclingSlotBackground(9),
        new CyclingSlotBackground(10)
    );

    @Nullable
    private EditBox searchBox;

    private final ArmorStandRenderState armorStandPreview = new ArmorStandRenderState();

    @Nullable
    private TexturedButton leftButton;

    @Nullable
    private TexturedButton rightButton;

    private ItemStack previewStack = ItemStack.EMPTY;
    private int scrollRow;
    private boolean draggingSlider;

    public TranscendenceSmithingScreen(
        TranscendenceSmithingMenu menu,
        Inventory playerInventory,
        Component title
    ) {
        super(menu, playerInventory, title);
        this.armorStandPreview.entityType = EntityType.ARMOR_STAND;
        this.armorStandPreview.showBasePlate = false;
        this.armorStandPreview.showArms = true;
        this.armorStandPreview.xRot = 25.0F;
        this.armorStandPreview.bodyRot = 210.0F;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.getImageWidth() - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
        this.createSearchBox();
        this.createFrostButtons();
        this.menu.addSlotListener(this);
        this.updateArmorStandPreview();
    }

    private void createSearchBox() {
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
        this.addRenderableWidget(this.searchBox);
    }

    private void createFrostButtons() {
        this.leftButton = this.addRenderableWidget(new TexturedButton(
            this.leftPos + 102,
            this.topPos + 32,
            7,
            11,
            TranscendenceSmithingScreen.LEFT,
            11,
            7,
            22,
            button -> this.turnFrostResult(true)
        ));
        this.rightButton = this.addRenderableWidget(new TexturedButton(
            this.leftPos + 119,
            this.topPos + 32,
            7,
            11,
            TranscendenceSmithingScreen.RIGHT,
            11,
            7,
            22,
            button -> this.turnFrostResult(false)
        ));
        this.updateFrostButtons();
    }

    private void turnFrostResult(boolean left) {
        this.menu.turnFrostResult(left);
        ClientPacketDistributor.sendToServer(new TranscendenceSmithingPackets.TurnResult(
            this.menu.containerId,
            left
        ));
        this.updateArmorStandPreview();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.clampScrollRow();
        this.tickSlotIcons();
        this.updateFrostButtons();
        this.updateArmorStandPreview();
    }

    private void tickSlotIcons() {
        switch (this.menu.getMode()) {
            case ROYAL -> this.tickRoyalIcons();
            case EMBER -> this.tickEmberIcons();
            case FROST -> this.tickFrostIcons();
            default -> throw new IllegalStateException("Unknown smithing mode: " + this.menu.getMode());
        }
    }

    private void tickRoyalIcons() {
        Optional<SmithingTemplateItem> template = this.getRoyalTemplate();
        this.firstInputIcon.tick(template.map(SmithingTemplateItem::getBaseSlotEmptyIcons).orElse(List.of()));
        this.secondInputIcon.tick(
            template.map(SmithingTemplateItem::getAdditionalSlotEmptyIcons).orElse(List.of())
        );
        this.clearEmberIcons();
    }

    private void tickEmberIcons() {
        this.firstInputIcon.tick(List.of());
        this.secondInputIcon.tick(List.of());
        Optional<BaseMultipleToOneTemplateItem> template = this.getEmberTemplate();
        this.emberMaterialIcon.tick(template.map(BaseMultipleToOneTemplateItem::getEmptySlotTextures).orElse(List.of()));
        ItemStack material = this.menu.getEmberMaterial();
        if (template.isPresent() && material.getItem() instanceof IMultipleMaterial multipleMaterial) {
            for (CyclingSlotBackground icon : this.emberInputIcons) {
                icon.tick(multipleMaterial.getEmptySlotTextures(
                    this.menu.getSelectedTemplate(),
                    icon.slotIndex - TranscendenceSmithingMenu.EMBER_INPUT_SLOT_START,
                    this.menu.getEmberInputStacks()
                ));
            }
            return;
        }
        this.emberInputIcons.forEach(icon -> icon.tick(List.of()));
    }

    private void tickFrostIcons() {
        this.clearEmberIcons();
        Item item = this.menu.getSelectedTemplate().getItem();
        if (item instanceof PermutationTemplateItem permutation) {
            this.firstInputIcon.tick(permutation.getEmptySlotTextures());
            ItemStack material = this.menu.getRoyalFrostFirstInput();
            if (material.getItem() instanceof IPermutationMaterial permutationMaterial) {
                this.secondInputIcon.tick(permutationMaterial.getEmptySlotTextures());
            } else {
                this.secondInputIcon.tick(List.of());
            }
            return;
        }
        if (item instanceof DeformationTemplateItem deformation) {
            this.firstInputIcon.tick(TranscendenceSmithingScreen.EMPTY_SLOT_DEFORMATION_MATERIAL);
            this.secondInputIcon.tick(deformation.getEmptySlotTextures());
            return;
        }
        this.firstInputIcon.tick(List.of());
        this.secondInputIcon.tick(List.of());
    }

    private void clearEmberIcons() {
        this.emberMaterialIcon.tick(List.of());
        this.emberInputIcons.forEach(icon -> icon.tick(List.of()));
    }

    private Optional<SmithingTemplateItem> getRoyalTemplate() {
        Item item = this.menu.getSelectedTemplate().getItem();
        return Util.castSafely(item, SmithingTemplateItem.class);
    }

    private Optional<BaseMultipleToOneTemplateItem> getEmberTemplate() {
        Item item = this.menu.getSelectedTemplate().getItem();
        return Util.castSafely(item, BaseMultipleToOneTemplateItem.class);
    }

    private void updateFrostButtons() {
        if (this.leftButton == null || this.rightButton == null) return;
        boolean visible = this.menu.getMode() == TranscendenceSmithingMenu.Mode.FROST
            && this.menu.getSelectedFrostResult() >= 0
            && this.menu.getFrostResults().size() > 1;
        this.leftButton.visible = visible;
        this.leftButton.active = visible;
        this.rightButton.visible = visible;
        this.rightButton.active = visible;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TranscendenceSmithingScreen.BACKGROUND,
            this.leftPos,
            this.topPos,
            0,
            0,
            this.getImageWidth(),
            this.getImageHeight(),
            256,
            256
        );
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            this.modeOverlay(),
            this.leftPos,
            this.topPos,
            0,
            0,
            this.getImageWidth(),
            TranscendenceSmithingScreen.OVERLAY_HEIGHT,
            256,
            128
        );
        this.extractTemplatePanel(graphics);
        this.extractVirtualTemplate(graphics);
        this.extractSlotIcons(graphics, partialTick);
        this.extractDisabledEmberSlots(graphics);
        this.extractErrorIcon(graphics);
        this.extractArmorStand(graphics);
    }

    private Identifier modeOverlay() {
        return switch (this.menu.getMode()) {
            case ROYAL -> TranscendenceSmithingScreen.ROYAL_OVERLAY;
            case EMBER -> TranscendenceSmithingScreen.EMBER_OVERLAY;
            case FROST -> TranscendenceSmithingScreen.FROST_OVERLAY;
        };
    }

    private void extractTemplatePanel(GuiGraphicsExtractor graphics) {
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TranscendenceSmithingScreen.TEMPLATE_PANEL,
            this.panelX(),
            this.panelY(),
            0,
            0,
            TranscendenceSmithingScreen.PANEL_WIDTH,
            TranscendenceSmithingScreen.PANEL_HEIGHT,
            TranscendenceSmithingScreen.PANEL_WIDTH,
            TranscendenceSmithingScreen.PANEL_HEIGHT
        );
        int maxScrollRow = this.maxScrollRow();
        if (maxScrollRow > 0) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                SharedTextures.SWITCH_TABLE_SLIDER,
                this.panelX() + TranscendenceSmithingScreen.SLIDER_X,
                this.sliderY(maxScrollRow),
                0,
                0,
                8,
                12,
                8,
                12
            );
        }
        this.extractTemplateItems(graphics);
    }

    private void extractTemplateItems(GuiGraphicsExtractor graphics) {
        List<ItemStack> templates = this.filteredTemplates();
        int start = this.scrollRow * TranscendenceSmithingScreen.COLUMN_COUNT;
        int end = Math.min(
            start + TranscendenceSmithingScreen.COLUMN_COUNT * TranscendenceSmithingScreen.VISIBLE_ROW_COUNT, templates.size());
        for (int index = start; index < end; index++) {
            int visibleIndex = index - start;
            int x = this.panelX() + TranscendenceSmithingScreen.TEMPLATE_GRID_X
                    + visibleIndex % TranscendenceSmithingScreen.COLUMN_COUNT * TranscendenceSmithingScreen.TEMPLATE_SLOT_SIZE;
            int y = this.panelY() + TranscendenceSmithingScreen.TEMPLATE_GRID_Y
                    + visibleIndex / TranscendenceSmithingScreen.COLUMN_COUNT * TranscendenceSmithingScreen.TEMPLATE_SLOT_SIZE;
            ItemStack template = templates.get(index);
            if (this.isFavorite(template)) {
                graphics.fill(RenderPipelines.GUI, x, y, x + 16, y + 16, 0x66FFFF00);
            }
            graphics.item(template, x, y, index);
            if (this.menu.isSelectedTemplate(template)) {
                graphics.fill(RenderPipelines.GUI, x, y, x + 16, y + 16, 0x80909090);
            }
        }
    }

    private void extractVirtualTemplate(GuiGraphicsExtractor graphics) {
        ItemStack template = this.menu.getSelectedTemplate();
        if (template.isEmpty()) return;
        graphics.item(
            template, this.leftPos + TranscendenceSmithingScreen.VIRTUAL_TEMPLATE_X,
            this.topPos + TranscendenceSmithingScreen.VIRTUAL_TEMPLATE_Y
        );
    }

    private void extractSlotIcons(GuiGraphicsExtractor graphics, float partialTick) {
        if (!this.menu.hasSelectedTemplate()) return;
        if (this.menu.getMode() == TranscendenceSmithingMenu.Mode.EMBER) {
            this.emberMaterialIcon.extractRenderState(this.menu, graphics, partialTick, this.leftPos, this.topPos);
            this.emberInputIcons.forEach(icon ->
                icon.extractRenderState(this.menu, graphics, partialTick, this.leftPos, this.topPos));
            return;
        }
        this.firstInputIcon.extractRenderState(this.menu, graphics, partialTick, this.leftPos, this.topPos);
        this.secondInputIcon.extractRenderState(this.menu, graphics, partialTick, this.leftPos, this.topPos);
    }

    private void extractDisabledEmberSlots(GuiGraphicsExtractor graphics) {
        if (this.menu.getMode() != TranscendenceSmithingMenu.Mode.EMBER) return;
        int inputSize = this.menu.getEmberInputSize();
        for (int index = TranscendenceSmithingMenu.EMBER_INPUT_SLOT_START;
             index < TranscendenceSmithingMenu.EMBER_INPUT_SLOT_END;
             index++) {
            if (index - TranscendenceSmithingMenu.EMBER_INPUT_SLOT_START < inputSize) continue;
            Slot slot = this.menu.getSlot(index);
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                SharedTextures.DISABLED_SLOT,
                this.leftPos + slot.x,
                this.topPos + slot.y,
                0,
                0,
                16,
                16,
                16,
                16
            );
        }
    }

    private void extractErrorIcon(GuiGraphicsExtractor graphics) {
        if (this.menu.getMode() == TranscendenceSmithingMenu.Mode.EMBER) {
            if (this.hasCompleteEmberInput() && this.menu.getActiveResult().isEmpty()) {
                graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    SharedTextures.ERROR_SPRITE,
                    this.leftPos + 123,
                    this.topPos + 48,
                    0,
                    0,
                    16,
                    16,
                    16,
                    16
                );
            }
            return;
        }
        if (this.hasCompleteRoyalFrostInput() && this.menu.getActiveResult().isEmpty()) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                SharedTextures.ERROR_SPRITE,
                this.leftPos + 83,
                this.topPos + 48,
                0,
                0,
                16,
                16,
                16,
                16
            );
        }
    }

    private void extractArmorStand(GuiGraphicsExtractor graphics) {
        if (this.menu.getMode() == TranscendenceSmithingMenu.Mode.EMBER) return;
        graphics.entity(
            this.armorStandPreview,
            25,
            TranscendenceSmithingScreen.ARMOR_STAND_TRANSLATION,
            TranscendenceSmithingScreen.ARMOR_STAND_ANGLE,
            null,
            this.leftPos + 131,
            this.topPos + 20,
            this.leftPos + 171,
            this.topPos + 60
        );
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        this.extractTemplateTooltip(graphics, mouseX, mouseY);
        this.extractVirtualTemplateTooltip(graphics, mouseX, mouseY);
        this.extractOnboardingTooltip(graphics, mouseX, mouseY);
    }

    private void extractTemplateTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack hovered = this.templateAt(mouseX, mouseY);
        if (hovered.isEmpty()) return;
        graphics.setTooltipForNextFrame(
            this.font,
            this.getTooltipFromContainerItem(hovered),
            hovered.getTooltipImage(),
            hovered,
            mouseX,
            mouseY
        );
    }

    private void extractVirtualTemplateTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!this.isHovering(
            TranscendenceSmithingScreen.VIRTUAL_TEMPLATE_X, TranscendenceSmithingScreen.VIRTUAL_TEMPLATE_Y, 16, 16, mouseX, mouseY)) {
            return;
        }
        ItemStack template = this.menu.getSelectedTemplate();
        if (template.isEmpty()) {
            graphics.setTooltipForNextFrame(
                this.font, this.font.split(TranscendenceSmithingScreen.MISSING_TEMPLATE_TOOLTIP, 115), mouseX, mouseY);
            return;
        }
        graphics.setTooltipForNextFrame(
            this.font,
            this.getTooltipFromContainerItem(template),
            template.getTooltipImage(),
            template,
            mouseX,
            mouseY
        );
    }

    private void extractOnboardingTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.extractErrorTooltip(graphics, mouseX, mouseY)) return;
        Slot hoveredSlot = this.hoveredSlot;
        if (hoveredSlot == null || !hoveredSlot.getItem().isEmpty()) return;
        switch (this.menu.getMode()) {
            case ROYAL -> this.extractRoyalSlotTooltip(graphics, mouseX, mouseY, hoveredSlot);
            case EMBER -> this.extractEmberSlotTooltip(graphics, mouseX, mouseY, hoveredSlot);
            case FROST -> this.extractFrostSlotTooltip(graphics, mouseX, mouseY, hoveredSlot);
            default -> throw new IllegalStateException("Unknown smithing mode: " + this.menu.getMode());
        }
    }

    private boolean extractErrorTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.menu.getMode() == TranscendenceSmithingMenu.Mode.EMBER) {
            if (this.hasCompleteEmberInput()
                && this.menu.getActiveResult().isEmpty()
                && this.isHovering(123, 48, 16, 16, mouseX, mouseY)) {
                graphics.setTooltipForNextFrame(this.font, this.font.split(TranscendenceSmithingScreen.ERROR_TOOLTIP, 115), mouseX, mouseY);
                return true;
            }
            return false;
        }
        if (this.hasCompleteRoyalFrostInput()
            && this.menu.getActiveResult().isEmpty()
            && this.isHovering(83, 48, 16, 16, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, this.font.split(TranscendenceSmithingScreen.ERROR_TOOLTIP, 115), mouseX, mouseY);
            return true;
        }
        return false;
    }

    private void extractRoyalSlotTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Slot hoveredSlot) {
        Item item = this.menu.getSelectedTemplate().getItem();
        if (!(item instanceof SmithingTemplateItem template)) return;
        Component tooltip = switch (hoveredSlot.index) {
            case TranscendenceSmithingMenu.ROYAL_FROST_FIRST_INPUT_SLOT -> template.getBaseSlotDescription();
            case TranscendenceSmithingMenu.ROYAL_FROST_SECOND_INPUT_SLOT -> template.getAdditionSlotDescription();
            default -> null;
        };
        if (tooltip != null) {
            graphics.setTooltipForNextFrame(this.font, this.font.split(tooltip, 115), mouseX, mouseY);
        }
    }

    private void extractEmberSlotTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Slot hoveredSlot) {
        Item item = this.menu.getSelectedTemplate().getItem();
        if (!(item instanceof BaseMultipleToOneTemplateItem template)) return;
        if (hoveredSlot.index == TranscendenceSmithingMenu.EMBER_MATERIAL_SLOT) {
            graphics.setTooltipForNextFrame(
                this.font,
                this.font.split(template.getMaterialTooltip(), 115),
                mouseX,
                mouseY
            );
            return;
        }
        if (hoveredSlot.index < TranscendenceSmithingMenu.EMBER_INPUT_SLOT_START
            || hoveredSlot.index >= TranscendenceSmithingMenu.EMBER_INPUT_SLOT_END) {
            return;
        }
        ItemStack material = this.menu.getEmberMaterial();
        if (material.getItem() instanceof IMultipleMaterial multipleMaterial) {
            Component tooltip = multipleMaterial.getInputTooltip(
                this.menu.getSelectedTemplate(),
                this.menu.getEmberInputStacks()
            );
            graphics.setTooltipForNextFrame(this.font, this.font.split(tooltip, 115), mouseX, mouseY);
        }
    }

    private void extractFrostSlotTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Slot hoveredSlot) {
        Item item = this.menu.getSelectedTemplate().getItem();
        if (item instanceof PermutationTemplateItem permutation) {
            if (hoveredSlot.index == TranscendenceSmithingMenu.ROYAL_FROST_FIRST_INPUT_SLOT) {
                graphics.setTooltipForNextFrame(
                    this.font,
                    this.font.split(permutation.getMaterialTooltip(), 115),
                    mouseX,
                    mouseY
                );
            } else if (hoveredSlot.index == TranscendenceSmithingMenu.ROYAL_FROST_SECOND_INPUT_SLOT
                && this.menu.getRoyalFrostFirstInput().getItem() instanceof IPermutationMaterial material) {
                Component tooltip = material.getInputTooltip(this.menu.getRoyalFrostFirstInput());
                graphics.setTooltipForNextFrame(this.font, this.font.split(tooltip, 115), mouseX, mouseY);
            }
        } else if (item instanceof DeformationTemplateItem deformation
            && hoveredSlot.index == TranscendenceSmithingMenu.ROYAL_FROST_SECOND_INPUT_SLOT) {
            graphics.setTooltipForNextFrame(
                this.font,
                this.font.split(deformation.getInputTooltip(), 115),
                mouseX,
                mouseY
            );
        }
    }

    private boolean hasCompleteRoyalFrostInput() {
        return this.menu.hasSelectedTemplate()
            && !this.menu.getRoyalFrostFirstInput().isEmpty()
            && !this.menu.getRoyalFrostSecondInput().isEmpty();
    }

    private boolean hasCompleteEmberInput() {
        if (!this.menu.hasSelectedTemplate() || this.menu.getEmberMaterial().isEmpty()) return false;
        for (int index = 0; index < this.menu.getEmberInputSize(); index++) {
            if (this.menu.getSlot(TranscendenceSmithingMenu.EMBER_INPUT_SLOT_START + index).getItem().isEmpty()) {
                return false;
            }
        }
        return this.menu.getEmberInputSize() > 0;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
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
            ClientPacketDistributor.sendToServer(new TranscendenceSmithingPackets.TemplateAction(
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
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
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
        if (!this.isOverTemplateGrid(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int maxScrollRow = this.maxScrollRow();
        if (maxScrollRow > 0 && scrollY != 0) {
            this.scrollRow = Mth.clamp(this.scrollRow - (int) Math.signum(scrollY), 0, maxScrollRow);
        }
        return true;
    }

    private List<ItemStack> filteredTemplates() {
        String search = this.searchBox == null ? "" : this.searchBox.getValue().strip().toLowerCase(Locale.ROOT);
        if (search.isEmpty()) return this.menu.getTemplates();
        return this.menu.getTemplates().stream().filter(stack -> {
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
            return name.contains(search) || id.contains(search);
        }).toList();
    }

    private ItemStack templateAt(double mouseX, double mouseY) {
        int relativeX = (int) mouseX - this.panelX() - TranscendenceSmithingScreen.TEMPLATE_GRID_X;
        int relativeY = (int) mouseY - this.panelY() - TranscendenceSmithingScreen.TEMPLATE_GRID_Y;
        if (relativeX < 0 || relativeY < 0) return ItemStack.EMPTY;
        int column = relativeX / TranscendenceSmithingScreen.TEMPLATE_SLOT_SIZE;
        int row = relativeY / TranscendenceSmithingScreen.TEMPLATE_SLOT_SIZE;
        if (column >= TranscendenceSmithingScreen.COLUMN_COUNT || row >= TranscendenceSmithingScreen.VISIBLE_ROW_COUNT) {
            return ItemStack.EMPTY;
        }
        if (relativeX % TranscendenceSmithingScreen.TEMPLATE_SLOT_SIZE >= 16
            || relativeY % TranscendenceSmithingScreen.TEMPLATE_SLOT_SIZE >= 16) {
            return ItemStack.EMPTY;
        }
        int index = (this.scrollRow + row) * TranscendenceSmithingScreen.COLUMN_COUNT + column;
        List<ItemStack> templates = this.filteredTemplates();
        return index < templates.size() ? templates.get(index) : ItemStack.EMPTY;
    }

    private boolean isFavorite(ItemStack template) {
        Identifier id = BuiltInRegistries.ITEM.getKey(template.getItem());
        return this.menu.getFavoriteTemplates().contains(id);
    }

    private void clampScrollRow() {
        this.scrollRow = Mth.clamp(this.scrollRow, 0, this.maxScrollRow());
    }

    private int maxScrollRow() {
        int rowCount =
            (this.filteredTemplates().size() + TranscendenceSmithingScreen.COLUMN_COUNT - 1) / TranscendenceSmithingScreen.COLUMN_COUNT;
        return Math.max(0, rowCount - TranscendenceSmithingScreen.VISIBLE_ROW_COUNT);
    }

    private int sliderY(int maxScrollRow) {
        if (maxScrollRow <= 0) return this.panelY() + TranscendenceSmithingScreen.SLIDER_MIN_Y;
        int travel = TranscendenceSmithingScreen.SLIDER_MAX_Y - TranscendenceSmithingScreen.SLIDER_MIN_Y;
        return this.panelY() + TranscendenceSmithingScreen.SLIDER_MIN_Y + Math.round((float) this.scrollRow / maxScrollRow * travel);
    }

    private void updateScrollFromSlider(double mouseY, int maxScrollRow) {
        if (maxScrollRow <= 0) {
            this.scrollRow = 0;
            return;
        }
        double sliderCenter = mouseY - this.panelY() - TranscendenceSmithingScreen.SLIDER_MIN_Y - 6;
        double progress = Mth.clamp(
            sliderCenter / (TranscendenceSmithingScreen.SLIDER_MAX_Y - TranscendenceSmithingScreen.SLIDER_MIN_Y), 0.0, 1.0);
        this.scrollRow = Mth.clamp((int) Math.round(progress * maxScrollRow), 0, maxScrollRow);
    }

    private boolean isOverTemplateGrid(double mouseX, double mouseY) {
        return mouseX >= this.panelX() + 3
               && mouseX < this.panelX() + 66
               && mouseY >= this.panelY() + 17
               && mouseY < this.panelY() + 129;
    }

    private boolean isOverTemplatePanel(double mouseX, double mouseY) {
        return mouseX >= this.panelX()
               && mouseX < this.panelX() + TranscendenceSmithingScreen.PANEL_WIDTH
               && mouseY >= this.panelY()
               && mouseY < this.panelY() + TranscendenceSmithingScreen.PANEL_HEIGHT;
    }

    private boolean isOverSlider(double mouseX, double mouseY) {
        return mouseX >= this.panelX() + TranscendenceSmithingScreen.SLIDER_X
               && mouseX < this.panelX() + TranscendenceSmithingScreen.SLIDER_X + 8
               && mouseY >= this.panelY() + TranscendenceSmithingScreen.SLIDER_MIN_Y
               && mouseY < this.panelY() + TranscendenceSmithingScreen.SLIDER_MAX_Y + 12;
    }

    private int panelX() {
        return this.leftPos - TranscendenceSmithingScreen.PANEL_RIGHT_BORDER;
    }

    private int panelY() {
        return this.topPos + TranscendenceSmithingScreen.PANEL_TOP_OFFSET;
    }

    private void updateArmorStandPreview() {
        ItemStack result = this.menu.getActiveResult();
        if (ItemStack.isSameItemSameComponents(this.previewStack, result)) return;
        this.previewStack = result.copy();
        this.armorStandPreview.leftHandItemStack = ItemStack.EMPTY;
        this.armorStandPreview.leftHandItemState.clear();
        this.armorStandPreview.headEquipment = ItemStack.EMPTY;
        this.armorStandPreview.headItem.clear();
        this.armorStandPreview.chestEquipment = ItemStack.EMPTY;
        this.armorStandPreview.legsEquipment = ItemStack.EMPTY;
        this.armorStandPreview.feetEquipment = ItemStack.EMPTY;
        if (result.isEmpty()) return;
        ItemStack resultCopy = result.copy();
        Equippable equippable = result.get(DataComponents.EQUIPPABLE);
        EquipmentSlot slot = equippable != null ? equippable.slot() : null;
        ItemModelResolver itemModelResolver = this.minecraft.getItemModelResolver();
        switch (slot) {
            case HEAD -> {
                if (HumanoidArmorLayer.shouldRender(result, EquipmentSlot.HEAD)) {
                    this.armorStandPreview.headEquipment = resultCopy;
                } else {
                    itemModelResolver.updateForTopItem(
                        this.armorStandPreview.headItem,
                        resultCopy,
                        ItemDisplayContext.HEAD,
                        null,
                        null,
                        0
                    );
                }
            }
            case CHEST -> this.armorStandPreview.chestEquipment = resultCopy;
            case LEGS -> this.armorStandPreview.legsEquipment = resultCopy;
            case FEET -> this.armorStandPreview.feetEquipment = resultCopy;
            case null, default -> {
                this.armorStandPreview.leftHandItemStack = resultCopy;
                itemModelResolver.updateForTopItem(
                    this.armorStandPreview.leftHandItemState,
                    resultCopy,
                    ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    null,
                    null,
                    0
                );
            }
        }
    }

    @Override
    public void slotChanged(AbstractContainerMenu container, int slotIndex, ItemStack stack) {
        if (slotIndex == TranscendenceSmithingMenu.ROYAL_FROST_RESULT_SLOT
            || slotIndex == TranscendenceSmithingMenu.EMBER_RESULT_SLOT) {
            this.updateArmorStandPreview();
        }
    }

    @Override
    public void dataChanged(AbstractContainerMenu container, int dataSlotIndex, int value) {
    }

    @Override
    public void removed() {
        this.menu.removeSlotListener(this);
        super.removed();
    }
}
