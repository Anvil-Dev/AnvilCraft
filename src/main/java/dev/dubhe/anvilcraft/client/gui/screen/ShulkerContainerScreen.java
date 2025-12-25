package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.client.gui.component.sc.overlay.BaseOverlay;
import dev.dubhe.anvilcraft.client.gui.component.sc.overlay.MainOverlay;
import dev.dubhe.anvilcraft.client.gui.component.sc.overlay.widget.MainSlots;
import dev.dubhe.anvilcraft.client.util.RenderUtil;
import dev.dubhe.anvilcraft.inventory.ShulkerContainerMenu;
import dev.dubhe.anvilcraft.inventory.component.sc.ShulkerContainerSlot;
import dev.dubhe.anvilcraft.network.multiple.ShulkerContainerPackets;
import dev.dubhe.anvilcraft.network.split.PacketSplitter;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
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

import java.util.Comparator;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ShulkerContainerScreen extends AbstractContainerScreen<ShulkerContainerMenu> {
    public static final Predicate<UnlimitedItemStack> ITEM_FILTER = ShulkerContainerScreen::shouldDisplay;
    public static final Comparator<UnlimitedItemStack> ITEM_SORTER = ShulkerContainerScreen::sortUnlimitedStack;
    public static EditBox searching;
    public static SearchMode searchMode = SearchMode.CLEAR;
    public static SortMode sortMode = SortMode.COUNT;
    public static SortOrderMode sortOrderMode = SortOrderMode.SEQUENTIAL;
    public static NbtDisplayMode nbtDisplayMode = NbtDisplayMode.UNFOLD;

    private BaseOverlay overlay;
    private int listenerIndex = -1;
    public MainSlots slots;

    private int lastClickStorageHash;
    private boolean isWaitingServerSync = true;

    public ShulkerContainerScreen(ShulkerContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 300;
        this.imageHeight = 222;
        this.titleLabelX = 111;
        this.titleLabelY = 7;
        this.inventoryLabelX = 111;
        this.inventoryLabelY = 128;

        if (this.menu.isWaitingServerSync()) {
            PacketDistributor.sendToServer(new ShulkerContainerPackets.IdSync(menu.blockEntity.getBlockPos()));
        }
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        if (!this.isWaitingServerSync() && this.listenerIndex != -1) this.menu.storage.removeSyncListener(this.listenerIndex);

        this.overlay = this.addWidget(this.overlay == null ? new MainOverlay(this) : this.overlay.recreate());
        if (!this.isWaitingServerSync()) this.listenerIndex = this.menu.storage.addSyncListener(this.overlay);
        if (this.overlay.hasSlots()) {
            this.slots = this.addRenderableWidget(new MainSlots(this));
        } else {
            this.slots = null;
            this.menu.removeContainerSlots();
        }

        if (!this.isWaitingServerSync()) {
            this.lastClickStorageHash = this.menu.storage.hashCode();
        }

        this.reorder();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.overlay != null) this.overlay.refreshTooltip(mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (this.overlay != null) this.overlay.renderWidget(graphics, mouseX, mouseY, partialTick);
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

        if (this.isWaitingServerSync()) return;
        int storageHash = this.menu.storage.hashCode();
        if (this.lastClickStorageHash != storageHash) {
            this.reorder();
            this.lastClickStorageHash = storageHash;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.slots != null && this.slots.scrollable.isScrolling()) {
            int top = this.topPos + 18;
            int bottom = top + 112;
            this.slots.scrollable.scrollOnDrag(15F, mouseY, top, bottom);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.slots != null) this.slots.scrollable.notScrolling();

        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected boolean insideScrollbar(double mouseX, double mouseY) {
        if (this.slots == null) return false;
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
        if (this.overlay != null) this.overlay.onClose();
        if (this.isWaitingServerSync()) return;
        this.menu.storage.removeSyncListener(this.listenerIndex);
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

    public void changeOverlay(BaseOverlay overlay) {
        this.menu.storage.removeSyncListener(this.listenerIndex);
        this.overlay.onClose();
        this.overlay = overlay;
        this.init();
    }

    public void reorder() {
        if (this.slots == null) return;
        this.slots.scrollable.scrollTo();
    }

    public void setSearchMode(SearchMode mode) {
        ShulkerContainerScreen.searchMode = mode;
        this.reorder();
    }

    public void setSortMode(SortMode mode) {
        ShulkerContainerScreen.sortMode = mode;
        this.reorder();
    }

    public void setSortOrderMode(SortOrderMode mode) {
        ShulkerContainerScreen.sortOrderMode = mode;
        this.reorder();
    }

    public void setNbtDisplayMode(NbtDisplayMode mode) {
        ShulkerContainerScreen.nbtDisplayMode = mode;
        this.reorder();
    }
    
    public boolean isWaitingServerSync() {
        if (!this.isWaitingServerSync) return false;
        var result = this.menu.isWaitingServerSync();
        if (!result && this.isWaitingServerSync) {
            if (this.overlay != null && this.listenerIndex == -1) {
                this.overlay.whenSynced(this.menu.storage);
                this.menu.storage.addSyncListener(this.overlay);
            }
        }
        return this.isWaitingServerSync = result;
    }

    public enum SearchMode {
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

    public enum SortMode {
        COUNT,
        MOD,
        NAME,
        ;

        public Component getTooltip() {
            return Component.translatable("screen.anvilcraft.shulker_container.sort." + this.name().toLowerCase(Locale.ROOT));
        }
    }

    public enum SortOrderMode {
        SEQUENTIAL,
        REVERSE,
        ;

        public Component getTooltip() {
            return Component.translatable("screen.anvilcraft.shulker_container.sort_order." + this.name().toLowerCase(Locale.ROOT));
        }
    }

    public enum NbtDisplayMode {
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

    public int getTitleLabelX() {
        return this.titleLabelX;
    }

    public int getTitleLabelY() {
        return this.titleLabelY;
    }

    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget) {
        return super.addRenderableWidget(widget);
    }
}
