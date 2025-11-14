package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.client.gui.component.SwitchableButton;
import dev.dubhe.anvilcraft.client.util.RenderUtil;
import dev.dubhe.anvilcraft.constant.TextureConstants;
import dev.dubhe.anvilcraft.inventory.ShulkerContainerMenu;
import dev.dubhe.anvilcraft.inventory.component.ShulkerContainerSlot;
import dev.dubhe.anvilcraft.network.ShulkerContainerSyncPacket;
import dev.dubhe.anvilcraft.network.split.PacketSplitter;
import dev.dubhe.anvilcraft.util.stack.UnlimitedItemStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class ShulkerContainerScreen extends AbstractContainerScreen<ShulkerContainerMenu> {
    private static final Predicate<UnlimitedItemStack> ITEM_FILTER = ShulkerContainerScreen::shouldDisplay;
    private static final Comparator<UnlimitedItemStack> ITEM_SORTER = ShulkerContainerScreen::sortUnlimitedStack;
    private static EditBox searching;
    private static SearchMode searchMode = SearchMode.CLEAR;
    private static SortMode sortMode = SortMode.COUNT;
    private static SortOrderMode sortOrderMode = SortOrderMode.SEQUENTIAL;
    private static NbtDisplayMode nbtDisplayMode = NbtDisplayMode.UNFOLD;

    private static final Int2ObjectMap<IntSet> SLOTS_ORDER_CACHE = new Int2ObjectArrayMap<>(20);

    private boolean scrolling = false;
    private float scrollOffs = 0.0f;

    public ShulkerContainerScreen(ShulkerContainerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 300;
        this.imageHeight = 222;
        this.titleLabelX = 111;
        this.titleLabelY = 7;
        this.inventoryLabelX = 111;
        this.inventoryLabelY = 128;
    }

    @Override
    protected void init() {
        super.init();
        if (ShulkerContainerScreen.searchMode == SearchMode.RETENTION) {
            ShulkerContainerScreen.searching = new EditBox(
                this.font,
                this.getGuiLeft() + 7,
                this.getGuiTop() + 7,
                92,
                9,
                ShulkerContainerScreen.searching,
                Component.empty()
            );
        } else {
            ShulkerContainerScreen.searching = new EditBox(
                this.font,
                this.getGuiLeft() + 7,
                this.getGuiTop() + 7,
                92,
                9,
                Component.empty()
            );
        }
        ShulkerContainerScreen.searching.setBordered(false);
        ShulkerContainerScreen.searching.setTextShadow(false);
        ShulkerContainerScreen.searching.setResponder(it -> this.onSearchingOther());
        this.addRenderableWidget(ShulkerContainerScreen.searching);
        this.addRenderableWidget(new SwitchableButton(
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
            (button, i) -> this.setSearchMode(SearchMode.values()[i])
        ).setCurrent(ShulkerContainerScreen.searchMode.ordinal()));
        this.addRenderableWidget(new SwitchableButton(
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
            (button, i) -> this.setSortMode(SortMode.values()[i])
        ).setCurrent(ShulkerContainerScreen.sortMode.ordinal()));
        this.addRenderableWidget(new SwitchableButton(
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
            (button, i) -> this.setSortOrderMode(SortOrderMode.values()[i])
        ).setCurrent(ShulkerContainerScreen.sortOrderMode.ordinal()));
        this.addRenderableWidget(new SwitchableButton(
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
            (button, i) -> this.setNbtDisplayMode(NbtDisplayMode.values()[i])
        ).setCurrent(ShulkerContainerScreen.nbtDisplayMode.ordinal()));

        this.scrollOffs = 0.0f;
        this.reorder();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(
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

        int itemSliderLeft = this.leftPos + 280;
        int itemSliderTop = this.topPos + 18;
        int itemSliderBottom = itemSliderTop + 112;
        if (this.menu.canScroll()) {
            ResourceLocation sliderTex = TextureConstants.SHULKER_CONTAINER_SLIDER_BIG;
            guiGraphics.blitSprite(sliderTex, itemSliderLeft, itemSliderTop + (int)((float)(itemSliderBottom - itemSliderTop - 17) * this.scrollOffs), 12, 15);
        }
    }

    @Override
    protected void renderSlotContents(GuiGraphics guiGraphics, ItemStack itemstack, Slot slot, @Nullable String countString) {
        int i = slot.x;
        int j = slot.y;
        int j1 = slot.x + slot.y * this.imageWidth;
        guiGraphics.renderFakeItem(itemstack, i, j, j1);

        if (slot instanceof ShulkerContainerSlot scSlot) {
            RenderUtil.renderItemDecorations(guiGraphics, this.font, scSlot.getUnlimitedItem(), i, j, countString);
        } else {
            guiGraphics.renderItemDecorations(this.font, itemstack, i, j, countString);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.insideScrollbar(mouseX, mouseY)) {
            this.scrolling = this.menu.canScroll();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrolling) {
            int top = this.topPos + 18;
            int bottom = top + 112;
            this.scrollOffs = (float) ((mouseY - top - 7.5F) / (bottom - top - 15.0F));
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.menu.scrollTo(this.scrollOffs);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) this.scrolling = false;

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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.menu.canScroll()) return false;
        this.scrollOffs = this.menu.subtractInputFromScroll(this.scrollOffs, scrollY);
        this.menu.scrollTo(this.scrollOffs);
        return true;
    }

    @Override
    public void onClose() {
        super.onClose();
        //noinspection DataFlowIssue - For Minecraft.getInstance().getConnection().registryAccess() - 此时已有Connection
        PacketSplitter.INSTANCE.split(
            ShulkerContainerSyncPacket.TYPE,
            ShulkerContainerSyncPacket.STREAM_CODEC,
            new ShulkerContainerSyncPacket(this.menu.blockEntity.getUUID()),
            1640,
            Minecraft.getInstance().getConnection().registryAccess(),
            PacketDistributor::sendToServer
        );
    }

    protected void reorder() {
        record CacheKey(String text, SearchMode search, SortMode sort, SortOrderMode sortOrder, NbtDisplayMode nbt) {
        }
        int key = new CacheKey(
            ShulkerContainerScreen.searching.getValue(),
            ShulkerContainerScreen.searchMode,
            ShulkerContainerScreen.sortMode,
            ShulkerContainerScreen.sortOrderMode,
            ShulkerContainerScreen.nbtDisplayMode
        ).hashCode();
        if (!ShulkerContainerScreen.SLOTS_ORDER_CACHE.containsKey(key)) {
            ShulkerContainerScreen.SLOTS_ORDER_CACHE.put(key, this.menu.storage.getOrder(ITEM_FILTER, ITEM_SORTER));
        }
        this.scrollOffs = this.menu.applyOrder(ShulkerContainerScreen.SLOTS_ORDER_CACHE.get(key), this.scrollOffs);
    }

    protected void onSearchingOther() {
        this.scrollOffs = 0.0f;
        this.reorder();
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
        CLEAR,
        RETENTION
    }

    protected enum SortMode {
        COUNT,
        MOD,
        NAME
    }

    protected enum SortOrderMode {
        SEQUENTIAL,
        REVERSE
    }

    protected enum NbtDisplayMode {
        UNFOLD,
        FOLD
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
                modCheck:
                {
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
                modCheck:
                {
                    String mod1 = id1.getNamespace();
                    String mod2 = id2.getNamespace();
                    boolean mod1IsMc = mod1.equals("minecraft");
                    boolean mod2IsMc = mod2.equals("minecraft");
                    if (mod1IsMc && mod2IsMc) break modCheck;
                    if (mod1IsMc || mod2IsMc) yield mod1IsMc ? 1 : -1;
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
                modCheck:
                {
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
}
