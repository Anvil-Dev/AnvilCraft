package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyMatcher;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorRegistry;
import dev.dubhe.anvilcraft.block.entity.celestial.GiantPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.LiquidCoverage;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.RockyPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.Temperature;
import dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.CelestialBodyRenderer;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CelestialForgingAnvilScreen extends AbstractContainerScreen<CelestialForgingAnvilMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("machine", "celestial_forging_anvil");

    private static final int TEX_WIDTH = 512;
    private static final int TEX_HEIGHT = 256;

    // Preview area (0-indexed)
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

    // Resource bar (thin strip below body + info panel)
    private static final int PV_RES_Y = 76;
    private static final int PV_RES_H = 20;

    // Search button (sprite: 48x32 = top half normal, bottom half hover)
    private static final int SB_X = 32;
    private static final int SB_Y = 121;
    private static final int SB_W = 48;
    private static final int SB_H = 16;

    // Refactor section
    private static final int RF_TITLE_X = 266;
    private static final int RF_TITLE_Y = 18;
    private static final int RF_TITLE_W = 71;
    private static final int RF_BTN_W = 36;
    private static final int RF_BTN_H = 35;

    // Four button positions (TL, TR, BL, BR)
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

    // Refactor option scrollbar
    private static final int RF_SCROLL_X = 331;
    private static final int RF_SCROLL_Y = 39;
    private static final int RF_SCROLL_H = 70;
    private static final int RF_SCROLL_W = 4;
    private static final int RF_SCROLL_THUMB_H = 12;
    private static final int RF_COLS = 2;
    private static final int RF_ROWS_VISIBLE = 2;

    // Start refactoring button (sprite: 48x16 each state, 48x32 total)
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

    // Celestial Maps guide
    private static final Identifier TEX_CELESTIAL_MAPS = SharedTextures.texture("block/celestial_maps");
    private static final int MAP_SIZE = 160;
    private static final int COLOR_TIME = 0xBF_A0FFA0;    // light green, 75% alpha
    private static final int COLOR_SPACE = 0xBF_00FFFF;   // cyan, 75% alpha
    private static final int COLOR_MASS = 0xBF_FFFFA0;    // light yellow, 75% alpha
    private static final int COLOR_ENERGY = 0xBF_FF8080;  // light red, 75% alpha

    private static final ItemStack[] GHOST_STACKS = {
        new ItemStack(ModBlocks.CONFINED_TIME_ANVILON.asItem()),
        new ItemStack(ModBlocks.CONFINED_SPACE_ANVILON.asItem()),
        new ItemStack(ModBlocks.CONFINED_MASS_ANVILON.asItem()),
        new ItemStack(ModBlocks.CONFINED_ENERGY_ANVILON.asItem())
    };

    // Search state
    private enum SearchState {
        IDLE, LOADING, DONE, FAIL, POWER_FAIL
    }

    private SearchState searchState = SearchState.IDLE;
    @Nullable
    private CelestialBodyData preSearchBody = null;

    // Lock state is persisted in BlockEntity
    private boolean isLocked() {
        return getMenu().getBlockEntity().isLocked();
    }

    private void setLocked(boolean v) {
        getMenu().getBlockEntity().setLocked(v);
    }

    private List<?> searchHistory() {
        return getMenu().getBlockEntity().getSearchHistory();
    }

    // Rotation animation
    private int previewRotTick = 0;

    // Info scroll
    private int scrollOffset = 0;

    // Resource bar scroll
    private int resourceScrollOffset = 0;

    // Local countdown for accelerator progress (client-side, decremented every tick)
    private int localAcceleratorTicksRemaining = 0;

    // Ring scale divisors: larger ring → larger divisor → rendered smaller to match ring 1
    private static final float RING1_SCALE_DIV = 1.00f;
    private static final float RING2_SCALE_DIV = 1.25f;
    private static final float RING4_SCALE_DIV = 1.60f;
    private static final float RING5_SCALE_DIV = 1.85f;
    private static final float RING6_SCALE_DIV = 2.10f;

    // Refactor state
    private List<CelestialRefactorOption> refactorOptions = List.of();
    private int selectedRefactorIndex = -1;
    private int rfScrollRow = 0;
    private boolean isDraggingRfScrollbar = false;
    private int refactorErrorTick = 0;
    @Nullable
    private Component refactorErrorMsg = null;
    private int unlockWarningTick = 0;

    // Guide trigger: show celestial maps when anvil counts change
    private final int[] previousAnvilCounts = new int[4];
    private boolean guideTriggered = false;

    private static final float UI_AXIAL_TILT = 25f;

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

        // Client-side countdown for accelerator progress display
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

    // ==================================================================
    // 26.1 extract-based rendering
    // ==================================================================

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

        // Search button
        Identifier btnTex = (this.searchState == SearchState.DONE && !this.searchHistory().isEmpty())
            ? TEX_RESEARCH : TEX_SEARCH;
        boolean hoverSearch = relX >= SB_X && relX < SB_X + SB_W && relY >= SB_Y && relY < SB_Y + SB_H;
        this.renderButton(graphics, btnTex, guiLeft + SB_X, guiTop + SB_Y, SB_W, SB_H, hoverSearch);

        // Preview area bottom buttons
        this.renderPreviewBottomButtons(graphics, guiLeft, guiTop, relX, relY);

        // Refactor section
        this.renderRefactorSection(graphics, guiLeft, guiTop, relX, relY);

        // Celestial maps guide (when triggered + not locked + not searching)
        boolean showGuide = !this.isLocked() && this.guideTriggered && this.searchState != SearchState.LOADING;
        if (showGuide) {
            this.renderCelestialMapsGuide(graphics, guiLeft, guiTop);
            // Re-render preview buttons on top of the guide map
            this.renderPreviewBottomButtons(graphics, guiLeft, guiTop, relX, relY);
        }

        // Preview area content (body preview + info panel + resource bar)
        this.renderPreviewAreaContents(graphics, guiLeft, guiTop);

        // Ghost slot items
        this.renderSlotGhosts(graphics, guiLeft, guiTop);

        // Status messages
        this.renderStatusMessages(graphics, guiLeft, guiTop);
    }

    /**
     * Render the preview area content when DONE: 3D body + info panel text + resource bar.
     * Uses absolute screen coordinates (extractContents context).
     */
    private void renderPreviewAreaContents(GuiGraphicsExtractor graphics, int guiLeft, int guiTop) {
        // Missing amplifier check
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
                    this.renderBodyPreview(graphics, body, guiLeft, guiTop);
                    this.renderBodyInfoAbsolute(graphics, body, guiLeft, guiTop);
                    this.renderResourceBarAbsolute(graphics, guiLeft, guiTop);
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
        // Suppress vanilla item tooltip for CFA-specific slots
        boolean isCfaSlot = this.hoveredSlot instanceof CelestialForgingAnvilMenu.CFAAnvilSlot
            || this.hoveredSlot instanceof CelestialForgingAnvilMenu.SeedSlot;
        if (!isCfaSlot) {
            super.extractTooltip(graphics, mouseX, mouseY);
        }

        int rx = mouseX - this.leftPos;
        int ry = mouseY - this.topPos;

        // Search button tooltip
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

        // Lock button tooltip
        if (this.isOverLockButton(rx, ry) && (this.searchState == SearchState.DONE || this.searchState == SearchState.LOADING)) {
            graphics.setTooltipForNextFrame(
                Component.translatable(this.isLocked() ? "screen.anvilcraft.cfa.unlock" : "screen.anvilcraft.cfa.lock"),
                mouseX, mouseY);
        }

        // Refactor option tooltips
        if (this.isLocked() && this.searchState == SearchState.DONE) {
            int refOpt = this.getRefactorOptionAt(rx, ry);
            if (refOpt >= 0 && refOpt < this.refactorOptions.size()) {
                CelestialRefactorOption option = this.refactorOptions.get(refOpt);
                Component name = Component.translatable(option.displayName());
                List<Component> tooltipLines = new ArrayList<>();
                tooltipLines.add(name);
                if (option.needsMaterial()) {
                    tooltipLines.add(Component.translatable(
                        "screen.anvilcraft.cfa.material_required",
                        option.material().getDisplayName(),
                        Component.literal(String.valueOf(option.materialCount()))
                    ));
                }
                if (this.minecraft.hasShiftDown()) {
                    tooltipLines.add(Component.translatable(option.displayName() + ".description")
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

        // Material slot tooltip
        if (this.isLocked() && this.searchState == SearchState.DONE
            && this.hoveredSlot instanceof CelestialForgingAnvilMenu.CFAMaterialSlot) {
            graphics.setTooltipForNextFrame(
                Component.translatable("screen.anvilcraft.cfa.refactor_materials"), mouseX, mouseY);
        }

        // Start refactoring button tooltip
        if (this.isLocked() && this.searchState == SearchState.DONE && this.isOverRefactorStart(rx, ry)) {
            graphics.setTooltipForNextFrame(
                Component.translatable("screen.anvilcraft.cfa.refactor_start_tooltip"), mouseX, mouseY);
        }

        // Seed slot tooltip
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

        // Anvil slot range tooltip
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

    // ==================================================================
    // Body Info Panel (absolute coords for extractContents)
    // ==================================================================

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
        List<Component> lines = this.buildInfoLines(body, be.getAgeAnvilCount(), be.getStellarMass(),
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

    // ==================================================================
    // Resource Bar (absolute coords for extractContents)
    // ==================================================================

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void renderResourceBarAbsolute(GuiGraphicsExtractor graphics, int guiLeft, int guiTop) {
        var be = getMenu().getBlockEntity();
        var resources = be.getPlanetaryResourceSet();
        if (resources == null || resources.isEmpty()) return;

        List<String> entries = new ArrayList<>();
        this.collectItemEntries(entries, resources.getMinerals());
        this.collectFluidEntries(entries, resources.getFluids());
        this.collectItemEntries(entries, resources.getGiantItems());
        this.collectFluidEntries(entries, resources.getGiantFluids());
        this.collectItemEntries(entries, resources.getBiologicalItems());
        this.collectFluidEntries(entries, resources.getBiologicalFluids());
        this.collectItemEntries(entries, resources.getOfferings());
        this.collectItemEntries(entries, resources.getWastelandItems());
        if (entries.isEmpty()) return;

        int spacing = 10;
        int totalW = -spacing;
        for (String e : entries) totalW += this.font.width(e) + spacing;
        int contentX = guiLeft + PV_X + 4;
        int contentW = PV_W - 8;
        int maxScroll = Math.max(0, totalW - contentW);
        this.resourceScrollOffset = Mth.clamp(this.resourceScrollOffset, 0, maxScroll);

        int ax = guiLeft + PV_X;
        int ay = guiTop + PV_RES_Y;
        int aw = PV_W;
        int ah = PV_RES_H;

        graphics.enableScissor(ax, ay, ax + aw, ay + ah);

        Component title = Component.translatable("screen.anvilcraft.cfa.resource_title");
        graphics.text(this.font, this.title, ax + (aw - this.font.width(this.title)) / 2, ay, 0xFFAAAAAA, false);

        int x = contentX - this.resourceScrollOffset;
        int y = ay + this.font.lineHeight + 1;
        for (String entry : entries) {
            int w = this.font.width(entry);
            if (x + w >= ax && x <= ax + aw) {
                graphics.text(this.font, entry, x, y, 0xFFFFFFFF, true);
            }
            x += w + spacing;
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

    private void collectItemEntries(List<String> out, List<PlanetaryResourceSet.WeightedItemStack> items) {
        int totalW = items.stream().mapToInt(PlanetaryResourceSet.WeightedItemStack::weight).sum();
        for (var entry : items) {
            var itemHolder = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(entry.itemId());
            String name;
            if (itemHolder.isPresent()) {
                Item item = itemHolder.get().value();
                name = item.getName(new ItemStack(item)).getString();
            } else {
                name = "???";
            }
            int pct = totalW > 0 ? entry.weight() * 100 / totalW : 0;
            out.add(name + " " + pct + "%");
        }
    }

    private void collectFluidEntries(List<String> out, List<PlanetaryResourceSet.WeightedFluidStack> fluids) {
        int totalW = fluids.stream().mapToInt(PlanetaryResourceSet.WeightedFluidStack::weight).sum();
        for (var entry : fluids) {
            var fluidHolder = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(entry.fluidId());
            String name;
            if (fluidHolder.isPresent()) {
                name = fluidHolder.get().value().getFluidType().getDescription().getString();
            } else {
                name = "???";
            }
            int pct = totalW > 0 ? entry.weight() * 100 / totalW : 0;
            out.add(name + " " + pct + "%");
        }
    }

    // ==================================================================
    // Build info lines
    // ==================================================================

    private List<Component> buildInfoLines(
        CelestialBodyData body,
        int ageAnvilCount,
        int massAnvilCount,
        float offsetAge,
        float offsetRadius,
        float offsetMass
    ) {
        List<Component> lines = new ArrayList<>();
        boolean isError = body instanceof SpecialCelestialBodyData special && special.isErrorPlanet();

        // Type name
        String typeKey;
        if (body instanceof RockyPlanetData rp) {
            typeKey = rockyTypeKey(rp);
        } else if (body instanceof SpecialCelestialBodyData s) {
            typeKey = "screen.anvilcraft.cfa.class.special." + s.name();
        } else {
            typeKey = "screen.anvilcraft.cfa.class." + body.bodyClass().name().toLowerCase();
        }
        lines.add(Component.translatable("screen.anvilcraft.cfa.type", Component.translatable(typeKey)));

        // Age
        if (isError) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.age", Component.literal("???")));
        } else {
            lines.add(Component.translatable(
                "screen.anvilcraft.cfa.age",
                CelestialForgingAnvilMenu.formatAgeOffset(ageAnvilCount, offsetAge)
            ));
        }

        // Radius
        if (isError) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.radius", Component.literal("???")));
        } else {
            lines.add(Component.translatable(
                "screen.anvilcraft.cfa.radius",
                CelestialForgingAnvilMenu.formatRadiusOffset(body.size(), offsetRadius)
            ));
        }

        // Mass
        if (isError) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.mass", Component.literal("???")));
        } else {
            lines.add(Component.translatable(
                "screen.anvilcraft.cfa.mass",
                CelestialForgingAnvilMenu.formatMassOffset(massAnvilCount, offsetMass)
            ));
        }

        switch (body) {
            case SpecialCelestialBodyData s -> {
                if (s.isErrorPlanet()) {
                    lines.add(Component.translatable("screen.anvilcraft.cfa.temp", Component.literal("???")));
                    lines.add(Component.translatable("screen.anvilcraft.cfa.atmos", Component.literal("???")));
                    lines.add(Component.translatable("screen.anvilcraft.cfa.liquid", Component.literal("???")));
                    lines.add(Component.translatable("screen.anvilcraft.cfa.mag", Component.literal("???")));
                    lines.add(Component.translatable("screen.anvilcraft.cfa.spin", Component.literal("???")));
                    lines.add(Component.translatable("screen.anvilcraft.cfa.tilt", Component.literal("???")));
                } else {
                    lines.add(Component.translatable(
                        "screen.anvilcraft.cfa.temp",
                        Component.translatable("screen.anvilcraft.cfa.temp." + s.temperature().getSerializedName())
                    ));
                    lines.add(Component.translatable(
                        "screen.anvilcraft.cfa.atmos",
                        Component.translatable(s.hasAtmosphere()
                            ? "screen.anvilcraft.cfa.atmos.yes" : "screen.anvilcraft.cfa.none")
                    ));
                    lines.add(Component.translatable(
                        "screen.anvilcraft.cfa.liquid",
                        Component.translatable("screen.anvilcraft.cfa.liquid." + s.liquidCoverage().getSerializedName())
                    ));
                    lines.add(this.magText(s.magneticFieldStrength()));
                    lines.add(this.spinText(s.rotationSpeed()));
                    lines.add(this.tiltText(s.axialTilt()));
                }
            }
            case StarData s -> {
                lines.add(this.magText(s.magneticFieldStrength()));
                lines.add(this.spinText(s.rotationSpeed()));
                if (s.axialTilt() > 0.1f) {
                    lines.add(this.tiltText(s.axialTilt()));
                }
            }
            case RockyPlanetData rp -> {
                lines.add(Component.translatable(
                    "screen.anvilcraft.cfa.temp",
                    Component.translatable("screen.anvilcraft.cfa.temp." + rp.temperature().getSerializedName())
                ));
                lines.add(Component.translatable(
                    "screen.anvilcraft.cfa.atmos",
                    Component.translatable(rp.hasAtmosphere()
                        ? "screen.anvilcraft.cfa.atmos.yes" : "screen.anvilcraft.cfa.none")
                ));
                lines.add(Component.translatable(
                    "screen.anvilcraft.cfa.liquid",
                    Component.translatable("screen.anvilcraft.cfa.liquid." + rp.liquidCoverage().getSerializedName())
                ));
                lines.add(this.magText(rp.magneticFieldStrength()));
                lines.add(this.spinText(rp.rotationSpeed()));
                lines.add(this.tiltText(rp.axialTilt()));
            }
            case GiantPlanetData gp -> {
                if (!gp.brownDwarf()) {
                    lines.add(Component.translatable(
                        "screen.anvilcraft.cfa.pressure",
                        Component.translatable("screen.anvilcraft.cfa.pressure." + gp.pressureType().getSerializedName())
                    ));
                }
                lines.add(Component.translatable(
                    "screen.anvilcraft.cfa.wind",
                    Component.translatable("screen.anvilcraft.cfa.wind." + gp.windSpeed().getSerializedName())
                ));
                lines.add(this.magText(gp.magneticFieldStrength()));
                lines.add(this.spinText(gp.rotationSpeed()));
                lines.add(this.tiltText(gp.axialTilt()));
            }
            default -> {
            }
        }
        return lines;
    }

    // ==================================================================
    // Rocky planet type key helpers
    // ==================================================================

    private static String rockyTypeKey(RockyPlanetData rp) {
        Temperature t = rp.temperature();
        LiquidCoverage l = rp.liquidCoverage();
        boolean a = rp.hasAtmosphere();
        boolean hasL = l != LiquidCoverage.NONE;

        if (t == Temperature.FREEZING) {
            if (!hasL && !a) return "screen.anvilcraft.cfa.class.freezing_no_liquid_no_atmos";
            if (!hasL) return "screen.anvilcraft.cfa.class.freezing_no_liquid_atmos";
            return "screen.anvilcraft.cfa.class.freezing_liquid";
        }
        if (t == Temperature.SCORCHED) {
            if (!hasL && !a) return "screen.anvilcraft.cfa.class.scorched_no_liquid_no_atmos";
            if (!hasL) return "screen.anvilcraft.cfa.class.scorched_no_liquid_atmos";
            return "screen.anvilcraft.cfa.class.scorched_liquid";
        }
        if (!a) return "screen.anvilcraft.cfa.class.deathly_planet";
        if (!hasL) return "screen.anvilcraft.cfa.class.desert_planet";
        return switch (l) {
            case LOW -> tempRiverbankKey(t);
            case MEDIUM -> tempLandOceanKey(t);
            case HIGH -> tempOceanKey(t);
            default -> "screen.anvilcraft.cfa.class.deathly_planet";
        };
    }

    private static String tempRiverbankKey(Temperature t) {
        return switch (t) {
            case COLD -> "screen.anvilcraft.cfa.class.cold_riverbank";
            case MILD -> "screen.anvilcraft.cfa.class.mild_riverbank";
            case HOT -> "screen.anvilcraft.cfa.class.hot_riverbank";
            default -> "screen.anvilcraft.cfa.class.mild_riverbank";
        };
    }

    private static String tempLandOceanKey(Temperature t) {
        return switch (t) {
            case COLD -> "screen.anvilcraft.cfa.class.cold_land_ocean";
            case MILD -> "screen.anvilcraft.cfa.class.mild_land_ocean";
            case HOT -> "screen.anvilcraft.cfa.class.hot_land_ocean";
            default -> "screen.anvilcraft.cfa.class.mild_land_ocean";
        };
    }

    private static String tempOceanKey(Temperature t) {
        return switch (t) {
            case COLD -> "screen.anvilcraft.cfa.class.cold_ocean";
            case MILD -> "screen.anvilcraft.cfa.class.mild_ocean";
            case HOT -> "screen.anvilcraft.cfa.class.hot_ocean";
            default -> "screen.anvilcraft.cfa.class.mild_ocean";
        };
    }

    private Component magText(int level) {
        String key = switch (level) {
            case 0 -> "screen.anvilcraft.cfa.none";
            case 1 -> "screen.anvilcraft.cfa.mag.very_weak";
            case 2 -> "screen.anvilcraft.cfa.mag.weak";
            case 3 -> "screen.anvilcraft.cfa.mag.medium";
            case 4 -> "screen.anvilcraft.cfa.mag.strong";
            case 5 -> "screen.anvilcraft.cfa.mag.very_strong";
            default -> "screen.anvilcraft.cfa.mag.extreme";
        };
        return Component.translatable("screen.anvilcraft.cfa.mag", Component.translatable(key));
    }

    private Component spinText(int level) {
        String key = switch (level) {
            case 0 -> "screen.anvilcraft.cfa.spin.very_slow";
            case 1 -> "screen.anvilcraft.cfa.spin.slow";
            case 2 -> "screen.anvilcraft.cfa.spin.medium";
            case 3 -> "screen.anvilcraft.cfa.spin.fast";
            case 4 -> "screen.anvilcraft.cfa.spin.very_fast";
            default -> "screen.anvilcraft.cfa.spin.super_fast";
        };
        return Component.translatable("screen.anvilcraft.cfa.spin", Component.translatable(key));
    }

    private Component tiltText(float tilt) {
        return Component.translatable("screen.anvilcraft.cfa.tilt", format3SigFig(tilt) + "°");
    }

    @SuppressWarnings("MalformedFormatString")
    private static String format3SigFig(double value) {
        if (Math.abs(value) < 1e-9) return "0";
        int pow = (int) Math.floor(Math.log10(Math.abs(value)));
        int digits = Math.clamp(2 - pow, 0, 6);
        return String.format(Locale.US, "%." + digits + "f", value);
    }

    // ==================================================================
    // 3D Body Preview (uses PoseStack + GuiRenderExtras.tessellateBlock)
    // ==================================================================

    // ==================================================================
    // 3D Body Preview (extractContents — absolute screen coords)
    // ==================================================================

    private void renderBodyPreview(GuiGraphicsExtractor graphics, CelestialBodyData body, int guiLeft, int guiTop) {
        int blockSize = Math.min(PV_BODY_W, PV_BODY_H) - 16;
        int bx = guiLeft + PV_X + (PV_BODY_W - blockSize) / 2;
        int by = guiTop + PV_Y + (PV_BODY_H - blockSize) / 2;

        float rotY = (this.previewRotTick * CelestialBodyData.getVisualRotationSpeed(body.rotationSpeed()))
            * (float) Math.PI / 180f;
        int bodyColor = getBodyColor(body);
        int rbs = blockSize;

        if (body instanceof StarData star) {
            if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR
                || star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
                if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
                    rbs = (int) (blockSize * 1.5f);
                }
                int bxc = bx - (rbs - blockSize) / 2;
                int byc = by - (rbs - blockSize) / 2;
                int remColor = star.bodyClass() == CelestialBodyClass.BLACK_HOLE
                    ? 0xFF_222233 : bodyColor;
                GuiRenderExtras.tessellateBlock(graphics,
                    Blocks.WHITE_CONCRETE.defaultBlockState(),
                    null, null, bxc, byc, bxc + rbs, byc + rbs,
                    remColor, true,
                    makeBodyPose(rotY, 1.0f));
                if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR) {
                    float magneticTilt = star.magneticFieldStrength() >= 5 ? 15f : 10f;
                    GuiRenderExtras.tessellateBlock(graphics,
                        Blocks.WHITE_CONCRETE.defaultBlockState(),
                        null, null, bxc, byc, bxc + rbs, byc + rbs,
                        0x80_88BBFF, true,
                        makeJetPose(rotY, magneticTilt));
                }
            } else {
                GuiRenderExtras.tessellateBlock(graphics,
                    Blocks.WHITE_CONCRETE.defaultBlockState(),
                    null, null, bx, by, bx + rbs, by + rbs,
                    bodyColor, true,
                    makeBodyPose(rotY, 1.0f));
                GuiRenderExtras.tessellateBlock(graphics,
                    Blocks.WHITE_CONCRETE.defaultBlockState(),
                    null, null, bx, by, bx + rbs, by + rbs,
                    getHaloColor(body), true,
                    makeBodyPose(rotY * 0.5f, 1.2f));
            }
        } else {
            GuiRenderExtras.tessellateBlock(graphics,
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                null, null, bx, by, bx + rbs, by + rbs,
                bodyColor, true,
                makeBodyPose(rotY, 1.0f));
            Temperature atmosTemp = getUiAtmosphereTemp(body);
            if (atmosTemp != null) {
                GuiRenderExtras.tessellateBlock(graphics,
                    Blocks.WHITE_CONCRETE.defaultBlockState(),
                    null, null, bx, by, bx + rbs, by + rbs,
                    getAtmosphereColor(atmosTemp), true,
                    makeBodyPose(rotY, 1.125f));
            }
        }
    }

    private static PoseStack makeBodyPose(float yrot, float modelScale) {
        PoseStack ps = new PoseStack();
        ps.translate(0.5f, 0.5f, 0.5f);
        ps.mulPose(Axis.XP.rotationDegrees(UI_AXIAL_TILT));
        ps.mulPose(Axis.YP.rotation(yrot));
        ps.scale(modelScale, modelScale, modelScale);
        ps.translate(-0.5f, -0.5f, -0.5f);
        return ps;
    }

    private static PoseStack makeJetPose(float yrot, float magneticTilt) {
        PoseStack ps = new PoseStack();
        ps.translate(0.5f, 0.5f, 0.5f);
        ps.mulPose(Axis.XP.rotationDegrees(UI_AXIAL_TILT + magneticTilt));
        ps.mulPose(Axis.YP.rotation(yrot));
        ps.scale(0.3f, 1.8f, 0.3f);
        ps.translate(-0.5f, -0.5f, -0.5f);
        return ps;
    }

    private static int getHaloColor(CelestialBodyData body) {
        if (body instanceof StarData star) {
            float[] srgb = CelestialBodyRenderer.getStarColor(star);
            return 0x50_000000
                | ((int) (srgb[0] * 255) << 16)
                | ((int) (srgb[1] * 255) << 8)
                | (int) (srgb[2] * 255);
        }
        return 0x50_FFFFFF;
    }

    private static int getAtmosphereColor(Temperature temp) {
        float[] ac = CelestialBodyRenderer.getAtmosphereColor(temp);
        return 0x60_000000
            | ((int) (ac[0] * 255) << 16)
            | ((int) (ac[1] * 255) << 8)
            | (int) (ac[2] * 255);
    }

    private static int getBodyColor(CelestialBodyData body) {
        switch (body) {
            case StarData star -> {
                return 0xFF_000000
                       | (star.colorR() << 16)
                       | (star.colorG() << 8)
                       | star.colorB();
            }
            case RockyPlanetData rp when rp.temperature() != null -> {
                float[] ac = CelestialBodyRenderer.getAtmosphereColor(rp.temperature());
                return 0xFF_000000
                       | ((int) (ac[0] * 255) << 16)
                       | ((int) (ac[1] * 255) << 8)
                       | (int) (ac[2] * 255);
            }
            case GiantPlanetData giantPlanetData -> {
                return 0xFF_B0C0E0;
            }
            case SpecialCelestialBodyData s when s.temperature() != null -> {
                float[] ac = CelestialBodyRenderer.getAtmosphereColor(s.temperature());
                return 0xFF_000000
                       | ((int) (ac[0] * 255) << 16)
                       | ((int) (ac[1] * 255) << 8)
                       | (int) (ac[2] * 255);
            }
            default -> {
            }
        }
        return 0xFF_CCCCCC;
    }

    @Nullable
    private static Temperature getUiAtmosphereTemp(CelestialBodyData body) {
        if (body instanceof RockyPlanetData rp && rp.hasAtmosphere()) return rp.temperature();
        if (body instanceof SpecialCelestialBodyData s && s.hasAtmosphere()) return s.temperature();
        return null;
    }

    // ==================================================================
    // Button rendering
    // ==================================================================

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
        // Previous button
        if (hasPrev) {
            boolean hover = this.isOverPrevButton(relX, relY);
            this.renderButton(graphics, TEX_PREV, guiLeft + PV_X + 4, guiTop + PV_BOT_Y + 14, 16, 16, hover);
        }
        // Next button
        if (hasNext) {
            boolean hover = this.isOverNextButton(relX, relY);
            this.renderButton(graphics, TEX_NEXT, guiLeft + PV_X + PV_W - 20, guiTop + PV_BOT_Y + 14, 16, 16, hover);
        }
        // Lock button
        boolean hoverLock = this.isOverLockButton(relX, relY);
        Identifier lockTex = this.isLocked() ? TEX_LOCKED : TEX_UNLOCKED;
        this.renderButton(graphics, lockTex, guiLeft + PV_X + PV_W / 2 - 8, guiTop + PV_BOT_Y + 14, 16, 16, hoverLock);
    }

    @SuppressWarnings("SameParameterValue")
    private void drawParamText(GuiGraphicsExtractor g, String text, int x, int y, int width) {
        int textX = x + (width - this.font.width(text)) / 2;
        g.text(this.font, text, textX, y, 0xFFFFFFFF, true);
    }

    // ==================================================================
    // Slot ghost rendering (called from extractContents — absolute coords)
    // ==================================================================

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

    // ==================================================================
    // Status messages (locked msg, refactor error, unlock warning, evo warning)
    // ==================================================================

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

    // ==================================================================
    // Celestial Maps Guide
    // ==================================================================

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void renderCelestialMapsGuide(GuiGraphicsExtractor g, int guiLeft, int guiTop) {
        int previewCenterX = guiLeft + PV_X + PV_W / 2;
        int previewCenterY = guiTop + PV_Y + PV_H / 2;

        var ps = g.pose();
        ps.pushMatrix();
        float scale = 0.5f;
        ps.translate(previewCenterX, previewCenterY);
        ps.scale(scale, scale);
        ps.translate(-MAP_SIZE / 2.0f, -MAP_SIZE / 2.0f);

        // Render the celestial maps image at 160x160 (scaled to 80x80 on screen)
        g.blit(RenderPipelines.GUI_TEXTURED, TEX_CELESTIAL_MAPS, 0, 0, 0, 0, MAP_SIZE, MAP_SIZE, MAP_SIZE, MAP_SIZE);

        int timeCount = getMenu().getBlockEntity().getAnvilCount(0);
        int spaceCount = getMenu().getBlockEntity().getAnvilCount(1);
        int massCount = getMenu().getBlockEntity().getAnvilCount(2);
        int energyCount = getMenu().getBlockEntity().getAnvilCount(3);

        // Time anvil: light green, vertical, full height (160px), x=12-76
        if (timeCount > 0) {
            int x = 11 + Math.round((timeCount - 1) * 64.0f / 63.0f);
            g.fill(x, 0, x + 2, MAP_SIZE, COLOR_TIME);
            String text = String.valueOf(timeCount);
            int textX = x + 1 - this.font.width(text) / 2;
            int textY = -this.font.lineHeight - 4;
            g.text(this.font, text, textX, textY, COLOR_TIME, false);
        }

        // Space anvil: cyan, horizontal, full width (160px), y=92-156
        if (spaceCount > 0) {
            int y = MAP_SIZE - Math.round(92 + (spaceCount - 1) * 64.0f / 63.0f) - 2;
            g.fill(0, y, MAP_SIZE, y + 2, COLOR_SPACE);
            String text = String.valueOf(spaceCount);
            int textX = -this.font.width(text) - 6;
            int textY = y + 1 - this.font.lineHeight / 2;
            g.text(this.font, text, textX, textY, COLOR_SPACE, false);
        }

        // Mass anvil: light yellow, vertical, upper half (80px), x=92-156
        if (massCount > 0) {
            int x = 91 + Math.round((massCount - 1) * 64.0f / 63.0f);
            g.fill(x, 0, x + 2, MAP_SIZE / 2, COLOR_MASS);
            String text = String.valueOf(massCount);
            int textX = x + 1 - this.font.width(text) / 2;
            int textY = -this.font.lineHeight - 4;
            g.text(this.font, text, textX, textY, COLOR_MASS, false);
        }

        // Energy anvil: light red, horizontal, left half (80px), y=12-76
        if (energyCount > 0) {
            int y = MAP_SIZE - Math.round(12 + (energyCount - 1) * 64.0f / 63.0f) - 2;
            g.fill(0, y, MAP_SIZE / 2, y + 2, COLOR_ENERGY);
            String text = String.valueOf(energyCount);
            int textX = -this.font.width(text) - 6;
            int textY = y + 1 - this.font.lineHeight / 2;
            g.text(this.font, text, textX, textY, COLOR_ENERGY, false);
        }

        // Three-step guide text
        this.renderGuideStepText(g, timeCount, spaceCount, massCount, energyCount);

        ps.popMatrix();
    }

    private void renderGuideStepText(GuiGraphicsExtractor g, int time, int space, int mass, int energy) {
        int textX = 88;
        int lineSpacing = this.font.lineHeight + 5;
        int y0 = 108;

        // Step 1: ↑ type from mass-radius diagram (mass + space)
        int step1Rgb = CelestialBodyMatcher.getMassRadiusRgb(mass, space);
        String step1Name = getTypeDisplayName(step1Rgb);
        this.drawGuideLine(g, "↑" + step1Name, textX, y0, 0xFFCCCCCC);

        // Step 2: ← type from age-temp diagram (time + energy)
        CelestialBodyClass step1Class = CelestialBodyClass.fromRgb(step1Rgb);
        int step2Rgb;
        if (step1Class != null && step1Class.step2UsesSp()) {
            step2Rgb = CelestialBodyMatcher.getAgeTempSpRgb(time, energy);
        } else {
            step2Rgb = CelestialBodyMatcher.getAgeTempRgb(time, energy);
        }
        String step2Name = getTypeDisplayName(step2Rgb);
        this.drawGuideLine(g, "←" + step2Name, textX, y0 + lineSpacing * 2, 0xFFCCCCCC);

        // Step 3: ↖ type from age-radius diagram (time + space)
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

    private void drawGuideLine(GuiGraphicsExtractor g, String text, int x, int y, int color) {
        g.text(this.font, text, x, y, color, false);
    }

    // ==================================================================
    // Refactor section
    // ==================================================================

    private void renderRefactorSection(GuiGraphicsExtractor g, int guiLeft, int guiTop, int relX, int relY) {
        CelestialBodyData body = getMenu().getBlockEntity().getCelestialBodyData();
        boolean hasAcceleratorActive = getMenu().getBlockEntity().isAcceleratorActive();
        boolean hasMegastructure = getMenu().getBlockEntity().getActiveMegastructureIndex() >= 0;
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

        int btnCount = this.refactorOptions.size();
        int totalRows = btnCount > 0 ? (btnCount + RF_COLS - 1) / RF_COLS : 0;
        int maxScroll = Math.max(0, totalRows - RF_ROWS_VISIBLE);
        if (this.rfScrollRow > maxScroll) this.rfScrollRow = maxScroll;
        if (this.rfScrollRow < 0) this.rfScrollRow = 0;
        if (this.selectedRefactorIndex >= btnCount) this.selectedRefactorIndex = -1;

        // Render option buttons (fixed grid, scrolled by this.rfScrollRow)
        for (int visibleRow = 0; visibleRow < RF_ROWS_VISIBLE; visibleRow++) {
            for (int col = 0; col < RF_COLS; col++) {
                int dataRow = this.rfScrollRow + visibleRow;
                int optIdx = dataRow * RF_COLS + col;
                if (optIdx >= btnCount) continue;

                int bx = guiLeft + RF_BTN_X[visibleRow * RF_COLS + col];
                int by = guiTop + RF_BTN_Y[visibleRow * RF_COLS + col];
                boolean hovered = relX >= RF_BTN_X[visibleRow * RF_COLS + col]
                    && relX < RF_BTN_X[visibleRow * RF_COLS + col] + RF_BTN_W
                    && relY >= RF_BTN_Y[visibleRow * RF_COLS + col]
                    && relY < RF_BTN_Y[visibleRow * RF_COLS + col] + RF_BTN_H;
                boolean selected = optIdx == this.selectedRefactorIndex;

                // Button background
                this.renderButton(g, TEX_REFACTOR_OPTIONS, bx, by, RF_BTN_W, RF_BTN_H, hovered || selected);

                // Render megastructure model in button
                CelestialRefactorOption option = this.refactorOptions.get(optIdx);
                this.renderMegastructureModel(g, option, bx, by, RF_BTN_W, RF_BTN_H);

                // Selected indicator (green tint overlay)
                if (selected) {
                    g.fill(bx, by, bx + RF_BTN_W, by + RF_BTN_H, 0x40_00FF00);
                }
            }
        }

        // Render scrollbar
        if (maxScroll > 0) {
            this.renderRefactorScrollbar(g, guiLeft, guiTop, totalRows);
        }

        // Render start button
        boolean hoverStart = relX >= RF_START_X && relX < RF_START_X + RF_START_W
            && relY >= RF_START_Y && relY < RF_START_Y + RF_START_H;
        this.renderButton(g, TEX_REFACTORING, guiLeft + RF_START_X, guiTop + RF_START_Y,
            RF_START_W, RF_START_H, hoverStart);

        // If not locked / no body / not DONE, show "need to lock first" text
        if (!this.isLocked() || body == null || this.searchState != SearchState.DONE) {
            Component needLock = Component.translatable("screen.anvilcraft.cfa.need_lock");
            int areaCX = (RF_BTN_X[0] + RF_BTN_X[1] + RF_BTN_W) / 2;
            int areaCY = (RF_BTN_Y[0] + RF_BTN_Y[2] + RF_BTN_H) / 2;
            int cx = guiLeft + areaCX - this.font.width(needLock) / 2;
            int cy = guiTop + areaCY - this.font.lineHeight / 2;
            g.text(this.font, needLock, cx, cy, 0xFF888888, false);
        }
    }

    private void renderRefactorScrollbar(GuiGraphicsExtractor g, int guiLeft, int guiTop, int totalRows) {
        int i = totalRows - RF_ROWS_VISIBLE;
        if (i < 1) return;
        int scrollX = guiLeft + RF_SCROLL_X;
        int maxY = RF_SCROLL_Y + RF_SCROLL_H - RF_SCROLL_THUMB_H;
        int scrollY = RF_SCROLL_Y + (this.rfScrollRow * (RF_SCROLL_H - RF_SCROLL_THUMB_H) / i);
        scrollY = Mth.clamp(scrollY, RF_SCROLL_Y, maxY);
        g.blit(RenderPipelines.GUI_TEXTURED, SharedTextures.SWITCH_TABLE_SLIDER,
            scrollX, guiTop + scrollY, 0, 0, RF_SCROLL_W, RF_SCROLL_THUMB_H, 8, 12);
    }

    /**
     * Render a megastructure block model inside a button.
     * Scale is divided by the ring's relative geometric size so all rings appear the same size.
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void renderMegastructureModel(GuiGraphicsExtractor g, CelestialRefactorOption option,
                                          int x, int y, int w, int h) {
        float divisor = switch (option.ring()) {
            case 1 -> RING1_SCALE_DIV;
            case 2 -> RING2_SCALE_DIV;
            case 4 -> RING4_SCALE_DIV;
            case 5 -> RING5_SCALE_DIV;
            case 6 -> RING6_SCALE_DIV;
            default -> 1.0f;
        };
        int size = (int) (Math.min(w, h) * 1.15f / divisor);
        int bx = x + (w - size) / 2;
        int by = y + (h - size) / 2;

        PoseStack ps = new PoseStack();
        ps.translate(0.5f, 0.5f, 0.5f);
        ps.mulPose(Axis.XP.rotationDegrees(30));
        ps.mulPose(Axis.YP.rotationDegrees((this.previewRotTick * 2) % 360));
        ps.translate(-0.5f, -0.5f, -0.5f);

        GuiRenderExtras.tessellateBlock(g,
            Blocks.WHITE_CONCRETE.defaultBlockState(),
            null, null, bx, by, bx + size, by + size,
            -1, true, ps);
    }

    // ==================================================================
    // Mouse handling (26.1 MouseButtonEvent API)
    // ==================================================================

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
            // Send lock toggle to server (button ID 200)
            this.sendButtonClick(200);
            this.setLocked(!this.isLocked());
            return true;
        }
        // Refactor option click
        int optIdx = this.getRefactorOptionAt(rx, ry);
        if (optIdx >= 0) {
            this.selectedRefactorIndex = optIdx;
            this.sendButtonClick(9 + optIdx);
            return true;
        }
        // Start refactoring button click
        if (this.isOverRefactorStart(rx, ry)) {
            this.handleRefactorStart();
            return true;
        }
        // Refactor scrollbar drag initiation
        if (this.isMouseInRefactorScrollbar(rx, ry)) {
            int totalRows = !this.refactorOptions.isEmpty() ? (this.refactorOptions.size() + RF_COLS - 1) / RF_COLS : 0;
            int maxScroll = Math.max(0, totalRows - RF_ROWS_VISIBLE);
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
            int totalRows = !this.refactorOptions.isEmpty() ? (this.refactorOptions.size() + RF_COLS - 1) / RF_COLS : 0;
            int maxScroll = Math.max(0, totalRows - RF_ROWS_VISIBLE);
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

        // Parameter area scroll → transfer anvils
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

        // Info area: scroll text
        if (relX >= PV_INFO_X && relX < PV_INFO_X + PV_INFO_W
            && relY >= PV_INFO_Y && relY < PV_INFO_Y + PV_INFO_H) {
            this.scrollOffset -= (int) scrollY;
            return true;
        }

        // Resource bar: horizontal pixel scroll
        if (relX >= PV_X && relX < PV_X + PV_W && relY >= PV_RES_Y && relY < PV_RES_Y + PV_RES_H) {
            this.resourceScrollOffset -= (int) scrollY * 30;
            return true;
        }

        // Refactor options area: scroll buttons
        if (this.isMouseInRefactorArea(relX, relY) || this.isMouseInRefactorScrollbar(relX, relY)) {
            int totalRows = !this.refactorOptions.isEmpty() ? (this.refactorOptions.size() + RF_COLS - 1) / RF_COLS : 0;
            int maxScroll = Math.max(0, totalRows - RF_ROWS_VISIBLE);
            if (maxScroll > 0) {
                this.rfScrollRow = (int) Mth.clamp(this.rfScrollRow - scrollY, 0, maxScroll);
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ==================================================================
    // Search
    // ==================================================================

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

    // ==================================================================
    // Hit tests
    // ==================================================================

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

    // ==================================================================
    // Refactor UI helpers
    // ==================================================================

    private int getRefactorOptionAt(int rx, int ry) {
        if (this.refactorOptions.isEmpty()) return -1;
        for (int visibleRow = 0; visibleRow < RF_ROWS_VISIBLE; visibleRow++) {
            for (int col = 0; col < RF_COLS; col++) {
                int dataRow = this.rfScrollRow + visibleRow;
                int optIdx = dataRow * RF_COLS + col;
                if (optIdx >= this.refactorOptions.size()) continue;
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
            if (!ItemStack.isSameItemSameComponents(inSlot, required)
                || inSlot.getCount() < required.getCount()) {
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
