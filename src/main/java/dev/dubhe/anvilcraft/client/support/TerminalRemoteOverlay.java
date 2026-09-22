package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.platform.InputConstants;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.TerminalSessions;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.client.rpc.TerminalReachabilityCache;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.TerminalItem;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class TerminalRemoteOverlay {
    private static final int SLOT_SIZE = 18;
    private static final int GRID_X = 4;
    private static final int GRID_Y = 18;
    private static final int SEARCH_Y = 6;
    private static final int REFRESH_INTERVAL = 20;
    private static final int FAST_RELOAD_INTERVAL = 5;
    private static final int FULL_SYNC_PAGE = 256;
    private static final int FULL_SYNC_PAGE_BUDGET = 4;
    private static final int FULL_SYNC_RETRY_REFRESHES = 2;
    private static final int TOOLTIP_GAP = 2;
    private static final int PANEL_OFFSET_X = 20;
    private static final int PANEL_OFFSET_Y = 14;

    private static @Nullable UUID storageId;
    private static int sizeMode;
    private static long generation;
    private static long refreshRequest;
    private static long appliedRefresh;
    private static @Nullable EditBox searchBox;
    private static String search = "";
    private static IntList order = new IntArrayList();
    private static IntList baseOrder = new IntArrayList();
    private static boolean fullSyncing;
    private static boolean fullSyncReplan;
    private static int fullSyncRetryRefresh;
    private static int fullSyncPages;
    private static final Int2ObjectMap<UnlimitedItemStack> CONTENTS = new Int2ObjectOpenHashMap<>();
    private static final Int2LongMap COUNTS = new Int2LongOpenHashMap();
    private static int cursor = -1;
    private static int left;
    private static int top;
    private static int frameCounter;
    private static int fastRetries;
    private static boolean taking;
    private static int lastTooltipX;
    private static int lastTooltipY;
    private static boolean dismissed;

    private TerminalRemoteOverlay() {
    }

    public static void setHovering(ItemStack stack) {
        UUID newId = TerminalRemoteOverlay.storageIdOf(stack);
        if (newId != null && !stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            TerminalReachabilityCache.ensure(newId);
            if (!TerminalReachabilityCache.isReachable(newId)) {
                newId = null;
            }
        }
        if (Objects.equals(newId, TerminalRemoteOverlay.storageId)) {
            return;
        }
        TerminalRemoteOverlay.reset();
        if (newId == null) {
            return;
        }
        TerminalRemoteOverlay.storageId = newId;
        TerminalRemoteOverlay.dismissed = false;
        TerminalRemoteOverlay.fastRetries = 0;
        TerminalRemoteOverlay.startLoad();
    }

    public static boolean isHovering() {
        return TerminalRemoteOverlay.storageId != null;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isDismissed() {
        return TerminalRemoteOverlay.dismissed;
    }

    public static void setDismissed(boolean dismissed) {
        TerminalRemoteOverlay.dismissed = dismissed;
    }

    public static boolean isBoundTerminal(ItemStack stack) {
        return TerminalRemoteOverlay.storageIdOf(stack) != null;
    }

    public static @Nullable UUID terminalIdOf(ItemStack stack) {
        return TerminalRemoteOverlay.storageIdOf(stack);
    }

    public static void render(GuiGraphicsExtractor graphics, Font font, float partialTick) {
        if (!TerminalRemoteOverlay.isHovering()) {
            return;
        }
        int w = TerminalRemoteOverlay.panelW();
        int h = TerminalRemoteOverlay.panelH();
        graphics.pose().pushMatrix();
        graphics.nextStratum();
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TerminalRemoteOverlay.background(),
            TerminalRemoteOverlay.left,
            TerminalRemoteOverlay.top,
            0,
            0,
            w,
            h,
            w,
            h
        );
        TerminalRemoteOverlay.renderSearchBox(graphics, font, partialTick);
        int gridSize = TerminalRemoteOverlay.gridSize();
        int pageStart = TerminalRemoteOverlay.pageStart();
        for (int i = 0; i < TerminalRemoteOverlay.pageSize(); i++) {
            int orderIndex = pageStart + i;
            if (orderIndex >= TerminalRemoteOverlay.order.size()) {
                break;
            }
            int slot = TerminalRemoteOverlay.order.getInt(orderIndex);
            UnlimitedItemStack stack = TerminalRemoteOverlay.CONTENTS.getOrDefault(slot, UnlimitedItemStack.EMPTY);
            int x = TerminalRemoteOverlay.left + TerminalRemoteOverlay.GRID_X + i % gridSize * TerminalRemoteOverlay.SLOT_SIZE;
            int y = TerminalRemoteOverlay.top + TerminalRemoteOverlay.GRID_Y + i / gridSize * TerminalRemoteOverlay.SLOT_SIZE;
            if (!stack.isEmpty()) {
                ItemStack itemStack = stack.toStack();
                graphics.fakeItem(itemStack, x + 1, y + 1);
            }
            if (TerminalRemoteOverlay.cursor >= 0 && orderIndex == TerminalRemoteOverlay.cursor) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, SharedTextures.BOX_SELECTION, x, y, 0, 0, 18, 18, 18, 18);
            }
            if (!stack.isEmpty()) {
                StorageScreen.itemDecorations(graphics, Minecraft.getInstance(), stack.toStack(),
                    TerminalRemoteOverlay.COUNTS.get(slot), x + 1, y + 1);
            }
        }
        graphics.pose().popMatrix();
    }

    private static void renderSearchBox(GuiGraphicsExtractor graphics, Font font, float partialTick) {
        TerminalRemoteOverlay.ensureSearchBox();
        if (TerminalRemoteOverlay.searchBox == null) {
            return;
        }
        TerminalRemoteOverlay.searchBox.setX(TerminalRemoteOverlay.left + TerminalRemoteOverlay.GRID_X + 2);
        TerminalRemoteOverlay.searchBox.setY(TerminalRemoteOverlay.top + TerminalRemoteOverlay.SEARCH_Y);
        TerminalRemoteOverlay.searchBox.setWidth(TerminalRemoteOverlay.maxSearchWidth());
        Minecraft minecraft = Minecraft.getInstance();
        int mouseX = (int) (minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth()
                            / minecraft.getWindow().getScreenWidth());
        int mouseY = (int) (minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight()
                            / minecraft.getWindow().getScreenHeight());
        TerminalRemoteOverlay.searchBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (!TerminalRemoteOverlay.searchBox.isFocused() && TerminalRemoteOverlay.searchBox.getValue().isEmpty()) {
            Component hint = Component.translatable("screen.anvilcraft.storage.search.tab").withStyle(ChatFormatting.GRAY);
            graphics.text(font, hint, TerminalRemoteOverlay.searchBox.getX(), TerminalRemoteOverlay.searchBox.getY(), 0xFFFFFFFF);
        }
    }

    public static boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        int button = event.button();
        if (TerminalRemoteOverlay.searchBox != null
            && TerminalRemoteOverlay.searchBox.isMouseOver(mouseX, mouseY)) {
            TerminalRemoteOverlay.searchBox.mouseClicked(event, doubleClick);
            TerminalRemoteOverlay.searchBox.setFocused(true);
            return true;
        }
        if (button != 0 && button != 1) {
            return false;
        }
        if (!TerminalRemoteOverlay.isHovering()) return false;
        if (TerminalRemoteOverlay.taking) return true;
        if (TerminalRemoteOverlay.cursor < 0) {
            return false;
        }
        int slot = TerminalRemoteOverlay.determineSlot(mouseX, mouseY);
        if (slot < 0) {
            TerminalRemoteOverlay.refresh();
            return false;
        }
        TerminalRemoteOverlay.taking = true;
        TerminalRemoteOverlay.take(slot, button, event.hasShiftDown());
        return true;
    }

    public static boolean mouseScrolled(int amount) {
        if (!TerminalRemoteOverlay.isHovering()) {
            return false;
        }
        if (TerminalRemoteOverlay.order.isEmpty()) {
            return true;
        }
        if (TerminalRemoteOverlay.cursor < 0) {
            TerminalRemoteOverlay.cursor = 0;
            TerminalRemoteOverlay.syncVisible();
            return true;
        }
        int step = 1;
        if (Minecraft.getInstance().hasControlDown()) {
            step = TerminalRemoteOverlay.pageSize();
        } else if (Minecraft.getInstance().hasShiftDown()) {
            step = TerminalRemoteOverlay.gridSize();
        }
        int size = TerminalRemoteOverlay.order.size();
        int next = Math.floorMod(amount > 0 ? TerminalRemoteOverlay.cursor - step : TerminalRemoteOverlay.cursor + step, size);
        if (next != TerminalRemoteOverlay.cursor) {
            TerminalRemoteOverlay.cursor = next;
            TerminalRemoteOverlay.syncVisible();
        }
        return true;
    }

    private static int determineSlot(int mouseX, int mouseY) {
        Integer gridIndex = TerminalRemoteOverlay.getSlotAt(mouseX, mouseY);
        if (gridIndex != null && gridIndex >= 0 && gridIndex < TerminalRemoteOverlay.order.size()) {
            return TerminalRemoteOverlay.order.getInt(gridIndex);
        }
        if (TerminalRemoteOverlay.cursor >= 0 && TerminalRemoteOverlay.cursor < TerminalRemoteOverlay.order.size()) {
            return TerminalRemoteOverlay.order.getInt(TerminalRemoteOverlay.cursor);
        }
        return -1;
    }

    public static boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (!TerminalRemoteOverlay.isHovering()) {
            return false;
        }
        if (keyCode == InputConstants.KEY_ESCAPE) {
            TerminalRemoteOverlay.dismissed = true;
            TerminalRemoteOverlay.setHovering(ItemStack.EMPTY);
            return true;
        }
        if (keyCode == InputConstants.KEY_LALT || keyCode == InputConstants.KEY_RALT) {
            TerminalRemoteOverlay.sizeMode = (TerminalRemoteOverlay.sizeMode + 1) % 3;
            TerminalRemoteOverlay.reanchor();
            return true;
        }
        if (!TerminalRemoteOverlay.searchBoxFocused()) {
            if (keyCode == InputConstants.KEY_TAB) {
                TerminalRemoteOverlay.ensureSearchBox();
                if (TerminalRemoteOverlay.searchBox != null) {
                    TerminalRemoteOverlay.searchBox.setFocused(true);
                }
                return true;
            }
            return false;
        }
        if (keyCode == InputConstants.KEY_TAB) {
            if (TerminalRemoteOverlay.searchBox != null) {
                TerminalRemoteOverlay.searchBox.setFocused(false);
            }
            return true;
        }
        TerminalRemoteOverlay.ensureSearchBox();
        if (TerminalRemoteOverlay.searchBox != null) {
            if (TerminalRemoteOverlay.searchBox.isFocused()) {
                TerminalRemoteOverlay.searchBox.keyPressed(event);
            }
        }
        return true;
    }

    public static boolean charTyped(CharacterEvent event) {
        if (!TerminalRemoteOverlay.isHovering() || !TerminalRemoteOverlay.searchBoxFocused()) {
            return false;
        }
        TerminalRemoteOverlay.ensureSearchBox();
        if (TerminalRemoteOverlay.searchBox != null && TerminalRemoteOverlay.searchBox.isFocused()) {
            TerminalRemoteOverlay.searchBox.charTyped(event);
        }
        return true;
    }

    public static boolean hasSelection() {
        return TerminalRemoteOverlay.cursor >= 0;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean searchBoxFocused() {
        return TerminalRemoteOverlay.searchBox != null && TerminalRemoteOverlay.searchBox.isFocused();
    }

    private static void ensureSearchBox() {
        if (TerminalRemoteOverlay.searchBox != null) {
            return;
        }
        TerminalRemoteOverlay.searchBox = new EditBox(
            Minecraft.getInstance().font,
            TerminalRemoteOverlay.left + TerminalRemoteOverlay.GRID_X + 1,
            TerminalRemoteOverlay.top + TerminalRemoteOverlay.SEARCH_Y,
            TerminalRemoteOverlay.maxSearchWidth(),
            9,
            Component.empty()
        );
        TerminalRemoteOverlay.searchBox.setBordered(false);
        TerminalRemoteOverlay.searchBox.setTextColor(0xFFFFFFFF);
        TerminalRemoteOverlay.searchBox.setMaxLength(50);
        TerminalRemoteOverlay.searchBox.setValue(TerminalRemoteOverlay.search);
        TerminalRemoteOverlay.searchBox.setResponder(text -> {
            if (!text.equals(TerminalRemoteOverlay.search)) {
                TerminalRemoteOverlay.search = text;
                TerminalRemoteOverlay.reorder();
            }
        });
        TerminalRemoteOverlay.searchBox.setCanLoseFocus(true);
    }

    public static void tick() {
        if (!TerminalRemoteOverlay.isHovering()) {
            return;
        }
        UUID id = TerminalRemoteOverlay.storageId;
        Player player = Minecraft.getInstance().player;
        if (id != null
            && player != null
            && (id.equals(TerminalSessions.localTerminalId(player.getUUID()))
                || id.equals(TerminalSessions.shulkerTerminalId(player.getUUID())))) {
            TerminalReachabilityCache.ensure(id);
            if (!TerminalReachabilityCache.isReachable(id)) {
                TerminalRemoteOverlay.reset();
                return;
            }
        }
        boolean fast = TerminalRemoteOverlay.order.isEmpty()
                       && TerminalRemoteOverlay.fastRetries < 12;
        int interval = fast ? TerminalRemoteOverlay.FAST_RELOAD_INTERVAL
                            : TerminalRemoteOverlay.REFRESH_INTERVAL;
        if (++TerminalRemoteOverlay.frameCounter % interval == 0) {
            if (fast) {
                TerminalRemoteOverlay.fastRetries++;
            }
            if (TerminalRemoteOverlay.fullSyncRetryRefresh > 0) {
                TerminalRemoteOverlay.fullSyncRetryRefresh--;
            }
            TerminalRemoteOverlay.refresh();
        }
    }

    private static void startLoad() {
        TerminalRemoteOverlay.refresh();
    }

    private static void refresh() {
        final long expectedGeneration = generation;
        UUID id = TerminalRemoteOverlay.storageId;
        if (id == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        final long request = ++refreshRequest;
        final String requestedSearch = TerminalRemoteOverlay.search;
        StorageTerminalClientStub.reorder(id, requestedSearch).whenComplete((newOrder, error) -> minecraft.execute(() -> {
            if (error != null || request < appliedRefresh || !requestedSearch.equals(TerminalRemoteOverlay.search)
                || !TerminalRemoteOverlay.isHoveringSame(id, expectedGeneration)) {
                return;
            }
            appliedRefresh = request;
            IntList next = new IntArrayList(newOrder);
            boolean orderChanged = !next.equals(TerminalRemoteOverlay.baseOrder);
            TerminalRemoteOverlay.baseOrder = next;
            if (TerminalRemoteOverlay.isPlainTextSearch(TerminalRemoteOverlay.search)) {
                if (TerminalRemoteOverlay.fullSyncRetryRefresh > 0) {
                    TerminalRemoteOverlay.applyServerOrder(next, orderChanged);
                    return;
                }
                TerminalRemoteOverlay.syncFullContents(id, orderChanged);
                return;
            }
            TerminalRemoteOverlay.applyServerOrder(next, orderChanged);
        }));
    }

    private static void applyServerOrder(IntList next, boolean orderChanged) {
        TerminalRemoteOverlay.order = next;
        if (orderChanged) {
            TerminalRemoteOverlay.trimContentCache();
        }
        if (TerminalRemoteOverlay.cursor >= 0) {
            TerminalRemoteOverlay.cursor = Mth.clamp(
                TerminalRemoteOverlay.cursor,
                0,
                Math.max(0, TerminalRemoteOverlay.order.size() - 1)
            );
        }
        TerminalRemoteOverlay.syncVisible();
    }

    private static void syncFullContents(UUID id, boolean orderChanged) {
        final long expectedGeneration = generation;
        if (orderChanged) {
            TerminalRemoteOverlay.trimContentCache();
        }
        if (TerminalRemoteOverlay.fullSyncing) {
            TerminalRemoteOverlay.fullSyncReplan = true;
            TerminalRemoteOverlay.syncVisible();
            return;
        }
        IntArrayList missing = new IntArrayList();
        for (int i = 0; i < TerminalRemoteOverlay.baseOrder.size(); i++) {
            int slot = TerminalRemoteOverlay.baseOrder.getInt(i);
            if (!TerminalRemoteOverlay.CONTENTS.containsKey(slot)) {
                missing.add(slot);
            }
        }
        if (missing.isEmpty()) {
            TerminalRemoteOverlay.applyClientFilter();
            return;
        }
        TerminalRemoteOverlay.fullSyncing = true;
        TerminalRemoteOverlay.fullSyncPages = 0;
        TerminalRemoteOverlay.fullSyncReplan = false;
        Minecraft minecraft = Minecraft.getInstance();
        StorageTerminalClientStub.ensureVirtualPos(id).whenComplete((virtualPos, error) -> minecraft.execute(() -> {
            if (!TerminalRemoteOverlay.isHoveringSame(id, expectedGeneration)) return;
            if (error != null) {
                TerminalRemoteOverlay.fullSyncing = false;
                return;
            }
            TerminalRemoteOverlay.syncFullPage(id, BlockPos.of(virtualPos), missing, 0, expectedGeneration);
        }));
    }

    private static void syncFullPage(UUID id, BlockPos virtualPos, IntList missing, int from, long expectedGeneration) {
        int to = Math.min(from + TerminalRemoteOverlay.FULL_SYNC_PAGE, missing.size());
        IntArrayList page = new IntArrayList(missing.subList(from, to));
        TerminalRemoteOverlay.fullSyncPages++;
        Minecraft minecraft = Minecraft.getInstance();
        StorageClientStub.sync(virtualPos, page).whenComplete((result, syncError) -> minecraft.execute(() -> {
            if (!TerminalRemoteOverlay.isHoveringSame(id, expectedGeneration)) return;
            if (syncError != null) {
                TerminalRemoteOverlay.fullSyncing = false;
                TerminalRemoteOverlay.fullSyncRetryRefresh = TerminalRemoteOverlay.FULL_SYNC_RETRY_REFRESHES;
                TerminalRemoteOverlay.applyServerOrder(
                    new IntArrayList(TerminalRemoteOverlay.baseOrder),
                    false
                );
                return;
            }
            if (!TerminalRemoteOverlay.isPlainTextSearch(TerminalRemoteOverlay.search)) {
                TerminalRemoteOverlay.fullSyncing = false;
                return;
            }
            TerminalRemoteOverlay.applySync(result);
            if (to < missing.size()) {
                if (TerminalRemoteOverlay.fullSyncPages >= TerminalRemoteOverlay.FULL_SYNC_PAGE_BUDGET) {
                    TerminalRemoteOverlay.fullSyncing = false;
                    return;
                }
                TerminalRemoteOverlay.syncFullPage(id, virtualPos, missing, to, expectedGeneration);
                return;
            }
            TerminalRemoteOverlay.fullSyncing = false;
            if (TerminalRemoteOverlay.isHoveringSame(id, expectedGeneration)
                && TerminalRemoteOverlay.fullSyncReplan) {
                TerminalRemoteOverlay.fullSyncReplan = false;
                TerminalRemoteOverlay.syncFullContents(id, false);
                return;
            }
            TerminalRemoteOverlay.applyClientFilter();
        }));
    }

    private static void applyClientFilter() {
        String plain = TerminalRemoteOverlay.search.strip().toLowerCase(Locale.ROOT);
        IntArrayList filtered = new IntArrayList(TerminalRemoteOverlay.baseOrder.size());
        for (int i = 0; i < TerminalRemoteOverlay.baseOrder.size(); i++) {
            int slot = TerminalRemoteOverlay.baseOrder.getInt(i);
            UnlimitedItemStack stack = TerminalRemoteOverlay.CONTENTS.get(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemStack itemStack = stack.toStack();
            String name = itemStack.getHoverName().getString().toLowerCase(Locale.ROOT);
            String idPath = BuiltInRegistries.ITEM.getKey(itemStack.getItem()).getPath().toLowerCase(Locale.ROOT);
            if (name.contains(plain) || idPath.contains(plain)) {
                filtered.add(slot);
            }
        }
        TerminalRemoteOverlay.order = filtered;
        if (TerminalRemoteOverlay.cursor >= 0) {
            TerminalRemoteOverlay.cursor = Mth.clamp(
                TerminalRemoteOverlay.cursor,
                0,
                Math.max(0, TerminalRemoteOverlay.order.size() - 1)
            );
        }
        TerminalRemoteOverlay.syncVisible();
    }

    private static boolean isPlainTextSearch(String search) {
        String stripped = search.strip();
        return !stripped.isEmpty() && stripped.charAt(0) != '@' && stripped.charAt(0) != '#';
    }

    private static void trimContentCache() {
        if (TerminalRemoteOverlay.CONTENTS.isEmpty()) {
            return;
        }
        IntOpenHashSet keep = new IntOpenHashSet(TerminalRemoteOverlay.baseOrder.size());
        for (int i = 0; i < TerminalRemoteOverlay.baseOrder.size(); i++) {
            keep.add(TerminalRemoteOverlay.baseOrder.getInt(i));
        }
        if (keep.size() == TerminalRemoteOverlay.CONTENTS.size()) {
            boolean allKept = true;
            for (int slot : TerminalRemoteOverlay.CONTENTS.keySet()) {
                if (!keep.contains(slot)) {
                    allKept = false;
                    break;
                }
            }
            if (allKept) {
                return;
            }
        }
        TerminalRemoteOverlay.CONTENTS.keySet().removeIf(slot -> !keep.contains(slot));
        TerminalRemoteOverlay.COUNTS.keySet().removeIf(slot -> !keep.contains(slot));
    }

    private static void reorder() {
        TerminalRemoteOverlay.fullSyncRetryRefresh = 0;
        TerminalRemoteOverlay.cursor = 0;
        TerminalRemoteOverlay.refresh();
    }

    private static void syncVisible() {
        final long expectedGeneration = generation;
        UUID id = TerminalRemoteOverlay.storageId;
        if (id == null) {
            return;
        }
        IntList slots = TerminalRemoteOverlay.getVisibleSlots();
        if (slots.isEmpty()) {
            if (!TerminalRemoteOverlay.isPlainTextSearch(TerminalRemoteOverlay.search)) {
                TerminalRemoteOverlay.CONTENTS.clear();
                TerminalRemoteOverlay.COUNTS.clear();
            }
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        StorageTerminalClientStub.ensureVirtualPos(id).whenComplete((virtualPos, error) -> minecraft.execute(() -> {
            if (error != null || !TerminalRemoteOverlay.isHoveringSame(id, expectedGeneration)) {
                return;
            }
            StorageClientStub.sync(BlockPos.of(virtualPos), slots).whenComplete((result, syncError) -> minecraft.execute(() -> {
                if (syncError != null || !TerminalRemoteOverlay.isHoveringSame(id, expectedGeneration)) {
                    return;
                }
                TerminalRemoteOverlay.applySync(result);
            }));
        }));
    }

    private static void applySync(StorageServerStub.SyncResult result) {
        for (StorageServerStub.StackUpdate update : result.updates()) {
            if (update.stack().isEmpty()) {
                TerminalRemoteOverlay.CONTENTS.remove(update.index());
                TerminalRemoteOverlay.COUNTS.remove(update.index());
            } else {
                TerminalRemoteOverlay.CONTENTS.put(update.index(), update.stack());
                TerminalRemoteOverlay.COUNTS.put(update.index(), update.count());
            }
        }
    }

    private static void take(int slot, int button, boolean shift) {
        final long expectedGeneration = generation;
        UUID id = TerminalRemoteOverlay.storageId;
        if (id == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        final var origin = minecraft.screen;
        final var actor = minecraft.player;
        ItemStack carried = minecraft.player != null
                            ? minecraft.player.containerMenu.getCarried()
                            : ItemStack.EMPTY;
        if (shift) {
            StorageTerminalClientStub.takeToInventory(id, slot, button).whenComplete((result, error) -> minecraft.execute(() -> {
                if (expectedGeneration == generation) TerminalRemoteOverlay.taking = false;
                if (error != null || !result.changed()) {
                    return;
                }
                if (TerminalRemoteOverlay.isHoveringSame(id, expectedGeneration)) {
                    TerminalRemoteOverlay.refresh();
                }
            }));
            return;
        }
        StorageTerminalClientStub.take(id, slot, button, carried).whenComplete((result, error) -> minecraft.execute(() -> {
            if (expectedGeneration == generation) TerminalRemoteOverlay.taking = false;
            if (error != null || !result.changed()) {
                return;
            }
            if (minecraft.screen == origin && minecraft.player == actor) {
                TerminalRemoteOverlay.applyCarriedIfCreative(result.carried());
            }
            if (TerminalRemoteOverlay.isHoveringSame(id, expectedGeneration)) {
                TerminalRemoteOverlay.refresh();
            }
        }));
    }

    public static void applyCarriedIfCreative(ItemStack carried) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof CreativeModeInventoryScreen creativeScreen
            && minecraft.player != null) {
            creativeScreen.getMenu().setCarried(carried);
        }
    }

    private static IntList getVisibleSlots() {
        int pageStart = TerminalRemoteOverlay.pageStart();
        IntArrayList slots = new IntArrayList();
        for (int i = pageStart; i < Math.min(pageStart + TerminalRemoteOverlay.pageSize(), TerminalRemoteOverlay.order.size()); i++) {
            slots.add(TerminalRemoteOverlay.order.getInt(i));
        }
        return slots;
    }

    private static int pageStart() {
        if (TerminalRemoteOverlay.order.isEmpty()) {
            return 0;
        }
        return TerminalRemoteOverlay.cursor / TerminalRemoteOverlay.pageSize() * TerminalRemoteOverlay.pageSize();
    }

    private static @Nullable Integer getSlotAt(int mouseX, int mouseY) {
        int gridSize = TerminalRemoteOverlay.gridSize();
        for (int row = 0; row < gridSize; row++) {
            for (int column = 0; column < gridSize; column++) {
                int x = TerminalRemoteOverlay.left + TerminalRemoteOverlay.GRID_X + column * TerminalRemoteOverlay.SLOT_SIZE;
                int y = TerminalRemoteOverlay.top + TerminalRemoteOverlay.GRID_Y + row * TerminalRemoteOverlay.SLOT_SIZE;
                if (
                    mouseX >= x - 1
                    && mouseX < x + TerminalRemoteOverlay.SLOT_SIZE - 1
                    && mouseY >= y - 1
                    && mouseY < y + TerminalRemoteOverlay.SLOT_SIZE - 1
                ) {
                    return TerminalRemoteOverlay.pageStart() + row * gridSize + column;
                }
            }
        }
        return null;
    }

    public static void updateForTooltip(int tooltipX, int tooltipY) {
        TerminalRemoteOverlay.lastTooltipX = tooltipX;
        TerminalRemoteOverlay.lastTooltipY = tooltipY;
        TerminalRemoteOverlay.reanchor();
    }

    private static void reanchor() {
        TerminalRemoteOverlay.left = TerminalRemoteOverlay.lastTooltipX
                                    - TerminalRemoteOverlay.panelW() - TerminalRemoteOverlay.TOOLTIP_GAP
                                    + TerminalRemoteOverlay.PANEL_OFFSET_X;
        TerminalRemoteOverlay.top = TerminalRemoteOverlay.lastTooltipY
                                   - TerminalRemoteOverlay.panelH() - TerminalRemoteOverlay.TOOLTIP_GAP
                                   - TerminalRemoteOverlay.PANEL_OFFSET_Y;
        TerminalRemoteOverlay.clampAnchor();
    }

    private static void clampAnchor() {
        Minecraft minecraft = Minecraft.getInstance();
        TerminalRemoteOverlay.left = Mth.clamp(
            TerminalRemoteOverlay.left,
            2,
            Math.max(2, minecraft.getWindow().getGuiScaledWidth() - TerminalRemoteOverlay.panelW() - 2)
        );
        TerminalRemoteOverlay.top = Mth.clamp(
            TerminalRemoteOverlay.top,
            2,
            Math.max(2, minecraft.getWindow().getGuiScaledHeight() - TerminalRemoteOverlay.panelH() - 2)
        );
    }

    private static int maxSearchWidth() {
        return 60 + 18 * TerminalRemoteOverlay.sizeMode;
    }

    private static int gridSize() {
        return 4 + TerminalRemoteOverlay.sizeMode;
    }

    private static int pageSize() {
        return TerminalRemoteOverlay.gridSize() * TerminalRemoteOverlay.gridSize();
    }

    private static int panelW() {
        return 80 + 18 * TerminalRemoteOverlay.sizeMode;
    }

    private static int panelH() {
        return 96 + 18 * TerminalRemoteOverlay.sizeMode;
    }

    private static Identifier background() {
        return SharedTextures.textureGui("misc/storage_station/remote_" + (4 + TerminalRemoteOverlay.sizeMode));
    }

    private static boolean isHoveringSame(UUID id, long expectedGeneration) {
        return expectedGeneration == generation && TerminalRemoteOverlay.isHovering()
            && Objects.equals(TerminalRemoteOverlay.storageId, id);
    }

    private static @Nullable UUID storageIdOf(ItemStack stack) {
        Player player = Minecraft.getInstance().player;
        return player != null && stack.getItem() instanceof TerminalItem terminal ? terminal.targetId(player, stack) : null;
    }

    public static void reset() {
        TerminalRemoteOverlay.generation++;
        TerminalRemoteOverlay.storageId = null;
        TerminalRemoteOverlay.search = "";
        TerminalRemoteOverlay.searchBox = null;
        TerminalRemoteOverlay.order = new IntArrayList();
        TerminalRemoteOverlay.baseOrder = new IntArrayList();
        TerminalRemoteOverlay.fullSyncing = false;
        TerminalRemoteOverlay.fullSyncReplan = false;
        TerminalRemoteOverlay.fullSyncRetryRefresh = 0;
        TerminalRemoteOverlay.fullSyncPages = 0;
        TerminalRemoteOverlay.CONTENTS.clear();
        TerminalRemoteOverlay.COUNTS.clear();
        TerminalRemoteOverlay.cursor = -1;
        TerminalRemoteOverlay.taking = false;
        TerminalRemoteOverlay.fastRetries = 0;
        TerminalRemoteOverlay.frameCounter = 0;
    }
}
