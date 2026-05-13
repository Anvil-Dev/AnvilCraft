package dev.dubhe.anvilcraft.client.gui.screen;

import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Scrollable;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.EnergyWeaponMakeMenu;
import dev.dubhe.anvilcraft.inventory.component.FilteredSlot;
import dev.dubhe.anvilcraft.network.multiple.EnergyWeaponMakePackets;
import dev.dubhe.anvilcraft.recipe.EnergyWeaponMakeRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

public class EnergyWeaponMakeScreen extends AbstractContainerScreen<EnergyWeaponMakeMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("machine", "energy_weapon_platform");
    private final Scrollable scrollable = new Scrollable() {
        @Override
        public int row() {
            return 2;
        }

        @Override
        public int column() {
            return 3;
        }

        @Override
        public int size() {
            return EnergyWeaponMakeScreen.this.menu.getRecipes().size();
        }

        @Override
        public void setHead(int head) {
            EnergyWeaponMakeScreen.this.head = head;
        }
    };
    private int head;
    private ItemStack renderingTooltip;
    private long cantCraftBlinkMs = 0;

    public EnergyWeaponMakeScreen(EnergyWeaponMakeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 175);
    }

    @Override
    protected void containerTick() {
        this.renderingTooltip = null;

        super.containerTick();
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

        this.addRenderableWidget(new TexturedButton(
            this.leftPos + 152,
            this.topPos + 34,
            16,
            16,
            SharedTextures.CONFIRM,
            16,
            16,
            32,
            _ -> {
                this.menu.make(Minecraft.getInstance().player);
                ClientPacketDistributor.sendToServer(new EnergyWeaponMakePackets.Make());
                this.cantCraftBlinkMs = 0;
            }
        ));
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        this.extractSelectingArea(graphics, mouseX, mouseY, a);
        this.extractCantCraftBlink(graphics);
    }

    protected void extractSelectingArea(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderingTooltip = null;
        if (this.menu.getRecipes().isEmpty()) return;
        for (int i = this.head; i < this.head + Math.min(this.menu.getRecipes().size() - this.head, 6); i++) {
            int x = this.leftPos + 7 + 18 * (i % 3);
            int y = this.topPos + 24 + 18 * ((i - this.head) / 3);

            RecipeHolder<EnergyWeaponMakeRecipe> recipe = ListUtil.safelyGet(this.menu.getRecipes(), i).orElse(null);
            if (recipe == null) continue;

            ItemStack willRender = recipe.value().result().create();

            int offsetV = 0;
            if (MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) {
                offsetV = 36;
                this.renderingTooltip = willRender;
            }

            boolean selected = false;
            if (this.menu.getSelectedIndex() == i) {
                offsetV = 18;
                selected = true;
            }

            graphics.blit(SharedTextures.SWITCH_TABLE_BUTTON, x, y, 0, offsetV, 18, 18, 18, 54);
            graphics.item(willRender, x + 1, y + (selected ? 1 : 0), (int) (partialTick * 100));
        }
    }

    private void extractCantCraftBlink(GuiGraphicsExtractor graphics) {
        if (this.menu.isCantCraft()) {
            this.menu.setCantCraft(false);
            this.cantCraftBlinkMs = System.currentTimeMillis();
        }
        float ms = System.currentTimeMillis() - this.cantCraftBlinkMs;
        if (ms > 1400) return;

        float subTick = ms % 350;
        float red = subTick / 350;
        if (Math.floor(ms / 350) % 2 == 1) red = 1 - red;
        for (int i = 0; i < 6; i++) {
            FilteredSlot slot = this.menu.getFilteredSlot(i);
            if (slot.isFilterEmpty()) return;
            if (!slot.canCraft()) {
                this.renderSingleCantCraftBlink(graphics, i, red);
            }
        }
    }

    private void renderSingleCantCraftBlink(GuiGraphicsExtractor graphics, int index, float alpha) {
        int x = this.leftPos + 88 + (index % 3) * 18;
        int y = this.topPos + 24 + (index / 2) * 18;
        graphics.fill(
            RenderPipelines.GUI,
            x,
            y,
            x + 18,
            y + 18,
            ARGB.colorFromFloat(alpha * 0.9F, 1, 0, 0)
        );
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int x, int y) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack stack = this.hoveredSlot.getItem();
            graphics.setTooltipForNextFrame(
                this.font,
                this.getTooltipFromContainerItem(stack),
                stack.getTooltipImage(),
                stack,
                x,
                y
            );
        } else if (this.renderingTooltip != null) {
            graphics.setTooltipForNextFrame(
                this.font,
                this.getTooltipFromContainerItem(this.renderingTooltip),
                this.renderingTooltip.getTooltipImage(),
                this.renderingTooltip,
                x,
                y
            );
        }
    }

    @Override
    protected void renderSlotContents(GuiGraphicsExtractor graphics, ItemStack stack, Slot slot, @Nullable String countString) {
        if (slot instanceof FilteredSlot filtered) {
            if (filtered.isFilterEmpty()) return;
            if (stack.isEmpty()) {
                int seed = slot.x + slot.y * this.getImageWidth();
                ItemStackTemplate[] stacks = filtered.getFilter().getItems();
                ItemStackTemplate display = stacks[(int) ((System.currentTimeMillis() / 1000) % stacks.length)];
                graphics.item(display.create(), slot.x, slot.y, seed);

                graphics.pose().pushMatrix();
                String s = String.valueOf(stack.getCount());
                graphics.pose().translate(0.0F, 0.0F);
                graphics.text(font, s, slot.x + 19 - 2 - font.width(s), slot.y + 6 + 3, 0xFF555555, true);
                graphics.pose().popMatrix();
                return;
            } else if (stack.getCount() < filtered.getFilter().count()) {
                int seed = slot.x + slot.y * this.getImageWidth();
                if (slot.isFake()) {
                    graphics.fakeItem(stack, slot.x, slot.y, seed);
                } else {
                    graphics.item(stack, slot.x, slot.y, seed);
                }
                graphics.pose().pushMatrix();
                String s = String.valueOf(stack.getCount());
                graphics.pose().translate(0.0F, 0.0F);
                graphics.text(font, s, slot.x + 19 - 2 - font.width(s), slot.y + 6 + 3, 0xFFFF5555, true);
                graphics.pose().popMatrix();
                return;
            }
        }
        super.renderSlotContents(graphics, stack, slot, countString);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(
            BACKGROUND,
            this.leftPos,
            this.topPos,
            0,
            0,
            this.getImageWidth(),
            this.getImageHeight(),
            this.getImageWidth(),
            this.getImageHeight()
        );

        if (this.menu.canScroll()) {
            int left = this.leftPos + 64;
            int top = this.topPos + 24;
            int down = top + 36;
            graphics.blit(
                SharedTextures.SWITCH_TABLE_SLIDER,
                left,
                top + (int) ((down - top - 12) * this.scrollable.getScrollOffs()),
                0,
                0,
                4,
                12,
                8,
                12
            );
        }
    }

    @Override
    public void resize(int width, int height) {
        this.scrollable.calculateScroll(this.head / 3);
        this.init(width, height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (event.button() == 0) {
            if (this.insideScrollbar(event.x(), event.y())) {
                this.scrollable.scrolling();
                return true;
            }
            for (int i = this.head; i < this.head + Math.min(this.menu.getRecipes().size() - this.head, 6); i++) {
                int x = this.leftPos + 7 + 18 * (i % 3);
                int y = this.topPos + 24 + 18 * ((i - this.head) / 3);

                if (!MathUtil.isInRange(event.x(), event.y(), x, y, x + 18, y + 18)) continue;
                if (this.menu.getSelectedIndex() == i) {
                    this.menu.setSelectedIndex(-1);
                    ClientPacketDistributor.sendToServer(new EnergyWeaponMakePackets.Select(-1));
                } else {
                    this.menu.setSelectedIndex(i);
                    ClientPacketDistributor.sendToServer(new EnergyWeaponMakePackets.Select(i));
                }
            }
        }

        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.scrollable.isScrolling()) {
            this.scrollable.notScrolling();
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.scrollable.isScrolling()) {
            int top = this.topPos + 24;
            this.scrollable.scrollOnDrag(12, event.y(), top, top + 36);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.scrollable.canScroll()) {
            return false;
        } else {
            this.scrollable.scrollOnScroll(scrollY / 1.2);
            return true;
        }
    }

    protected boolean insideScrollbar(double mouseX, double mouseY) {
        int left = this.leftPos + 64;
        int top = this.topPos + 24;
        int right = left + 4;
        int down = top + 36;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, down);
    }
}
