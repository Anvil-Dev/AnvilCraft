package dev.dubhe.anvilcraft.client.gui.screen;

import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyMatcher;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorRegistry;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.client.gui.screen.cfa.CelestialBodyInfoFormatter;
import dev.dubhe.anvilcraft.client.gui.screen.cfa.CelestialBodyPreviewRenderer;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.CelestialForgingAnvilMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CelestialForgingAnvilScreen extends AbstractContainerScreen<CelestialForgingAnvilMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("machine", "celestial_forging_anvil");

    private static final int TEX_WIDTH = 512;
    private static final int TEX_HEIGHT = 256;

    // 天体预览区域，坐标从界面左上角开始计算。
    private static final int PV_X = 98;
    private static final int PV_Y = 15;
    private static final int PV_W = 148;
    private static final int PV_H = 99;
    private static final int PV_BODY_W = 59;
    private static final int PV_BODY_H = 59;
    private static final int PV_INFO_X = 157;
    private static final int PV_INFO_Y = 15;
    private static final int PV_INFO_W = 89;
    private static final int PV_INFO_H = 60;
    private static final int PV_BOT_Y = 84;

    // 天体和信息面板下方的资源条。
    private static final int PV_RES_Y = 76;
    private static final int PV_RES_H = 20;

    // 搜索按钮贴图上半部分为普通状态，下半部分为悬停状态。
    private static final int SB_X = 32;
    private static final int SB_Y = 121;
    private static final int SB_W = 48;
    private static final int SB_H = 16;

    // 巨构重构区域。
    private static final int RF_TITLE_X = 266;
    private static final int RF_TITLE_Y = 18;
    private static final int RF_TITLE_W = 71;
    private static final int RF_BTN_W = 36;
    private static final int RF_BTN_H = 35;

    // 四个巨构选项按钮的位置，顺序为左上、右上、左下、右下。
    private static final int[] RF_BTN_X = {
        255,
        291,
        255,
        291
    };
    private static final int[] RF_BTN_Y = {
        39,
        39,
        74,
        74
    };

    // 巨构选项滚动条。
    private static final int RF_SCROLL_X = 331;
    private static final int RF_SCROLL_Y = 39;
    private static final int RF_SCROLL_H = 70;
    private static final int RF_SCROLL_W = 4;
    private static final int RF_SCROLL_THUMB_H = 12;
    private static final int RF_COLS = 2;
    private static final int RF_ROWS_VISIBLE = 2;

    // 开始重构按钮，每种状态在贴图中占 48×16 像素。
    private static final int RF_START_X = 290;
    private static final int RF_START_Y = 121;
    private static final int RF_START_W = 48;
    private static final int RF_START_H = 16;

    private static final String BTN_DIR = "machine/celestial_forging_anvil/";
    private static final Identifier TEX_FORGING = SharedTextures.textureGui(BTN_DIR + "forging");
    private static final Identifier TEX_REFORGING = SharedTextures.textureGui(BTN_DIR + "re_forging");
    private static final Identifier TEX_PREV = SharedTextures.textureGui(BTN_DIR + "previous");
    private static final Identifier TEX_NEXT = SharedTextures.textureGui(BTN_DIR + "next");
    private static final Identifier TEX_UNLOCKED = SharedTextures.textureGui(BTN_DIR + "unlocked");
    private static final Identifier TEX_LOCKED = SharedTextures.textureGui(BTN_DIR + "locked");
    private static final Identifier TEX_REFACTOR_OPTIONS = SharedTextures.textureGui(BTN_DIR + "refactor_options");
    private static final Identifier TEX_REFACTORING = SharedTextures.textureGui(BTN_DIR + "refactoring");

    // 星图引导。
    private static final Identifier TEX_CELESTIAL_MAPS = SharedTextures.texture("block/celestial_maps");
    private static final int MAP_SIZE = 160;
    private static final int COLOR_TIME = 0xBF_A0FFA0;    // light green, 75% alpha
    private static final int COLOR_SPACE = 0xBF_00FFFF;   // cyan, 75% alpha
    private static final int COLOR_MASS = 0xBF_FFFFA0;    // light yellow, 75% alpha
    private static final int COLOR_ENERGY = 0xBF_FF8080;  // light red, 75% alpha
    private static final int COLOR_MINERAL = 0xFFFFFF;
    private static final int COLOR_FLUID = 0x55AAFF;
    private static final int COLOR_BIOLOGICAL = 0x55FF55;
    private static final int COLOR_BIOLOGICAL_FLUID = 0xFF88CC;
    private static final int COLOR_GIANT_ITEM = 0x55FFFF;
    private static final int COLOR_GIANT_FLUID = 0x5555FF;
    private static final int COLOR_OFFERING = 0xFFAA00;
    private static final int COLOR_WASTELAND = 0xAA5500;

    private static final ItemStack[] GHOST_STACKS = {
        new ItemStack(ModBlocks.CONFINED_TIME_ANVILON.asItem()),
        new ItemStack(ModBlocks.CONFINED_SPACE_ANVILON.asItem()),
        new ItemStack(ModBlocks.CONFINED_MASS_ANVILON.asItem()),
        new ItemStack(ModBlocks.CONFINED_ENERGY_ANVILON.asItem())
    };

    // 天体搜索状态。
    private enum SearchState {
        IDLE, LOADING, DONE, FAIL, POWER_FAIL
    }

    private SearchState searchState = SearchState.IDLE;
    @Nullable
    private CelestialBodyData preSearchBody = null;

    // 锁定状态由方块实体持久化。
    private boolean isLocked() {
        return getMenu().getBlockEntity().isLocked();
    }

    private void setLocked(boolean v) {
        getMenu().getBlockEntity().setLocked(v);
    }

    private List<?> searchHistory() {
        return getMenu().getBlockEntity().getSearchHistory();
    }

    // 预览旋转动画。
    private int previewRotTick = 0;

    // 天体信息滚动位置。
    private int scrollOffset = 0;

    // 资源条滚动位置。
    private int resourceScrollOffset = 0;

    // 恒星演化进度的客户端倒计时，每刻递减一次。
    private int localAcceleratorTicksRemaining = 0;

    // 重构区域状态
    private List<CelestialRefactorOption> refactorOptions = List.of();
    private int selectedRefactorIndex = -1;
    private int rfScrollRow = 0;
    private int refactorMaxScroll = 0;
    private boolean isDraggingRfScrollbar = false;
    private static final int BUILT_MEGASTRUCTURE_TEXT_WIDTH = 72;
    private static final int BUILT_MEGASTRUCTURE_TEXT_PADDING = 2;
    private static final int BUILT_MEGASTRUCTURE_WRAP_WIDTH = BUILT_MEGASTRUCTURE_TEXT_WIDTH
        - BUILT_MEGASTRUCTURE_TEXT_PADDING * 2;
    private static final int BUILT_MEGASTRUCTURE_TEXT_SPACING = 12;
    private int refactorErrorTick = 0;
    @Nullable
    private Component refactorErrorMsg = null;
    private int unlockWarningTick = 0;

    // 砧子数量变化时触发星图引导。
    private final int[] previousAnvilCounts = new int[4];
    private boolean guideTriggered = false;

    public CelestialForgingAnvilScreen(CelestialForgingAnvilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 344, 207);
    }

    @Override
    protected void init() {
        super.init();
        CelestialBodyMatcher.warmup();
        int titleAreaCenter = (3 + 342) / 2 - 1;
        this.titleLabelX = titleAreaCenter - this.font.width(this.title) / 2;
        this.titleLabelY = 2;

        var be = getMenu().getBlockEntity();
        if (be.isSearching() && this.searchState == SearchState.IDLE) {
            this.searchState = SearchState.LOADING;
        } else if (be.getCelestialBodyData() != null && this.searchState == SearchState.IDLE) {
            this.searchState = SearchState.DONE;
        }

        for (int i = 0; i < 4; i++) {
            this.previousAnvilCounts[i] = be.getAnvilCount(i);
        }
        this.guideTriggered = false;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.previewRotTick++;

        var be = getMenu().getBlockEntity();
        for (int i = 0; i < 4; i++) {
            int cur = be.getAnvilCount(i);
            if (cur != this.previousAnvilCounts[i]) {
                this.guideTriggered = true;
            }
            this.previousAnvilCounts[i] = cur;
        }
        if (this.searchState == SearchState.LOADING) {
            this.guideTriggered = false;
        }

        if (this.lockedMsgTick > 0) this.lockedMsgTick--;
        if (this.refactorErrorTick > 0) this.refactorErrorTick--;
        if (this.unlockWarningTick > 0) this.unlockWarningTick--;

        // 在客户端推进恒星演化倒计时，避免界面只按服务端同步频率跳动。
        if (be.isAcceleratorActive()) {
            int serverTicks = be.getAcceleratorTicksRemaining();
            if (this.localAcceleratorTicksRemaining <= 0 || serverTicks < this.localAcceleratorTicksRemaining) {
                this.localAcceleratorTicksRemaining = serverTicks;
            }
            if (this.localAcceleratorTicksRemaining > 0) {
                this.localAcceleratorTicksRemaining--;
            }
        } else {
            this.localAcceleratorTicksRemaining = 0;
        }

        if (this.searchState == SearchState.LOADING) {
            CelestialBodyData cur = be.getCelestialBodyData();
            if (be.isPowerInsufficient()) {
                this.searchState = SearchState.POWER_FAIL;
            } else if (cur != null && cur != this.preSearchBody) {
                this.searchState = SearchState.DONE;
            } else if (be.isSearchFailed()) {
                this.searchState = SearchState.FAIL;
            }
        }
    }

    // ==================== 26.1 提取式界面渲染 ====================

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND,
            this.leftPos, this.topPos, 0, 0,
            this.imageWidth, this.imageHeight, TEX_WIDTH, TEX_HEIGHT);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        int guiLeft = this.leftPos;
        int guiTop = this.topPos;
        int relX = mouseX - guiLeft;
        int relY = mouseY - guiTop;

        // 搜索按钮。
        Identifier btnTex = (this.searchState == SearchState.DONE && !this.searchHistory().isEmpty())
            ? TEX_REFORGING : TEX_FORGING;
        boolean hoverSearch = relX >= SB_X && relX < SB_X + SB_W && relY >= SB_Y && relY < SB_Y + SB_H;
        this.renderButton(graphics, btnTex, guiLeft + SB_X, guiTop + SB_Y, SB_W, SB_H, hoverSearch);

        // 预览区域底部按钮。
        this.renderPreviewBottomButtons(graphics, guiLeft, guiTop, relX, relY);

        // 巨构重构区域。
        this.renderRefactorSection(graphics, guiLeft, guiTop, relX, relY);

        // 引导已触发、界面未锁定且未搜索时显示星图。
        // 指南显示时完全取代天体预览/信息/资源框——不再叠加渲染，避免混乱（对齐 1.21：指南分支 return）。
        boolean showGuide = !this.isLocked() && this.guideTriggered && this.searchState != SearchState.LOADING;
        if (showGuide) {
            this.renderCelestialMapsGuide(graphics, guiLeft, guiTop);
            // 在星图上层重新绘制预览按钮。
            this.renderPreviewBottomButtons(graphics, guiLeft, guiTop, relX, relY);
        } else {
            // 预览区域内容包括天体、信息面板和资源条。
            this.renderPreviewAreaContents(graphics, guiLeft, guiTop);
        }

        // 空槽位的提示物品。
        this.renderSlotGhosts(graphics, guiLeft, guiTop);

        // 状态提示消息。
        this.renderStatusMessages(graphics, guiLeft, guiTop);
    }

    /**
     * 搜索完成后绘制三维天体、信息面板和资源条。
     * 此方法在提取阶段使用绝对屏幕坐标。
     */
    private void renderPreviewAreaContents(GuiGraphicsExtractor graphics, int guiLeft, int guiTop) {
        // 增幅天体缺少增幅器时不显示预览内容。
        CelestialBodyData body = getMenu().getBlockEntity().getCelestialBodyData();
        boolean missingAmplifier = body instanceof StarData && !getMenu().getBlockEntity().isAmplifierPresent();
        if (missingAmplifier) {
            Component line1 = Component.translatable("screen.anvilcraft.cfa.missing_amplifier.line1");
            Component line2 = Component.translatable("screen.anvilcraft.cfa.missing_amplifier.line2");
            Component line3 = Component.translatable("screen.anvilcraft.cfa.missing_amplifier.line3");
            int cx = guiLeft + PV_X + PV_W / 2;
            int cy = guiTop + PV_Y + PV_H / 2;
            graphics.text(this.font, line1, cx - this.font.width(line1) / 2, cy - this.font.lineHeight * 3 / 2, 0xFFFF5555, true);
            graphics.text(this.font, line2, cx - this.font.width(line2) / 2, cy - this.font.lineHeight / 2, 0xFFFF5555, true);
            graphics.text(this.font, line3, cx - this.font.width(line3) / 2, cy + this.font.lineHeight / 2, 0xFFFF5555, true);
            return;
        }

        switch (this.searchState) {
            case LOADING -> {
                String base = Component.translatable("screen.anvilcraft.cfa.search_loading").getString();
                int dots = (this.previewRotTick / 10) % 3;
                String text = base + ".".repeat(dots + 1);
                int cx = guiLeft + PV_X + PV_W / 2;
                int cy = guiTop + PV_Y + PV_H / 2;
                graphics.text(this.font, text, cx - this.font.width(text) / 2, cy - this.font.lineHeight / 2, 0xFFFFFFFF, true);
            }
            case FAIL -> {
                Component fail = Component.translatable("screen.anvilcraft.cfa.search_fail");
                int cx = guiLeft + PV_X + PV_W / 2;
                int cy = guiTop + PV_Y + PV_H / 2;
                graphics.text(this.font, fail, cx - this.font.width(fail) / 2, cy - this.font.lineHeight / 2, 0xFFFF5555, true);
            }
            case POWER_FAIL -> {
                Component fail = Component.translatable("screen.anvilcraft.cfa.power_fail");
                int cx = guiLeft + PV_X + PV_W / 2;
                int cy = guiTop + PV_Y + PV_H / 2;
                graphics.text(this.font, fail, cx - this.font.width(fail) / 2, cy - this.font.lineHeight / 2, 0xFFFF5555, true);
            }
            case DONE -> {
                if (body != null) {
                    if (body instanceof SpecialCelestialBodyData special && special.isPlayerHead()) {
                        this.renderPlayerHeadPreview(graphics, special, guiLeft, guiTop);
                    } else {
                        this.renderBodyPreview(graphics, body, guiLeft, guiTop);
                        this.renderBodyInfoAbsolute(graphics, body, guiLeft, guiTop);
                        this.renderResourceBarAbsolute(graphics, guiLeft, guiTop);
                    }
                }
            }
            default -> {
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);

        Component paramsLabel = Component.translatable("screen.anvilcraft.cfa.celestial_params");
        int paramsX = 7 + (72 - this.font.width(paramsLabel)) / 2;
        graphics.text(this.font, paramsLabel, paramsX, 18, 0xFF404040, false);

        Component refactorTitle = Component.translatable("screen.anvilcraft.cfa.refactor_title");
        int refactorTitleX = RF_TITLE_X + (RF_TITLE_W - this.font.width(refactorTitle)) / 2;
        graphics.text(this.font, refactorTitle, refactorTitleX, RF_TITLE_Y, 0xFF404040, false);

        CelestialForgingAnvilMenu menu = this.getMenu();
        this.drawParamText(graphics, CelestialForgingAnvilMenu.formatAge(menu.getBlockEntity().getAnvilCount(0)), 33, 43, 58);
        this.drawParamText(graphics, CelestialForgingAnvilMenu.formatRadius(menu.getBlockEntity().getAnvilCount(1)), 33, 61, 58);
        this.drawParamText(graphics, CelestialForgingAnvilMenu.formatMass(menu.getBlockEntity().getAnvilCount(2)), 33, 79, 58);
        this.drawParamText(graphics, CelestialForgingAnvilMenu.formatTemperature(menu.getBlockEntity().getAnvilCount(3)), 33, 97, 58);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 锻星砧专用槽位使用自定义提示，不提交原版物品提示。
        boolean isCfaSlot = this.hoveredSlot instanceof CelestialForgingAnvilMenu.CFAAnvilSlot
            || this.hoveredSlot instanceof CelestialForgingAnvilMenu.SeedSlot;
        if (!isCfaSlot) {
            super.extractTooltip(graphics, mouseX, mouseY);
        }

        int rx = mouseX - this.leftPos;
        int ry = mouseY - this.topPos;

        // 搜索按钮提示。
        if (rx >= SB_X && rx < SB_X + SB_W && ry >= SB_Y && ry < SB_Y + SB_H) {
            Component tip;
            if (this.isLocked()) {
                tip = Component.translatable("screen.anvilcraft.cfa.locked_tooltip");
            } else if (this.searchState == SearchState.DONE && !this.searchHistory().isEmpty()) {
                tip = Component.translatable("screen.anvilcraft.cfa.re_search_tooltip");
            } else {
                tip = Component.translatable("screen.anvilcraft.cfa.search_tooltip");
            }
            graphics.setTooltipForNextFrame(tip, mouseX, mouseY);
        }

        // 锁定按钮提示。
        if (this.isOverLockButton(rx, ry) && (this.searchState == SearchState.DONE || this.searchState == SearchState.LOADING)) {
            graphics.setTooltipForNextFrame(
                Component.translatable(this.isLocked() ? "screen.anvilcraft.cfa.unlock" : "screen.anvilcraft.cfa.lock"),
                mouseX, mouseY);
        }

        // 已建造巨构和可选重构项的提示。
        if (this.isLocked() && this.searchState == SearchState.DONE) {
            CelestialRefactorOption hoveredOption = null;
            if (this.isHoveringBuiltMegastructureButton(rx, ry)) {
                hoveredOption = getMenu().getBlockEntity().getActiveMegastructureOption();
            } else {
                int refOpt = this.getRefactorOptionAt(rx, ry);
                if (refOpt >= 0 && refOpt < this.refactorOptions.size()) {
                    hoveredOption = this.refactorOptions.get(refOpt);
                }
            }
            if (hoveredOption != null) {
                Component name = Component.translatable(hoveredOption.displayName());
                List<Component> tooltipLines = new ArrayList<>();
                tooltipLines.add(name);
                if (hoveredOption.needsMaterial()) {
                    tooltipLines.add(Component.translatable(
                        "screen.anvilcraft.cfa.material_required",
                        hoveredOption.material().getDisplayName(),
                        Component.literal(String.valueOf(hoveredOption.materialCount()))
                    ));
                }
                if (this.minecraft.hasShiftDown()) {
                    tooltipLines.add(Component.translatable(hoveredOption.displayName() + ".description")
                        .withStyle(ChatFormatting.DARK_GRAY));
                } else {
                    tooltipLines.add(Component.translatable(
                        "tooltip.anvilcraft.press_key",
                        Component.literal("Shift").withStyle(ChatFormatting.DARK_GRAY)
                    ).withStyle(ChatFormatting.DARK_GRAY));
                }
                graphics.setTooltipForNextFrame(this.font, tooltipLines, java.util.Optional.empty(), mouseX, mouseY);
            }
        }

        // 建材槽位提示。
        if (this.isLocked() && this.searchState == SearchState.DONE
            && this.hoveredSlot instanceof CelestialForgingAnvilMenu.CFAMaterialSlot) {
            graphics.setTooltipForNextFrame(
                Component.translatable("screen.anvilcraft.cfa.refactor_materials"), mouseX, mouseY);
        }

        // 开始重构按钮提示。
        if (this.isLocked() && this.searchState == SearchState.DONE
            && this.isRefactorStartVisible() && this.isOverRefactorStart(rx, ry)) {
            graphics.setTooltipForNextFrame(
                Component.translatable("screen.anvilcraft.cfa.refactor_start_tooltip"), mouseX, mouseY);
        }

        // 种子槽位提示。
        if (this.hoveredSlot instanceof CelestialForgingAnvilMenu.SeedSlot) {
            List<Component> seedTooltip = new ArrayList<>();
            seedTooltip.add(Component.translatable("screen.anvilcraft.cfa.seed_slot.title"));
            if (this.minecraft.hasShiftDown()) {
                seedTooltip.add(Component.translatable("screen.anvilcraft.cfa.seed_slot.description")
                    .withStyle(ChatFormatting.DARK_GRAY));
            } else {
                seedTooltip.add(Component.translatable(
                    "tooltip.anvilcraft.press_key",
                    Component.literal("[Shift]").withStyle(ChatFormatting.DARK_GRAY)
                ).withStyle(ChatFormatting.DARK_GRAY));
            }
            graphics.setTooltipForNextFrame(this.font, seedTooltip, java.util.Optional.empty(), mouseX, mouseY);
        }

        // 砧子槽位的有效数量范围提示。
        if (this.hoveredSlot instanceof CelestialForgingAnvilMenu.CFAAnvilSlot cfaSlot) {
            var be = getMenu().getBlockEntity();
            int[] range = CelestialBodyMatcher.getValidRange(
                be.getAnvilCount(0),
                be.getAnvilCount(1),
                be.getAnvilCount(2),
                be.getAnvilCount(3),
                be.isAmplify(),
                cfaSlot.getSlotIndex()
            );
            if (range != null) {
                String text = range[0] == range[1] ? String.valueOf(range[0]) : range[0] + " - " + range[1];
                graphics.setTooltipForNextFrame(Component.literal(text), mouseX, mouseY);
            }
        }
    }

    // ==================== 天体信息面板 ====================

    private void renderBodyInfoAbsolute(GuiGraphicsExtractor graphics, CelestialBodyData body,
                                         int guiLeft, int guiTop) {
        var be = getMenu().getBlockEntity();
        if (be.isAcceleratorActive()) {
            this.renderAcceleratorProgressAbsolute(graphics, be, guiLeft, guiTop);
            return;
        }

        float offsetAge = be.getDisplayOffset(0);
        float offsetRadius = be.getDisplayOffset(1);
        float offsetMass = be.getDisplayOffset(2);
        List<Component> lines = CelestialBodyInfoFormatter.format(body, be.getAgeAnvilCount(), be.getStellarMass(),
            offsetAge, offsetRadius, offsetMass);

        List<Component> displayLines = new ArrayList<>();
        for (Component comp : lines) {
            String text = comp.getString();
            int colonSpace = text.indexOf(": ");
            if (colonSpace > 0) {
                String label = text.substring(0, colonSpace + 1);
                String value = text.substring(colonSpace + 2);
                displayLines.add(Component.literal(label).withStyle(comp.getStyle()));
                displayLines.add(Component.literal(value).withStyle(comp.getStyle()));
            } else {
                displayLines.add(comp);
            }
        }

        int lineHeight = this.font.lineHeight + 1;
        int maxLines = PV_INFO_H / lineHeight;
        int maxScroll = Math.max(0, displayLines.size() - maxLines);
        if (this.scrollOffset > maxScroll) this.scrollOffset = maxScroll;
        if (this.scrollOffset < 0) this.scrollOffset = 0;

        int ax = guiLeft + PV_INFO_X;
        int ay = guiTop + PV_INFO_Y + 2;
        int aw = PV_INFO_W;
        int ah = PV_INFO_H;

        graphics.enableScissor(ax, ay, ax + aw, ay + ah);
        for (int i = this.scrollOffset; i < Math.min(displayLines.size(), this.scrollOffset + maxLines); i++) {
            String lineText = displayLines.get(i).getString();
            int color = lineText.endsWith(":") ? 0xFF888888 : 0xFFFFFFFF;
            graphics.text(this.font, displayLines.get(i),
                ax, ay + (i - this.scrollOffset) * lineHeight, color, false);
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int sbX = ax + aw - 3;
            int sbW = 2;
            graphics.fill(sbX, ay, sbX + sbW, ay + ah, 0x40FFFFFF);
            int thumbH = Math.max(12, ah * maxLines / displayLines.size());
            int thumbY = ay + (ah - thumbH) * this.scrollOffset / maxScroll;
            graphics.fill(sbX, thumbY, sbX + sbW, thumbY + thumbH, 0x80CCCCCC);
        }
    }

    private void renderAcceleratorProgressAbsolute(GuiGraphicsExtractor graphics,
                                                    CelestialForgingAnvilBlockEntity be,
                                                    int guiLeft, int guiTop) {
        List<Component> lines = new ArrayList<>();
        int stage = be.getAcceleratorStage();
        String stageKey = switch (stage) {
            case 1 -> "screen.anvilcraft.cfa.evolution.stage1";
            case 2 -> "screen.anvilcraft.cfa.evolution.stage2";
            case 3 -> "screen.anvilcraft.cfa.evolution.stage3";
            case 4 -> "screen.anvilcraft.cfa.evolution.stage4";
            default -> "screen.anvilcraft.cfa.evolution.stage_unknown";
        };
        lines.add(Component.translatable(stageKey));
        int displayTicks = this.localAcceleratorTicksRemaining > 0
            ? this.localAcceleratorTicksRemaining : be.getAcceleratorTicksRemaining();
        int secondsRemaining = displayTicks / 20;
        lines.add(Component.translatable("screen.anvilcraft.cfa.evolution.time_remaining",
            Component.literal(formatDuration(secondsRemaining))));
        if (be.getAcceleratorTicksTotal() > 0) {
            int pct = (int) ((1.0f - (float) displayTicks / be.getAcceleratorTicksTotal()) * 100);
            lines.add(Component.literal(pct + "%"));
        }
        if (be.isInfinitePower()) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.evolution.infinite_power"));
        }

        int ax = guiLeft + PV_INFO_X;
        int y = guiTop + PV_INFO_Y + 10;
        for (Component line : lines) {
            graphics.text(this.font, line, ax, y, 0xFFFFFFFF, false);
            y += this.font.lineHeight + 1;
        }
    }

    private static String formatDuration(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    // ==================== 天体资源条 ====================

    private void renderResourceBarAbsolute(GuiGraphicsExtractor graphics, int guiLeft, int guiTop) {
        var be = getMenu().getBlockEntity();
        var resources = be.getPlanetaryResourceSet();
        if (resources == null || resources.isEmpty()) return;

        List<ColoredEntry> entries = new ArrayList<>();
        this.collectItemEntries(entries, resources.getMinerals(), COLOR_MINERAL,
            "screen.anvilcraft.cfa.resource.mineral");
        this.collectFluidEntries(entries, resources.getFluids(), COLOR_FLUID,
            "screen.anvilcraft.cfa.resource.fluid");
        this.collectItemEntries(entries, resources.getGiantItems(), COLOR_GIANT_ITEM,
            "screen.anvilcraft.cfa.resource.giant_item");
        this.collectFluidEntries(entries, resources.getGiantFluids(), COLOR_GIANT_FLUID,
            "screen.anvilcraft.cfa.resource.giant_fluid");
        this.collectItemEntries(entries, resources.getBiologicalItems(), COLOR_BIOLOGICAL,
            "screen.anvilcraft.cfa.resource.biological_item");
        this.collectFluidEntries(entries, resources.getBiologicalFluids(), COLOR_BIOLOGICAL_FLUID,
            "screen.anvilcraft.cfa.resource.biological_fluid");
        this.collectItemEntries(entries, resources.getOfferings(), COLOR_OFFERING,
            "screen.anvilcraft.cfa.resource.offering");
        this.collectItemEntries(entries, resources.getWastelandItems(), COLOR_WASTELAND,
            "screen.anvilcraft.cfa.resource.wasteland");
        if (entries.isEmpty()) return;

        String itemSeparator = ", ";
        int headerSpacing = 12;
        int totalW = 0;
        for (int index = 0; index < entries.size(); index++) {
            ColoredEntry entry = entries.get(index);
            totalW += this.resourceEntrySpacing(entries, index, itemSeparator, headerSpacing);
            totalW += this.font.width(entry.text());
        }
        final int contentX = guiLeft + PV_X + 4;
        int contentW = PV_W - 8;
        int maxScroll = Math.max(0, totalW - contentW);
        this.resourceScrollOffset = Mth.clamp(this.resourceScrollOffset, 0, maxScroll);

        int ax = guiLeft + PV_X;
        int ay = guiTop + PV_RES_Y;
        int aw = PV_W;
        int ah = PV_RES_H;

        graphics.enableScissor(ax, ay, ax + aw, ay + ah);

        Component title = Component.translatable("screen.anvilcraft.cfa.resource_title");
        graphics.text(this.font, title, ax + (aw - this.font.width(title)) / 2, ay, 0xFFAAAAAA, false);

        int x = contentX - this.resourceScrollOffset;
        int y = ay + this.font.lineHeight + 1;
        for (int index = 0; index < entries.size(); index++) {
            ColoredEntry entry = entries.get(index);
            x += this.resourceEntrySpacing(entries, index, itemSeparator, headerSpacing);
            int w = this.font.width(entry.text());
            if (x + w >= ax && x <= ax + aw) {
                graphics.text(this.font, entry.text(), x, y, 0xFF000000 | entry.color(), true);
            }
            x += w;
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            int sbY = ay + ah;
            int trackX = ax + 2;
            int trackW = aw - 4;
            graphics.fill(trackX, sbY, trackX + trackW, sbY + 2, 0x40FFFFFF);
            int thumbW = Math.max(12, trackW * contentW / totalW);
            int thumbX = trackX + (trackW - thumbW) * this.resourceScrollOffset / maxScroll;
            graphics.fill(thumbX, sbY, thumbX + thumbW, sbY + 2, 0x80CCCCCC);
        }
    }

    private int resourceEntrySpacing(
        List<ColoredEntry> entries,
        int index,
        String itemSeparator,
        int headerSpacing
    ) {
        if (index == 0) return 0;
        if (entries.get(index).header()) return headerSpacing;
        return entries.get(index - 1).header() ? 0 : this.font.width(itemSeparator);
    }

    private void collectItemEntries(
        List<ColoredEntry> out,
        List<PlanetaryResourceSet.WeightedItemStack> items,
        int color,
        String headerKey
    ) {
        if (items.isEmpty()) return;
        out.add(new ColoredEntry(Component.translatable(headerKey).getString(), color, true));
        items.stream()
            .sorted(Comparator.comparingInt(PlanetaryResourceSet.WeightedItemStack::weight).reversed())
            .forEach(entry -> {
                var itemHolder = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(entry.itemId());
                String name = itemHolder
                    .map(holder -> {
                        Item item = holder.value();
                        return item.getName(new ItemStack(item)).getString();
                    })
                    .orElse("???");
                out.add(new ColoredEntry(name, color, false));
            });
    }

    private void collectFluidEntries(
        List<ColoredEntry> out,
        List<PlanetaryResourceSet.WeightedFluidStack> fluids,
        int color,
        String headerKey
    ) {
        if (fluids.isEmpty()) return;
        out.add(new ColoredEntry(Component.translatable(headerKey).getString(), color, true));
        fluids.stream()
            .sorted(Comparator.comparingInt(PlanetaryResourceSet.WeightedFluidStack::weight).reversed())
            .forEach(entry -> {
                var fluidHolder = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(entry.fluidId());
                String name = fluidHolder
                    .map(holder -> holder.value().getFluidType().getDescription().getString())
                    .orElse("???");
                out.add(new ColoredEntry(name, color, false));
            });
    }

    private record ColoredEntry(String text, int color, boolean header) {
    }

    // ==================== 三维天体预览 ====================

    private void renderBodyPreview(GuiGraphicsExtractor graphics, CelestialBodyData body, int guiLeft, int guiTop) {
        CelestialBodyPreviewRenderer.render(
            graphics,
            body,
            this.previewRotTick,
            getMenu().getBlockEntity().getBlockPos().asLong(),
            guiLeft + PV_X,
            guiTop + PV_Y,
            PV_BODY_W,
            PV_BODY_H
        );
    }

    private void renderPlayerHeadPreview(
        GuiGraphicsExtractor graphics,
        SpecialCelestialBodyData special,
        int guiLeft,
        int guiTop
    ) {
        String playerName = CelestialBodyPreviewRenderer.playerName(special);
        if (playerName != null) {
            int nameX = guiLeft + PV_X + (PV_W - this.font.width(playerName)) / 2;
            graphics.text(this.font, playerName, nameX, guiTop + PV_Y + 4, 0xFFFFFFFF, true);
        }
        CelestialBodyPreviewRenderer.render(
            graphics,
            special,
            this.previewRotTick,
            getMenu().getBlockEntity().getBlockPos().asLong(),
            guiLeft + PV_X,
            guiTop + PV_Y + 10,
            PV_W,
            PV_H - 10
        );
    }

    // ==================== 按钮渲染 ====================

    private void renderButton(GuiGraphicsExtractor g, Identifier tex, int x, int y, int w, int h, boolean hovered) {
        int v = hovered ? h : 0;
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0, v, w, h, w, h * 2);
    }

    private void renderPreviewBottomButtons(GuiGraphicsExtractor graphics, int guiLeft, int guiTop,
                                            int relX, int relY) {
        if (this.searchState != SearchState.DONE && this.searchState != SearchState.LOADING) return;
        var be = getMenu().getBlockEntity();
        boolean hasPrev = be.hasPreviousHistory();
        boolean hasNext = be.hasNextHistory();
        // 上一个天体按钮。
        if (hasPrev) {
            boolean hover = this.isOverPrevButton(relX, relY);
            this.renderButton(graphics, TEX_PREV, guiLeft + PV_X + 4, guiTop + PV_BOT_Y + 14, 16, 16, hover);
        }
        // 下一个天体按钮。
        if (hasNext) {
            boolean hover = this.isOverNextButton(relX, relY);
            this.renderButton(graphics, TEX_NEXT, guiLeft + PV_X + PV_W - 20, guiTop + PV_BOT_Y + 14, 16, 16, hover);
        }
        // 锁定按钮。
        boolean hoverLock = this.isOverLockButton(relX, relY);
        Identifier lockTex = this.isLocked() ? TEX_LOCKED : TEX_UNLOCKED;
        this.renderButton(graphics, lockTex, guiLeft + PV_X + PV_W / 2 - 8, guiTop + PV_BOT_Y + 14, 16, 16, hoverLock);
    }

    @SuppressWarnings("SameParameterValue")
    private void drawParamText(GuiGraphicsExtractor g, String text, int x, int y, int width) {
        int textX = x + (width - this.font.width(text)) / 2;
        g.text(this.font, text, textX, y, 0xFFFFFFFF, true);
    }

    // ==================== 槽位提示物品渲染 ====================

    private void renderSlotGhosts(GuiGraphicsExtractor g, int guiLeft, int guiTop) {
        for (Slot slot : this.menu.slots) {
            if (slot instanceof CelestialForgingAnvilMenu.CFAAnvilSlot && !slot.hasItem()) {
                int idx = slot.getSlotIndex();
                if (idx >= 0 && idx < GHOST_STACKS.length) {
                    int sx = guiLeft + slot.x;
                    int sy = guiTop + slot.y;
                    GuiRenderExtras.itemWithTransparency(g, GHOST_STACKS[idx], sx, sy, 0.52F);
                    g.fill(sx, sy, sx + 16, sy + 16, 0x60FFAAAA);
                }
            }
            if (slot instanceof CelestialForgingAnvilMenu.CFAMaterialSlot && !slot.hasItem()) {
                var be = getMenu().getBlockEntity();
                ItemStack filter = be.getMaterialFilter();
                if (!filter.isEmpty()) {
                    int sx = guiLeft + slot.x;
                    int sy = guiTop + slot.y;
                    GuiRenderExtras.itemWithTransparency(g, filter, sx, sy, 0.52F);
                    g.fill(sx, sy, sx + 16, sy + 16, 0x60FFAAAA);
                    int limit = be.getMaterialLimit();
                    if (limit > 0 && !filter.is(Items.BARRIER)) {
                        String countStr = String.valueOf(limit);
                        var ps = g.pose();
                        ps.pushMatrix();
                        ps.translate(0, 0);
                        ps.scale(0.6f, 0.6f);
                        int tx = (int) ((sx + 16.25f - this.font.width(countStr) * 0.6f) / 0.6f);
                        int ty = (int) ((sy + 14f - this.font.lineHeight * 2f * 0.6f + 1) / 0.6f);
                        g.text(this.font, countStr, tx, ty, 0xFFFFA0A0, true);
                        ps.popMatrix();
                    }
                }
            }
        }
    }

    // ==================== 锁定、重构及演化状态提示 ====================

    private void renderStatusMessages(GuiGraphicsExtractor g, int guiLeft, int guiTop) {
        if (this.lockedMsgTick > 0) {
            Component msg = Component.translatable("screen.anvilcraft.cfa.locked_tooltip");
            int w = this.font.width(msg);
            g.text(this.font, msg, guiLeft + (this.imageWidth - w) / 2, guiTop - 12, 0xFFFF5555, true);
        }
        if (this.refactorErrorTick > 0 && this.refactorErrorMsg != null) {
            int w = this.font.width(this.refactorErrorMsg);
            g.text(this.font, this.refactorErrorMsg, guiLeft + (this.imageWidth - w) / 2, guiTop - 12, 0xFFFF5555, true);
        }
        if (this.unlockWarningTick > 0) {
            Component msg = Component.translatable("screen.anvilcraft.cfa.unlock_warning");
            int w = this.font.width(msg);
            g.text(this.font, msg, guiLeft + (this.imageWidth - w) / 2, guiTop - 12, 0xFFFF5555, true);
        }
        if (getMenu().getBlockEntity().isAcceleratorActive()) {
            Component msg = Component.translatable("screen.anvilcraft.cfa.evolution_cannot_unlock");
            int w = this.font.width(msg);
            g.text(this.font, msg, guiLeft + (this.imageWidth - w) / 2, guiTop - 12, 0xFFFF5555, true);
        }
    }

    // ==================== 星图引导 ====================

    private void renderCelestialMapsGuide(GuiGraphicsExtractor g, int guiLeft, int guiTop) {
        int previewCenterX = guiLeft + PV_X + PV_W / 2;
        int previewCenterY = guiTop + PV_Y + PV_H / 2;

        var ps = g.pose();
        ps.pushMatrix();
        float scale = 0.5f;
        ps.translate(previewCenterX, previewCenterY);
        ps.scale(scale, scale);
        ps.translate(-MAP_SIZE / 2.0f, -MAP_SIZE / 2.0f);

        // 将 160×160 的星图贴图缩放到界面中的 80×80 区域。
        g.blit(RenderPipelines.GUI_TEXTURED, TEX_CELESTIAL_MAPS, 0, 0, 0, 0, MAP_SIZE, MAP_SIZE, MAP_SIZE, MAP_SIZE);

        int timeCount = getMenu().getBlockEntity().getAnvilCount(0);
        int spaceCount = getMenu().getBlockEntity().getAnvilCount(1);
        int massCount = getMenu().getBlockEntity().getAnvilCount(2);
        final int energyCount = getMenu().getBlockEntity().getAnvilCount(3);

        // 时间砧：浅绿色纵向区域，覆盖贴图完整高度。
        if (timeCount > 0) {
            int x = 11 + Math.round((timeCount - 1) * 64.0f / 63.0f);
            g.fill(x, 0, x + 2, MAP_SIZE, COLOR_TIME);
            String text = String.valueOf(timeCount);
            int textX = x + 1 - this.font.width(text) / 2;
            int textY = -this.font.lineHeight - 4;
            g.text(this.font, text, textX, textY, COLOR_TIME, false);
        }

        // 空间砧：青色横向区域，覆盖贴图完整宽度。
        if (spaceCount > 0) {
            int y = MAP_SIZE - Math.round(92 + (spaceCount - 1) * 64.0f / 63.0f) - 2;
            g.fill(0, y, MAP_SIZE, y + 2, COLOR_SPACE);
            String text = String.valueOf(spaceCount);
            int textX = -this.font.width(text) - 6;
            int textY = y + 1 - this.font.lineHeight / 2;
            g.text(this.font, text, textX, textY, COLOR_SPACE, false);
        }

        // 质量砧：浅黄色纵向区域，位于贴图上半部分。
        if (massCount > 0) {
            int x = 91 + Math.round((massCount - 1) * 64.0f / 63.0f);
            g.fill(x, 0, x + 2, MAP_SIZE / 2, COLOR_MASS);
            String text = String.valueOf(massCount);
            int textX = x + 1 - this.font.width(text) / 2;
            int textY = -this.font.lineHeight - 4;
            g.text(this.font, text, textX, textY, COLOR_MASS, false);
        }

        // 能量砧：浅红色横向区域，位于贴图左半部分。
        if (energyCount > 0) {
            int y = MAP_SIZE - Math.round(12 + (energyCount - 1) * 64.0f / 63.0f) - 2;
            g.fill(0, y, MAP_SIZE / 2, y + 2, COLOR_ENERGY);
            String text = String.valueOf(energyCount);
            int textX = -this.font.width(text) - 6;
            int textY = y + 1 - this.font.lineHeight / 2;
            g.text(this.font, text, textX, textY, COLOR_ENERGY, false);
        }

        // 三步星图匹配提示。
        this.renderGuideStepText(g, timeCount, spaceCount, massCount, energyCount);

        ps.popMatrix();
    }

    private void renderGuideStepText(GuiGraphicsExtractor g, int time, int space, int mass, int energy) {
        int textX = 88;
        int lineSpacing = this.font.lineHeight + 5;
        int y0 = 108;

        // 第一步：根据质量与空间在质量-半径图中向上确定类型。
        int step1Rgb = CelestialBodyMatcher.getMassRadiusRgb(mass, space);
        String step1Name = getTypeDisplayName(step1Rgb);
        this.drawGuideLine(g, "↑" + step1Name, textX, y0, 0xFFCCCCCC);

        // 第二步：根据时间与能量在年龄-温度图中向左确定类型。
        CelestialBodyClass step1Class = CelestialBodyClass.fromRgb(step1Rgb);
        int step2Rgb;
        if (step1Class != null && step1Class.step2UsesSp()) {
            step2Rgb = CelestialBodyMatcher.getAgeTempSpRgb(time, energy);
        } else {
            step2Rgb = CelestialBodyMatcher.getAgeTempRgb(time, energy);
        }
        String step2Name = getTypeDisplayName(step2Rgb);
        this.drawGuideLine(g, "←" + step2Name, textX, y0 + lineSpacing * 2, 0xFFCCCCCC);

        // 第三步：根据时间与空间在年龄-半径图中向左上确定类型。
        int step3Rgb = CelestialBodyMatcher.getAgeRadiusRgb(time, space);
        String step3Name = getTypeDisplayName(step3Rgb);
        this.drawGuideLine(g, "↖" + step3Name, textX, y0 + lineSpacing, 0xFFCCCCCC);
    }

    private static String getTypeDisplayName(int rgb) {
        if (rgb == 0x000000) {
            return Component.translatable("screen.anvilcraft.cfa.class.no_match").getString();
        }
        CelestialBodyClass bodyClass = CelestialBodyClass.fromRgb(rgb);
        if (bodyClass == null) {
            return Component.translatable("screen.anvilcraft.cfa.class.no_match").getString();
        }
        String key;
        if (bodyClass.isRockyPlanet()) {
            key = "screen.anvilcraft.cfa.class.rocky_planet";
        } else {
            key = "screen.anvilcraft.cfa.class." + bodyClass.name().toLowerCase();
        }
        return Component.translatable(key).getString();
    }

    @SuppressWarnings("SameParameterValue")
    private void drawGuideLine(GuiGraphicsExtractor g, String text, int x, int y, int color) {
        g.text(this.font, text, x, y, color, false);
    }

    // ==================== 巨构重构区域 ====================

    private void renderRefactorSection(GuiGraphicsExtractor g, int guiLeft, int guiTop, int relX, int relY) {
        CelestialBodyData body = getMenu().getBlockEntity().getCelestialBodyData();
        boolean hasAcceleratorActive = getMenu().getBlockEntity().isAcceleratorActive();
        boolean hasMegastructure = getMenu().getBlockEntity().getActiveMegastructureIndex() >= 0;
        CelestialRefactorOption activeOption = hasMegastructure
            ? getMenu().getBlockEntity().getActiveMegastructureOption()
            : null;
        boolean isAmplifiedPlanet = getMenu().getBlockEntity().isAmplify() && !(body instanceof StarData);
        boolean isActive = this.isLocked() && body != null && this.searchState == SearchState.DONE
            && !hasAcceleratorActive;

        if (isActive) {
            this.refactorOptions = CelestialRefactorRegistry.getOptions(
                body,
                getMenu().getBlockEntity().isAmplify(),
                getMenu().getBlockEntity().getPlanetaryResourceSet()
            );
            if (hasMegastructure) {
                this.refactorOptions = this.refactorOptions.stream()
                    .filter(opt -> "stellar_evolution_accelerator".equals(opt.megastructure()))
                    .toList();
            }
        } else {
            this.refactorOptions = List.of();
        }
        if (isAmplifiedPlanet) {
            this.refactorOptions = List.of();
        }

        int btnCount = this.refactorOptions.size();
        List<FormattedCharSequence> usageLines = List.of();
        if (hasMegastructure && activeOption != null) {
            Component usage = Component.translatable(
                "screen.anvilcraft.cfa.megastructure." + activeOption.megastructure() + ".usage"
            );
            usageLines = this.font.split(usage, BUILT_MEGASTRUCTURE_WRAP_WIDTH);
        }

        int linesPerTextRow = 3;
        if (hasMegastructure) {
            int buttonRows = (1 + btnCount + RF_COLS - 1) / RF_COLS;
            int textRows = usageLines.isEmpty() ? 0 : (usageLines.size() + linesPerTextRow - 1) / linesPerTextRow;
            this.refactorMaxScroll = Math.max(0, buttonRows + textRows - RF_ROWS_VISIBLE);
        } else {
            int totalRows = btnCount > 0 ? (btnCount + RF_COLS - 1) / RF_COLS : 0;
            this.refactorMaxScroll = Math.max(0, totalRows - RF_ROWS_VISIBLE);
        }
        if (this.rfScrollRow > this.refactorMaxScroll) this.rfScrollRow = this.refactorMaxScroll;
        if (this.rfScrollRow < 0) this.rfScrollRow = 0;
        if (this.selectedRefactorIndex >= btnCount) this.selectedRefactorIndex = -1;

        // 渲染可见的按钮行或已建造巨构说明行。
        for (int visibleRow = 0; visibleRow < RF_ROWS_VISIBLE; visibleRow++) {
            int contentRow = this.rfScrollRow + visibleRow;
            if (hasMegastructure) {
                int buttonRows = (1 + btnCount + RF_COLS - 1) / RF_COLS;
                if (contentRow < buttonRows) {
                    for (int col = 0; col < RF_COLS; col++) {
                        boolean builtButton = contentRow == 0 && col == 0;
                        int optIdx = builtButton ? -1 : contentRow * RF_COLS + col - 1;
                        int bx = guiLeft + RF_BTN_X[visibleRow * RF_COLS + col];
                        int by = guiTop + RF_BTN_Y[visibleRow * RF_COLS + col];

                        if (builtButton && activeOption != null) {
                            this.renderButton(g, TEX_REFACTOR_OPTIONS, bx, by, RF_BTN_W, RF_BTN_H, true);
                            this.renderMegastructureModel(g, activeOption, bx, by);
                        } else if (optIdx >= 0 && optIdx < btnCount) {
                            boolean hovered = isOverRefactorButton(relX, relY, visibleRow, col);
                            this.renderRefactorOptionButton(g, optIdx, bx, by, hovered);
                        }
                    }
                } else {
                    int textRow = contentRow - buttonRows;
                    int rowY = guiTop + RF_BTN_Y[visibleRow * RF_COLS] + BUILT_MEGASTRUCTURE_TEXT_PADDING;
                    for (int line = 0; line < linesPerTextRow; line++) {
                        int lineIndex = textRow * linesPerTextRow + line;
                        if (lineIndex >= usageLines.size()) break;
                        g.text(
                            this.font,
                            usageLines.get(lineIndex),
                            guiLeft + RF_BTN_X[0] + BUILT_MEGASTRUCTURE_TEXT_PADDING,
                            rowY + line * BUILT_MEGASTRUCTURE_TEXT_SPACING,
                            0xFFAAAAAA,
                            false
                        );
                    }
                }
            } else {
                for (int col = 0; col < RF_COLS; col++) {
                    int optIdx = contentRow * RF_COLS + col;
                    if (optIdx >= btnCount) continue;
                    int bx = guiLeft + RF_BTN_X[visibleRow * RF_COLS + col];
                    int by = guiTop + RF_BTN_Y[visibleRow * RF_COLS + col];
                    boolean hovered = isOverRefactorButton(relX, relY, visibleRow, col);
                    this.renderRefactorOptionButton(g, optIdx, bx, by, hovered);
                }
            }
        }

        if (this.refactorMaxScroll > 0) {
            this.renderRefactorScrollbar(g, guiLeft, guiTop, this.refactorMaxScroll);
        }

        boolean showStartButton = !hasMegastructure || !this.refactorOptions.isEmpty();
        if (showStartButton) {
            boolean hoverStart = relX >= RF_START_X && relX < RF_START_X + RF_START_W
                && relY >= RF_START_Y && relY < RF_START_Y + RF_START_H;
            this.renderButton(g, TEX_REFACTORING, guiLeft + RF_START_X, guiTop + RF_START_Y,
                RF_START_W, RF_START_H, hoverStart);
        }

        if (isAmplifiedPlanet && this.isLocked() && body != null && this.searchState == SearchState.DONE) {
            Component warning = Component.translatable("screen.anvilcraft.cfa.amplified_planet_warning");
            List<FormattedCharSequence> warningLines = this.font.split(warning, BUILT_MEGASTRUCTURE_TEXT_WIDTH);
            int areaCenterY = (RF_BTN_Y[0] + RF_BTN_Y[2] + RF_BTN_H) / 2;
            int startY = guiTop + areaCenterY - warningLines.size() * (this.font.lineHeight + 1) / 2;
            int areaCenterX = (RF_BTN_X[0] + RF_BTN_X[1] + RF_BTN_W) / 2;
            for (int i = 0; i < warningLines.size(); i++) {
                FormattedCharSequence line = warningLines.get(i);
                int x = guiLeft + areaCenterX - this.font.width(line) / 2;
                g.text(this.font, line, x, startY + i * (this.font.lineHeight + 1), 0xFFFF5555, true);
            }
        } else if (!this.isLocked() || body == null || this.searchState != SearchState.DONE) {
            Component needLock = Component.translatable("screen.anvilcraft.cfa.need_lock");
            int areaCX = (RF_BTN_X[0] + RF_BTN_X[1] + RF_BTN_W) / 2;
            int areaCY = (RF_BTN_Y[0] + RF_BTN_Y[2] + RF_BTN_H) / 2;
            int cx = guiLeft + areaCX - this.font.width(needLock) / 2;
            int cy = guiTop + areaCY - this.font.lineHeight / 2;
            g.text(this.font, needLock, cx, cy, 0xFF888888, false);
        }
    }

    private void renderRefactorScrollbar(GuiGraphicsExtractor g, int guiLeft, int guiTop, int maxScroll) {
        if (maxScroll < 1) return;
        int scrollX = guiLeft + RF_SCROLL_X;
        int maxY = RF_SCROLL_Y + RF_SCROLL_H - RF_SCROLL_THUMB_H;
        int scrollY = RF_SCROLL_Y + (this.rfScrollRow * (RF_SCROLL_H - RF_SCROLL_THUMB_H) / maxScroll);
        scrollY = Mth.clamp(scrollY, RF_SCROLL_Y, maxY);
        g.blit(RenderPipelines.GUI_TEXTURED, SharedTextures.SWITCH_TABLE_SLIDER,
            scrollX, guiTop + scrollY, 0, 0, RF_SCROLL_W, RF_SCROLL_THUMB_H, 8, 12);
    }

    /** 在按钮内提交对应巨构的真实模型预览。 */
    private void renderMegastructureModel(
        GuiGraphicsExtractor graphics,
        CelestialRefactorOption option,
        int x,
        int y
    ) {
        CelestialBodyPreviewRenderer.renderMegastructure(
            graphics,
            option,
            this.previewRotTick,
            x,
            y,
            RF_BTN_W,
            RF_BTN_H
        );
    }

    private void renderRefactorOptionButton(
        GuiGraphicsExtractor graphics,
        int optionIndex,
        int x,
        int y,
        boolean hovered
    ) {
        boolean selected = optionIndex == this.selectedRefactorIndex;
        this.renderButton(
            graphics,
            TEX_REFACTOR_OPTIONS,
            x,
            y,
            RF_BTN_W,
            RF_BTN_H,
            hovered || selected
        );
        this.renderMegastructureModel(graphics, this.refactorOptions.get(optionIndex), x, y);
        if (selected) {
            graphics.fill(x, y, x + RF_BTN_W, y + RF_BTN_H, 0x40_00FF00);
        }
    }

    private static boolean isOverRefactorButton(int mouseX, int mouseY, int visibleRow, int column) {
        int index = visibleRow * RF_COLS + column;
        return mouseX >= RF_BTN_X[index]
            && mouseX < RF_BTN_X[index] + RF_BTN_W
            && mouseY >= RF_BTN_Y[index]
            && mouseY < RF_BTN_Y[index] + RF_BTN_H;
    }

    // ==================== 26.1 鼠标事件处理 ====================

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int rx = (int) event.x() - this.leftPos;
        int ry = (int) event.y() - this.topPos;

        if (this.isOverSearchButton(rx, ry)) {
            if (this.isLocked()) {
                this.showLockedMessage();
                return true;
            }
            this.performSearch();
            return true;
        }
        if (this.isOverPrevButton(rx, ry) && getMenu().getBlockEntity().hasPreviousHistory()) {
            if (this.isLocked()) {
                this.showLockedMessage();
                return true;
            }
            this.sendButtonClick(201);
            return true;
        }
        if (this.isOverNextButton(rx, ry) && getMenu().getBlockEntity().hasNextHistory()) {
            if (this.isLocked()) {
                this.showLockedMessage();
                return true;
            }
            this.sendButtonClick(202);
            return true;
        }
        if (this.isOverLockButton(rx, ry) && (this.searchState == SearchState.DONE || this.searchState == SearchState.LOADING)) {
            if (getMenu().getBlockEntity().isAcceleratorActive()) {
                return true;
            }
            if (this.isLocked() && !this.minecraft.hasShiftDown()) {
                this.showUnlockWarning();
                return true;
            }
            // 使用按钮编号 200 将锁定切换请求发送到服务端。
            this.sendButtonClick(200);
            this.setLocked(!this.isLocked());
            return true;
        }
        // 处理巨构选项点击。
        int optIdx = this.getRefactorOptionAt(rx, ry);
        if (optIdx >= 0) {
            this.selectedRefactorIndex = optIdx;
            this.sendButtonClick(9 + optIdx);
            return true;
        }
        // 处理开始重构按钮点击。
        if (this.isRefactorStartVisible() && this.isOverRefactorStart(rx, ry)) {
            this.handleRefactorStart();
            return true;
        }
        // 开始拖动巨构选项滚动条。
        if (this.isMouseInRefactorScrollbar(rx, ry)) {
            int maxScroll = this.getRefactorMaxScroll();
            if (maxScroll > 0) {
                this.isDraggingRfScrollbar = true;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isDraggingRfScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        int ry = (int) event.y() - this.topPos;
        if (this.isDraggingRfScrollbar) {
            int maxScroll = this.getRefactorMaxScroll();
            if (maxScroll > 0) {
                float scroll = (ry - RF_SCROLL_Y - RF_SCROLL_THUMB_H / 2f)
                    / (RF_SCROLL_H - RF_SCROLL_THUMB_H);
                this.rfScrollRow = Mth.clamp((int) (scroll * maxScroll + 0.5f), 0, maxScroll);
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;

        // 在参数区域滚动时转移对应砧子。
        if (MathUtil.isInRange(relX, 33, 91)) {
            int paramIdx = -1;
            if (MathUtil.isInRange(relY, 42, 53)) {
                paramIdx = 0;
            } else if (MathUtil.isInRange(relY, 60, 71)) {
                paramIdx = 1;
            } else if (MathUtil.isInRange(relY, 78, 89)) {
                paramIdx = 2;
            } else if (MathUtil.isInRange(relY, 96, 107)) {
                paramIdx = 3;
            }
            if (paramIdx >= 0 && this.minecraft.player != null) {
                int buttonId = scrollY > 0 ? 1 + paramIdx : 5 + paramIdx;
                this.minecraft.player.connection.send(
                    new ServerboundContainerButtonClickPacket(this.menu.containerId, buttonId));
                return true;
            }
        }

        // 在信息区域滚动文本。
        if (relX >= PV_INFO_X && relX < PV_INFO_X + PV_INFO_W
            && relY >= PV_INFO_Y && relY < PV_INFO_Y + PV_INFO_H) {
            this.scrollOffset -= (int) scrollY;
            return true;
        }

        // 在资源条上按像素横向滚动。
        if (relX >= PV_X && relX < PV_X + PV_W && relY >= PV_RES_Y && relY < PV_RES_Y + PV_RES_H) {
            this.resourceScrollOffset -= (int) scrollY * 30;
            return true;
        }

        // 在巨构选项区域滚动按钮行。
        if (this.isMouseInRefactorArea(relX, relY) || this.isMouseInRefactorScrollbar(relX, relY)) {
            int maxScroll = this.getRefactorMaxScroll();
            if (maxScroll > 0) {
                this.rfScrollRow = (int) Mth.clamp(this.rfScrollRow - scrollY, 0, maxScroll);
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ==================== 天体搜索 ====================

    private void performSearch() {
        if (this.searchState == SearchState.LOADING) return;
        var be = getMenu().getBlockEntity();
        boolean hasSeedItem = !be.getAnvilInventory().getItem(4).isEmpty();
        if (!hasSeedItem && this.minecraft.level != null) {
            var preCheck = CelestialBodyMatcher.match(
                be.getAnvilCount(0),
                be.getAnvilCount(1),
                be.getAnvilCount(2),
                be.getAnvilCount(3),
                be.isAmplify(),
                this.minecraft.level.getRandom()
            );
            if (preCheck == null) {
                this.searchState = SearchState.FAIL;
                return;
            }
        }
        this.preSearchBody = be.getCelestialBodyData();
        this.sendButtonClick(0);
        this.searchState = SearchState.LOADING;
    }

    // ==================== 鼠标命中检测 ====================

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
        return rx >= PV_X + PV_W / 2 - 8 && rx < PV_X + PV_W / 2 + 8
            && ry >= PV_BOT_Y + 14 && ry < PV_BOT_Y + 30;
    }

    private int lockedMsgTick = 0;

    private void showLockedMessage() {
        this.lockedMsgTick = 60;
    }

    private void showUnlockWarning() {
        this.unlockWarningTick = 60;
    }

    // ==================== 巨构重构界面工具方法 ====================

    private int getRefactorOptionAt(int rx, int ry) {
        if (this.refactorOptions.isEmpty()) return -1;
        boolean hasMegastructure = getMenu().getBlockEntity().getActiveMegastructureIndex() >= 0;
        for (int visibleRow = 0; visibleRow < RF_ROWS_VISIBLE; visibleRow++) {
            for (int col = 0; col < RF_COLS; col++) {
                int contentRow = this.rfScrollRow + visibleRow;
                int optIdx;
                if (hasMegastructure) {
                    if (contentRow == 0 && col == 0) continue;
                    optIdx = contentRow * RF_COLS + col - 1;
                } else {
                    optIdx = contentRow * RF_COLS + col;
                }
                if (optIdx < 0 || optIdx >= this.refactorOptions.size()) continue;
                int bx = RF_BTN_X[visibleRow * RF_COLS + col];
                int by = RF_BTN_Y[visibleRow * RF_COLS + col];
                if (rx >= bx && rx < bx + RF_BTN_W && ry >= by && ry < by + RF_BTN_H) {
                    return optIdx;
                }
            }
        }
        return -1;
    }

    private boolean isMouseInRefactorArea(int rx, int ry) {
        return rx >= RF_BTN_X[0]
            && rx < RF_BTN_X[0] + RF_BTN_W * RF_COLS + (RF_SCROLL_X - RF_BTN_X[1] - RF_BTN_W)
            && ry >= RF_BTN_Y[0]
            && ry < RF_BTN_Y[0] + RF_BTN_H * RF_ROWS_VISIBLE;
    }

    private boolean isMouseInRefactorScrollbar(int rx, int ry) {
        return rx >= RF_SCROLL_X && rx < RF_SCROLL_X + RF_SCROLL_W
            && ry >= RF_SCROLL_Y && ry < RF_SCROLL_Y + RF_SCROLL_H;
    }

    private int getRefactorMaxScroll() {
        return this.refactorMaxScroll;
    }

    private boolean isHoveringBuiltMegastructureButton(int rx, int ry) {
        if (this.rfScrollRow != 0 || getMenu().getBlockEntity().getActiveMegastructureIndex() < 0) return false;
        return rx >= RF_BTN_X[0] && rx < RF_BTN_X[0] + RF_BTN_W
            && ry >= RF_BTN_Y[0] && ry < RF_BTN_Y[0] + RF_BTN_H;
    }

    private boolean isRefactorStartVisible() {
        return getMenu().getBlockEntity().getActiveMegastructureIndex() < 0 || !this.refactorOptions.isEmpty();
    }

    private boolean isOverRefactorStart(int rx, int ry) {
        return rx >= RF_START_X && rx < RF_START_X + RF_START_W
            && ry >= RF_START_Y && ry < RF_START_Y + RF_START_H;
    }

    private void handleRefactorStart() {
        if (this.selectedRefactorIndex < 0 || this.selectedRefactorIndex >= this.refactorOptions.size()) {
            this.showRefactorError(Component.translatable("screen.anvilcraft.cfa.no_refactor_option"));
            return;
        }
        var be = getMenu().getBlockEntity();
        CelestialRefactorOption option = this.refactorOptions.get(this.selectedRefactorIndex);
        if (option.needsMaterial()) {
            ItemStack inSlot = be.getMaterialContainer().getItem(0);
            ItemStack required = option.material().copyWithCount(option.materialCount());
            if (!ItemStack.isSameItem(inSlot, required) || inSlot.getCount() < required.getCount()) {
                this.showRefactorError(Component.translatable("screen.anvilcraft.cfa.insufficient_materials"));
                return;
            }
        }
        this.sendButtonClick(100 + this.selectedRefactorIndex);
    }

    private void showRefactorError(Component msg) {
        this.refactorErrorMsg = msg;
        this.refactorErrorTick = 60;
    }

    private void sendButtonClick(int id) {
        if (this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, id);
        }
    }

    @Override
    public void resize(int width, int height) {
        this.init(width, height);
    }
}
