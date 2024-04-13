package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Optional;

public class RoyalSmithingScreen extends ItemCombinerScreen<RoyalSmithingMenu> {
    private static final ResourceLocation SMITHING_LOCATION =
        AnvilCraft.of("textures/gui/container/royal_smithing_table.png");
    private static final ResourceLocation ERROR = AnvilCraft.of("textures/gui/container/error.png");
    private static final ResourceLocation EMPTY_SLOT_SMITHING_TEMPLATE_ARMOR_TRIM =
        new ResourceLocation("item/empty_slot_smithing_template_armor_trim");
    private static final ResourceLocation EMPTY_SLOT_SMITHING_TEMPLATE_NETHERITE_UPGRADE =
        new ResourceLocation("item/empty_slot_smithing_template_netherite_upgrade");
    private static final Component MISSING_TEMPLATE_TOOLTIP =
        Component.translatable("container.upgrade.missing_template_tooltip");
    private static final Component ERROR_TOOLTIP =
        Component.translatable("container.upgrade.error_tooltip");
    private static final List<ResourceLocation> EMPTY_SLOT_SMITHING_TEMPLATES =
        List.of(EMPTY_SLOT_SMITHING_TEMPLATE_ARMOR_TRIM, EMPTY_SLOT_SMITHING_TEMPLATE_NETHERITE_UPGRADE);
    public static final Quaternionf ARMOR_STAND_ANGLE = new Quaternionf()
        .rotationXYZ(0.43633232f, 0.0f, (float) Math.PI);
    private final CyclingSlotBackground templateIcon = new CyclingSlotBackground(0);
    private final CyclingSlotBackground baseIcon = new CyclingSlotBackground(1);
    private final CyclingSlotBackground additionalIcon = new CyclingSlotBackground(2);
    @Nullable
    private ArmorStand armorStandPreview;

    /**
     * 皇家锻造台 GUI
     *
     * @param menu            菜单
     * @param playerInventory 背包
     * @param title           标题
     */
    public RoyalSmithingScreen(RoyalSmithingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, SMITHING_LOCATION);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void subInit() {
        if (this.minecraft != null && this.minecraft.level != null) {
            this.armorStandPreview = new ArmorStand(this.minecraft.level, 0.0, 0.0, 0.0);
            this.armorStandPreview.setNoBasePlate(true);
            this.armorStandPreview.setShowArms(true);
            this.armorStandPreview.yBodyRot = 210.0f;
            this.armorStandPreview.setXRot(25.0f);
            this.armorStandPreview.yHeadRot = this.armorStandPreview.getYRot();
            this.armorStandPreview.yHeadRotO = this.armorStandPreview.getYRot();
        }
        this.updateArmorStandPreview(this.menu.getSlot(3).getItem());
    }

    @Override
    public void containerTick() {
        super.containerTick();
        Optional<SmithingTemplateItem> optional = this.getTemplateItem();
        this.templateIcon.tick(EMPTY_SLOT_SMITHING_TEMPLATES);
        this.baseIcon.tick(optional.map(SmithingTemplateItem::getBaseSlotEmptyIcons).orElse(List.of()));
        this.additionalIcon.tick(optional.map(SmithingTemplateItem::getAdditionalSlotEmptyIcons).orElse(List.of()));
    }

    private Optional<SmithingTemplateItem> getTemplateItem() {
        Item item;
        ItemStack itemStack = this.menu.getSlot(0).getItem();
        if (!itemStack.isEmpty() && (item = itemStack.getItem()) instanceof SmithingTemplateItem) {
            SmithingTemplateItem smithingTemplateItem = (SmithingTemplateItem) item;
            return Optional.of(smithingTemplateItem);
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
        this.baseIcon.render(this.menu, guiGraphics, partialTick, this.leftPos, this.topPos);
        this.additionalIcon.render(this.menu, guiGraphics, partialTick, this.leftPos, this.topPos);
        if (this.armorStandPreview == null) return;
        InventoryScreen.renderEntityInInventory(guiGraphics, this.leftPos + 149, this.topPos + 75,
            25, ARMOR_STAND_ANGLE, null, this.armorStandPreview);
    }

    @Override
    public void slotChanged(
        @NotNull AbstractContainerMenu containerToSend, int dataSlotIndex, @NotNull ItemStack stack
    ) {
        if (dataSlotIndex == 3) {
            this.updateArmorStandPreview(stack);
        }
    }

    private void updateArmorStandPreview(ItemStack stack) {
        if (this.armorStandPreview == null) {
            return;
        }
        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            this.armorStandPreview.setItemSlot(equipmentSlot, ItemStack.EMPTY);
        }
        if (!stack.isEmpty()) {
            ItemStack itemStack = stack.copy();
            Item item = stack.getItem();
            if (item instanceof ArmorItem armorItem) {
                this.armorStandPreview.setItemSlot(armorItem.getEquipmentSlot(), itemStack);
            } else {
                this.armorStandPreview.setItemSlot(EquipmentSlot.OFFHAND, itemStack);
            }
        }
    }

    @Override
    protected void renderErrorIcon(@NotNull GuiGraphics guiGraphics, int x, int y) {
        if (this.hasRecipeError()) {
            guiGraphics.blit(ERROR, x + 65, y + 46, 0, 0, 16, 16, 16, 16);
        }
    }

    private void renderOnboardingTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Optional<Component> optional = Optional.empty();
        if (this.hasRecipeError() && this.isHovering(65, 46, 28, 21, mouseX, mouseY)) {
            optional = Optional.of(ERROR_TOOLTIP);
        }
        if (this.hoveredSlot != null) {
            ItemStack itemStack = this.menu.getSlot(0).getItem();
            ItemStack itemStack2 = this.hoveredSlot.getItem();
            if (itemStack.isEmpty()) {
                if (this.hoveredSlot.index == 0) {
                    optional = Optional.of(MISSING_TEMPLATE_TOOLTIP);
                }
            } else {
                Item item = itemStack.getItem();
                if (item instanceof SmithingTemplateItem smithingTemplateItem) {
                    if (itemStack2.isEmpty()) {
                        if (this.hoveredSlot.index == 1) {
                            optional = Optional.of(smithingTemplateItem.getBaseSlotDescription());
                        } else if (this.hoveredSlot.index == 2) {
                            optional = Optional.of(smithingTemplateItem.getAdditionSlotDescription());
                        }
                    }
                }
            }
        }
        optional.ifPresent(component ->
            guiGraphics.renderTooltip(this.font, this.font.split(component, 115), mouseX, mouseY));
    }

    private boolean hasRecipeError() {
        return this.menu.getSlot(0).hasItem()
            && this.menu.getSlot(1).hasItem()
            && this.menu.getSlot(2).hasItem()
            && !this.menu.getSlot(this.menu.getResultSlot()).hasItem();
    }
}
