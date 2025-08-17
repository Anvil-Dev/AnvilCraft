package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.IMultipleMaterial;
import dev.dubhe.anvilcraft.inventory.EmberSmithingMenu;
import dev.dubhe.anvilcraft.item.template.BaseMultipleToOneTemplateItem;
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

    // 空槽位纹理 - 模板
    private static final ResourceLocation EMPTY_SLOT_TWO_TO_ONE_SMITHING_TEMPLATE =
        AnvilCraft.of("item/empty_slot_two_to_one_smithing_template");
    private static final ResourceLocation EMPTY_SLOT_FOUR_TO_ONE_SMITHING_TEMPLATE =
        AnvilCraft.of("item/empty_slot_four_to_one_smithing_template");
    private static final ResourceLocation EMPTY_SLOT_EIGHT_TO_ONE_SMITHING_TEMPLATE =
        AnvilCraft.of("item/empty_slot_eight_to_one_smithing_template");

    // tooltips
    private static final Component MISSING_TEMPLATE_TOOLTIP = Component.translatable(
        "screen.anvilcraft.ember_smithing.tooltip.missing_template");
    private static final Component ERROR_TOOLTIP = Component.translatable("container.upgrade.error_tooltip");

    public static final List<ResourceLocation> EMPTY_SLOT_SMITHING_TEMPLATES = List.of(
        EMPTY_SLOT_TWO_TO_ONE_SMITHING_TEMPLATE,
        EMPTY_SLOT_FOUR_TO_ONE_SMITHING_TEMPLATE,
        EMPTY_SLOT_EIGHT_TO_ONE_SMITHING_TEMPLATE
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
     * 余烬锻造台 GUI
     *
     * @param menu      菜单
     * @param inventory 背包
     * @param title     标题
     */
    public EmberSmithingScreen(EmberSmithingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BACKGROUND);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        Optional<BaseMultipleToOneTemplateItem> templateOptional = this.getTemplateItem();
        Optional<ItemStack> materialOptional = this.getMaterialItem();
        if (templateOptional.isPresent()) {
            this.materialIcon.tick(templateOptional.get().getEmptySlotTextures());
            if (materialOptional.isPresent() && materialOptional.get().getItem() instanceof IMultipleMaterial material) {
                this.inputIcons.forEach(
                    icon -> icon.tick(material.getEmptySlotTextures(
                        this.menu.getSlot(0).getItem(), icon.slotIndex - 2, this.menu.getInputStacks())));
            } else {
                this.inputIcons.forEach(icon -> icon.tick(List.of()));
            }
        } else {
            this.templateIcon.tick(EMPTY_SLOT_SMITHING_TEMPLATES);
            this.materialIcon.tick(List.of());
            this.inputIcons.forEach(icon -> icon.tick(List.of()));
        }
    }

    private Optional<BaseMultipleToOneTemplateItem> getTemplateItem() {
        ItemStack itemStack = this.menu.getSlot(0).getItem();
        if (!itemStack.isEmpty() && itemStack.getItem() instanceof BaseMultipleToOneTemplateItem template) {
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

        for (int i = 2; i < 10; i++) {
            if (this.isSlotEnabled(i)) continue;
            Slot slot = this.menu.getSlot(i);
            guiGraphics.blit(DISABLED_SLOT, this.leftPos + slot.x, this.topPos + slot.y, 0, 0, 16, 16, 16, 16);
        }
    }

    protected boolean isSlotEnabled(int slot) {
        return slot >= 2 && slot < 10 && slot - 2 < this.menu.getInputSize();
    }

    @Override
    protected void renderErrorIcon(@NotNull GuiGraphics guiGraphics, int x, int y) {
        if (this.menu.canCreateResult()) {
            guiGraphics.blit(ERROR, x + 123, y + 48, 0, 0, 16, 16, 16, 16);
        }
    }

    private void renderOnboardingTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Optional<Component> optional = Optional.empty();
        if (this.menu.canCreateResult() && this.isHovering(123, 48, 16, 16, mouseX, mouseY)) {
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
                if (template.getItem() instanceof BaseMultipleToOneTemplateItem templateItem && hovered.isEmpty()) {
                    if (this.hoveredSlot.index == 1) {
                        optional = Optional.of(templateItem.getMaterialTooltip());
                    } else if (
                        this.hoveredSlot.index >= 2 && this.hoveredSlot.index <= 9
                            && material.getItem() instanceof IMultipleMaterial materialItem
                            && this.isSlotEnabled(this.hoveredSlot.index)
                    ) {
                        optional = Optional.of(materialItem.getInputTooltip(
                            this.menu.getSlot(0).getItem(), this.menu.getInputStacks()));
                    }
                }
            }
        }
        optional.ifPresent(
            component -> guiGraphics.renderTooltip(this.font, this.font.split(component, 115), mouseX, mouseY));
    }
}
