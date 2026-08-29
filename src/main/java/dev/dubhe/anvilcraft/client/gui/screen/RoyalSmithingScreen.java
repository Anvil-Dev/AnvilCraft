package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.equipment.Equippable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

public class RoyalSmithingScreen extends AdjacentSmithingScreen<RoyalSmithingMenu> {
    private static final Identifier SMITHING_LOCATION = SharedTextures.bg("crafting", "royal_smithing_table");
    private static final Identifier EMPTY_SLOT_SMITHING_TEMPLATE_ARMOR_TRIM =
        Identifier.withDefaultNamespace("container/slot/smithing_template_armor_trim");
    private static final Identifier EMPTY_SLOT_SMITHING_TEMPLATE_NETHERITE_UPGRADE =
        Identifier.withDefaultNamespace("container/slot/smithing_template_netherite_upgrade");
    private static final Component MISSING_TEMPLATE_TOOLTIP =
        Component.translatable("container.upgrade.missing_template_tooltip");
    private static final Component ERROR_TOOLTIP = Component.translatable("container.upgrade.error_tooltip");
    private static final List<Identifier> EMPTY_SLOT_SMITHING_TEMPLATES =
        List.of(
            RoyalSmithingScreen.EMPTY_SLOT_SMITHING_TEMPLATE_ARMOR_TRIM,
            RoyalSmithingScreen.EMPTY_SLOT_SMITHING_TEMPLATE_NETHERITE_UPGRADE
        );
    private static final Vector3f ARMOR_STAND_TRANSLATION = new Vector3f(0.0F, 1.0F, 0.0F);
    public static final Quaternionf ARMOR_STAND_ANGLE =
        new Quaternionf().rotationXYZ(0.43633232F, 0.0F, (float) Math.PI);
    private final CyclingSlotBackground templateIcon = new CyclingSlotBackground(0);
    private final CyclingSlotBackground baseIcon = new CyclingSlotBackground(1);
    private final CyclingSlotBackground additionalIcon = new CyclingSlotBackground(2);

    private final ArmorStandRenderState armorStandPreview = new ArmorStandRenderState();

    /// 皇家锻造台 GUI
    ///
    /// @param menu            菜单
    /// @param playerInventory 背包
    /// @param title           标题
    public RoyalSmithingScreen(RoyalSmithingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, RoyalSmithingScreen.SMITHING_LOCATION);
        this.armorStandPreview.entityType = EntityType.ARMOR_STAND;
        this.armorStandPreview.showBasePlate = false;
        this.armorStandPreview.showArms = true;
        this.armorStandPreview.xRot = 25.0F;
        this.armorStandPreview.bodyRot = 210.0F;
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
    protected void subInit() {
        this.updateArmorStandPreview(this.menu.getSlot(3).getItem());
    }

    @Override
    public void containerTick() {
        super.containerTick();
        Optional<SmithingTemplateItem> optional = this.getTemplateItem();
        this.templateIcon.tick(RoyalSmithingScreen.EMPTY_SLOT_SMITHING_TEMPLATES);
        this.baseIcon.tick(
            optional.map(SmithingTemplateItem::getBaseSlotEmptyIcons).orElse(List.of()));
        this.additionalIcon.tick(
            optional.map(SmithingTemplateItem::getAdditionalSlotEmptyIcons).orElse(List.of()));
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
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        this.extractOnboardingTooltips(graphics, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        this.templateIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
        this.baseIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
        this.additionalIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);

        int x0 = this.leftPos + 131;
        int y0 = this.topPos + 20;
        int x1 = this.leftPos + 171;
        int y1 = this.topPos + 60;
        graphics.entity(
            this.armorStandPreview,
            25,
            RoyalSmithingScreen.ARMOR_STAND_TRANSLATION,
            RoyalSmithingScreen.ARMOR_STAND_ANGLE,
            null,
            x0,
            y0,
            x1,
            y1
        );
    }

    @Override
    public void slotChanged(AbstractContainerMenu containerToSend, int dataSlotIndex, ItemStack stack) {
        if (dataSlotIndex == 3) {
            this.updateArmorStandPreview(stack);
        }
    }

    private void updateArmorStandPreview(ItemStack stack) {
        this.armorStandPreview.leftHandItemStack = ItemStack.EMPTY;
        this.armorStandPreview.leftHandItemState.clear();
        this.armorStandPreview.headEquipment = ItemStack.EMPTY;
        this.armorStandPreview.headItem.clear();
        this.armorStandPreview.chestEquipment = ItemStack.EMPTY;
        this.armorStandPreview.legsEquipment = ItemStack.EMPTY;
        this.armorStandPreview.feetEquipment = ItemStack.EMPTY;
        if (stack.isEmpty()) return;
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        EquipmentSlot slot = equippable != null ? equippable.slot() : null;
        ItemModelResolver itemModelResolver = this.minecraft.getItemModelResolver();
        switch (slot) {
            case HEAD -> {
                if (HumanoidArmorLayer.shouldRender(stack, EquipmentSlot.HEAD)) {
                    this.armorStandPreview.headEquipment = stack.copy();
                } else {
                    itemModelResolver.updateForTopItem(this.armorStandPreview.headItem, stack, ItemDisplayContext.HEAD, null, null, 0);
                }
            }
            case CHEST -> this.armorStandPreview.chestEquipment = stack.copy();
            case LEGS -> this.armorStandPreview.legsEquipment = stack.copy();
            case FEET -> this.armorStandPreview.feetEquipment = stack.copy();
            case null, default -> {
                this.armorStandPreview.leftHandItemStack = stack.copy();
                itemModelResolver.updateForTopItem(
                    this.armorStandPreview.leftHandItemState, stack, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, null, null, 0
                );
            }
        }
    }

    @Override
    protected void extractErrorIcon(GuiGraphicsExtractor graphics, int x, int y) {
        if (this.hasRecipeError()) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, SharedTextures.ERROR_SPRITE, x + 83, y + 48, 0, 0, 16, 16, 16, 16);
        }
    }

    private void extractOnboardingTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Optional<Component> optional = Optional.empty();
        if (this.hasRecipeError() && this.isHovering(83, 48, 16, 16, mouseX, mouseY)) {
            optional = Optional.of(RoyalSmithingScreen.ERROR_TOOLTIP);
        }
        if (this.hoveredSlot != null) {
            ItemStack itemStack = this.menu.getSlot(0).getItem();
            ItemStack itemStack2 = this.hoveredSlot.getItem();
            if (itemStack.isEmpty()) {
                if (this.hoveredSlot.index == 0) {
                    optional = Optional.of(RoyalSmithingScreen.MISSING_TEMPLATE_TOOLTIP);
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
        optional.ifPresent(component -> graphics.setTooltipForNextFrame(
            this.font,
            this.font.split(component, 115),
            mouseX,
            mouseY
        ));
    }

    private boolean hasRecipeError() {
        return this.menu.getSlot(0).hasItem()
            && this.menu.getSlot(1).hasItem()
            && this.menu.getSlot(2).hasItem()
            && !this.menu.getSlot(this.menu.getResultSlot()).hasItem();
    }
}
