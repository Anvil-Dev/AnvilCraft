package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.api.container.ContainerStorage;
import dev.dubhe.anvilcraft.api.container.category.FilterCategory;
import dev.dubhe.anvilcraft.api.container.category.provider.CategoryProvider;
import dev.dubhe.anvilcraft.client.gui.component.SwitchableButton;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.gui.component.shulkercontainer.CategoryList;
import dev.dubhe.anvilcraft.client.util.RegistryUtil;
import dev.dubhe.anvilcraft.client.util.RenderUtil;
import dev.dubhe.anvilcraft.constant.TextureConstants;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.inventory.ShulkerContainerMenu;
import dev.dubhe.anvilcraft.inventory.component.ShulkerContainerSlot;
import dev.dubhe.anvilcraft.network.ShulkerContainerPackets;
import dev.dubhe.anvilcraft.network.split.PacketSplitter;
import dev.dubhe.anvilcraft.util.Scrollable;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ShulkerContainerScreen extends AbstractContainerScreen<ShulkerContainerMenu> {
    private static final Predicate<UnlimitedItemStack> ITEM_FILTER = ShulkerContainerScreen::shouldDisplay;
    private static final Comparator<UnlimitedItemStack> ITEM_SORTER = ShulkerContainerScreen::sortUnlimitedStack;
    private static EditBox searching;
    private static SearchMode searchMode = SearchMode.CLEAR;
    private static SortMode sortMode = SortMode.COUNT;
    private static SortOrderMode sortOrderMode = SortOrderMode.SEQUENTIAL;
    private static NbtDisplayMode nbtDisplayMode = NbtDisplayMode.UNFOLD;

    private Overlay overlay = new CategoryOverlay();
    private MainSlots slots;

    private int lastClickStorageHash;

    public ShulkerContainerScreen(ShulkerContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 300;
        this.imageHeight = 222;
        this.titleLabelX = 111;
        this.titleLabelY = 7;
        this.inventoryLabelX = 111;
        this.inventoryLabelY = 128;

        if (menu.isWaitingServerSync()) {
            PacketDistributor.sendToServer(new ShulkerContainerPackets.IdSync(menu.blockEntity.getBlockPos()));
        }
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.overlay = this.addWidget(this.overlay.recreate());
        if (this.overlay.hasSlots()) {
            this.slots = this.addRenderableWidget(new MainSlots());
        } else {
            this.slots = null;
            this.menu.removeContainerSlots();
        }

        if (!this.menu.isWaitingServerSync()) {
            this.lastClickStorageHash = this.menu.storage.hashCode();
        }

        this.reorder();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.overlay.refreshTooltip(mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        var title = this.title.getVisualOrderText();
        guiGraphics.drawString(this.font, title, this.titleLabelX + font.width(title) / 2, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(
            TextureConstants.SHULKER_CONTAINER_BG,
            this.leftPos,
            this.topPos,
            0,
            0,
            300,
            222,
            512,
            256
        );
        this.overlay.renderWidget(graphics, mouseX, mouseY, partialTick);

        if (this.menu.isWaitingServerSync()) {
            int minX = this.leftPos + 6;
            int minY = this.topPos + 48;
            int maxX = minX + 94;
            int maxY = minY + 142;
            graphics.fill(minX, minY, maxX, maxY, 0x44000000); // 类别列表
            minX = this.leftPos + 113;
            minY = this.topPos + 17;
            maxX = minX + 162;
            maxY = minY + 108;
            graphics.fill(minX, minY, maxX, maxY, 0x44000000); // 槽位
            graphics.drawCenteredString(
                this.font,
                Component.translatable("screen.anvilcraft.shulker_container.waiting_sync"),
                minX + 81,
                minY + 50,
                0xEE2222
            );
        }
    }

    @Override
    protected void renderSlotContents(GuiGraphics guiGraphics, ItemStack itemstack, Slot slot, @Nullable String countString) {
        int i = slot.x;
        int j = slot.y;
        int j1 = slot.x + slot.y * this.imageWidth;
        guiGraphics.renderFakeItem(itemstack, i, j, j1);

        if (slot instanceof ShulkerContainerSlot scSlot) {
            RenderUtil.renderItemDecorations(guiGraphics, this.font, scSlot, scSlot.getUnlimitedItem(), i, j, countString);
        } else {
            guiGraphics.renderItemDecorations(this.font, itemstack, i, j, countString);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.insideScrollbar(mouseX, mouseY)) {
            this.slots.scrollable.scrolling();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        super.slotClicked(slot, slotId, mouseButton, type);

        if (this.menu.isWaitingServerSync()) return;
        int storageHash = this.menu.storage.hashCode();
        if (this.lastClickStorageHash != storageHash) {
            this.reorder();
            this.lastClickStorageHash = storageHash;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.slots.scrollable.isScrolling()) {
            int top = this.topPos + 18;
            int bottom = top + 112;
            this.slots.scrollable.scrollOnDrag(15F, mouseY, top, bottom);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) this.slots.scrollable.notScrolling();

        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected boolean insideScrollbar(double mouseX, double mouseY) {
        int left = this.leftPos + 280;
        int top = this.topPos + 18;
        int right = left + 14;
        int bottom = top + 112;
        return mouseX >= (double) left
               && mouseY >= (double) top
               && mouseX < (double) right
               && mouseY < (double) bottom;
    }

    @Override
    public void onClose() {
        super.onClose();
        if (this.menu.isWaitingServerSync()) return;
        // noinspection DataFlowIssue - For Minecraft.getInstance().getConnection().registryAccess() - 此时已有Connection
        PacketSplitter.INSTANCE.split(
            ShulkerContainerPackets.StorageSync.TYPE,
            ShulkerContainerPackets.StorageSync.STREAM_CODEC,
            new ShulkerContainerPackets.StorageSync(this.menu.storage),
            Minecraft.getInstance().getConnection().registryAccess(),
            PacketDistributor::sendToServer
        );
        PacketDistributor.sendToServer(new ShulkerContainerPackets.ScreenClose(this.menu.blockEntity.getBlockPos()));
    }

    protected void changeOverlay(Overlay overlay) {
        this.overlay = overlay;
        this.init();
    }

    protected void reorder() {
        if (this.menu.isWaitingServerSync() || this.slots == null) return;
        this.slots.scrollable.scrollTo();
    }

    protected void setSearchMode(SearchMode mode) {
        ShulkerContainerScreen.searchMode = mode;
        this.reorder();
    }

    protected void setSortMode(SortMode mode) {
        ShulkerContainerScreen.sortMode = mode;
        this.reorder();
    }

    protected void setSortOrderMode(SortOrderMode mode) {
        ShulkerContainerScreen.sortOrderMode = mode;
        this.reorder();
    }

    protected void setNbtDisplayMode(NbtDisplayMode mode) {
        ShulkerContainerScreen.nbtDisplayMode = mode;
        this.reorder();
    }

    protected enum SearchMode {
        CLEAR(text -> text.withStyle(ChatFormatting.RED)),
        RETENTION(text -> text.withStyle(ChatFormatting.GREEN)),
        ;

        private final Consumer<MutableComponent> styler;

        SearchMode(Consumer<MutableComponent> styler) {
            this.styler = styler;
        }

        public Component getTooltip() {
            var tooltip = Component.translatable("screen.anvilcraft.shulker_container.search." + this.name().toLowerCase(Locale.ROOT));
            this.styler.accept(tooltip);
            return tooltip;
        }
    }

    protected enum SortMode {
        COUNT,
        MOD,
        NAME,
        ;

        public Component getTooltip() {
            return Component.translatable("screen.anvilcraft.shulker_container.sort." + this.name().toLowerCase(Locale.ROOT));
        }
    }

    protected enum SortOrderMode {
        SEQUENTIAL,
        REVERSE,
        ;

        public Component getTooltip() {
            return Component.translatable("screen.anvilcraft.shulker_container.sort_order." + this.name().toLowerCase(Locale.ROOT));
        }
    }

    protected enum NbtDisplayMode {
        UNFOLD(text -> text.withStyle(ChatFormatting.RED)),
        FOLD(text -> text.withStyle(ChatFormatting.GREEN)),
        ;

        private final Consumer<MutableComponent> styler;

        NbtDisplayMode(Consumer<MutableComponent> styler) {
            this.styler = styler;
        }

        public Component getTooltip() {
            var tooltip = Component.translatable("screen.anvilcraft.shulker_container.nbt." + this.name().toLowerCase(Locale.ROOT));
            this.styler.accept(tooltip);
            return tooltip;
        }
    }

    protected static boolean shouldDisplay(UnlimitedItemStack stack) {
        String searching = ShulkerContainerScreen.searching.getValue();
        if (searching.startsWith("@")) {
            // mod mode
            return BuiltInRegistries.ITEM.getKey(stack.getStack().getItem()).getNamespace().contains(searching.substring(1));
        } else if (searching.startsWith("#")) {
            // tag mode
            String tagName = searching.substring(1);
            return BuiltInRegistries.ITEM.getTagNames()
                .filter(tag -> tag.location().getPath().contains(tagName))
                .anyMatch(tag -> stack.getStack().is(tag));
        } else if (searching.startsWith("$")) {
            // id mode
            return BuiltInRegistries.ITEM.getKey(stack.getStack().getItem()).getPath().contains(searching.substring(1));
        } else {
            // name mode
            return stack.getStack().getDisplayName().getString().contains(searching);
        }
    }

    protected static int sortUnlimitedStack(UnlimitedItemStack stack1, UnlimitedItemStack stack2) {
        int side = ShulkerContainerScreen.sortOrderMode == SortOrderMode.SEQUENTIAL ? 1 : -1;
        return switch (ShulkerContainerScreen.sortMode) {
            case COUNT -> {
                // countCheck
                int result = stack1.getCount() - stack2.getCount();
                if (result != 0) yield result * side;

                ResourceLocation id1 = BuiltInRegistries.ITEM.getKey(stack1.getStack().getItem());
                ResourceLocation id2 = BuiltInRegistries.ITEM.getKey(stack2.getStack().getItem());
                modCheck: {
                    String mod1 = id1.getNamespace();
                    String mod2 = id2.getNamespace();
                    boolean mod1IsMc = mod1.equals("minecraft");
                    boolean mod2IsMc = mod2.equals("minecraft");
                    if (mod1IsMc && mod2IsMc) break modCheck;
                    if (mod1IsMc || mod2IsMc) yield mod1IsMc ? 1 : -1;
                    result = mod1.compareTo(mod2);
                    if (result != 0) yield result * side;
                }

                // nameCheck
                String name1 = stack1.getStack().getDisplayName().getString();
                String name2 = stack2.getStack().getDisplayName().getString();
                result = name1.compareTo(name2);
                if (result != 0) yield result * side;

                // idCheck
                String path1 = id1.getPath();
                String path2 = id2.getPath();
                yield path1.compareTo(path2) * side;
            }
            case MOD -> {
                ResourceLocation id1 = BuiltInRegistries.ITEM.getKey(stack1.getStack().getItem());
                ResourceLocation id2 = BuiltInRegistries.ITEM.getKey(stack2.getStack().getItem());
                modCheck: {
                    String mod1 = id1.getNamespace();
                    String mod2 = id2.getNamespace();
                    boolean mod1IsMc = mod1.equals("minecraft");
                    boolean mod2IsMc = mod2.equals("minecraft");
                    if (mod1IsMc && mod2IsMc) break modCheck;
                    if (mod1IsMc || mod2IsMc) yield mod1IsMc ? -1 : 1;
                    int result = mod1.compareTo(mod2);
                    if (result != 0) yield result * side;
                }

                // nameCheck
                String name1 = stack1.getStack().getDisplayName().getString();
                String name2 = stack2.getStack().getDisplayName().getString();
                int result = name1.compareTo(name2);
                if (result != 0) yield result * side;

                // idCheck
                String path1 = id1.getPath();
                String path2 = id2.getPath();
                result = path1.compareTo(path2);
                if (result != 0) yield result * side;

                // countCheck
                yield stack1.getCount() - stack2.getCount() * side;
            }
            case NAME -> {
                // nameCheck
                String name1 = stack1.getStack().getDisplayName().getString();
                String name2 = stack2.getStack().getDisplayName().getString();
                int result = name1.compareTo(name2);
                if (result != 0) yield result * side;

                ResourceLocation id1 = BuiltInRegistries.ITEM.getKey(stack1.getStack().getItem());
                ResourceLocation id2 = BuiltInRegistries.ITEM.getKey(stack2.getStack().getItem());
                modCheck: {
                    String mod1 = id1.getNamespace();
                    String mod2 = id2.getNamespace();
                    boolean mod1IsMc = mod1.equals("minecraft");
                    boolean mod2IsMc = mod2.equals("minecraft");
                    if (mod1IsMc && mod2IsMc) break modCheck;
                    if (mod1IsMc || mod2IsMc) yield mod1IsMc ? 1 : -1;
                    result = mod1.compareTo(mod2);
                    if (result != 0) yield result * side;
                }

                // idCheck
                String path1 = id1.getPath();
                String path2 = id2.getPath();
                result = path1.compareTo(path2);
                if (result != 0) yield result * side;

                // countCheck
                yield stack1.getCount() - stack2.getCount() * side;
            }
        };
    }

    protected abstract class Overlay extends AbstractWidget {
        public Overlay() {
            super(0, 0, 300, 222, Component.empty());
        }

        abstract Overlay recreate();

        abstract ResourceLocation bg();

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.blit(
                this.bg(),
                this.getGuiLeft(),
                this.getGuiTop(),
                0,
                0,
                106,
                222,
                512,
                256
            );
        }

        void refreshTooltip(int x, int y) {
        }

        void setTooltip(Component tooltip) {
            ShulkerContainerScreen.this.setTooltipForNextRenderPass(tooltip);
        }

        int getGuiLeft() {
            return ShulkerContainerScreen.this.getGuiLeft();
        }

        int getGuiTop() {
            return ShulkerContainerScreen.this.getGuiTop();
        }

        void whenSynced(ContainerStorage storage) {
        }

        Minecraft minecraft() {
            return ShulkerContainerScreen.this.minecraft;
        }

        ContainerStorage storage() {
            return ShulkerContainerScreen.this.menu.storage;
        }

        boolean hasSlots() {
            return true;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }

    private class MainOverlay extends Overlay {
        private final CategoryList categoryList;

        public MainOverlay() {
            if (ShulkerContainerScreen.searchMode == SearchMode.RETENTION) {
                ShulkerContainerScreen.searching = new EditBox(
                    ShulkerContainerScreen.this.font,
                    this.getGuiLeft() + 7,
                    this.getGuiTop() + 7,
                    92,
                    9,
                    ShulkerContainerScreen.searching,
                    Component.empty()
                );
            } else {
                ShulkerContainerScreen.searching = new EditBox(
                    ShulkerContainerScreen.this.font,
                    this.getGuiLeft() + 7,
                    this.getGuiTop() + 7,
                    92,
                    9,
                    Component.empty()
                );
            }
            ShulkerContainerScreen.searching.setBordered(false);
            ShulkerContainerScreen.searching.setTextShadow(false);
            ShulkerContainerScreen.searching.setCanLoseFocus(true);
            ShulkerContainerScreen.this.addRenderableWidget(ShulkerContainerScreen.searching);
            ShulkerContainerScreen.this.addRenderableWidget(new SwitchableButton(
                this.getGuiLeft() + 2,
                this.getGuiTop() + 23,
                24,
                20,
                List.of(
                    TextureConstants.SHULKER_CONTAINER_SEARCH_CLEAR,
                    TextureConstants.SHULKER_CONTAINER_SEARCH_RETENTION
                ),
                20,
                24,
                40,
                (button, i) -> ShulkerContainerScreen.this.setSearchMode(SearchMode.values()[i])
            ).setCurrent(ShulkerContainerScreen.searchMode.ordinal()));
            ShulkerContainerScreen.this.addRenderableWidget(new SwitchableButton(
                this.getGuiLeft() + 28,
                this.getGuiTop() + 23,
                24,
                20,
                List.of(
                    TextureConstants.SHULKER_CONTAINER_SORT_BY_NUMBER,
                    TextureConstants.SHULKER_CONTAINER_SORT_BY_MOD,
                    TextureConstants.SHULKER_CONTAINER_SORT_BY_NAME
                ),
                20,
                24,
                40,
                (button, i) -> ShulkerContainerScreen.this.setSortMode(SortMode.values()[i])
            ).setCurrent(ShulkerContainerScreen.sortMode.ordinal()));
            ShulkerContainerScreen.this.addRenderableWidget(new SwitchableButton(
                this.getGuiLeft() + 54,
                this.getGuiTop() + 23,
                24,
                20,
                List.of(
                    TextureConstants.SHULKER_CONTAINER_SEQUENTIAL_ORDER,
                    TextureConstants.SHULKER_CONTAINER_REVERSE_ORDER
                ),
                20,
                24,
                40,
                (button, i) -> ShulkerContainerScreen.this.setSortOrderMode(SortOrderMode.values()[i])
            ).setCurrent(ShulkerContainerScreen.sortOrderMode.ordinal()));
            ShulkerContainerScreen.this.addRenderableWidget(new SwitchableButton(
                this.getGuiLeft() + 80,
                this.getGuiTop() + 23,
                24,
                20,
                List.of(
                    TextureConstants.SHULKER_CONTAINER_NBT_UNFOLD,
                    TextureConstants.SHULKER_CONTAINER_NBT_FOLD
                ),
                20,
                24,
                40,
                (button, i) -> ShulkerContainerScreen.this.setNbtDisplayMode(NbtDisplayMode.values()[i])
            ).setCurrent(ShulkerContainerScreen.nbtDisplayMode.ordinal()));
            this.categoryList = ShulkerContainerScreen.this.addRenderableWidget(new CategoryList(
                this.getGuiLeft() + 7,
                this.getGuiTop() + 49,
                ShulkerContainerScreen.this.menu.storage,
                button -> ShulkerContainerScreen.this.reorder()
            ));
            ShulkerContainerScreen.this.addRenderableWidget(new TexturedButton(
                this.getGuiLeft() + 2,
                this.getGuiTop() + 198,
                102,
                20,
                TextureConstants.SHULKER_CONTAINER_UPGRADE,
                20,
                102,
                40,
                button -> {
                    // 打开升级界面
                }
            ));

            if (ShulkerContainerScreen.this.menu.isWaitingServerSync()) {
                this.categoryList.active = false;
                this.categoryList.visible = false;
            }
        }

        @Override
        Overlay recreate() {
            return new MainOverlay();
        }

        @Override
        ResourceLocation bg() {
            return TextureConstants.SHULKER_CONTAINER_BG;
        }

        @Override
        void whenSynced(ContainerStorage storage) {
            super.whenSynced(storage);
            this.categoryList.sync(storage);
        }

        @Override
        public void onClick(double mouseX, double mouseY, int button) {
            ShulkerContainerScreen.searching.setFocused(this.insideSearchBox(mouseX, mouseY));
            super.onClick(mouseX, mouseY, button);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            String oldSearching = ShulkerContainerScreen.searching.getValue();
            if (ShulkerContainerScreen.searching.keyPressed(keyCode, scanCode, modifiers)) {
                if (!Objects.equals(oldSearching, ShulkerContainerScreen.searching.getValue())) {
                    ShulkerContainerScreen.this.slots.scrollable.reset();
                    ShulkerContainerScreen.this.reorder();
                }

                return true;
            }
            return ShulkerContainerScreen.searching.isFocused()
                   && ShulkerContainerScreen.searching.isVisible()
                   && keyCode != 256
                   || super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        void refreshTooltip(int x, int y) {
            if (this.insideSearchModeButton(x, y)) {
                this.setTooltip(Component.translatable(
                    "screen.anvilcraft.shulker_container.search",
                    ShulkerContainerScreen.searchMode.getTooltip()
                ));
            } else if (this.insideSortModeButton(x, y)) {
                this.setTooltip(Component.translatable(
                    "screen.anvilcraft.shulker_container.sort",
                    ShulkerContainerScreen.sortMode.getTooltip()
                ));
            } else if (this.insideSortOrderModeButton(x, y)) {
                this.setTooltip(Component.translatable(
                    "screen.anvilcraft.shulker_container.sort_order",
                    ShulkerContainerScreen.sortMode.getTooltip()
                ));
            } else if (this.insideNbtDisplayModeButton(x, y)) {
                this.setTooltip(Component.translatable(
                    "screen.anvilcraft.shulker_container.nbt",
                    ShulkerContainerScreen.nbtDisplayMode.getTooltip()
                ));
            }
        }

        protected boolean insideSearchBox(double mouseX, double mouseY) {
            int left = this.getGuiLeft() + 7;
            int top = this.getGuiTop() + 7;
            int right = left + 92;
            int bottom = top + 9;
            return mouseX >= (double) left
                   && mouseY >= (double) top
                   && mouseX < (double) right
                   && mouseY < (double) bottom;
        }

        protected boolean insideSearchModeButton(double mouseX, double mouseY) {
            int left = this.getGuiLeft() + 2;
            int top = this.getGuiTop() + 23;
            int right = left + 24;
            int bottom = top + 20;
            return mouseX >= (double) left
                   && mouseY >= (double) top
                   && mouseX < (double) right
                   && mouseY < (double) bottom;
        }

        protected boolean insideSortModeButton(double mouseX, double mouseY) {
            int left = this.getGuiLeft() + 28;
            int top = this.getGuiTop() + 23;
            int right = left + 24;
            int bottom = top + 20;
            return mouseX >= (double) left
                   && mouseY >= (double) top
                   && mouseX < (double) right
                   && mouseY < (double) bottom;
        }

        protected boolean insideSortOrderModeButton(double mouseX, double mouseY) {
            int left = this.getGuiLeft() + 54;
            int top = this.getGuiTop() + 23;
            int right = left + 24;
            int bottom = top + 20;
            return mouseX >= (double) left
                   && mouseY >= (double) top
                   && mouseX < (double) right
                   && mouseY < (double) bottom;
        }

        protected boolean insideNbtDisplayModeButton(double mouseX, double mouseY) {
            int left = this.getGuiLeft() + 80;
            int top = this.getGuiTop() + 23;
            int right = left + 24;
            int bottom = top + 20;
            return mouseX >= (double) left
                   && mouseY >= (double) top
                   && mouseX < (double) right
                   && mouseY < (double) bottom;
        }
    }

    private class CategoryOverlay extends Overlay {
        private final List<CategoryProvider> enabledCategories;
        private final List<CategoryProvider> alternateCategories;
        private final TexturedButton addCategory;

        private int enabledHead = 0;
        private final Scrollable enabledScroll = new Scrollable() {
            @Override
            public int row() {
                return 10;
            }

            @Override
            public int column() {
                return 1;
            }

            @Override
            public int size() {
                return CategoryOverlay.this.enabledCategories.size();
            }

            @Override
            public void set(int targetIndex, int contentIndex) {
            }

            @Override
            public void setEmpty(int targetIndex) {
            }

            @Override
            public void scrollTo() {
                CategoryOverlay.this.enabledHead = this.getRowIndex();
            }
        };

        private int alternateHead = 0;
        private final Scrollable alternateScroll = new Scrollable() {
            @Override
            public int row() {
                return 6;
            }

            @Override
            public int column() {
                return 2;
            }

            @Override
            public int size() {
                return CategoryOverlay.this.enabledCategories.size() + 1;
            }

            @Override
            public void set(int targetIndex, int contentIndex) {
            }

            @Override
            public void setEmpty(int targetIndex) {
            }

            @Override
            public void scrollTo() {
                CategoryOverlay.this.alternateHead = this.getRowIndex() * 2;
                var addAvailable = CategoryOverlay.this.alternateHead == 0;
                CategoryOverlay.this.addCategory.active = addAvailable;
                CategoryOverlay.this.addCategory.visible = addAvailable;
            }
        };

        public CategoryOverlay() {
            super();
            this.enabledCategories = new ArrayList<>(this.storage().getCategories().getProviders());
            this.alternateCategories = ShulkerContainerScreen.this.minecraft.getConnection().registryAccess()
                .lookup(ModRegistries.CATEGORY_KEY)
                .orElseThrow()
                .listElementIds()
                .map(CategoryProvider::new)
                .filter(Predicate.not(this.enabledCategories::contains))
                .collect(Collectors.toCollection(ArrayList::new));

            ShulkerContainerScreen.this.addRenderableWidget(new TexturedButton(
                this.getGuiLeft() + 278,
                this.getGuiTop() + 139,
                18,
                20,
                TextureConstants.SHULKER_CONTAINER_CONFIRM,
                20,
                18,
                40,
                button -> {
                    this.storage().getClientCategories().applyProviders(this.enabledCategories);
                    ShulkerContainerScreen.this.changeOverlay(new MainOverlay());
                }
            ));
            ShulkerContainerScreen.this.addRenderableWidget(new TexturedButton(
                this.getGuiLeft() + 278,
                this.getGuiTop() + 161,
                18,
                20,
                TextureConstants.SHULKER_CONTAINER_CANCEL,
                20,
                18,
                40,
                button -> ShulkerContainerScreen.this.changeOverlay(new MainOverlay())
            ));

            this.addCategory = ShulkerContainerScreen.this.addRenderableWidget(new TexturedButton(
                this.getGuiLeft() + 113,
                this.getGuiTop() + 7,
                86,
                20,
                TextureConstants.SHULKER_CONTAINER_CATEGORY_ADD,
                20,
                86,
                40,
                button -> {
                }
            ));
        }

        @Override
        Overlay recreate() {
            return new CategoryOverlay();
        }

        @Override
        ResourceLocation bg() {
            return TextureConstants.SHULKER_CONTAINER_BG;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            for (int i = this.enabledHead; i < this.enabledCategories.size(); i++) {
                final CategoryProvider provider = this.enabledCategories.get(i);
                graphics.blit(
                    TextureConstants.SHULKER_CONTAINER_CATEGORY,
                    this.getGuiLeft() + 7,
                    this.getGuiTop() + 7 + i * 20,
                    0,
                    0,
                    86,
                    20,
                    86,
                    80
                );

                var pose = graphics.pose();
                pose.pushPose();
                pose.translate(this.getX(), this.getY(), 20);
                pose.scale(0.75f, 0.75f, 1);

                @SuppressWarnings("DataFlowIssue")
                var category = provider.get(() -> RegistryUtil.lookup(ModRegistries.CATEGORY_KEY));

                ItemStack icon = category.icon();
                int x = 4;
                int y = 4;
                graphics.renderFakeItem(icon, x, y);
                graphics.renderItemDecorations(this.minecraft().font, icon, x, y);

                pose.popPose();

                Component name = category.name();
                int left = this.getX() + 17;
                int top = this.getY() + 5;
                int width = this.minecraft().font.width(name);
                if (width < 65) { // 小于最大宽度，需要居中
                    graphics.drawCenteredString(this.minecraft().font, name, left + 32, top, 0xFFFFFFFF);
                } else if (width > 65) { // 大于最大宽度，需要左右横跳
                    graphics.drawScrollingString(this.minecraft().font, name, left, left + 65, top, 0xFFFFFFFF);
                } else { // 等于最大宽度，直接渲染
                    graphics.drawString(this.minecraft().font, name, left, top, 0xFFFFFFFF, false);
                }
            }


            for (int i = this.enabledHead; i <= this.enabledCategories.size(); i++) {
                if (i == 0) continue;
                final CategoryProvider provider = this.enabledCategories.get(i - 1);
                graphics.blit(
                    TextureConstants.SHULKER_CONTAINER_CATEGORY,
                    this.getGuiLeft() + 7,
                    this.getGuiTop() + 7 + i * 20,
                    0,
                    0,
                    86,
                    20,
                    86,
                    80
                );

                var pose = graphics.pose();
                pose.pushPose();
                pose.translate(this.getX(), this.getY(), 20);
                pose.scale(0.75f, 0.75f, 1);

                @SuppressWarnings("DataFlowIssue")
                var category = provider.get(() -> RegistryUtil.lookup(ModRegistries.CATEGORY_KEY));

                ItemStack icon = category.icon();
                int x = 4;
                int y = 4;
                graphics.renderFakeItem(icon, x, y);
                graphics.renderItemDecorations(this.minecraft().font, icon, x, y);

                pose.popPose();

                Component name = category.name();
                int left = this.getX() + 17;
                int top = this.getY() + 5;
                int width = this.minecraft().font.width(name);
                if (width < 65) { // 小于最大宽度，需要居中
                    graphics.drawCenteredString(this.minecraft().font, name, left + 32, top, 0xFFFFFFFF);
                } else if (width > 65) { // 大于最大宽度，需要左右横跳
                    graphics.drawScrollingString(this.minecraft().font, name, left, left + 65, top, 0xFFFFFFFF);
                } else { // 等于最大宽度，直接渲染
                    graphics.drawString(this.minecraft().font, name, left, top, 0xFFFFFFFF, false);
                }
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY, int button) {
            if (button == 0 && this.insideEnabledScrollbar(mouseX, mouseY)) {
                this.enabledScroll.scrolling();
                return;
            } else if (button == 0 && this.insideAlternateScrollbar(mouseX, mouseY)) {
                this.alternateScroll.scrolling();
                return;
            } else if (button == 0 && this.insideAddCategoryButton(mouseX, mouseY)) {
                var stack = ShulkerContainerScreen.this.menu.getCarried();
                if (!stack.has(ModComponents.FILTER_CONTENT)) return;
                this.alternateCategories.add(new CategoryProvider(new FilterCategory(stack)));
                return;
            }
            super.onClick(mouseX, mouseY, button);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            super.onDrag(mouseX, mouseY, dragX, dragY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (this.insideEnabledScrollbar(mouseX, mouseY)) {
                this.enabledScroll.scrollOnScroll(scrollY);
            } else if (this.insideAlternateScrollbar(mouseX, mouseY)) {
                this.alternateScroll.scrollOnScroll(scrollY);
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            this.enabledScroll.notScrolling();
            this.alternateScroll.notScrolling();
            super.onRelease(mouseX, mouseY);
        }

        @Override
        void refreshTooltip(int x, int y) {
            if (this.insideEnabled(x, y)) {
                this.setTooltip(Component.translatable("screen.anvilcraft.shulker_container.category.enabled"));
            } else if (this.insideAddCategoryButton(x, y)) {
                this.setTooltip(Component.translatable("screen.anvilcraft.shulker_container.category.add"));
            } else {
                var index = this.insideAlternate(x, y);
                if (index == -1) return;
                var isCustom = this.alternateCategories.get(index).isCustom();
                this.setTooltip(Component.translatable(
                    "screen.anvilcraft.shulker_container.category." + (isCustom ? "unremovable" : "removable")
                ));
            }
        }

        @Override
        boolean hasSlots() {
            return false;
        }

        protected boolean insideEnabled(double mouseX, double mouseY) {
            int left = this.getGuiLeft() + 7;
            int top = this.getGuiTop() + 7;
            int right = left + 86;
            int bottom = top + Math.min(this.enabledCategories.size() - this.enabledHead, 10) * 20;
            return mouseX >= (double) left
                   && mouseY >= (double) top
                   && mouseX < (double) right
                   && mouseY < (double) bottom;
        }

        protected boolean insideAddCategoryButton(double mouseX, double mouseY) {
            if (!this.addCategory.active) return false;
            int left = this.getGuiLeft() + 113;
            int top = this.getGuiTop() + 7;
            int right = left + 86;
            int bottom = top + 20;
            return mouseX >= (double) left
                   && mouseY >= (double) top
                   && mouseX < (double) right
                   && mouseY < (double) bottom;
        }

        protected int insideAlternate(double mouseX, double mouseY) {
            for (int i = 0; i < 10; i++) {
                int index = i + this.alternateHead + 1;
                if (index >= this.alternateCategories.size()) return -1;
                int left = this.getGuiLeft() + 113 + (i % 2) * 88;
                int right = left + 86;
                int top = this.getGuiTop() + 7 + i * 20;
                int bottom = top + 20;
                if (
                    mouseX >= (double) left
                    && mouseY >= (double) top
                    && mouseX < (double) right
                    && mouseY < (double) bottom
                ) {
                    return index;
                }
            }
            return -1;
        }

        protected boolean insideEnabledScrollbar(double mouseX, double mouseY) {
            if (!this.enabledScroll.canScroll()) return false;
            int left = this.getX() + 95;
            int top = this.getY() + 7;
            int right = left + 4;
            int bottom = top + 200;
            return mouseX >= (double) left
                   && mouseY >= (double) top
                   && mouseX < (double) right
                   && mouseY < (double) bottom;
        }

        protected boolean insideAlternateScrollbar(double mouseX, double mouseY) {
            if (!this.alternateScroll.canScroll()) return false;
            int left = this.getX() + 289;
            int top = this.getY() + 7;
            int right = left + 4;
            int bottom = top + 120;
            return mouseX >= (double) left
                   && mouseY >= (double) top
                   && mouseX < (double) right
                   && mouseY < (double) bottom;
        }
    }

    private abstract class OverlayWidget extends AbstractWidget {
        public OverlayWidget(int x, int y, int width, int height, Component message) {
            super(x, y, width, height, message);
        }

        @Override
        protected abstract void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
        
        public int getGuiLeft() {
            return ShulkerContainerScreen.this.getGuiLeft();
        }

        public int getGuiTop() {
            return ShulkerContainerScreen.this.getGuiTop();
        }

        public ContainerStorage storage() {
            return ShulkerContainerScreen.this.menu.storage;
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }

    private class MainSlots extends OverlayWidget {
        final Scrollable scrollable = new Scrollable() {
            @Override
            public int row() {
                return 6;
            }

            @Override
            public int column() {
                return 9;
            }

            @Override
            public int size() {
                return MainSlots.this.storage().getEntries().stackSize();
            }

            @Override
            public void set(int targetIndex, int contentIndex) {
            }

            @Override
            public void setEmpty(int targetIndex) {
            }

            @Override
            public void scrollTo() {
                Int2BooleanMap order = ShulkerContainerScreen.this.menu.storage.getOrder(
                    ITEM_FILTER,
                    ITEM_SORTER,
                    ShulkerContainerScreen.nbtDisplayMode == NbtDisplayMode.FOLD
                );
                ShulkerContainerScreen.this.menu.applyOrder(order, this.getScrollOffs());
                PacketDistributor.sendToServer(new ShulkerContainerPackets.ScreenSync(
                    ShulkerContainerScreen.this.menu.slotsOrder,
                    this.getScrollOffs()
                ));
            }
        };

        public MainSlots() {
            super(107, 0, 194, 133, Component.empty());
            ShulkerContainerScreen.this.menu.addContainerSlots();
            this.scrollable.scrollTo();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // 滚动条
            int itemSliderLeft = this.getGuiLeft() + 280;
            int itemSliderTop = this.getGuiTop() + 18;
            int itemSliderBottom = itemSliderTop + 112;
            if (this.scrollable.canScroll()) {
                ResourceLocation sliderTex = TextureConstants.SHULKER_CONTAINER_SLIDER_BIG;
                graphics.blitSprite(
                    sliderTex,
                    itemSliderLeft,
                    itemSliderTop + (int) ((float) (itemSliderBottom - itemSliderTop - 15) * this.scrollable.getScrollOffs()),
                    12,
                    15
                );
            }

            // 标题
            graphics.drawCenteredString(
                ShulkerContainerScreen.this.font,
                ShulkerContainerScreen.this.title,
                ShulkerContainerScreen.this.titleLabelX + 93,
                ShulkerContainerScreen.this.titleLabelY,
                0x404040
            );
        }
    }
}
