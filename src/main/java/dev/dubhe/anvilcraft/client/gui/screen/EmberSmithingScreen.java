package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.inventory.EmberSmithingMenu;
import dev.dubhe.anvilcraft.item.template.MultipleToOneTemplateItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class EmberSmithingScreen extends ItemCombinerScreen<EmberSmithingMenu> {
    private static final ResourceLocation BACKGROUND =
        AnvilCraft.of("textures/gui/container/smithing/background/ember_smithing_table.png");
    private static final ResourceLocation DISABLED_SLOT = AnvilCraft.of("textures/gui/container/machine/disabled_slot.png");
    private static final ResourceLocation ERROR = AnvilCraft.of("textures/gui/container/smithing/error.png");

    private static final ResourceLocation EMPTY_SLOT_TWO_TO_ONE_SMITHING_TEMPLATE =
        AnvilCraft.of("item/empty_slot_two_to_one_smithing_template");
    private static final ResourceLocation EMPTY_SLOT_FOUR_TO_ONE_SMITHING_TEMPLATE =
        AnvilCraft.of("item/empty_slot_four_to_one_smithing_template");
    private static final ResourceLocation EMPTY_SLOT_EIGHT_TO_ONE_SMITHING_TEMPLATE =
        AnvilCraft.of("item/empty_slot_eight_to_one_smithing_template");

    private static final ResourceLocation EMPTY_SLOT_MULTIPHASE_MATTER =
        AnvilCraft.of("item/empty_slot_multiphase_matter");

    private static final ResourceLocation EMPTY_SLOT_AXE =
        ResourceLocation.withDefaultNamespace("item/empty_slot_axe");
    private static final ResourceLocation EMPTY_SLOT_HOE =
        ResourceLocation.withDefaultNamespace("item/empty_slot_hoe");
    private static final ResourceLocation EMPTY_SLOT_PICKAXE =
        ResourceLocation.withDefaultNamespace("item/empty_slot_pickaxe");
    private static final ResourceLocation EMPTY_SLOT_SHOVEL =
        ResourceLocation.withDefaultNamespace("item/empty_slot_shovel");
    private static final ResourceLocation EMPTY_SLOT_SWORD =
        ResourceLocation.withDefaultNamespace("item/empty_slot_sword");

    private static final Component MISSING_TEMPLATE_TOOLTIP = Component.translatable(
        "screen.anvilcraft.ember_smithing.tooltip.missing_template");
    private static final Component TWO_MISSING_MULTIPHASE_MATTER_TOOLTIP = Component.translatable(
        "screen.anvilcraft.ember_smithing.tooltip.two_missing_multiphase_matter");
    private static final Component TWO_MULTIPHASE_MATTER_MISSING_TOOLS_TOOLTIP = Component.translatable(
        "screen.anvilcraft.ember_smithing.tooltip.two_multiphase_matter_missing_tool");
    private static final Component ERROR_TOOLTIP = Component.translatable("container.upgrade.error_tooltip");

    private static final List<ResourceLocation> EMPTY_SLOT_SMITHING_TEMPLATES = List.of(
        EMPTY_SLOT_TWO_TO_ONE_SMITHING_TEMPLATE,
        EMPTY_SLOT_FOUR_TO_ONE_SMITHING_TEMPLATE,
        EMPTY_SLOT_EIGHT_TO_ONE_SMITHING_TEMPLATE
    );
    private static final List<ResourceLocation> EMPTY_SLOT_MATERIALS_TWO = List.of(
        EMPTY_SLOT_MULTIPHASE_MATTER
    );
    private static final List<ResourceLocation> EMPTY_SLOT_MATERIALS_FOUR = List.of(
    );
    private static final List<ResourceLocation> EMPTY_SLOT_MATERIALS_EIGHT = List.of(
    );
    private static final List<ResourceLocation> EMPTY_SLOT_TOOLS = List.of(
        EMPTY_SLOT_AXE,
        EMPTY_SLOT_HOE,
        EMPTY_SLOT_PICKAXE,
        EMPTY_SLOT_SHOVEL,
        EMPTY_SLOT_SWORD
    );

    private final CyclingSlotBackground templateIcon = new CyclingSlotBackground(0);
    private final CyclingSlotBackground materialIcon = new CyclingSlotBackground(1);
    private final List<CyclingSlotBackground> inputIcons = List.of(
        new CyclingSlotBackground(2),
        new CyclingSlotBackground(3),
        new CyclingSlotBackground(4),
        new CyclingSlotBackground(5),
        new CyclingSlotBackground(6),
        new CyclingSlotBackground(7),
        new CyclingSlotBackground(8),
        new CyclingSlotBackground(9)
    );

    /**
     * 皇家锻造台 GUI
     *
     * @param menu            菜单
     * @param playerInventory 背包
     * @param title           标题
     */
    public EmberSmithingScreen(EmberSmithingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, BACKGROUND);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.templateIcon.tick(EMPTY_SLOT_SMITHING_TEMPLATES);

        Optional<MultipleToOneTemplateItem> template = this.getTemplateItem();
        Optional<ItemStack> material = this.getMaterialItem();
        if (template.isPresent()) {
            MultipleToOneTemplateItem item = template.get();
            if (item.getSize() == 2) {
                this.materialIcon.tick(EMPTY_SLOT_MATERIALS_TWO);
                if (material.isPresent()) {
                    if (material.get().is(ModItems.MULTIPHASE_MATTER)) {
                        this.inputIcons.forEach(icon -> icon.tick(EMPTY_SLOT_TOOLS));
                    }
                } else {
                    this.inputIcons.forEach(icon -> icon.tick(List.of()));
                }
            } else if (item.getSize() == 4) {
                this.materialIcon.tick(EMPTY_SLOT_MATERIALS_FOUR);
            } else if (item.getSize() == 8) {
                this.materialIcon.tick(EMPTY_SLOT_MATERIALS_EIGHT);
            }
        } else {
            this.materialIcon.tick(List.of());
            this.inputIcons.forEach(icon -> icon.tick(List.of()));
        }
    }

    private Optional<MultipleToOneTemplateItem> getTemplateItem() {
        ItemStack itemStack = this.menu.getSlot(0).getItem();
        if (!itemStack.isEmpty() && itemStack.getItem() instanceof MultipleToOneTemplateItem template) {
            return Optional.of(template);
        }
        return Optional.empty();
    }

    private Optional<ItemStack> getMaterialItem() {
        ItemStack itemStack = this.menu.getSlot(1).getItem();
        if (!itemStack.isEmpty()) {
            return Optional.of(itemStack);
        }
        return Optional.empty();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderOnboardingTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        this.templateIcon.render(this.menu, guiGraphics, partialTick, this.leftPos, this.topPos);
        this.materialIcon.render(this.menu, guiGraphics, partialTick, this.leftPos, this.topPos);
        this.inputIcons.forEach(icon -> icon.render(this.menu, guiGraphics, partialTick, this.leftPos, this.topPos));

        for (int i = 2 + this.menu.getInputSize(); i < 10; i++) {
            Slot slot = this.menu.getSlot(i);
            guiGraphics.blit(DISABLED_SLOT, this.leftPos + slot.x, this.topPos + slot.y, 0, 0, 16, 16, 16, 16);
        }
    }

    @Override
    protected void renderErrorIcon(@NotNull GuiGraphics guiGraphics, int x, int y) {
        if (this.hasRecipeError()) {
            guiGraphics.blit(ERROR, x + 83, y + 48, 0, 0, 16, 16, 16, 16);
        }
    }

    private void renderOnboardingTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Optional<Component> optional = Optional.empty();
        if (this.hasRecipeError() && this.isHovering(83, 48, 16, 16, mouseX, mouseY)) {
            optional = Optional.of(ERROR_TOOLTIP);
        }
        if (this.hoveredSlot != null) {
            ItemStack template = this.menu.getSlot(0).getItem();
            ItemStack material = this.menu.getSlot(1).getItem();
            ItemStack hovered = this.hoveredSlot.getItem();
            if (template.isEmpty()) {
                if (this.hoveredSlot.index == 0) {
                    optional = Optional.of(MISSING_TEMPLATE_TOOLTIP);
                }
            } else {
                if (template.getItem() instanceof MultipleToOneTemplateItem templateItem && hovered.isEmpty()) {
                    if (this.hoveredSlot.index == 1) {
                        if (templateItem.getSize() == 2) {
                            optional = Optional.of(TWO_MISSING_MULTIPHASE_MATTER_TOOLTIP);
                        }
                    } else if (this.hoveredSlot.index >= 2 && this.hoveredSlot.index <= 9) {
                        if (templateItem.getSize() == 2 && material.is(ModItems.MULTIPHASE_MATTER)) {
                            optional = Optional.of(TWO_MULTIPHASE_MATTER_MISSING_TOOLS_TOOLTIP);
                        }
                    }
                }
            }
        }
        optional.ifPresent(
            component -> guiGraphics.renderTooltip(this.font, this.font.split(component, 115), mouseX, mouseY));
    }

    private boolean hasRecipeError() {
        ItemStack template = this.menu.getSlot(0).getItem();
        ItemStack material = this.menu.getSlot(1).getItem();
        boolean hasInput = false;

        if (template.getItem() instanceof MultipleToOneTemplateItem templateItem) {
            if (templateItem.getSize() == 2) {
                hasInput =
                    this.menu.getSlot(2).hasItem()
                        && this.menu.getSlot(3).hasItem();
            } else if (templateItem.getSize() == 4) {
                hasInput =
                    this.menu.getSlot(2).hasItem()
                        && this.menu.getSlot(3).hasItem()
                        && this.menu.getSlot(4).hasItem()
                        && this.menu.getSlot(5).hasItem();
            } else if (templateItem.getSize() == 8) {
                hasInput =
                    this.menu.getSlot(2).hasItem()
                        && this.menu.getSlot(3).hasItem()
                        && this.menu.getSlot(4).hasItem()
                        && this.menu.getSlot(5).hasItem()
                        && this.menu.getSlot(6).hasItem()
                        && this.menu.getSlot(7).hasItem()
                        && this.menu.getSlot(8).hasItem()
                        && this.menu.getSlot(9).hasItem();
            }
        }

        return this.menu.getSlot(0).hasItem()
            && this.menu.getSlot(1).hasItem()
            && hasInput
            && !this.menu.getSlot(this.menu.getResultSlot()).hasItem();
    }
}
