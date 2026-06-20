package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
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
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.CelestialBodyRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.CelestialBodyTextureBakery;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.CelestialForgingAnvilMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CelestialForgingAnvilScreen extends AbstractContainerScreen<CelestialForgingAnvilMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "celestial_forging_anvil");

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

    // Refactor option scrollbar (track at bg pixel 332-335, y=40-109 in 512x256)
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
    private static final ResourceLocation TEX_SEARCH = SharedTextures.textureGui(BTN_DIR + "search");
    private static final ResourceLocation TEX_RESEARCH = SharedTextures.textureGui(BTN_DIR + "re_search");
    private static final ResourceLocation TEX_PREV = SharedTextures.textureGui(BTN_DIR + "previous");
    private static final ResourceLocation TEX_NEXT = SharedTextures.textureGui(BTN_DIR + "next");
    private static final ResourceLocation TEX_UNLOCKED = SharedTextures.textureGui(BTN_DIR + "unlocked");
    private static final ResourceLocation TEX_LOCKED = SharedTextures.textureGui(BTN_DIR + "locked");
    private static final ResourceLocation TEX_REFACTOR_OPTIONS = SharedTextures.textureGui(BTN_DIR + "refactor_options");
    private static final ResourceLocation TEX_REFACTORING = SharedTextures.textureGui(BTN_DIR + "refactoring");

    // Celestial Maps guide
    private static final ResourceLocation TEX_CELESTIAL_MAPS = SharedTextures.texture("block/celestial_maps");
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

    // History browsing is now server-side; button clicks send packet IDs 201/202

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
    // ring_small parent (rings 1-2): main ring ~8 unit radius
    // ring_big parent (rings 4-5): main ring ~12.75 unit radius (~1.6×)
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

    public CelestialForgingAnvilScreen(CelestialForgingAnvilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 344;
        this.imageHeight = 207;
    }

    @Override
    protected void init() {
        super.init();
        CelestialBodyMatcher.warmup();
        int titleAreaCenter = (3 + 342) / 2 - 1;
        this.titleLabelX = titleAreaCenter - this.font.width(this.title) / 2;
        this.titleLabelY = 2;

        // Restore state from persisted data
        var be = getMenu().getBlockEntity();
        if (be.isSearching() && searchState == SearchState.IDLE) {
            searchState = SearchState.LOADING;
        } else if (be.getCelestialBodyData() != null && searchState == SearchState.IDLE) {
            searchState = SearchState.DONE;
        }

        // Capture initial anvil counts so guide only triggers on change
        for (int i = 0; i < 4; i++) {
            previousAnvilCounts[i] = be.getAnvilCount(i);
        }
        guideTriggered = false;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        previewRotTick++;

        // Detect anvil count changes to trigger the celestial maps guide
        var be = getMenu().getBlockEntity();
        for (int i = 0; i < 4; i++) {
            int cur = be.getAnvilCount(i);
            if (cur != previousAnvilCounts[i]) {
                guideTriggered = true;
            }
            previousAnvilCounts[i] = cur;
        }
        // Reset guide trigger when a new search begins
        if (searchState == SearchState.LOADING) {
            guideTriggered = false;
        }

        if (lockedMsgTick > 0) lockedMsgTick--;
        if (refactorErrorTick > 0) refactorErrorTick--;
        if (unlockWarningTick > 0) unlockWarningTick--;

        // Client-side countdown for accelerator progress display
        {
            var beAccel = getMenu().getBlockEntity();
            if (beAccel.isAcceleratorActive()) {
                int serverTicks = beAccel.getAcceleratorTicksRemaining();
                // First time init or server value is strictly ahead (lower)
                if (localAcceleratorTicksRemaining <= 0 || serverTicks < localAcceleratorTicksRemaining) {
                    localAcceleratorTicksRemaining = serverTicks;
                }
                if (localAcceleratorTicksRemaining > 0) {
                    localAcceleratorTicksRemaining--;
                }
            } else {
                localAcceleratorTicksRemaining = 0;
            }
        }

        if (searchState == SearchState.LOADING) {
            CelestialBodyData cur = be.getCelestialBodyData();
            if (be.isPowerInsufficient()) {
                searchState = SearchState.POWER_FAIL;
            } else if (cur != null && cur != preSearchBody) {
                searchState = SearchState.DONE;
            } else if (be.isSearchFailed()) {
                searchState = SearchState.FAIL;
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BACKGROUND, i, j, 0, 0, this.imageWidth, this.imageHeight, TEX_WIDTH, TEX_HEIGHT);

        int relX = mouseX - i;
        int relY = mouseY - j;

        // Search button (sprite: 48x16 each state, 48x32 total)
        ResourceLocation btnTex = (searchState == SearchState.DONE && !searchHistory().isEmpty()) ? TEX_RESEARCH : TEX_SEARCH;
        boolean hoverSearch = relX >= SB_X && relX < SB_X + SB_W && relY >= SB_Y && relY < SB_Y + SB_H;
        renderButton(guiGraphics, btnTex, i + SB_X, j + SB_Y, SB_W, SB_H, hoverSearch);

        // Preview area bottom buttons
        renderPreviewBottomButtons(guiGraphics, i, j, relX, relY);

        // Refactor section
        renderRefactorSection(guiGraphics, i, j, relX, relY);
    }

    /**
     * Render the prev/next/lock buttons below the preview area.
     * Called from {@link #renderBg} and re-called after guide rendering to stay on top.
     */
    private void renderPreviewBottomButtons(GuiGraphics guiGraphics, int guiLeft, int guiTop, int relX, int relY) {
        if (searchState != SearchState.DONE && searchState != SearchState.LOADING) return;
        boolean hasPrev = getMenu().getBlockEntity().hasPreviousHistory();
        boolean hasNext = getMenu().getBlockEntity().hasNextHistory();
        // Previous button
        if (hasPrev) {
            boolean hover = isOverPrevButton(relX, relY);
            renderButton(guiGraphics, TEX_PREV, guiLeft + PV_X + 4, guiTop + PV_BOT_Y + 14, 16, 16, hover);
        }
        // Next button
        if (hasNext) {
            boolean hover = isOverNextButton(relX, relY);
            renderButton(guiGraphics, TEX_NEXT, guiLeft + PV_X + PV_W - 20, guiTop + PV_BOT_Y + 14, 16, 16, hover);
        }
        // Lock button
        boolean hoverLock = isOverLockButton(relX, relY);
        ResourceLocation lockTex = isLocked() ? TEX_LOCKED : TEX_UNLOCKED;
        renderButton(guiGraphics, lockTex, guiLeft + PV_X + PV_W / 2 - 8, guiTop + PV_BOT_Y + 14, 16, 16, hoverLock);
    }

    /**
     * Render a sprite-sheet button: top half = normal, bottom half = hover.
     */
    private void renderButton(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, boolean hovered) {
        RenderSystem.enableDepthTest();
        int v = hovered ? h : 0;
        g.blit(tex, x, y, 0, v, w, h, w, h * 2);
    }

    /**
     * Render the Celestial Restriction Ring Refactor section.
     */
    private void renderRefactorSection(GuiGraphics guiGraphics, int guiLeft, int guiTop, int relX, int relY) {
        // Refresh available options for the locked body
        CelestialBodyData body = getMenu().getBlockEntity().getCelestialBodyData();
        boolean hasAcceleratorActive = getMenu().getBlockEntity().isAcceleratorActive();
        boolean hasMegastructure = getMenu().getBlockEntity().getActiveMegastructureIndex() >= 0;
        // Show options when locked & body present & search done, and accelerator not active
        boolean isActive = isLocked() && body != null && searchState == SearchState.DONE
            && !hasAcceleratorActive;
        if (isActive) {
            refactorOptions = CelestialRefactorRegistry.getOptions(
                body,
                getMenu().getBlockEntity().isAmplify(),
                getMenu().getBlockEntity().getPlanetaryResourceSet()
            );
            // If another megastructure is built, only show the accelerator (which can coexist)
            if (hasMegastructure) {
                refactorOptions = refactorOptions.stream()
                    .filter(opt -> "stellar_evolution_accelerator".equals(opt.megastructure()))
                    .toList();
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

        // Render option buttons (fixed grid, scrolled by rfScrollRow)
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

                // Button background
                renderButton(guiGraphics, TEX_REFACTOR_OPTIONS, bx, by, RF_BTN_W, RF_BTN_H, hovered || selected);

                // Render megastructure model
                CelestialRefactorOption option = refactorOptions.get(optIdx);
                renderMegastructureModel(guiGraphics, option, bx, by, RF_BTN_W, RF_BTN_H);

                // Selected indicator (green tint overlay)
                if (selected) {
                    guiGraphics.fill(bx, by, bx + RF_BTN_W, by + RF_BTN_H, 0x40_00FF00);
                }
            }
        }

        // Render scrollbar
        if (maxScroll > 0) {
            renderRefactorScrollbar(guiGraphics, guiLeft, guiTop, totalRows);
        }

        // Render start button
        boolean hoverStart = relX >= RF_START_X && relX < RF_START_X + RF_START_W && relY >= RF_START_Y && relY < RF_START_Y + RF_START_H;
        renderButton(guiGraphics, TEX_REFACTORING, guiLeft + RF_START_X, guiTop + RF_START_Y, RF_START_W, RF_START_H, hoverStart);

        // If not locked, show "need to lock first" text centered in the button area
        if (!isLocked() || body == null || searchState != SearchState.DONE) {
            Component needLock = Component.translatable("screen.anvilcraft.cfa.need_lock");
            int areaCX = (RF_BTN_X[0] + RF_BTN_X[1] + RF_BTN_W) / 2; // 291
            int areaCY = (RF_BTN_Y[0] + RF_BTN_Y[2] + RF_BTN_H) / 2; // 74
            int cx = guiLeft + areaCX - font.width(needLock) / 2;
            int cy = guiTop + areaCY - font.lineHeight / 2;
            guiGraphics.drawString(font, needLock, cx, cy, 0x888888, true);
        }
    }

    /**
     * Render the scrollbar for the refactor options grid.
     */
    private void renderRefactorScrollbar(GuiGraphics guiGraphics, int guiLeft, int guiTop, int totalRows) {
        int i = totalRows - RF_ROWS_VISIBLE;
        if (i < 1) return;
        int scrollX = guiLeft + RF_SCROLL_X;
        int maxY = RF_SCROLL_Y + RF_SCROLL_H - RF_SCROLL_THUMB_H;
        int scrollY = RF_SCROLL_Y + (rfScrollRow * (RF_SCROLL_H - RF_SCROLL_THUMB_H) / i);
        scrollY = Mth.clamp(scrollY, RF_SCROLL_Y, maxY);
        guiGraphics.blit(SharedTextures.SWITCH_TABLE_SLIDER, scrollX, guiTop + scrollY, 0, 0, RF_SCROLL_W, RF_SCROLL_THUMB_H, 8, 12);
    }

    /**
     * Render a megastructure BakedModel inside a button.
     * Scale is divided by the ring's relative geometric size so all rings appear the same size.
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void renderMegastructureModel(GuiGraphics guiGraphics, CelestialRefactorOption option, int x, int y, int w, int h) {
        BakedModel model = null;
        if (minecraft != null) {
            model = minecraft.getModelManager().getModel(option.modelLocation());
        }

        ModelBlockRenderer modelRenderer = minecraft.getBlockRenderer().getModelRenderer();
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(x + w / 2f, y + h / 2f, 100);
        float baseScale = Math.min(w, h) * 1.15f;
        float divisor = switch (option.ring()) {
            case 1 -> RING1_SCALE_DIV;
            case 2 -> RING2_SCALE_DIV;
            case 4 -> RING4_SCALE_DIV;
            case 5 -> RING5_SCALE_DIV;
            case 6 -> RING6_SCALE_DIV;
            default -> 1.0f;
        };
        float scale = baseScale / divisor;
        poseStack.scale(scale, -scale, scale);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(30));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((previewRotTick * 2) % 360));

        var bufferSource = guiGraphics.bufferSource();
        var consumer = bufferSource.getBuffer(RenderType.cutout());
        modelRenderer.renderModel(poseStack.last(), consumer, null, model, 0, 0, 0, LightTexture.FULL_BRIGHT, 0);
        bufferSource.endBatch();
        poseStack.popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        Component paramsLabel = Component.translatable("screen.anvilcraft.cfa.celestial_params");
        int paramsX = 7 + (72 - this.font.width(paramsLabel)) / 2;
        guiGraphics.drawString(this.font, paramsLabel, paramsX, 18, 0x404040, false);

        // Refactor title (centered in the title area)
        Component refactorTitle = Component.translatable("screen.anvilcraft.cfa.refactor_title");
        int refactorTitleX = RF_TITLE_X + (RF_TITLE_W - this.font.width(refactorTitle)) / 2;
        guiGraphics.drawString(this.font, refactorTitle, refactorTitleX, RF_TITLE_Y, 0x404040, false);

        CelestialForgingAnvilMenu menu = this.getMenu();
        drawParamText(guiGraphics, CelestialForgingAnvilMenu.formatAge(menu.getBlockEntity().getAnvilCount(0)), 33, 43, 58);
        drawParamText(guiGraphics, CelestialForgingAnvilMenu.formatRadius(menu.getBlockEntity().getAnvilCount(1)), 33, 61, 58);
        drawParamText(guiGraphics, CelestialForgingAnvilMenu.formatMass(menu.getBlockEntity().getAnvilCount(2)), 33, 79, 58);
        drawParamText(guiGraphics, CelestialForgingAnvilMenu.formatTemperature(menu.getBlockEntity().getAnvilCount(3)), 33, 97, 58);

        renderPreviewArea(guiGraphics);
    }

    private void renderPreviewArea(GuiGraphics guiGraphics) {
        // Check for missing amplifier with stellar body
        CelestialBodyData body = getMenu().getBlockEntity().getCelestialBodyData();
        boolean missingAmplifier = body instanceof StarData && !getMenu().getBlockEntity().isAmplifierPresent();
        if (missingAmplifier) {
            // Check if wormhole stabilizer is active — use its specific message
            var be = getMenu().getBlockEntity();
            boolean isWormholeActive = be.getActiveMegastructureIndex() >= 0
                && be.getCelestialBodyData() instanceof StarData star
                && star.bodyClass() == CelestialBodyClass.BLACK_HOLE;
            Component line1 = isWormholeActive
                ? Component.translatable("screen.anvilcraft.cfa.wormhole.missing_amplifier.line1")
                : Component.translatable("screen.anvilcraft.cfa.missing_amplifier.line1");
            Component line2;
            Component line3;
            if (isWormholeActive) {
                line2 = Component.translatable("screen.anvilcraft.cfa.wormhole.missing_amplifier.line2");
                line3 = Component.empty();
            } else {
                line2 = Component.translatable("screen.anvilcraft.cfa.missing_amplifier.line2");
                line3 = Component.translatable("screen.anvilcraft.cfa.missing_amplifier.line3");
            }
            int cx1 = PV_X + (PV_W - font.width(line1)) / 2;
            int cx2 = PV_X + (PV_W - font.width(line2)) / 2;
            int cx3 = PV_X + (PV_W - font.width(line3)) / 2;
            int cy = PV_Y + PV_H / 2 - font.lineHeight * 3 / 2;
            guiGraphics.drawString(font, line1, cx1, cy, 0xFF5555, true);
            guiGraphics.drawString(font, line2, cx2, cy + font.lineHeight + 1, 0xFF5555, true);
            guiGraphics.drawString(font, line3, cx3, cy + (font.lineHeight + 1) * 2, 0xFF5555, true);
            return;
        }
        // When anvil counts change while unlocked and not searching, show the guide.
        // The guide stays visible until a new search begins (guideTriggered is reset).
        if (!isLocked() && guideTriggered && searchState != SearchState.LOADING) {
            renderCelestialMapsGuide(guiGraphics);
            return;
        }
        switch (searchState) {
            case LOADING -> {
                String base = Component.translatable("screen.anvilcraft.cfa.search_loading").getString();
                int dots = (previewRotTick / 10) % 3;
                String text = base + ".".repeat(dots + 1);
                int cx = PV_X + (PV_W - font.width(text)) / 2;
                int cy = PV_Y + PV_H / 2 - font.lineHeight / 2;
                guiGraphics.drawString(font, text, cx, cy, 0xFFFFFF, true);
            }
            case FAIL -> {
                Component fail = Component.translatable("screen.anvilcraft.cfa.search_fail");
                int cx = PV_X + (PV_W - font.width(fail)) / 2;
                int cy = PV_Y + PV_H / 2 - font.lineHeight / 2;
                guiGraphics.drawString(font, fail, cx, cy, 0xFF5555, true);
            }
            case POWER_FAIL -> {
                Component fail = Component.translatable("screen.anvilcraft.cfa.power_fail");
                int cx = PV_X + (PV_W - font.width(fail)) / 2;
                int cy = PV_Y + PV_H / 2 - font.lineHeight / 2;
                guiGraphics.drawString(font, fail, cx, cy, 0xFF5555, true);
            }
            case DONE -> {
                CelestialBodyData cur = getMenu().getBlockEntity().getCelestialBodyData();
                if (cur != null) {
                    renderBodyPreview(guiGraphics, cur);
                    renderBodyInfo(guiGraphics, cur);
                    renderResourceBar(guiGraphics);
                }
            }
            // IDLE 与 default 相同
            default -> {
            }
        }
    }

    /**
     * Render the celestial maps guide with 4 colored indicator lines.
     * Each line represents one anvil type and moves based on its count (1-64).
     * Lines and count labels are hidden when the corresponding count is 0.
     * Everything is rendered at 50% scale using pose scaling.
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void renderCelestialMapsGuide(GuiGraphics guiGraphics) {
        int previewCenterX = PV_X + PV_W / 2;
        int previewCenterY = PV_Y + PV_H / 2;

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        float scale = 0.5f;
        pose.translate(previewCenterX, previewCenterY, 0);
        pose.scale(scale, scale, 1.0f);
        pose.translate(-MAP_SIZE / 2.0f, -MAP_SIZE / 2.0f, 0);

        // Render the celestial maps image at 160x160 (scaled to 80x80 on screen)
        guiGraphics.blit(TEX_CELESTIAL_MAPS, 0, 0, 0, 0, MAP_SIZE, MAP_SIZE, MAP_SIZE, MAP_SIZE);

        int timeCount = getMenu().getBlockEntity().getAnvilCount(0);
        int spaceCount = getMenu().getBlockEntity().getAnvilCount(1);
        int massCount = getMenu().getBlockEntity().getAnvilCount(2);
        int energyCount = getMenu().getBlockEntity().getAnvilCount(3);

        // Time anvil: light green, vertical, full height (160px), x=12-76 (1-indexed from left)
        if (timeCount > 0) {
            int x = 11 + Math.round((timeCount - 1) * 64.0f / 63.0f);
            guiGraphics.fill(x, 0, x + 2, MAP_SIZE, COLOR_TIME); // 2px → 1px after 0.5x scale
            String text = String.valueOf(timeCount);
            int textX = x + 1 - font.width(text) / 2;
            int textY = -font.lineHeight - 4;
            guiGraphics.drawString(font, text, textX, textY, COLOR_TIME, false);
        }

        // Space anvil: cyan, horizontal, full width (160px), y=92-156 (1-indexed from bottom)
        if (spaceCount > 0) {
            int y = 160 - Math.round(92 + (spaceCount - 1) * 64.0f / 63.0f) - 2; // -2 scaled px = -1 screen px
            guiGraphics.fill(0, y, MAP_SIZE, y + 2, COLOR_SPACE); // 2px → 1px after 0.5x scale
            String text = String.valueOf(spaceCount);
            int textX = -font.width(text) - 6;
            int textY = y + 1 - font.lineHeight / 2;
            guiGraphics.drawString(font, text, textX, textY, COLOR_SPACE, false);
        }

        // Mass anvil: light yellow, vertical, upper half (80px), x=92-156 (1-indexed from left)
        if (massCount > 0) {
            int x = 91 + Math.round((massCount - 1) * 64.0f / 63.0f);
            guiGraphics.fill(x, 0, x + 2, MAP_SIZE / 2, COLOR_MASS); // upper half, 2px→1px
            String text = String.valueOf(massCount);
            int textX = x + 1 - font.width(text) / 2;
            int textY = -font.lineHeight - 4;
            guiGraphics.drawString(font, text, textX, textY, COLOR_MASS, false);
        }

        // Energy anvil: light red, horizontal, left half (80px), y=12-76 (1-indexed from bottom)
        if (energyCount > 0) {
            int y = 160 - Math.round(12 + (energyCount - 1) * 64.0f / 63.0f) - 2; // -2 scaled px = -1 screen px
            guiGraphics.fill(0, y, MAP_SIZE / 2, y + 2, COLOR_ENERGY); // left half, 2px→1px
            String text = String.valueOf(energyCount);
            int textX = -font.width(text) - 6;
            int textY = y + 1 - font.lineHeight / 2;
            guiGraphics.drawString(font, text, textX, textY, COLOR_ENERGY, false);
        }

        // Three-step guide text to the right of the map
        renderGuideStepText(guiGraphics, timeCount, spaceCount, massCount, energyCount);

        pose.popPose();
    }

    /**
     * Render the three-step celestial type guide text in the bottom-right of the map.
     * Rendered inside the 0.5x scaled pose. Coordinates are in the 160×160 image space.
     */
    private void renderGuideStepText(
        GuiGraphics guiGraphics, int time, int space, int mass, int energy
    ) {
        int textX = 88; // bottom-right empty area of the map
        int lineSpacing = font.lineHeight + 5; // scaled units
        int y0 = 108;

        // Step 1: ↑ type from mass-radius diagram (mass + space)
        int step1Rgb = CelestialBodyMatcher.getMassRadiusRgb(mass, space);
        String step1Name = getTypeDisplayName(step1Rgb);
        drawGuideLine(guiGraphics, "↑" + step1Name, textX, y0, 0xFF_CCCCCC);

        // Step 2: ← type from age-temp diagram (time + energy)
        // Brown dwarfs use age_temp_sp; everything else uses age_temp
        CelestialBodyClass step1Class = CelestialBodyClass.fromRgb(step1Rgb);
        int step2Rgb;
        if (step1Class != null && step1Class.step2UsesSp()) {
            step2Rgb = CelestialBodyMatcher.getAgeTempSpRgb(time, energy);
        } else {
            step2Rgb = CelestialBodyMatcher.getAgeTempRgb(time, energy);
        }
        String step2Name = getTypeDisplayName(step2Rgb);
        drawGuideLine(guiGraphics, "←" + step2Name, textX, y0 + lineSpacing * 2, 0xFF_CCCCCC);

        // Step 3: ↖ type from age-radius diagram (time + space)
        int step3Rgb = CelestialBodyMatcher.getAgeRadiusRgb(time, space);
        String step3Name = getTypeDisplayName(step3Rgb);
        drawGuideLine(guiGraphics, "↖" + step3Name, textX, y0 + lineSpacing, 0xFF_CCCCCC);
    }

    /**
     * Convert a diagram RGB color to a display name via translation keys.
     * Uses the existing {@code screen.anvilcraft.cfa.class.<name>} key pattern.
     * Rocky planet subtypes all map to {@code rocky_planet}.
     */
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

    private void drawGuideLine(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        guiGraphics.drawString(font, text, x, y, color, false);
    }

    private static final float UI_AXIAL_TILT = 25f;

    private static final ModelResourceLocation UI_STAR_MODEL = ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/star"));
    private static final ModelResourceLocation UI_NEUTRON_STAR_MODEL =
        ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/neutron_star"));
    private static final ModelResourceLocation UI_NEUTRON_STAR_JET_MODEL =
        ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/neutron_star_jet"));
    private static final ModelResourceLocation UI_BLACK_HOLE_MODEL =
        ModelResourceLocation.standalone(AnvilCraft.of("block/celestial_body/black_hole"));

    private static ModelResourceLocation getUiStarModel(StarData star) {
        if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR) return UI_NEUTRON_STAR_MODEL;
        if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) return UI_BLACK_HOLE_MODEL;
        return UI_STAR_MODEL;
    }

    private void renderBodyPreview(GuiGraphics guiGraphics, CelestialBodyData body) {
        // Complex custom models (shattered, hollow, flesh, intelligence, error)
        if (body instanceof SpecialCelestialBodyData s && s.specialType().needsCustomModel()) {
            renderComplexModelPreview(guiGraphics, s);
            return;
        }
        int size = Math.min(PV_BODY_W, PV_BODY_H) - 16;
        float scale = size / 2f;
        int cx = PV_X + PV_BODY_W / 2;
        int cy = PV_Y + PV_BODY_H / 2;
        float rotY = (previewRotTick * CelestialBodyData.getVisualRotationSpeed(body.rotationSpeed())) * (float) Math.PI / 180f;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(cx, cy, 100);
        guiGraphics.pose().scale(scale, -scale, scale);
        guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(UI_AXIAL_TILT));
        guiGraphics.pose().mulPose(Axis.YP.rotation(rotY));
        guiGraphics.pose().translate(-0.5, -0.5, -0.5);

        // Star: model loading with color overlay + halo (120% size)
        // Neutron stars and black holes use dedicated models without color overlay/halo
        if (body instanceof StarData star) {
            if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR
                || star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
                renderRemnantModelPreview(guiGraphics, star);
                // Render neutron star jet with lighthouse effect
                if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR) {
                    renderNeutronStarJetPreview(guiGraphics, star);
                }
                guiGraphics.pose().popPose();
                return;
            }
            renderStarPreview(guiGraphics, star);
            guiGraphics.pose().popPose();
            return;
        }
        ResourceLocation tex = CelestialBodyTextureBakery.getOrBakeBody(body);
        if (tex == null) {
            guiGraphics.pose().popPose();
            return;
        }

        var buf = guiGraphics.bufferSource();

        // Planet body (cutout)
        var rt = ModRenderTypes.STAR_CUTOUT.apply(tex);
        VertexConsumer vc = buf.getBuffer(rt);
        CelestialBodyRenderer.renderPlanetBody(guiGraphics.pose(), vc, 0x00F000F0, 0);

        // Atmosphere (translucent)
        Temperature atmosTemp = getUiAtmosphereTemp(body);
        if (atmosTemp != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.5, 0.5, 0.5);
            guiGraphics.pose().scale(1.125f, 1.125f, 1.125f);
            guiGraphics.pose().translate(-0.5, -0.5, -0.5);
            CelestialBodyRenderer.renderAtmosphere(
                guiGraphics.pose(),
                buf,
                atmosTemp,
                LightTexture.FULL_BRIGHT,
                0,
                getMenu().getBlockEntity().getBlockPos().asLong()
            );
            guiGraphics.pose().popPose();
        }

        // Render ring (translucent, depth-tested against planet body)
        ResourceLocation ringTex = CelestialBodyTextureBakery.getOrBakeRing(body);
        if (ringTex != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.5, 0.5, 0.5);
            guiGraphics.pose().scale(1.2f, 1.2f, 1.2f);
            guiGraphics.pose().translate(-0.5, -0.5, -0.5);
            var ringConsumer = buf.getBuffer(RenderType.entityTranslucent(ringTex));
            CelestialBodyRenderer.renderRing(guiGraphics.pose(), ringConsumer, LightTexture.FULL_BRIGHT, 0);
            guiGraphics.pose().popPose();
        }

        // Flush all layers together — cutout body first, then translucent atmosphere + ring
        buf.endBatch();

        guiGraphics.pose().popPose();
    }

    /**
     * Render a star in the UI: animated base model + color overlay + halo.
     */
    @SuppressWarnings(
        {
            "checkstyle:VariableDeclarationUsageDistance",
            "checkstyle:Indentation"
        }
    )
    private void renderStarPreview(GuiGraphics guiGraphics, StarData star) {
        if (minecraft == null) return;
        BakedModel model = minecraft.getModelManager().getModel(getUiStarModel(star));
        if (model == minecraft.getModelManager().getMissingModel()) return;

        var buf = guiGraphics.bufferSource();
        // Animated grayscale star model
        minecraft.getBlockRenderer().getModelRenderer()
            .renderModel(guiGraphics.pose().last(),
                buf.getBuffer(RenderType.cutout()),
                null,
                model,
                1.0f,
                1.0f,
                1.0f,
                LightTexture.FULL_BRIGHT,
                0
            );

        // Color overlay — multiplicative blend
        float[] rgb = CelestialBodyTextureBakery.starColor(star);
        long seed = getMenu().getBlockEntity().getBlockPos().asLong();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.5, 0.5, 0.5);
        guiGraphics.pose().scale(1.005f, 1.005f, 1.005f);
        guiGraphics.pose().translate(-0.5, -0.5, -0.5);
        renderStarColorOverlay(guiGraphics, rgb[0], rgb[1], rgb[2]);
        guiGraphics.pose().popPose();

        // Halo
        CelestialBodyRenderer.renderStarHalo(guiGraphics.pose(), buf, star, LightTexture.FULL_BRIGHT, 0, seed);
        buf.endBatch();
    }

    /**
     * Renders a multiplicative-blend color overlay for star preview.
     */
    private void renderStarColorOverlay(GuiGraphics guiGraphics, float r, float g, float b) {
        BakedModel cubeModel = minecraft.getBlockRenderer().getBlockModel(Blocks.WHITE_CONCRETE.defaultBlockState());
        VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(ModRenderTypes.STAR_COLOR_OVERLAY);
        RandomSource random = RandomSource.create(42L);
        for (Direction dir : Direction.values()) {
            for (BakedQuad quad : cubeModel.getQuads(null, dir, random, ModelData.EMPTY, null)) {
                consumer.putBulkData(guiGraphics.pose().last(), quad, r, g, b, 1.0f, LightTexture.FULL_BRIGHT, 0);
            }
        }
        for (BakedQuad quad : cubeModel.getQuads(null, null, random, ModelData.EMPTY, null)) {
            consumer.putBulkData(guiGraphics.pose().last(), quad, r, g, b, 1.0f, LightTexture.FULL_BRIGHT, 0);
        }
    }

    /**
     * Render a complex-model celestial body (shattered, hollow, error) via its block model.
     */
    private void renderComplexModelPreview(GuiGraphics guiGraphics, SpecialCelestialBodyData special) {
        if (minecraft == null) return;
        var modelLoc = special.specialType().getModelLocation();
        BakedModel model = minecraft.getModelManager().getModel(modelLoc);
        if (model == minecraft.getModelManager().getMissingModel()) return;

        int size = Math.min(PV_BODY_W, PV_BODY_H) - 16;
        float scale = size / 2f;
        int cx = PV_X + PV_BODY_W / 2;
        int cy = PV_Y + PV_BODY_H / 2;
        float rotY = (previewRotTick * CelestialBodyData.getVisualRotationSpeed(special.rotationSpeed())) * (float) Math.PI / 180f;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(cx, cy, 100);
        guiGraphics.pose().scale(scale, -scale, scale);
        guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(UI_AXIAL_TILT));
        guiGraphics.pose().mulPose(Axis.YP.rotation(rotY));
        guiGraphics.pose().translate(-0.5, -0.5, -0.5);

        var modelRenderer = minecraft.getBlockRenderer().getModelRenderer();
        var buf = guiGraphics.bufferSource();
        modelRenderer.renderModel(
            guiGraphics.pose().last(),
            buf.getBuffer(RenderType.cutout()),
            null,
            model,
            1.0f,
            1.0f,
            1.0f,
            LightTexture.FULL_BRIGHT,
            0
        );

        // Atmosphere for complex-model bodies that have it
        if (special.hasAtmosphere() && special.temperature() != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.5, 0.5, 0.5);
            guiGraphics.pose().scale(1.125f, 1.125f, 1.125f);
            guiGraphics.pose().translate(-0.5, -0.5, -0.5);
            CelestialBodyRenderer.renderAtmosphere(
                guiGraphics.pose(),
                buf,
                special.temperature(),
                LightTexture.FULL_BRIGHT,
                0,
                getMenu().getBlockEntity().getBlockPos().asLong()
            );
            guiGraphics.pose().popPose();
        }

        buf.endBatch();

        guiGraphics.pose().popPose();
    }

    @org.jetbrains.annotations.Nullable
    private static Temperature getUiAtmosphereTemp(CelestialBodyData body) {
        if (body instanceof RockyPlanetData rp && rp.hasAtmosphere()) return rp.temperature();
        if (body instanceof SpecialCelestialBodyData s && s.hasAtmosphere()) return s.temperature();
        return null;
    }

    /**
     * Render a neutron star or black hole model in the preview area.
     * The pose is already set up by the caller — just render the model, no color overlay or halo.
     */
    private void renderRemnantModelPreview(GuiGraphics guiGraphics, StarData star) {
        if (minecraft == null) return;
        BakedModel model = minecraft.getModelManager().getModel(getUiStarModel(star));
        if (model == minecraft.getModelManager().getMissingModel()) return;

        // Black hole event horizon is small but accretion disk is large — scale up
        if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.5, 0.5, 0.5);
            guiGraphics.pose().scale(1.5f, 1.5f, 1.5f);
            guiGraphics.pose().translate(-0.5, -0.5, -0.5);
        }

        var buf = guiGraphics.bufferSource();
        minecraft.getBlockRenderer().getModelRenderer().renderModel(
            guiGraphics.pose().last(),
            buf.getBuffer(RenderType.cutout()),
            null,
            model,
            1.0f, 1.0f, 1.0f,
            LightTexture.FULL_BRIGHT,
            0
        );
        buf.endBatch();
        if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
            guiGraphics.pose().popPose();
        }
    }

    /**
     * Render neutron star relativistic jet in the preview window.
     * Jet rotates at 1.5× body speed and is tilted along the magnetic axis
     * to produce the classic pulsar lighthouse effect.
     */
    private void renderNeutronStarJetPreview(GuiGraphics guiGraphics, StarData star) {
        if (minecraft == null) return;
        BakedModel jetModel = minecraft.getModelManager().getModel(UI_NEUTRON_STAR_JET_MODEL);
        if (jetModel == minecraft.getModelManager().getMissingModel()) return;

        float magneticTilt = star.magneticFieldStrength() >= 5 ? 15f : 10f;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.5, 0.5, 0.5);
        guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(magneticTilt));
        guiGraphics.pose().translate(-0.5, -0.5, -0.5);

        var modelRenderer = minecraft.getBlockRenderer().getModelRenderer();
        modelRenderer.renderModel(
            guiGraphics.pose().last(),
            guiGraphics.bufferSource().getBuffer(RenderType.translucent()),
            null,
            jetModel,
            1.0f, 1.0f, 1.0f,
            LightTexture.FULL_BRIGHT,
            0
        );
        guiGraphics.bufferSource().endBatch();
        guiGraphics.pose().popPose();
    }

    private void renderBodyInfo(GuiGraphics guiGraphics, CelestialBodyData body) {
        var be = getMenu().getBlockEntity();

        // Show accelerator evolution progress instead of normal body info
        if (be.isAcceleratorActive()) {
            renderAcceleratorProgress(guiGraphics, be);
            return;
        }

        float offsetAge = be.getDisplayOffset(0);
        float offsetRadius = be.getDisplayOffset(1);
        float offsetMass = be.getDisplayOffset(2);
        List<Component> lines = buildInfoLines(body, be.getAgeAnvilCount(), be.getStellarMass(), offsetAge, offsetRadius, offsetMass);
        // Split "Label: value" into "Label:" + "value" on separate lines
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
        int lineHeight = font.lineHeight + 1;
        int maxLines = PV_INFO_H / lineHeight;
        int maxScroll = Math.max(0, displayLines.size() - maxLines);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        guiGraphics.enableScissor(leftPos + PV_INFO_X, topPos + PV_INFO_Y, leftPos + PV_INFO_X + PV_INFO_W, topPos + PV_INFO_Y + PV_INFO_H);

        for (int i = scrollOffset; i < Math.min(displayLines.size(), scrollOffset + maxLines); i++) {
            String lineText = displayLines.get(i).getString();
            int color = lineText.endsWith(":") ? 0x888888 : 0xFFFFFF;
            guiGraphics.drawString(font, displayLines.get(i), PV_INFO_X, PV_INFO_Y + (i - scrollOffset) * lineHeight, color, false);
        }
        guiGraphics.disableScissor();

        // Scrollbar on right edge of info panel (track + thumb style)
        if (maxScroll > 0) {
            int sbX = PV_INFO_X + PV_INFO_W - 3;
            int sbW = 2;
            guiGraphics.fill(sbX, PV_INFO_Y, sbX + sbW, PV_INFO_Y + PV_INFO_H, 0x40_FFFFFF);
            int thumbH = Math.max(12, PV_INFO_H * maxLines / displayLines.size());
            int thumbY = PV_INFO_Y + (PV_INFO_H - thumbH) * scrollOffset / maxScroll;
            guiGraphics.fill(sbX, thumbY, sbX + sbW, thumbY + thumbH, 0x80_CCCCCC);
        }
    }

    /**
     * Render the stellar evolution accelerator progress in the info panel.
     */
    private void renderAcceleratorProgress(GuiGraphics guiGraphics, CelestialForgingAnvilBlockEntity be) {
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
        // Time remaining (use local countdown that decrements every client tick)
        int displayTicks = localAcceleratorTicksRemaining > 0 ? localAcceleratorTicksRemaining : be.getAcceleratorTicksRemaining();
        int secondsRemaining = displayTicks / 20;
        lines.add(Component.translatable("screen.anvilcraft.cfa.evolution.time_remaining",
            Component.literal(formatDuration(secondsRemaining))));
        // Progress bar info
        if (be.getAcceleratorTicksTotal() > 0) {
            int pct = (int) ((1.0f - (float) displayTicks / be.getAcceleratorTicksTotal()) * 100);
            lines.add(Component.literal(pct + "%"));
        }
        // Infinite power indicator for Stage 1
        if (stage == 1 && be.getActiveMegastructureIndex() >= 0) {
            var opt = be.getActiveMegastructureOption();
            if (opt != null && (opt.megastructure().contains("dyson_sphere"))) {
                lines.add(Component.translatable("screen.anvilcraft.cfa.evolution.infinite_power"));
            }
        }

        int lineHeight = font.lineHeight + 1;
        int y = PV_INFO_Y;
        for (Component line : lines) {
            guiGraphics.drawString(font, line, PV_INFO_X, y, 0xFFFFFF, false);
            y += lineHeight;
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

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void renderResourceBar(GuiGraphics guiGraphics) {
        var be = getMenu().getBlockEntity();
        var resources = be.getPlanetaryResourceSet();
        if (resources == null || resources.isEmpty()) return;

        List<String> entries = new ArrayList<>();
        collectItemEntries(entries, resources.getMinerals());
        collectFluidEntries(entries, resources.getFluids());
        collectItemEntries(entries, resources.getGiantItems());
        collectFluidEntries(entries, resources.getGiantFluids());
        collectItemEntries(entries, resources.getBiologicalItems());
        collectFluidEntries(entries, resources.getBiologicalFluids());
        collectItemEntries(entries, resources.getOfferings());
        collectItemEntries(entries, resources.getWastelandItems());
        if (entries.isEmpty()) return;

        // Compute total text width for scroll bounds
        int spacing = 10;
        int totalW = -spacing;
        for (String e : entries) totalW += font.width(e) + spacing;
        int contentX = PV_X + 4;
        int contentW = PV_W - 8;
        int maxScroll = Math.max(0, totalW - contentW);
        resourceScrollOffset = Mth.clamp(resourceScrollOffset, 0, maxScroll);

        // Scissor (absolute screen coords)
        guiGraphics.enableScissor(leftPos + PV_X, topPos + PV_RES_Y, leftPos + PV_X + PV_W, topPos + PV_RES_Y + PV_RES_H);

        // Title — centered, same style as entries
        Component title = Component.translatable("screen.anvilcraft.cfa.resource_title");
        guiGraphics.drawString(font, title, PV_X + (PV_W - font.width(title)) / 2, PV_RES_Y, 0xFF_AAAAAA, false);

        // Resource entries (GUI-relative coords)
        int x = contentX - resourceScrollOffset;
        int y = PV_RES_Y + font.lineHeight + 1;
        for (String entry : entries) {
            int w = font.width(entry);
            if (x + w >= PV_X && x <= PV_X + PV_W) {
                guiGraphics.drawString(font, entry, x, y, 0xFFFFFF, true);
            }
            x += w + spacing;
        }

        guiGraphics.disableScissor();

        // Horizontal scrollbar
        if (maxScroll > 0) {
            int sbY = PV_RES_Y + PV_RES_H;
            int trackX = PV_X + 2;
            int trackW = PV_W - 4;
            guiGraphics.fill(trackX, sbY, trackX + trackW, sbY + 2, 0x40_FFFFFF);
            int thumbW = Math.max(12, trackW * contentW / totalW);
            int thumbX = trackX + (trackW - thumbW) * resourceScrollOffset / maxScroll;
            guiGraphics.fill(thumbX, sbY, thumbX + thumbW, sbY + 2, 0x80_CCCCCC);
        }
    }

    private void collectItemEntries(List<String> out, List<PlanetaryResourceSet.WeightedItemStack> items) {
        int totalW = items.stream().mapToInt(PlanetaryResourceSet.WeightedItemStack::weight).sum();
        for (var entry : items) {
            var it = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(entry.itemId());
            String name = it.getDescription().getString();
            int pct = totalW > 0 ? entry.weight() * 100 / totalW : 0;
            out.add(name + " " + pct + "%");
        }
    }

    private void collectFluidEntries(List<String> out, List<PlanetaryResourceSet.WeightedFluidStack> fluids) {
        int totalW = fluids.stream().mapToInt(PlanetaryResourceSet.WeightedFluidStack::weight).sum();
        for (var entry : fluids) {
            var f = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(entry.fluidId());
            String name;
            name = f.getFluidType().getDescription().getString();
            int pct = totalW > 0 ? entry.weight() * 100 / totalW : 0;
            out.add(name + " " + pct + "%");
        }
    }

    private List<Component> buildInfoLines(
        CelestialBodyData body,
        int ageAnvilCount,
        int massAnvilCount,
        float offsetAge,
        float offsetRadius,
        float offsetMass
    ) {
        List<Component> lines = new ArrayList<>();
        boolean isError = body instanceof SpecialCelestialBodyData special && special.specialType().isErrorPlanet();
        // Type name
        String typeKey;
        if (body instanceof RockyPlanetData rp) {
            typeKey = rockyTypeKey(rp);
        } else if (body instanceof SpecialCelestialBodyData s) {
            typeKey = "screen.anvilcraft.cfa.class.special." + s.specialType().getName();
        } else {
            typeKey = "screen.anvilcraft.cfa.class." + body.bodyClass().name().toLowerCase();
        }
        lines.add(Component.translatable("screen.anvilcraft.cfa.type", Component.translatable(typeKey)));
        // Age (Error Planet shows "???")
        if (isError) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.age", Component.literal("???")));
        } else {
            lines.add(Component.translatable(
                "screen.anvilcraft.cfa.age",
                CelestialForgingAnvilMenu.formatAgeOffset(ageAnvilCount, offsetAge)
            ));
        }
        // Radius (Error Planet shows "???")
        if (isError) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.radius", Component.literal("???")));
        } else {
            lines.add(Component.translatable(
                "screen.anvilcraft.cfa.radius",
                CelestialForgingAnvilMenu.formatRadiusOffset(body.size(), offsetRadius)
            ));
        }
        // Mass (Error Planet shows "???")
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
                if (s.specialType().isErrorPlanet()) {
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
                        Component.translatable(s.hasAtmosphere() ? "screen.anvilcraft.cfa.atmos.yes" : "screen.anvilcraft.cfa.none")
                    ));
                    lines.add(Component.translatable(
                        "screen.anvilcraft.cfa.liquid",
                        Component.translatable("screen.anvilcraft.cfa.liquid." + s.liquidCoverage().getSerializedName())
                    ));
                    lines.add(magText(s.magneticFieldStrength()));
                    lines.add(spinText(s.rotationSpeed()));
                    lines.add(tiltText(s.axialTilt()));
                }
            }
            case StarData s -> {
                lines.add(this.magText(s.magneticFieldStrength()));
                lines.add(this.spinText(s.rotationSpeed()));
                // Axial tilt only if non-zero
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
                    Component.translatable(rp.hasAtmosphere() ? "screen.anvilcraft.cfa.atmos.yes" : "screen.anvilcraft.cfa.none")
                ));
                lines.add(Component.translatable(
                    "screen.anvilcraft.cfa.liquid",
                    Component.translatable("screen.anvilcraft.cfa.liquid." + rp.liquidCoverage().getSerializedName())
                ));
                lines.add(magText(rp.magneticFieldStrength()));
                lines.add(spinText(rp.rotationSpeed()));
                lines.add(tiltText(rp.axialTilt()));
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
                lines.add(magText(gp.magneticFieldStrength()));
                lines.add(spinText(gp.rotationSpeed()));
                lines.add(tiltText(gp.axialTilt()));
            }
            default -> {
            }
        }
        return lines;
    }

    /**
     * Compute the type display key for a rocky planet based on temperature,
     * liquid coverage, and atmosphere — not just the body class.
     */
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
        // COLD, MILD, HOT
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

    /**
     * Format to 3 significant figures.
     */
    @SuppressWarnings("MalformedFormatString")
    private static String format3SigFig(double value) {
        if (Math.abs(value) < 1e-9) return "0";
        int pow = (int) Math.floor(Math.log10(Math.abs(value)));
        int digits = Math.clamp(2 - pow, 0, 6);
        return String.format(Locale.US, "%." + digits + "f", value);
    }

    @SuppressWarnings("SameParameterValue")
    private void drawParamText(GuiGraphics guiGraphics, String text, int x, int y, int width) {
        int textX = x + (width - this.font.width(text)) / 2;
        guiGraphics.drawString(this.font, text, textX, y, 0xFFFFFF, true);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // Re-render preview buttons on top of the guide map when it's visible
        if (!isLocked() && guideTriggered && searchState != SearchState.LOADING) {
            int relX = mouseX - leftPos;
            int relY = mouseY - topPos;
            renderPreviewBottomButtons(guiGraphics, leftPos, topPos, relX, relY);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (lockedMsgTick > 0) {
            Component msg = Component.translatable("screen.anvilcraft.cfa.locked_tooltip");
            int w = font.width(msg);
            guiGraphics.drawString(font, msg, leftPos + (imageWidth - w) / 2, topPos - 12, 0xFF5555, true);
        }
        if (refactorErrorTick > 0 && refactorErrorMsg != null) {
            int w = font.width(refactorErrorMsg);
            guiGraphics.drawString(font, refactorErrorMsg, leftPos + (imageWidth - w) / 2, topPos - 12, 0xFF5555, true);
        }
        if (unlockWarningTick > 0) {
            Component msg = Component.translatable("screen.anvilcraft.cfa.unlock_warning");
            int w = font.width(msg);
            guiGraphics.drawString(font, msg, leftPos + (imageWidth - w) / 2, topPos - 12, 0xFF5555, true);
        }
        if (getMenu().getBlockEntity().isAcceleratorActive()) {
            Component msg = Component.translatable("screen.anvilcraft.cfa.evolution_cannot_unlock");
            int w = font.width(msg);
            guiGraphics.drawString(font, msg, leftPos + (imageWidth - w) / 2, topPos - 12, 0xFF5555, true);
        }
    }

    @Override
    public void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        super.renderSlot(guiGraphics, slot);
        if (slot instanceof CelestialForgingAnvilMenu.CFAAnvilSlot && !slot.hasItem()) {
            int index = slot.getSlotIndex();
            ItemStack ghost = GHOST_STACKS[index];
            if (!ghost.isEmpty()) {
                RenderSupport.renderItemWithTransparency(ghost, guiGraphics.pose(), slot.x, slot.y, 0.52f);
                guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x60ffaaaa);
            }
        }
        if (slot instanceof CelestialForgingAnvilMenu.CFAMaterialSlot && !slot.hasItem()) {
            var be = getMenu().getBlockEntity();
            ItemStack filter = be.getMaterialFilter();
            if (!filter.isEmpty()) {
                RenderSupport.renderItemWithTransparency(filter, guiGraphics.pose(), slot.x, slot.y, 0.52f);
                guiGraphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x60ffaaaa);
                // Render count (matching IFilterScreen.renderSlotLimit style)
                int limit = be.getMaterialLimit();
                if (limit > 0 && !filter.is(Items.BARRIER)) {
                    String countStr = String.valueOf(limit);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(0, 0, 300);
                    float scale = 0.6f;
                    guiGraphics.pose().scale(scale, scale, 1.0f);
                    int width = font.width(countStr);
                    int x = (int) ((slot.x + 16.25 - width * scale) / scale);
                    int y = (int) ((slot.y + 14 - font.lineHeight * 2 * scale + 1) / scale);
                    guiGraphics.drawString(font, countStr, x, y, 0xFFA0A0, true);
                    guiGraphics.pose().popPose();
                }
            }
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        // Suppress vanilla item tooltip for CFA-specific slots (anvil + seed).
        // Player inventory slots still get the standard item tooltip.
        boolean isCfaSlot = this.hoveredSlot instanceof CelestialForgingAnvilMenu.CFAAnvilSlot
            || this.hoveredSlot instanceof CelestialForgingAnvilMenu.SeedSlot;
        if (!isCfaSlot) {
            super.renderTooltip(guiGraphics, x, y);
        }
        int relX = x - leftPos;
        int relY = y - topPos;
        if (relX >= SB_X && relX < SB_X + SB_W && relY >= SB_Y && relY < SB_Y + SB_H) {
            if (isLocked()) {
                guiGraphics.renderTooltip(font, Component.translatable("screen.anvilcraft.cfa.locked_tooltip"), x, y);
            } else if (searchState == SearchState.DONE && !searchHistory().isEmpty()) {
                guiGraphics.renderTooltip(font, Component.translatable("screen.anvilcraft.cfa.re_search_tooltip"), x, y);
            } else {
                guiGraphics.renderTooltip(font, Component.translatable("screen.anvilcraft.cfa.search_tooltip"), x, y);
            }
        }
        if (isOverLockButton(relX, relY) && (searchState == SearchState.DONE || searchState == SearchState.LOADING)) {
            guiGraphics.renderTooltip(
                font,
                Component.translatable(isLocked() ? "screen.anvilcraft.cfa.unlock" : "screen.anvilcraft.cfa.lock"),
                x,
                y
            );
        }
        // Refactor option tooltips
        if (isLocked() && searchState == SearchState.DONE) {
            int refOpt = getRefactorOptionAt(relX, relY);
            if (refOpt >= 0 && refOpt < refactorOptions.size()) {
                CelestialRefactorOption option = refactorOptions.get(refOpt);
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
                guiGraphics.renderTooltip(font, tooltipLines, java.util.Optional.empty(), x, y);
            }
        }
        // Material slot tooltip
        if (isLocked() && searchState == SearchState.DONE && this.hoveredSlot instanceof CelestialForgingAnvilMenu.CFAMaterialSlot) {
            guiGraphics.renderTooltip(font, Component.translatable("screen.anvilcraft.cfa.refactor_materials"), x, y);
        }
        // Start refactoring button tooltip
        if (isLocked() && searchState == SearchState.DONE && isOverRefactorStart(relX, relY)) {
            guiGraphics.renderTooltip(font, Component.translatable("screen.anvilcraft.cfa.refactor_start_tooltip"), x, y);
        }
        // Seed slot tooltip
        if (this.hoveredSlot instanceof CelestialForgingAnvilMenu.SeedSlot) {
            List<Component> seedTooltip = new ArrayList<>();
            seedTooltip.add(Component.translatable("screen.anvilcraft.cfa.seed_slot.line1"));
            seedTooltip.add(Component.translatable("screen.anvilcraft.cfa.seed_slot.line2"));
            guiGraphics.renderTooltip(font, seedTooltip, java.util.Optional.empty(), x, y);
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
                guiGraphics.renderTooltip(font, Component.literal(text), x, y);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;

        if (isOverSearchButton(relX, relY)) {
            if (isLocked()) {
                showLockedMessage();
                return true;
            }
            performSearch();
            return true;
        }
        if (isOverPrevButton(relX, relY) && getMenu().getBlockEntity().hasPreviousHistory()) {
            if (isLocked()) {
                showLockedMessage();
                return true;
            }
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, 201);
            }
            return true;
        }
        if (isOverNextButton(relX, relY) && getMenu().getBlockEntity().hasNextHistory()) {
            if (isLocked()) {
                showLockedMessage();
                return true;
            }
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, 202);
            }
            return true;
        }
        if (isOverLockButton(relX, relY) && (searchState == SearchState.DONE || searchState == SearchState.LOADING)) {
            if (getMenu().getBlockEntity().isAcceleratorActive()) {
                return true;
            }
            if (isLocked() && !hasShiftDown()) {
                showUnlockWarning();
                return true;
            }
            // Send lock toggle to server (button ID 200)
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, 200);
            }
            // Also toggle locally for immediate UI feedback
            setLocked(!isLocked());
            return true;
        }
        // Refactor option click
        int optIdx = getRefactorOptionAt(relX, relY);
        if (optIdx >= 0) {
            selectedRefactorIndex = optIdx;
            // Tell server to configure the material slot for this option
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, 9 + optIdx);
            }
            return true;
        }
        // Start refactoring button click
        if (isOverRefactorStart(relX, relY)) {
            handleRefactorStart();
            return true;
        }
        // Refactor scrollbar drag initiation
        if (isMouseInRefactorScrollbar(relX, relY)) {
            int totalRows = !refactorOptions.isEmpty() ? (refactorOptions.size() + RF_COLS - 1) / RF_COLS : 0;
            int maxScroll = Math.max(0, totalRows - RF_ROWS_VISIBLE);
            if (maxScroll > 0) {
                isDraggingRfScrollbar = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
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
            if (paramIdx >= 0 && this.minecraft != null && this.minecraft.player != null) {
                int buttonId = scrollY > 0 ? 1 + paramIdx : 5 + paramIdx;
                this.minecraft.player.connection.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, buttonId));
                return true;
            }
        }
        // Info area: scroll text
        if (relX >= PV_INFO_X && relX < PV_INFO_X + PV_INFO_W && relY >= PV_INFO_Y && relY < PV_INFO_Y + PV_INFO_H) {
            scrollOffset -= (int) scrollY;
            return true;
        }
        // Resource bar: horizontal pixel scroll
        if (relX >= PV_X && relX < PV_X + PV_W && relY >= PV_RES_Y && relY < PV_RES_Y + PV_RES_H) {
            resourceScrollOffset -= (int) scrollY * 30;
            return true;
        }
        // Refactor options area: scroll buttons
        if (isMouseInRefactorArea(relX, relY) || isMouseInRefactorScrollbar(relX, relY)) {
            int totalRows = !refactorOptions.isEmpty() ? (refactorOptions.size() + RF_COLS - 1) / RF_COLS : 0;
            int maxScroll = Math.max(0, totalRows - RF_ROWS_VISIBLE);
            if (maxScroll > 0) {
                rfScrollRow = (int) Mth.clamp(rfScrollRow - scrollY, 0, maxScroll);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;
        if (isDraggingRfScrollbar) {
            int totalRows = !refactorOptions.isEmpty() ? (refactorOptions.size() + RF_COLS - 1) / RF_COLS : 0;
            int maxScroll = Math.max(0, totalRows - RF_ROWS_VISIBLE);
            if (maxScroll > 0) {
                float scroll = (relY - RF_SCROLL_Y - RF_SCROLL_THUMB_H / 2f) / (RF_SCROLL_H - RF_SCROLL_THUMB_H);
                rfScrollRow = Mth.clamp((int) (scroll * maxScroll + 0.5f), 0, maxScroll);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    // === Search ===

    private void performSearch() {
        if (searchState == SearchState.LOADING) return; // already searching
        var be = getMenu().getBlockEntity();
        // Check if seed item is present — skip diagram pre-check for special body discovery
        boolean hasSeedItem = !be.getAnvilInventory().getItem(4).isEmpty();
        // Client-side pre-check: immediate fail if match impossible (skip when seed item present)
        if (!hasSeedItem && minecraft != null && minecraft.level != null) {
            var preCheck = CelestialBodyMatcher.match(
                be.getAnvilCount(0),
                be.getAnvilCount(1),
                be.getAnvilCount(2),
                be.getAnvilCount(3),
                be.isAmplify(),
                minecraft.level.getRandom()
            );
            if (preCheck == null) {
                searchState = SearchState.FAIL;
                return;
            }
        }
        // Remember current body to detect new result
        preSearchBody = be.getCelestialBodyData();
        // Send button click to server
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, 0);
        }
        searchState = SearchState.LOADING;
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

    private int lockedMsgTick = 0;

    private void showLockedMessage() {
        lockedMsgTick = 60; // Show for 3 seconds
    }

    private void showUnlockWarning() {
        unlockWarningTick = 60; // Show for 3 seconds
    }

    // === Refactor UI helpers ===

    /**
     * Get the refactor option index at the given relative mouse position.
     * Returns -1 if no option button is hit.
     */
    private int getRefactorOptionAt(int rx, int ry) {
        if (refactorOptions.isEmpty()) return -1;
        for (int visibleRow = 0; visibleRow < RF_ROWS_VISIBLE; visibleRow++) {
            for (int col = 0; col < RF_COLS; col++) {
                int dataRow = rfScrollRow + visibleRow;
                int optIdx = dataRow * RF_COLS + col;
                if (optIdx >= refactorOptions.size()) continue;
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
        return rx >= RF_SCROLL_X && rx < RF_SCROLL_X + RF_SCROLL_W && ry >= RF_SCROLL_Y && ry < RF_SCROLL_Y + RF_SCROLL_H;
    }

    private boolean isOverRefactorStart(int rx, int ry) {
        return rx >= RF_START_X && rx < RF_START_X + RF_START_W && ry >= RF_START_Y && ry < RF_START_Y + RF_START_H;
    }

    private void handleRefactorStart() {
        if (selectedRefactorIndex < 0 || selectedRefactorIndex >= refactorOptions.size()) {
            showRefactorError(Component.translatable("screen.anvilcraft.cfa.no_refactor_option"));
            return;
        }
        var be = getMenu().getBlockEntity();
        CelestialRefactorOption option = refactorOptions.get(selectedRefactorIndex);
        // Only block if the SAME megastructure is already built
        if (be.getActiveMegastructureIndex() >= 0) {
            var activeOption = be.getActiveMegastructureOption();
            if (activeOption != null && activeOption.megastructure().equals(option.megastructure())) {
                showRefactorError(Component.translatable("screen.anvilcraft.cfa.already_built"));
                return;
            }
        }
        if (option.needsMaterial()) {
            // Check the material slot (slot index = anvil slots count = 5, which is slot 5 in the container)
            // The material slot is the CFA's materialContainer, mapped in the menu
            ItemStack inSlot = be.getMaterialContainer().getItem(0);
            ItemStack required = option.material().copyWithCount(option.materialCount());
            if (!ItemStack.isSameItemSameComponents(inSlot, required) || inSlot.getCount() < required.getCount()) {
                showRefactorError(Component.translatable("screen.anvilcraft.cfa.insufficient_materials"));
                return;
            }
        }
        // Send build request to server via button click with encoded option index
        if (minecraft != null && minecraft.gameMode != null) {
            // Use button ID 100 + optionIndex to signal build request
            minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, 100 + selectedRefactorIndex);
        }
    }

    private void showRefactorError(Component msg) {
        refactorErrorMsg = msg;
        refactorErrorTick = 60;
    }
}
