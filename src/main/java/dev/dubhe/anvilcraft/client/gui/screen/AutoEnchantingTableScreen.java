package dev.dubhe.anvilcraft.client.gui.screen;

import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Scrollable;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.block.workstation.AutoEnchantingTableBlock;
import dev.dubhe.anvilcraft.client.gui.component.FluidDisplayWidget;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.AutoEnchantingTableMenu;
import dev.dubhe.anvilcraft.network.AutoEnchantingTableSyncPacket;
import dev.dubhe.anvilcraft.util.TickDebouncer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class AutoEnchantingTableScreen extends AbstractContainerScreen<AutoEnchantingTableMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("machine", "auto_enchanting_table");

    private final IntList filteredIndexes = new IntArrayList();
    private final Scrollable scrollable = new Scrollable() {
        @Override
        public int row() {
            return 2;
        }

        @Override
        public int column() {
            return 5;
        }

        @Override
        public int size() {
            return AutoEnchantingTableScreen.this.filteredIndexes.size();
        }

        @Override
        public void setHead(int head) {
            AutoEnchantingTableScreen.this.head = head;
        }
    };
    private int head = 0;
    @Nullable
    private EditBox editBox;
    @Nullable
    private TickDebouncer editBoxTickDebouncer;
    private int errorCooldown = 0;
    private @Nullable ItemStack renderingTooltipEnchantedBook;
    private ItemStack finishItem = ItemStack.EMPTY;

    public AutoEnchantingTableScreen(AutoEnchantingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        menu.getBlockEntity().registerUpdateListener(this::rebuildFilter);
        this.refreshFinishItem();
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.addRenderableWidget(
            new FluidDisplayWidget(
                this.leftPos + 151, this.topPos + 16,
                18, 56,
                this.menu.getBlockEntity().getFluidHandler(),
                (fluidHandler) -> Component.translatable(
                    "screen.anvilcraft.auto_enchanting_table.fluid_display",
                    fluidHandler.getAmountAsInt(0) + "/" + fluidHandler.getCapacityAsInt(0, FluidResource.EMPTY)
                )
            )
        );
        this.editBox = this.addRenderableWidget(
            new EditBox(this.font, this.leftPos + 46, this.topPos + 17, 99, 12, Component.empty())
        );
        this.editBoxTickDebouncer = new TickDebouncer(20, this::rebuildFilter);
        this.editBox.setResponder(_ -> Objects.requireNonNull(this.editBoxTickDebouncer).trigger());
        this.rebuildFilter();
    }

    private void rebuildFilter() {
        this.filteredIndexes.clear();
        String enchantmentName = this.editBox == null ? "" : this.editBox.getValue();
        List<Holder<Enchantment>> enchantments = this.menu.getEnchantmentList();
        for (int i = 0; i < enchantments.size(); i++) {
            Holder<Enchantment> enchantment = enchantments.get(i);
            if (enchantmentName.isBlank() || enchantment.value().description().getString().contains(enchantmentName)) {
                this.filteredIndexes.add(i);
            }
        }
        this.scrollable.reset();
    }

    private void refreshFinishItem() {
        ItemStack itemStack = this.menu.getBlockEntity().getItems().getFirst().copyWithCount(1);
        if (itemStack.isEmpty()) {
            this.finishItem = ItemStack.EMPTY;
            return;
        }
        ItemStack enchantedBook = Items.ENCHANTED_BOOK.getDefaultInstance().copyWithCount(1);
        for (int selectedIndex : this.menu.getSelectedIndexes()) {
            ListUtil.safelyGet(this.menu.getEnchantmentList(), selectedIndex).ifPresent(
                enchantment -> enchantedBook.enchant(enchantment, enchantment.value().getMaxLevel())
            );
        }
        AutoEnchantingTableBlockEntity.applyEnchantment(itemStack, enchantedBook);
        this.finishItem = itemStack.copyWithCount(1);
    }

    private int getBookShelf(Level level, BlockPos pos) {
        float bookcases = 0;
        for (BlockPos offset : AutoEnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                bookcases += level.getBlockState(pos.offset(offset)).getEnchantPowerBonus(level, pos.offset(offset));
            }
        }
        return (int) bookcases;
    }

    private boolean canSelect(int index) {
        Holder<Enchantment> enchantment = ListUtil.safelyGet(this.menu.getEnchantmentList(), index).orElse(null);
        if (enchantment == null) return false;
        for (int selectedIndex : this.menu.getSelectedIndexes()) {
            Holder<Enchantment> selected = ListUtil.safelyGet(this.menu.getEnchantmentList(), selectedIndex).orElse(null);
            if (selected != null && !Enchantment.areCompatible(enchantment, selected)) {
                return false;
            }
        }
        int totalLevel = enchantment.value().getMaxLevel();
        for (int selectedIndex : this.menu.getSelectedIndexes()) {
            Holder<Enchantment> selected = ListUtil.safelyGet(this.menu.getEnchantmentList(), selectedIndex).orElse(null);
            if (selected != null) {
                totalLevel += selected.value().getMaxLevel();
            }
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return false;
        return totalLevel <= this.getBookShelf(level, this.menu.getBlockEntity().getBlockPos())
            && totalLevel * 400 <= this.menu.getBlockEntity().getFluidHandler().getCapacityAsInt(0, FluidResource.EMPTY);
    }

    @Override
    protected void containerTick() {
        this.renderingTooltipEnchantedBook = null;
        if (this.editBoxTickDebouncer != null) {
            this.editBoxTickDebouncer.tick();
        }
        if (this.errorCooldown > 0) {
            this.errorCooldown--;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            if (this.insideScrollbar(event.x(), event.y())) {
                this.scrollable.scrolling();
                return true;
            }
            for (int i = this.head; i < this.head + Math.min(this.filteredIndexes.size() - this.head, 10); i++) {
                int x = this.leftPos + 47 + 18 * (i % 5);
                int y = this.topPos + 32 + 18 * ((i - this.head) / 5);

                if (!MathUtil.isInRange(event.x(), event.y(), x, y, x + 18, y + 18)) continue;

                int index = this.filteredIndexes.getInt(i);
                if (this.menu.getSelectedIndexes().contains(index)) {
                    this.menu.unselect(index);
                    ClientPacketDistributor.sendToServer(new AutoEnchantingTableSyncPacket(index, false));
                } else if (this.canSelect(index)) {
                    this.menu.select(index);
                    ClientPacketDistributor.sendToServer(new AutoEnchantingTableSyncPacket(index, true));
                } else {
                    this.errorCooldown = 80;
                }
                this.refreshFinishItem();
                return true;
            }
        } else if (event.button() == 1) {
            if (this.editBox != null && this.editBox.isMouseOver(event.x(), event.y())) {
                this.editBox.setValue("");
                this.rebuildFilter();
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.scrollable.isScrolling()) {
            int top = this.topPos + 32;
            this.scrollable.scrollOnDrag(12, event.y(), top, top + 36);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.scrollable.canScroll()) {
            return false;
        } else {
            this.scrollable.scrollOnScroll(scrollY / 1.2);
            return true;
        }
    }

    protected boolean insideScrollbar(double mouseX, double mouseY) {
        int left = this.leftPos + 140;
        int top = this.topPos + 32;
        int right = left + 4;
        int down = top + 36;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, down);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        this.extractEnchantmentSelectingArea(graphics, mouseX, mouseY, a);
        if (this.errorCooldown > 0) {
            graphics.fill(this.leftPos + 47, this.topPos + 32, this.leftPos + 137, this.topPos + 68, ARGB.color(128, 255, 0, 0));
        }
        if (this.menu.getBlockEntity().getItems().get(1).isEmpty() && !this.finishItem.isEmpty()) {
            graphics.item(this.finishItem, this.leftPos + 7, this.topPos + 52);
            graphics.fill(this.leftPos + 7, this.topPos + 52, this.leftPos + 23, this.topPos + 68, 0x99777777);
        }
    }

    protected void extractEnchantmentSelectingArea(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderingTooltipEnchantedBook = null;
        if (this.filteredIndexes.isEmpty()) return;
        for (int i = this.head; i < this.head + Math.min(this.filteredIndexes.size() - this.head, 10); i++) {
            int x = this.leftPos + 47 + 18 * (i % 5);
            int y = this.topPos + 32 + 18 * ((i - this.head) / 5);

            Holder<Enchantment> enchantment = ListUtil
                .safelyGet(this.menu.getEnchantmentList(), this.filteredIndexes.getInt(i))
                .orElse(null);
            if (enchantment == null) continue;

            ItemStack willRender = EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, enchantment.value().getMaxLevel()));

            int offsetV = 0;
            if (MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) {
                offsetV = 36;
                this.renderingTooltipEnchantedBook = willRender;
            }

            boolean selected = false;
            if (this.menu.getSelectedIndexes().contains(this.filteredIndexes.getInt(i))) {
                offsetV = 18;
                selected = true;
            }

            graphics.blit(RenderPipelines.GUI_TEXTURED, SharedTextures.SWITCH_TABLE_BUTTON, x, y, 0, offsetV, 18, 18, 18, 54);
            graphics.item(willRender, x + 1, y + (selected ? 1 : 0), (int) (partialTick * 100));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            AutoEnchantingTableScreen.BACKGROUND,
            this.leftPos,
            this.topPos,
            0,
            0,
            this.getImageWidth(),
            this.getImageHeight(),
            256,
            256
        );
        if (this.scrollable.canScroll()) {
            int left = this.leftPos + 140;
            int top = this.topPos + 32;
            int down = top + 36;
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
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
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        int x = (this.imageWidth - this.font.width(this.title)) / 2;
        graphics.text(this.font, this.title, x, 2, -12566464, false);
        if (this.errorCooldown > 0) {
            MutableComponent text = Component.translatable("screen.anvilcraft.auto_enchanting_table.out_of_limit");
            x = (this.imageWidth - this.font.width(text)) / 2;
            graphics.text(this.font, text, x, -15, ARGB.color(255, 0, 0), false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack outputTooltipItem = this.getOutputTooltipItem();
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem() && this.hoveredSlot.index != 37) {
            ItemStack item = this.hoveredSlot.getItem();
            if (
                this.menu.getCarried().isEmpty()
                    || item.getTooltipImage()
                    .map(ClientTooltipComponent::create)
                    .map(ClientTooltipComponent::showTooltipWithItemInHand)
                    .orElse(false)
            ) {
                graphics.setTooltipForNextFrame(
                    this.font,
                    this.getTooltipFromContainerItem(item),
                    item.getTooltipImage(),
                    item,
                    mouseX,
                    mouseY,
                    item.get(DataComponents.TOOLTIP_STYLE)
                );
            }
        } else if (this.renderingTooltipEnchantedBook != null) {
            graphics.setTooltipForNextFrame(
                this.font,
                this.getTooltipFromContainerItem(this.renderingTooltipEnchantedBook),
                this.renderingTooltipEnchantedBook.getTooltipImage(),
                this.renderingTooltipEnchantedBook,
                mouseX,
                mouseY,
                this.renderingTooltipEnchantedBook.get(DataComponents.TOOLTIP_STYLE)
            );
        } else if (!outputTooltipItem.isEmpty() && this.isHovering(7, 52, 16, 16, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(
                this.font,
                this.getTooltipFromContainerItem(outputTooltipItem),
                outputTooltipItem.getTooltipImage(),
                outputTooltipItem,
                mouseX,
                mouseY,
                outputTooltipItem.get(DataComponents.TOOLTIP_STYLE)
            );
        }
        if (this.isHovering(151, 16, 18, 56, mouseX, mouseY)) {
            graphics.tooltip(
                Minecraft.getInstance().font,
                List.of(
                    ClientTooltipComponent.create(
                        Component.translatable(
                            "screen.anvilcraft.auto_enchanting_table.fluid_display",
                            this.menu.getBlockEntity().getFluidHandler().getAmountAsInt(0)
                                + "/"
                                + this.menu.getBlockEntity().getFluidHandler().getCapacityAsInt(0, FluidResource.EMPTY)
                        ).getVisualOrderText()
                    )
                ),
                mouseX,
                mouseY,
                DefaultTooltipPositioner.INSTANCE,
                null
            );
        }
    }

    /// 输出栏产物 tooltip：引物模式下显示附魔，无引物时不显示附魔
    private ItemStack getOutputTooltipItem() {
        ItemStack item = this.menu.getSlot(37).getItem();
        if (item.isEmpty()) {
            item = this.finishItem;
        }
        if (item.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (this.menu.getPrologueSlot().getItem().isEmpty()) {
            item = item.copy();
            item.remove(DataComponents.ENCHANTMENTS);
            item.remove(DataComponents.STORED_ENCHANTMENTS);
        }
        return item;
    }

    @Override
    public void resize(int width, int height) {
        this.scrollable.calculateScroll(this.head / 5);
        this.init(width, height);
    }
}
