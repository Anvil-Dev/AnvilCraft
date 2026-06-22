package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorRegistry;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.CelestialForgingAnvilMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CelestialForgingAnvilScreen extends AbstractContainerScreen<CelestialForgingAnvilMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("machine", "celestial_forging_anvil");

    private static final int TEX_WIDTH = 512;
    private static final int TEX_HEIGHT = 256;

    // Preview area
    private static final int PV_X = 98;
    private static final int PV_Y = 15;
    private static final int PV_W = 148;
    private static final int PV_H = 99;
    private static final int PV_BOT_Y = 84;

    // Search button
    private static final int SB_X = 32;
    private static final int SB_Y = 121;
    private static final int SB_W = 48;
    private static final int SB_H = 16;

    // Refactor section
    private static final int RF_BTN_W = 36;
    private static final int RF_BTN_H = 35;
    private static final int[] RF_BTN_X = {255, 291, 255, 291};
    private static final int[] RF_BTN_Y = {39, 39, 74, 74};

    private static final int RF_SCROLL_X = 331;
    private static final int RF_SCROLL_Y = 39;
    private static final int RF_SCROLL_H = 70;
    private static final int RF_SCROLL_W = 4;
    private static final int RF_SCROLL_THUMB_H = 12;
    private static final int RF_COLS = 2;
    private static final int RF_ROWS_VISIBLE = 2;

    private static final int RF_START_X = 290;
    private static final int RF_START_Y = 121;
    private static final int RF_START_W = 48;
    private static final int RF_START_H = 16;

    private static final String BTN_DIR = "machine/celestial_forging_anvil/";
    private static final Identifier TEX_SEARCH = SharedTextures.textureGui(BTN_DIR + "search");
    private static final Identifier TEX_RESEARCH = SharedTextures.textureGui(BTN_DIR + "re_search");
    private static final Identifier TEX_PREV = SharedTextures.textureGui(BTN_DIR + "previous");
    private static final Identifier TEX_NEXT = SharedTextures.textureGui(BTN_DIR + "next");
    private static final Identifier TEX_UNLOCKED = SharedTextures.textureGui(BTN_DIR + "unlocked");
    private static final Identifier TEX_LOCKED = SharedTextures.textureGui(BTN_DIR + "locked");
    private static final Identifier TEX_REFACTOR_OPTIONS = SharedTextures.textureGui(BTN_DIR + "refactor_options");
    private static final Identifier TEX_REFACTORING = SharedTextures.textureGui(BTN_DIR + "refactoring");

    private enum SearchState { IDLE, LOADING, DONE, FAIL, POWER_FAIL }

    private SearchState searchState = SearchState.IDLE;
    @Nullable
    private CelestialBodyData preSearchBody = null;

    private int previewRotTick = 0;
    private int localAcceleratorTicksRemaining = 0;

    private List<CelestialRefactorOption> refactorOptions = List.of();
    private int selectedRefactorIndex = -1;
    private int rfScrollRow = 0;
    private boolean isDraggingRfScrollbar = false;
    private int refactorErrorTick = 0;
    @Nullable
    private Component refactorErrorMsg = null;
    private int unlockWarningTick = 0;
    private int lockedMsgTick = 0;

    private final int[] previousAnvilCounts = new int[4];
    private boolean guideTriggered = false;

    public CelestialForgingAnvilScreen(CelestialForgingAnvilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 344, 207);
    }

    @Override
    protected void init() {
        super.init();
        int titleAreaCenter = (3 + 342) / 2 - 1;
        this.titleLabelX = titleAreaCenter - this.font.width(this.title) / 2;
        this.titleLabelY = 2;

        var be = getMenu().getBlockEntity();
        if (be.isSearching() && searchState == SearchState.IDLE) {
            searchState = SearchState.LOADING;
        } else if (be.getCelestialBodyData() != null && searchState == SearchState.IDLE) {
            searchState = SearchState.DONE;
        }

        for (int i = 0; i < 4; i++) {
            previousAnvilCounts[i] = be.getAnvilCount(i);
        }
        guideTriggered = false;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        previewRotTick++;

        var be = getMenu().getBlockEntity();
        for (int i = 0; i < 4; i++) {
            int cur = be.getAnvilCount(i);
            if (cur != previousAnvilCounts[i]) guideTriggered = true;
            previousAnvilCounts[i] = cur;
        }
        if (searchState == SearchState.LOADING) guideTriggered = false;

        if (lockedMsgTick > 0) lockedMsgTick--;
        if (refactorErrorTick > 0) refactorErrorTick--;
        if (unlockWarningTick > 0) unlockWarningTick--;

        if (be.isAcceleratorActive()) {
            int serverTicks = be.getAcceleratorTicksRemaining();
            if (localAcceleratorTicksRemaining <= 0 || serverTicks < localAcceleratorTicksRemaining) {
                localAcceleratorTicksRemaining = serverTicks;
            }
            if (localAcceleratorTicksRemaining > 0) localAcceleratorTicksRemaining--;
        } else {
            localAcceleratorTicksRemaining = 0;
        }

        if (searchState == SearchState.LOADING) {
            CelestialBodyData cur = be.getCelestialBodyData();
            if (be.isPowerInsufficient()) searchState = SearchState.POWER_FAIL;
            else if (cur != null && cur != preSearchBody) searchState = SearchState.DONE;
            else if (be.isSearchFailed()) searchState = SearchState.FAIL;
        }
    }

    // === Rendering (26.1: extract-based API) ===

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        int i = this.leftPos;
        int j = this.topPos;
        int relX = mouseX - i;
        int relY = mouseY - j;

        Identifier btnTex = (searchState == SearchState.DONE && !getMenu().getBlockEntity().getSearchHistory().isEmpty())
            ? TEX_RESEARCH : TEX_SEARCH;
        boolean hoverSearch = relX >= SB_X && relX < SB_X + SB_W && relY >= SB_Y && relY < SB_Y + SB_H;
        renderButton(graphics, btnTex, i + SB_X, j + SB_Y, SB_W, SB_H, hoverSearch);

        renderPreviewBottomButtons(graphics, i, j, relX, relY);
        renderRefactorSection(graphics, i, j, relX, relY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
            this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEX_WIDTH, TEX_HEIGHT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int rx = mouseX - this.leftPos;
        int ry = mouseY - this.topPos;

        List<Component> tooltip = new ArrayList<>();
        if (isOverSearchButton(rx, ry)) {
            tooltip.add(Component.translatable("screen.anvilcraft.cfa.search"));
            if (searchState == SearchState.POWER_FAIL) {
                tooltip.add(Component.translatable("screen.anvilcraft.cfa.power_insufficient").withStyle(ChatFormatting.RED));
            } else if (searchState == SearchState.FAIL) {
                tooltip.add(Component.translatable("screen.anvilcraft.cfa.search_failed").withStyle(ChatFormatting.RED));
            }
        }
        if (isOverLockButton(rx, ry)) {
            var be = getMenu().getBlockEntity();
            if (be.isLocked() && be.isAcceleratorActive()) {
                tooltip.add(Component.translatable("screen.anvilcraft.cfa.cannot_unlock_during_evolution").withStyle(ChatFormatting.RED));
            } else if (be.isLocked()) {
                tooltip.add(Component.translatable("screen.anvilcraft.cfa.unlock"));
            } else {
                tooltip.add(Component.translatable("screen.anvilcraft.cfa.lock"));
            }
        }
        if (isOverPrevButton(rx, ry)) tooltip.add(Component.translatable("screen.anvilcraft.cfa.previous"));
        if (isOverNextButton(rx, ry)) tooltip.add(Component.translatable("screen.anvilcraft.cfa.next"));

        int refIdx = getRefactorOptionAt(rx, ry);
        if (refIdx >= 0 && refIdx < refactorOptions.size()) {
            CelestialRefactorOption opt = refactorOptions.get(refIdx);
            tooltip.add(Component.translatable("screen.anvilcraft.cfa.refactor." + opt.megastructure()));
            if (opt.needsMaterial()) {
                tooltip.add(Component.translatable("screen.anvilcraft.cfa.refactor.material",
                    opt.materialCount(), opt.material().getDisplayName()).withStyle(ChatFormatting.GRAY));
            }
        }
        if (isOverRefactorStart(rx, ry)) {
            tooltip.add(Component.translatable("screen.anvilcraft.cfa.start_refactoring"));
        }
        if (refactorErrorTick > 0 && refactorErrorMsg != null) {
            tooltip.add(refactorErrorMsg.copy().withStyle(ChatFormatting.RED));
        }
        if (lockedMsgTick > 0) {
            tooltip.add(Component.translatable("screen.anvilcraft.cfa.locked").withStyle(ChatFormatting.GOLD));
        }
        if (!tooltip.isEmpty()) {
            graphics.setTooltipForNextFrame(tooltip.getFirst(), mouseX, mouseY);
        }
    }

    // === Button rendering ===

    private void renderButton(GuiGraphicsExtractor g, Identifier tex, int x, int y, int w, int h, boolean hovered) {
        int v = hovered ? h : 0;
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0, v, w, h, w, h * 2);
    }

    private void renderPreviewBottomButtons(GuiGraphicsExtractor g, int guiLeft, int guiTop, int relX, int relY) {
        if (searchState != SearchState.DONE && searchState != SearchState.LOADING) return;
        var be = getMenu().getBlockEntity();
        if (be.hasPreviousHistory()) {
            renderButton(g, TEX_PREV, guiLeft + PV_X + 4, guiTop + PV_BOT_Y + 14, 16, 16,
                isOverPrevButton(relX, relY));
        }
        if (be.hasNextHistory()) {
            renderButton(g, TEX_NEXT, guiLeft + PV_X + PV_W - 20, guiTop + PV_BOT_Y + 14, 16, 16,
                isOverNextButton(relX, relY));
        }
        boolean hoverLock = isOverLockButton(relX, relY);
        Identifier lockTex = be.isLocked() ? TEX_LOCKED : TEX_UNLOCKED;
        renderButton(g, lockTex, guiLeft + PV_X + PV_W / 2 - 8, guiTop + PV_BOT_Y + 14, 16, 16, hoverLock);
    }

    // === Refactor section ===

    private void renderRefactorSection(GuiGraphicsExtractor g, int guiLeft, int guiTop, int relX, int relY) {
        var be = getMenu().getBlockEntity();
        CelestialBodyData body = be.getCelestialBodyData();
        boolean hasAcceleratorActive = be.isAcceleratorActive();
        boolean hasMegastructure = be.getActiveMegastructureIndex() >= 0;
        boolean isActive = be.isLocked() && body != null && searchState == SearchState.DONE
            && !hasAcceleratorActive;

        if (isActive) {
            refactorOptions = CelestialRefactorRegistry.getOptions(body, be.isAmplify(), be.getPlanetaryResourceSet());
            if (hasMegastructure) {
                refactorOptions = refactorOptions.stream()
                    .filter(opt -> "stellar_evolution_accelerator".equals(opt.megastructure())).toList();
            }
        } else {
            refactorOptions = List.of();
        }

        int btnCount = refactorOptions.size();
        int totalRows = btnCount > 0 ? (btnCount + RF_COLS - 1) / RF_COLS : 0;
        int maxScroll = Math.max(0, totalRows - RF_ROWS_VISIBLE);
        if (rfScrollRow > maxScroll) rfScrollRow = maxScroll;
        if (rfScrollRow < 0) rfScrollRow = 0;
        if (selectedRefactorIndex >= btnCount) selectedRefactorIndex = -1;

        for (int visibleRow = 0; visibleRow < RF_ROWS_VISIBLE; visibleRow++) {
            for (int col = 0; col < RF_COLS; col++) {
                int dataRow = rfScrollRow + visibleRow;
                int optIdx = dataRow * RF_COLS + col;
                if (optIdx >= btnCount) continue;
                int bx = guiLeft + RF_BTN_X[visibleRow * RF_COLS + col];
                int by = guiTop + RF_BTN_Y[visibleRow * RF_COLS + col];
                boolean hovered = relX >= RF_BTN_X[visibleRow * RF_COLS + col]
                    && relX < RF_BTN_X[visibleRow * RF_COLS + col] + RF_BTN_W
                    && relY >= RF_BTN_Y[visibleRow * RF_COLS + col]
                    && relY < RF_BTN_Y[visibleRow * RF_COLS + col] + RF_BTN_H;
                boolean selected = optIdx == selectedRefactorIndex;
                renderButton(g, TEX_REFACTOR_OPTIONS, bx, by, RF_BTN_W, RF_BTN_H, hovered || selected);
                if (selected) {
                    g.fill(bx, by, bx + RF_BTN_W, by + RF_BTN_H, 0x40_00FF00);
                }
            }
        }
        if (maxScroll > 0) renderRefactorScrollbar(g, guiLeft, guiTop, totalRows);

        boolean hoverStart = relX >= RF_START_X && relX < RF_START_X + RF_START_W
            && relY >= RF_START_Y && relY < RF_START_Y + RF_START_H;
        renderButton(g, TEX_REFACTORING, guiLeft + RF_START_X, guiTop + RF_START_Y, RF_START_W, RF_START_H, hoverStart);

        if (!isActive) {
            Component needLock = Component.translatable("screen.anvilcraft.cfa.need_lock");
            int areaCX = (RF_BTN_X[0] + RF_BTN_X[1] + RF_BTN_W) / 2;
            int areaCY = (RF_BTN_Y[0] + RF_BTN_Y[2] + RF_BTN_H) / 2;
            g.text(font, needLock, guiLeft + areaCX - font.width(needLock) / 2,
                guiTop + areaCY - font.lineHeight / 2, 0x888888, false);
        }
    }

    private void renderRefactorScrollbar(GuiGraphicsExtractor g, int guiLeft, int guiTop, int totalRows) {
        int i = totalRows - RF_ROWS_VISIBLE;
        if (i < 1) return;
        int scrollX = guiLeft + RF_SCROLL_X;
        int maxY = RF_SCROLL_Y + RF_SCROLL_H - RF_SCROLL_THUMB_H;
        int scrollY = RF_SCROLL_Y + (rfScrollRow * (RF_SCROLL_H - RF_SCROLL_THUMB_H) / i);
        scrollY = Mth.clamp(scrollY, RF_SCROLL_Y, maxY);
        g.blit(RenderPipelines.GUI_TEXTURED, SharedTextures.SWITCH_TABLE_SLIDER,
            scrollX, guiTop + scrollY, 0, 0, RF_SCROLL_W, RF_SCROLL_THUMB_H, 8, 12);
    }

    // === Mouse handling (26.1: MouseButtonEvent-based) ===

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int rx = (int) event.x() - this.leftPos;
        int ry = (int) event.y() - this.topPos;

        if (isOverSearchButton(rx, ry)) {
            sendButtonClick(0);
            searchState = SearchState.LOADING;
            preSearchBody = getMenu().getBlockEntity().getCelestialBodyData();
            return true;
        }
        if (isOverPrevButton(rx, ry)) { sendButtonClick(201); return true; }
        if (isOverNextButton(rx, ry)) { sendButtonClick(202); return true; }
        if (isOverLockButton(rx, ry)) {
            var be = getMenu().getBlockEntity();
            if (be.isLocked() && be.isAcceleratorActive()) {
                showUnlockWarning();
            } else {
                sendButtonClick(200);
                if (be.isLocked() && be.getCelestialBodyData() != null) showLockedMessage();
            }
            return true;
        }
        int refIdx = getRefactorOptionAt(rx, ry);
        if (refIdx >= 0) {
            selectedRefactorIndex = refIdx;
            sendButtonClick(9 + refIdx);
            return true;
        }
        if (isMouseInRefactorScrollbar(rx, ry)) {
            isDraggingRfScrollbar = true;
            return true;
        }
        if (isOverRefactorStart(rx, ry)) {
            handleRefactorStart();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        isDraggingRfScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (isDraggingRfScrollbar) {
            int ry = (int) event.y() - this.topPos - RF_SCROLL_Y;
            int totalRows = (refactorOptions.size() + RF_COLS - 1) / RF_COLS;
            int maxScroll = Math.max(0, totalRows - RF_ROWS_VISIBLE);
            if (maxScroll > 0) {
                rfScrollRow = (ry * maxScroll) / (RF_SCROLL_H - RF_SCROLL_THUMB_H);
                rfScrollRow = Mth.clamp(rfScrollRow, 0, maxScroll);
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int rx = (int) mouseX - this.leftPos;
        int ry = (int) mouseY - this.topPos;
        if (isMouseInRefactorArea(rx, ry)) {
            int totalRows = (refactorOptions.size() + RF_COLS - 1) / RF_COLS;
            int maxScroll = Math.max(0, totalRows - RF_ROWS_VISIBLE);
            if (maxScroll > 0) {
                rfScrollRow = Mth.clamp(rfScrollRow - (int) Math.signum(scrollY), 0, maxScroll);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // === Hit tests ===

    private boolean isOverSearchButton(int rx, int ry) {
        return rx >= SB_X && rx < SB_X + SB_W && ry >= SB_Y && ry < SB_Y + SB_H;
    }
    private boolean isOverPrevButton(int rx, int ry) {
        return rx >= PV_X + 4 && rx < PV_X + 20 && ry >= PV_BOT_Y + 14 && ry < PV_BOT_Y + 30;
    }
    private boolean isOverNextButton(int rx, int ry) {
        return rx >= PV_X + PV_W - 20 && rx < PV_X + PV_W - 4 && ry >= PV_BOT_Y + 14 && ry < PV_BOT_Y + 30;
    }
    private boolean isOverLockButton(int rx, int ry) {
        return rx >= PV_X + PV_W / 2 - 8 && rx < PV_X + PV_W / 2 + 8 && ry >= PV_BOT_Y + 14 && ry < PV_BOT_Y + 30;
    }

    private void showLockedMessage() { lockedMsgTick = 60; }
    private void showUnlockWarning() { unlockWarningTick = 60; }

    private int getRefactorOptionAt(int rx, int ry) {
        if (refactorOptions.isEmpty()) return -1;
        for (int visibleRow = 0; visibleRow < RF_ROWS_VISIBLE; visibleRow++) {
            for (int col = 0; col < RF_COLS; col++) {
                int dataRow = rfScrollRow + visibleRow;
                int optIdx = dataRow * RF_COLS + col;
                if (optIdx >= refactorOptions.size()) continue;
                int bx = RF_BTN_X[visibleRow * RF_COLS + col];
                int by = RF_BTN_Y[visibleRow * RF_COLS + col];
                if (rx >= bx && rx < bx + RF_BTN_W && ry >= by && ry < by + RF_BTN_H) return optIdx;
            }
        }
        return -1;
    }

    private boolean isMouseInRefactorArea(int rx, int ry) {
        return rx >= RF_BTN_X[0] && rx < RF_BTN_X[0] + RF_BTN_W * RF_COLS
            + (RF_SCROLL_X - RF_BTN_X[1] - RF_BTN_W)
            && ry >= RF_BTN_Y[0] && ry < RF_BTN_Y[0] + RF_BTN_H * RF_ROWS_VISIBLE;
    }

    private boolean isMouseInRefactorScrollbar(int rx, int ry) {
        return rx >= RF_SCROLL_X && rx < RF_SCROLL_X + RF_SCROLL_W
            && ry >= RF_SCROLL_Y && ry < RF_SCROLL_Y + RF_SCROLL_H;
    }

    private boolean isOverRefactorStart(int rx, int ry) {
        return rx >= RF_START_X && rx < RF_START_X + RF_START_W
            && ry >= RF_START_Y && ry < RF_START_Y + RF_START_H;
    }

    private void handleRefactorStart() {
        if (selectedRefactorIndex < 0 || selectedRefactorIndex >= refactorOptions.size()) {
            showRefactorError(Component.translatable("screen.anvilcraft.cfa.no_refactor_option"));
            return;
        }
        var be = getMenu().getBlockEntity();
        CelestialRefactorOption option = refactorOptions.get(selectedRefactorIndex);
        if (option.needsMaterial()) {
            ItemStack inSlot = be.getMaterialContainer().getItem(0);
            ItemStack required = option.material().copyWithCount(option.materialCount());
            if (!ItemStack.isSameItemSameComponents(inSlot, required) || inSlot.getCount() < required.getCount()) {
                showRefactorError(Component.translatable("screen.anvilcraft.cfa.insufficient_materials"));
                return;
            }
        }
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, 100 + selectedRefactorIndex);
        }
    }

    private void showRefactorError(Component msg) {
        refactorErrorMsg = msg;
        refactorErrorTick = 60;
    }

    private void sendButtonClick(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, id);
        }
    }

    @Override
    public void resize(int width, int height) {
        this.init(width, height);
    }
}
