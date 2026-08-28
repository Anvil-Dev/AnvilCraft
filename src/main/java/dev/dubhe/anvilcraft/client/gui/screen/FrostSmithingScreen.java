package dev.dubhe.anvilcraft.client.gui.screen;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.IPermutationMaterial;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.FrostSmithingMenu;
import dev.dubhe.anvilcraft.item.template.frost.DeformationTemplateItem;
import dev.dubhe.anvilcraft.item.template.frost.PermutationTemplateItem;
import dev.dubhe.anvilcraft.network.multiple.FrostSmithingPackets;
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
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class FrostSmithingScreen extends AdjacentSmithingScreen<FrostSmithingMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("crafting", "frost_smithing_table");

    private static final Identifier LEFT =
        SharedTextures.textureGui("crafting/frost_smithing_table/button_left");
    private static final Identifier RIGHT =
        SharedTextures.textureGui("crafting/frost_smithing_table/button_right");

    private static final Identifier EMPTY_SLOT_PERMUTATION_SMITHING_TEMPLATE =
        AnvilCraft.of("item/empty_slot_permutation_smithing_template");
    private static final Identifier EMPTY_SLOT_DEFORMATION_SMITHING_TEMPLATE =
        AnvilCraft.of("item/empty_slot_deformation_smithing_template");
    private static final Identifier EMPTY_SLOT_INGOT =
        Identifier.withDefaultNamespace("container/slot/ingot");

    private static final Component MISSING_TEMPLATE_TOOLTIP = Component.translatable(
        "screen.anvilcraft.frost_smithing.tooltip.missing_template"
    );
    private static final Component ERROR_TOOLTIP = Component.translatable("container.upgrade.error_tooltip");

    private static final List<Identifier> EMPTY_SLOT_SMITHING_TEMPLATES = List.of(
        FrostSmithingScreen.EMPTY_SLOT_PERMUTATION_SMITHING_TEMPLATE,
        FrostSmithingScreen.EMPTY_SLOT_DEFORMATION_SMITHING_TEMPLATE
    );
    private static final List<Identifier> EMPTY_SLOT_DEFORM_MATERIAL = List.of(
        FrostSmithingScreen.EMPTY_SLOT_INGOT
    );
    private static final Vector3f ARMOR_STAND_TRANSLATION = new Vector3f(0.0F, 1.0F, 0.0F);
    public static final Quaternionf ARMOR_STAND_ANGLE = new Quaternionf().rotationXYZ(0.43633232F, 0.0F, (float) Math.PI);

    private final CyclingSlotBackground templateIcon = new CyclingSlotBackground(0);
    private final CyclingSlotBackground materialIcon = new CyclingSlotBackground(1);
    private final CyclingSlotBackground inputIcon = new CyclingSlotBackground(2);

    private @Nullable TexturedButton left;
    private @Nullable TexturedButton right;

    private final ArmorStandRenderState armorStandPreview = new ArmorStandRenderState();

    /// 皇家锻造台 GUI
    ///
    /// @param menu            菜单
    /// @param playerInventory 背包
    /// @param title           标题
    public FrostSmithingScreen(FrostSmithingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, FrostSmithingScreen.BACKGROUND);
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
        this.left = this.addRenderableWidget(new TexturedButton(
            this.leftPos + 102,
            this.topPos + 32,
            7,
            11,
            FrostSmithingScreen.LEFT,
            11,
            7,
            22,
            _ -> {
                this.menu.turn(true);
                ClientPacketDistributor.sendToServer(new FrostSmithingPackets.ClickButton(true));
                this.updateArmorStandPreview(this.menu.getSlot(3).getItem());
            }
        ));
        this.right = this.addRenderableWidget(new TexturedButton(
            this.leftPos + 119,
            this.topPos + 32,
            7,
            11,
            FrostSmithingScreen.RIGHT,
            11,
            7,
            22,
            _ -> {
                this.menu.turn(false);
                ClientPacketDistributor.sendToServer(new FrostSmithingPackets.ClickButton(false));
                this.updateArmorStandPreview(this.menu.getSlot(3).getItem());
            }
        ));
        this.modifyButtons(false);
    }

    @Override
    protected void subInit() {
        this.updateArmorStandPreview(this.menu.getSlot(3).getItem());
    }

    @Override
    public void containerTick() {
        super.containerTick();

        this.templateIcon.tick(FrostSmithingScreen.EMPTY_SLOT_SMITHING_TEMPLATES);
        var permut = this.getPermutTemplateItem();
        if (permut.isPresent()) {
            this.materialIcon.tick(permut.get().getEmptySlotTextures());
            this.inputIcon.tick(this.getMaterialItem().map(IPermutationMaterial::getEmptySlotTextures).orElse(List.of()));
            return;
        }
        var deform = this.getDeformTemplateItem();
        if (deform.isPresent()) {
            this.materialIcon.tick(FrostSmithingScreen.EMPTY_SLOT_DEFORM_MATERIAL);
            this.inputIcon.tick(deform.get().getEmptySlotTextures());
        } else {
            this.materialIcon.tick(List.of());
            this.inputIcon.tick(List.of());
        }
    }

    private Optional<PermutationTemplateItem> getPermutTemplateItem() {
        ItemStack stack = this.menu.getSlot(0).getItem();
        if (stack.isEmpty()) return Optional.empty();
        return Util.castSafely(stack.getItem(), PermutationTemplateItem.class);
    }

    private Optional<IPermutationMaterial> getMaterialItem() {
        ItemStack stack = this.menu.getSlot(1).getItem();
        if (stack.isEmpty()) return Optional.empty();
        return Util.castSafely(stack.getItem(), IPermutationMaterial.class);
    }

    private Optional<DeformationTemplateItem> getDeformTemplateItem() {
        ItemStack stack = this.menu.getSlot(0).getItem();
        if (stack.isEmpty()) return Optional.empty();
        return Util.castSafely(stack.getItem(), DeformationTemplateItem.class);
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
        this.materialIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
        this.inputIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);

        if (!this.menu.getSlot(0).getItem().isEmpty()) {
            List<RecipeResult> results = this.menu.results;
            this.modifyButtons(this.menu.selected != -1 && results != null && results.size() != 1);
        } else {
            this.modifyButtons(false);
        }

        int x0 = this.leftPos + 131;
        int y0 = this.topPos + 20;
        int x1 = this.leftPos + 171;
        int y1 = this.topPos + 60;
        graphics.entity(
            this.armorStandPreview,
            25,
            FrostSmithingScreen.ARMOR_STAND_TRANSLATION,
            FrostSmithingScreen.ARMOR_STAND_ANGLE,
            null,
            x0,
            y0,
            x1,
            y1
        );
    }

    private void modifyButtons(boolean enabled) {
        TexturedButton left = this.left;
        TexturedButton right = this.right;
        if (left == null || right == null) return;
        left.active = enabled;
        left.visible = enabled;
        right.active = enabled;
        right.visible = enabled;
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
        if (
            (this.menu.getSlot(0).hasItem() && this.menu.getSlot(2).hasItem())
            && !this.menu.getSlot(this.menu.getResultSlot()).hasItem()
        ) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, SharedTextures.ERROR_SPRITE, x + 83, y + 48, 0, 0, 16, 16, 16, 16);
        }
    }

    private void extractOnboardingTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (
            (this.menu.getSlot(0).hasItem() && this.menu.getSlot(2).hasItem())
            && !this.menu.getSlot(this.menu.getResultSlot()).hasItem()
            && this.isHovering(83, 48, 16, 16, mouseX, mouseY)
        ) {
            graphics.setTooltipForNextFrame(this.font, this.font.split(FrostSmithingScreen.ERROR_TOOLTIP, 115), mouseX, mouseY);
            return;
        }

        if (this.hoveredSlot == null) return;

        ItemStack template = this.menu.getSlot(0).getItem();
        if (template.isEmpty()) {
            if (this.hoveredSlot.index == 0) {
                graphics.setTooltipForNextFrame(
                    this.font, this.font.split(FrostSmithingScreen.MISSING_TEMPLATE_TOOLTIP, 115), mouseX, mouseY);
            }
            return;
        }

        ItemStack hovered = this.hoveredSlot.getItem();
        if (!hovered.isEmpty()) return;

        Item item = template.getItem();
        if (item instanceof PermutationTemplateItem permutation) {
            if (this.hoveredSlot.index == 1) {
                graphics.setTooltipForNextFrame(
                    this.font,
                    this.font.split(permutation.getMaterialTooltip(), 115),
                    mouseX,
                    mouseY
                );
            } else if (this.hoveredSlot.index == 2 && this.menu.getSlot(1).getItem().getItem() instanceof IPermutationMaterial material) {
                graphics.setTooltipForNextFrame(
                    this.font,
                    this.font.split(material.getInputTooltip(this.menu.getSlot(1).getItem()), 115),
                    mouseX,
                    mouseY
                );
            }
        } else if (item instanceof DeformationTemplateItem deformation) {
            if (this.hoveredSlot.index == 2) {
                graphics.setTooltipForNextFrame(this.font, this.font.split(deformation.getInputTooltip(), 115), mouseX, mouseY);
            }
        }
    }
}
