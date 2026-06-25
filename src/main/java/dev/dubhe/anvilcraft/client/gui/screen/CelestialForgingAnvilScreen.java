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
import net.minecraft.ChatFormatting;
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
import net.minecraft.util.FormattedCharSequence;
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

    /// 预览区域（使用0索引坐标）
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

    /// 资源条（位于天体预览和信息面板下方的细条）
    private static final int PV_RES_Y = 76;
    private static final int PV_RES_H = 20;

    /// 搜索按钮（精灵图尺寸48x32，上半为正常状态，下半为悬停状态）
    private static final int SB_X = 32;
    private static final int SB_Y = 121;
    private static final int SB_W = 48;
    private static final int SB_H = 16;

    /// 重构区域
    private static final int RF_TITLE_X = 266;
    private static final int RF_TITLE_Y = 18;
    private static final int RF_TITLE_W = 71;
    private static final int RF_BTN_W = 36;
    private static final int RF_BTN_H = 35;

    /// 四个按钮位置（左上、右上、左下、右下）
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

    /// 重构选项滚动条（轨道位于背景像素332-335，y=40-109，在512x256贴图中）
    private static final int RF_SCROLL_X = 331;
    private static final int RF_SCROLL_Y = 39;
    private static final int RF_SCROLL_H = 70;
    private static final int RF_SCROLL_W = 4;
    private static final int RF_SCROLL_THUMB_H = 12;
    private static final int RF_COLS = 2;
    private static final int RF_ROWS_VISIBLE = 2;

    /// 开始重构按钮（精灵图每状态48x16，总计48x32）
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

    /// 星图指南贴图
    private static final ResourceLocation TEX_CELESTIAL_MAPS = SharedTextures.texture("block/celestial_maps");
    private static final int MAP_SIZE = 160;
    private static final int COLOR_TIME = 0xBF_A0FFA0;    /// 浅绿色，75%透明度
    private static final int COLOR_SPACE = 0xBF_00FFFF;   /// 青色，75%透明度
    private static final int COLOR_MASS = 0xBF_FFFFA0;    /// 浅黄色，75%透明度
    private static final int COLOR_ENERGY = 0xBF_FF8080;  /// 浅红色，75%透明度

    private static final ItemStack[] GHOST_STACKS = {
        new ItemStack(ModBlocks.CONFINED_TIME_ANVILON.asItem()),
        new ItemStack(ModBlocks.CONFINED_SPACE_ANVILON.asItem()),
        new ItemStack(ModBlocks.CONFINED_MASS_ANVILON.asItem()),
        new ItemStack(ModBlocks.CONFINED_ENERGY_ANVILON.asItem())
    };

    /// 搜索状态枚举
    private enum SearchState {
        IDLE, LOADING, DONE, FAIL, POWER_FAIL
    }

    private SearchState searchState = SearchState.IDLE;
    @Nullable
    private CelestialBodyData preSearchBody = null;

    /// 历史浏览现在由服务端处理，按钮点击发送数据包ID 201/202

    /// 锁定状态持久化在方块实体中
    private boolean isLocked() {
        return getMenu().getBlockEntity().isLocked();
    }

    private void setLocked(boolean v) {
        getMenu().getBlockEntity().setLocked(v);
    }

    private List<?> searchHistory() {
        return getMenu().getBlockEntity().getSearchHistory();
    }

    /// 旋转动画计时器
    private int previewRotTick = 0;

    /// 信息面板滚动偏移
    private int scrollOffset = 0;

    /// 资源条滚动偏移
    private int resourceScrollOffset = 0;

    /// 加速器进度本地倒计时（客户端侧，每tick递减）
    private int localAcceleratorTicksRemaining = 0;

    /// 星环缩放除数：环越大除数越大，渲染后与环1大小一致
    /// ring_small 父级（环1-2）：主环半径约8单位
    /// ring_big 父级（环4-5）：主环半径约12.75单位（约1.6倍）
    private static final float RING1_SCALE_DIV = 1.00f;
    private static final float RING2_SCALE_DIV = 1.25f;
    private static final float RING4_SCALE_DIV = 1.60f;
    private static final float RING5_SCALE_DIV = 1.85f;
    private static final float RING6_SCALE_DIV = 2.10f;

    /// 重构相关状态
    private List<CelestialRefactorOption> refactorOptions = List.of();
    private int selectedRefactorIndex = -1;
    private int rfScrollRow = 0;
    private int refactorMaxScroll = 0;
    private boolean isDraggingRfScrollbar = false;
    private int refactorErrorTick = 0;
    @Nullable
    private Component refactorErrorMsg = null;
    private int unlockWarningTick = 0;

    /// 已建造巨构文本显示常量
    private static final int BMT_TEXT_W = 72; /// 两个按钮宽度（网格区域全宽）
    private static final int BMT_TEXT_PAD = 2; /// 面板内文本的左右上边距
    private static final int BMT_WRAP_W = BMT_TEXT_W - BMT_TEXT_PAD * 2; /// 68px，去边距后的可用换行宽度
    private static final int BMT_TEXT_SPACING = 12; /// 文本行内的行间距（使间隔接近均匀：3,3,2像素）

    /// 指南触发：当砧子数量变化时显示星图
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

        /// 从持久化数据恢复状态
        var be = getMenu().getBlockEntity();
        if (be.isSearching() && searchState == SearchState.IDLE) {
            searchState = SearchState.LOADING;
        } else if (be.getCelestialBodyData() != null && searchState == SearchState.IDLE) {
            searchState = SearchState.DONE;
        }

        /// 捕获初始砧子数量，以便指南仅在变化时触发
        for (int i = 0; i < 4; i++) {
            previousAnvilCounts[i] = be.getAnvilCount(i);
        }
        guideTriggered = false;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        previewRotTick++;

        /// 检测砧子数量变化以触发星图指南
        var be = getMenu().getBlockEntity();
        for (int i = 0; i < 4; i++) {
            int cur = be.getAnvilCount(i);
            if (cur != previousAnvilCounts[i]) {
                guideTriggered = true;
            }
            previousAnvilCounts[i] = cur;
        }
        /// 开始新搜索时重置指南触发状态
        if (searchState == SearchState.LOADING) {
            guideTriggered = false;
        }

        if (lockedMsgTick > 0) lockedMsgTick--;
        if (refactorErrorTick > 0) refactorErrorTick--;
        if (unlockWarningTick > 0) unlockWarningTick--;

        /// 加速器进度的客户端倒计时显示
        {
            var beAccel = getMenu().getBlockEntity();
            if (beAccel.isAcceleratorActive()) {
                int serverTicks = beAccel.getAcceleratorTicksRemaining();
                /// 首次初始化或服务端值严格领先（数值更小）
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

        /// 搜索按钮（精灵图每状态48x16，总计48x32）
        ResourceLocation btnTex = (searchState == SearchState.DONE && !searchHistory().isEmpty()) ? TEX_RESEARCH : TEX_SEARCH;
        boolean hoverSearch = relX >= SB_X && relX < SB_X + SB_W && relY >= SB_Y && relY < SB_Y + SB_H;
        renderButton(guiGraphics, btnTex, i + SB_X, j + SB_Y, SB_W, SB_H, hoverSearch);

        /// 预览区域底部按钮
        renderPreviewBottomButtons(guiGraphics, i, j, relX, relY);

        /// 重构区域
        renderRefactorSection(guiGraphics, i, j, relX, relY);
    }

    /// 渲染预览区域下方的上一个/下一个/锁定按钮。
    /// 由 #renderBg 调用，并在指南渲染后再次调用以保持在上层。
    private void renderPreviewBottomButtons(GuiGraphics guiGraphics, int guiLeft, int guiTop, int relX, int relY) {
        if (searchState != SearchState.DONE && searchState != SearchState.LOADING) return;
        boolean hasPrev = getMenu().getBlockEntity().hasPreviousHistory();
        boolean hasNext = getMenu().getBlockEntity().hasNextHistory();
        /// 上一个按钮
        if (hasPrev) {
            boolean hover = isOverPrevButton(relX, relY);
            renderButton(guiGraphics, TEX_PREV, guiLeft + PV_X + 4, guiTop + PV_BOT_Y + 14, 16, 16, hover);
        }
        /// 下一个按钮
        if (hasNext) {
            boolean hover = isOverNextButton(relX, relY);
            renderButton(guiGraphics, TEX_NEXT, guiLeft + PV_X + PV_W - 20, guiTop + PV_BOT_Y + 14, 16, 16, hover);
        }
        /// 锁定按钮
        boolean hoverLock = isOverLockButton(relX, relY);
        ResourceLocation lockTex = isLocked() ? TEX_LOCKED : TEX_UNLOCKED;
        renderButton(guiGraphics, lockTex, guiLeft + PV_X + PV_W / 2 - 8, guiTop + PV_BOT_Y + 14, 16, 16, hoverLock);
    }

    /// 渲染精灵图按钮：上半为正常状态，下半为悬停状态。
    private void renderButton(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, boolean hovered) {
        RenderSystem.enableDepthTest();
        int v = hovered ? h : 0;
        g.blit(tex, x, y, 0, v, w, h, w, h * 2);
    }

    /// 渲染星禁环重构区域。
    /// 使用基于行的滚动。当巨构建造后，已建造按钮、加速器选项和使用说明文本
    /// 按35px行组织，共享一个滚动条。文本间距为12px，使间隔接近均匀
    ///（3,3,2像素模式，替代旧的1,1,6）。
    private void renderRefactorSection(GuiGraphics guiGraphics, int guiLeft, int guiTop, int relX, int relY) {
        CelestialBodyData body = getMenu().getBlockEntity().getCelestialBodyData();
        boolean hasAcceleratorActive = getMenu().getBlockEntity().isAcceleratorActive();
        boolean hasMegastructure = getMenu().getBlockEntity().getActiveMegastructureIndex() >= 0;
        boolean showOptions = isLocked() && body != null && searchState == SearchState.DONE;
        boolean isActive = showOptions && !hasAcceleratorActive;

        CelestialRefactorOption activeOption = null;
        if (hasMegastructure) {
            activeOption = getMenu().getBlockEntity().getActiveMegastructureOption();
        }

        if (hasMegastructure && isActive) {
            refactorOptions = CelestialRefactorRegistry.getOptions(
                body,
                getMenu().getBlockEntity().isAmplify(),
                getMenu().getBlockEntity().getPlanetaryResourceSet()
            ).stream()
                .filter(opt -> "stellar_evolution_accelerator".equals(opt.megastructure()))
                .toList();
        } else if (!hasMegastructure && isActive) {
            refactorOptions = CelestialRefactorRegistry.getOptions(
                body,
                getMenu().getBlockEntity().isAmplify(),
                getMenu().getBlockEntity().getPlanetaryResourceSet()
            );
        } else {
            refactorOptions = List.of();
        }

        /// 在增幅锻星砧上，只有恒星天体才能承载巨构
        boolean isAmplifiedPlanet = getMenu().getBlockEntity().isAmplify()
            && body != null && !(body instanceof StarData);
        if (isAmplifiedPlanet) {
            refactorOptions = List.of();
        }

        int btnCount = refactorOptions.size();

        /// 计算已建造巨构的换行使用说明文本
        List<FormattedCharSequence> wrappedUsageLines = List.of();
        if (hasMegastructure && activeOption != null) {
            String usageKey = "screen.anvilcraft.cfa.megastructure." + activeOption.megastructure() + ".usage";
            Component usageText = Component.translatable(usageKey);
            wrappedUsageLines = font.split(usageText, BMT_WRAP_W);
        }

        /// 计算总内容行数
        int linesPerTextRow = 3; /// 每35px行3行文本，间距12px可使间隔接近均匀
        if (hasMegastructure) {
            int totalSlots = 1 + btnCount; /// 1个已建造按钮 + 加速器选项
            int buttonRows = (totalSlots + RF_COLS - 1) / RF_COLS;
            int textRows = wrappedUsageLines.isEmpty() ? 0
                : (wrappedUsageLines.size() + linesPerTextRow - 1) / linesPerTextRow;
            int totalRows = buttonRows + textRows;
            refactorMaxScroll = Math.max(0, totalRows - RF_ROWS_VISIBLE);
        } else {
            int totalRows = btnCount > 0 ? (btnCount + RF_COLS - 1) / RF_COLS : 0;
            refactorMaxScroll = Math.max(0, totalRows - RF_ROWS_VISIBLE);
        }
        if (rfScrollRow > refactorMaxScroll) rfScrollRow = refactorMaxScroll;
        if (rfScrollRow < 0) rfScrollRow = 0;
        if (selectedRefactorIndex >= btnCount) selectedRefactorIndex = -1;

        /// 渲染可见行（视口中2行）
        for (int visibleRow = 0; visibleRow < RF_ROWS_VISIBLE; visibleRow++) {
            int contentRow = rfScrollRow + visibleRow;

            if (hasMegastructure) {
                int totalSlots = 1 + btnCount;
                int buttonRows = (totalSlots + RF_COLS - 1) / RF_COLS;

                if (contentRow < buttonRows) {
                    /// 渲染按钮网格行
                    for (int col = 0; col < RF_COLS; col++) {
                        boolean isBuiltButton = (contentRow == 0 && col == 0);
                        int optIdx = isBuiltButton ? -1 : contentRow * RF_COLS + col - 1;

                        int bx = guiLeft + RF_BTN_X[visibleRow * RF_COLS + col];
                        int by = guiTop + RF_BTN_Y[visibleRow * RF_COLS + col];

                        if (isBuiltButton && activeOption != null) {
                            renderButton(guiGraphics, TEX_REFACTOR_OPTIONS, bx, by, RF_BTN_W, RF_BTN_H, true);
                            renderMegastructureModel(guiGraphics, activeOption, bx, by, RF_BTN_W, RF_BTN_H);
                        } else if (optIdx >= 0 && optIdx < btnCount) {
                            boolean hovered = relX >= RF_BTN_X[visibleRow * RF_COLS + col]
                                              && relX < RF_BTN_X[visibleRow * RF_COLS + col] + RF_BTN_W
                                              && relY >= RF_BTN_Y[visibleRow * RF_COLS + col]
                                              && relY < RF_BTN_Y[visibleRow * RF_COLS + col] + RF_BTN_H;
                            boolean selected = optIdx == selectedRefactorIndex;
                            renderButton(guiGraphics, TEX_REFACTOR_OPTIONS, bx, by, RF_BTN_W, RF_BTN_H,
                                hovered || selected);
                            CelestialRefactorOption option = refactorOptions.get(optIdx);
                            renderMegastructureModel(guiGraphics, option, bx, by, RF_BTN_W, RF_BTN_H);
                            if (selected) {
                                guiGraphics.fill(bx, by, bx + RF_BTN_W, by + RF_BTN_H, 0x40_00FF00);
                            }
                        }
                    }
                } else {
                    /// 渲染文本行：3行文本使用BMT_TEXT_SPACING间距，间隔接近均匀（3,3,2像素）
                    int textRow = contentRow - buttonRows;
                    int rowBaseY = guiTop + RF_BTN_Y[visibleRow * RF_COLS] + BMT_TEXT_PAD;
                    for (int li = 0; li < linesPerTextRow; li++) {
                        int lineIdx = textRow * linesPerTextRow + li;
                        if (lineIdx >= wrappedUsageLines.size()) break;
                        guiGraphics.drawString(font, wrappedUsageLines.get(lineIdx),
                            guiLeft + RF_BTN_X[0] + BMT_TEXT_PAD,
                            rowBaseY + li * BMT_TEXT_SPACING,
                            0xAAAAAA, false);
                    }
                }
            } else {
                /// 普通模式：2列网格，未建造巨构
                for (int col = 0; col < RF_COLS; col++) {
                    int optIdx = (rfScrollRow + visibleRow) * RF_COLS + col;
                    if (optIdx >= btnCount) continue;

                    int bx = guiLeft + RF_BTN_X[visibleRow * RF_COLS + col];
                    int by = guiTop + RF_BTN_Y[visibleRow * RF_COLS + col];
                    boolean hovered = relX >= RF_BTN_X[visibleRow * RF_COLS + col]
                                      && relX < RF_BTN_X[visibleRow * RF_COLS + col] + RF_BTN_W
                                      && relY >= RF_BTN_Y[visibleRow * RF_COLS + col]
                                      && relY < RF_BTN_Y[visibleRow * RF_COLS + col] + RF_BTN_H;
                    boolean selected = optIdx == selectedRefactorIndex;

                    renderButton(guiGraphics, TEX_REFACTOR_OPTIONS, bx, by, RF_BTN_W, RF_BTN_H,
                        hovered || selected);
                    CelestialRefactorOption option = refactorOptions.get(optIdx);
                    renderMegastructureModel(guiGraphics, option, bx, by, RF_BTN_W, RF_BTN_H);
                    if (selected) {
                        guiGraphics.fill(bx, by, bx + RF_BTN_W, by + RF_BTN_H, 0x40_00FF00);
                    }
                }
            }
        }

        /// 渲染滚动条
        if (refactorMaxScroll > 0) {
            renderRefactorScrollbar(guiGraphics, guiLeft, guiTop, refactorMaxScroll);
        }

        /// 渲染开始重构按钮（除非巨构已建造且无加速器选项，否则始终显示）
        boolean showStartButton = !hasMegastructure || !refactorOptions.isEmpty();
        if (showStartButton) {
            boolean hoverStart = relX >= RF_START_X && relX < RF_START_X + RF_START_W
                && relY >= RF_START_Y && relY < RF_START_Y + RF_START_H;
            renderButton(guiGraphics, TEX_REFACTORING, guiLeft + RF_START_X, guiTop + RF_START_Y,
                RF_START_W, RF_START_H, hoverStart);
        }

        /// 增幅器+行星警告（红色文本，在按钮区域居中）
        if (isAmplifiedPlanet && isLocked() && body != null && searchState == SearchState.DONE) {
            Component warning = Component.translatable("screen.anvilcraft.cfa.amplified_planet_warning");
            List<FormattedCharSequence> warningLines = font.split(warning, BMT_TEXT_W);
            int areaCY = (RF_BTN_Y[0] + RF_BTN_Y[2] + RF_BTN_H) / 2; /// 中心Y = 74
            int startY = guiTop + areaCY - warningLines.size() * (font.lineHeight + 1) / 2;
            int areaCX = (RF_BTN_X[0] + RF_BTN_X[1] + RF_BTN_W) / 2; /// 中心X = 291
            for (int i = 0; i < warningLines.size(); i++) {
                FormattedCharSequence line = warningLines.get(i);
                int cx = guiLeft + areaCX - font.width(line) / 2;
                guiGraphics.drawString(font, line, cx, startY + i * (font.lineHeight + 1), 0xFF5555, true);
            }
        } else if (!isLocked() || body == null || searchState != SearchState.DONE) {
            /// 未锁定时在按钮区域居中显示"需要先锁定"文本
            Component needLock = Component.translatable("screen.anvilcraft.cfa.need_lock");
            int areaCX = (RF_BTN_X[0] + RF_BTN_X[1] + RF_BTN_W) / 2; /// 中心X = 291
            int areaCY = (RF_BTN_Y[0] + RF_BTN_Y[2] + RF_BTN_H) / 2; /// 中心Y = 74
            int cx = guiLeft + areaCX - font.width(needLock) / 2;
            int cy = guiTop + areaCY - font.lineHeight / 2;
            guiGraphics.drawString(font, needLock, cx, cy, 0x888888, true);
        }
    }

    /// 渲染重构选项网格的滚动条。
    private void renderRefactorScrollbar(GuiGraphics guiGraphics, int guiLeft, int guiTop, int maxScroll) {
        if (maxScroll < 1) return;
        int scrollX = guiLeft + RF_SCROLL_X;
        int maxY = RF_SCROLL_Y + RF_SCROLL_H - RF_SCROLL_THUMB_H;
        int scrollY = RF_SCROLL_Y + (rfScrollRow * (RF_SCROLL_H - RF_SCROLL_THUMB_H) / maxScroll);
        scrollY = Mth.clamp(scrollY, RF_SCROLL_Y, maxY);
        guiGraphics.blit(SharedTextures.SWITCH_TABLE_SLIDER, scrollX, guiTop + scrollY, 0, 0, RF_SCROLL_W, RF_SCROLL_THUMB_H, 8, 12);
    }

    /// 在按钮内渲染巨构的BakedModel。
    /// 缩放值除以星环的相对几何尺寸，使所有环显示为相同大小。
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

        /// 重构标题（在标题区域居中）
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
        /// 检查恒星天体是否缺少增幅器
        CelestialBodyData body = getMenu().getBlockEntity().getCelestialBodyData();
        boolean missingAmplifier = body instanceof StarData && !getMenu().getBlockEntity().isAmplifierPresent();
        if (missingAmplifier) {
            Component line1 = Component.translatable("screen.anvilcraft.cfa.missing_amplifier.line1");
            Component line2 = Component.translatable("screen.anvilcraft.cfa.missing_amplifier.line2");
            Component line3 = Component.translatable("screen.anvilcraft.cfa.missing_amplifier.line3");
            int cx1 = PV_X + (PV_W - font.width(line1)) / 2;
            int cx2 = PV_X + (PV_W - font.width(line2)) / 2;
            int cx3 = PV_X + (PV_W - font.width(line3)) / 2;
            int cy = PV_Y + PV_H / 2 - font.lineHeight * 3 / 2;
            guiGraphics.drawString(font, line1, cx1, cy, 0xFF5555, true);
            guiGraphics.drawString(font, line2, cx2, cy + font.lineHeight + 1, 0xFF5555, true);
            guiGraphics.drawString(font, line3, cx3, cy + (font.lineHeight + 1) * 2, 0xFF5555, true);
            return;
        }
        /// 砧子数量变化且未锁定且未搜索时，显示星图指南。
        /// 指南保持可见直到新搜索开始（guideTriggered被重置）。
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
            /// IDLE与default相同，不显示内容
            default -> {
            }
        }
    }

    /// 渲染带4条彩色指示线的星图指南。
    /// 每条线代表一种砧子类型，根据其数量（1-64）移动。
    /// 当对应数量为0时，线条和数量标签隐藏。
    /// 所有内容使用姿态缩放以50%比例渲染。
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

        /// 以160x160渲染星图图像（屏幕上缩放至80x80）
        guiGraphics.blit(TEX_CELESTIAL_MAPS, 0, 0, 0, 0, MAP_SIZE, MAP_SIZE, MAP_SIZE, MAP_SIZE);

        int timeCount = getMenu().getBlockEntity().getAnvilCount(0);
        int spaceCount = getMenu().getBlockEntity().getAnvilCount(1);
        int massCount = getMenu().getBlockEntity().getAnvilCount(2);
        int energyCount = getMenu().getBlockEntity().getAnvilCount(3);

        /// 时间砧子：浅绿色，垂直线，全高（160px），x=12-76（从左起索引1）
        if (timeCount > 0) {
            int x = 11 + Math.round((timeCount - 1) * 64.0f / 63.0f);
            guiGraphics.fill(x, 0, x + 2, MAP_SIZE, COLOR_TIME); /// 2px → 0.5x缩放后1px
            String text = String.valueOf(timeCount);
            int textX = x + 1 - font.width(text) / 2;
            int textY = -font.lineHeight - 4;
            guiGraphics.drawString(font, text, textX, textY, COLOR_TIME, false);
        }

        /// 空间砧子：青色，水平线，全宽（160px），y=92-156（从底部起索引1）
        if (spaceCount > 0) {
            int y = 160 - Math.round(92 + (spaceCount - 1) * 64.0f / 63.0f) - 2; /// -2缩放像素 = -1屏幕像素
            guiGraphics.fill(0, y, MAP_SIZE, y + 2, COLOR_SPACE); /// 2px → 0.5x缩放后1px
            String text = String.valueOf(spaceCount);
            int textX = -font.width(text) - 6;
            int textY = y + 1 - font.lineHeight / 2;
            guiGraphics.drawString(font, text, textX, textY, COLOR_SPACE, false);
        }

        /// 质量砧子：浅黄色，垂直线，上半部分（80px），x=92-156（从左起索引1）
        if (massCount > 0) {
            int x = 91 + Math.round((massCount - 1) * 64.0f / 63.0f);
            guiGraphics.fill(x, 0, x + 2, MAP_SIZE / 2, COLOR_MASS); /// 上半部分，2px→1px
            String text = String.valueOf(massCount);
            int textX = x + 1 - font.width(text) / 2;
            int textY = -font.lineHeight - 4;
            guiGraphics.drawString(font, text, textX, textY, COLOR_MASS, false);
        }

        /// 能量砧子：浅红色，水平线，左半部分（80px），y=12-76（从底部起索引1）
        if (energyCount > 0) {
            int y = 160 - Math.round(12 + (energyCount - 1) * 64.0f / 63.0f) - 2; /// -2缩放像素 = -1屏幕像素
            guiGraphics.fill(0, y, MAP_SIZE / 2, y + 2, COLOR_ENERGY); /// 左半部分，2px→1px
            String text = String.valueOf(energyCount);
            int textX = -font.width(text) - 6;
            int textY = y + 1 - font.lineHeight / 2;
            guiGraphics.drawString(font, text, textX, textY, COLOR_ENERGY, false);
        }

        /// 地图右侧的三步指南文本
        renderGuideStepText(guiGraphics, timeCount, spaceCount, massCount, energyCount);

        pose.popPose();
    }

    /// 在地图右下角渲染三步天体类型指南文本。
    /// 在0.5倍缩放的姿态内渲染。坐标位于160×160图像空间内。
    private void renderGuideStepText(
        GuiGraphics guiGraphics, int time, int space, int mass, int energy
    ) {
        int textX = 88; /// 地图右下角空白区域
        int lineSpacing = font.lineHeight + 5; /// 缩放后的单位
        int y0 = 108;

        /// 步骤1：↑ 从质量-半径图表推导的类型（质量 + 空间）
        int step1Rgb = CelestialBodyMatcher.getMassRadiusRgb(mass, space);
        String step1Name = getTypeDisplayName(step1Rgb);
        drawGuideLine(guiGraphics, "↑" + step1Name, textX, y0, 0xFF_CCCCCC);

        /// 步骤2：← 从年龄-温度图表推导的类型（时间 + 能量）
        /// 棕矮星使用 age_temp_sp；其他使用 age_temp
        CelestialBodyClass step1Class = CelestialBodyClass.fromRgb(step1Rgb);
        int step2Rgb;
        if (step1Class != null && step1Class.step2UsesSp()) {
            step2Rgb = CelestialBodyMatcher.getAgeTempSpRgb(time, energy);
        } else {
            step2Rgb = CelestialBodyMatcher.getAgeTempRgb(time, energy);
        }
        String step2Name = getTypeDisplayName(step2Rgb);
        drawGuideLine(guiGraphics, "←" + step2Name, textX, y0 + lineSpacing * 2, 0xFF_CCCCCC);

        /// 步骤3：↖ 从年龄-半径图表推导的类型（时间 + 空间）
        int step3Rgb = CelestialBodyMatcher.getAgeRadiusRgb(time, space);
        String step3Name = getTypeDisplayName(step3Rgb);
        drawGuideLine(guiGraphics, "↖" + step3Name, textX, y0 + lineSpacing, 0xFF_CCCCCC);
    }

    /// 通过翻译键将图表RGB颜色转换为显示名称。
    /// 使用现有的 screen.anvilcraft.cfa.class.<name> 键模式。
    /// 岩石行星子类型全部映射到 rocky_planet。
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
        /// 复杂自定义模型（粉碎、空洞、血肉、智能、错误天体）
        if (body instanceof SpecialCelestialBodyData s && s.needsCustomModel()) {
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

        /// 恒星：模型加载 + 颜色叠加 + 光晕（120%大小）
        /// 中子星和黑洞使用专用模型，无颜色叠加和光晕
        if (body instanceof StarData star) {
            if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR
                || star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
                renderRemnantModelPreview(guiGraphics, star);
                /// 渲染中子星射流，产生灯塔效应
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

        /// 行星主体（cutout渲染类型）
        var rt = ModRenderTypes.STAR_CUTOUT.apply(tex);
        VertexConsumer vc = buf.getBuffer(rt);
        CelestialBodyRenderer.renderPlanetBody(guiGraphics.pose(), vc, 0x00F000F0, 0);

        /// 大气层（半透明）
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

        /// 渲染星环（半透明）
        ResourceLocation ringTex = CelestialBodyTextureBakery.getOrBakeRing(body);
        if (ringTex != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.5, 0.5, 0.5);
            guiGraphics.pose().scale(1.2f, 1.2f, 1.2f);
            guiGraphics.pose().translate(-0.5, -0.5, -0.5);
            var ringConsumer = buf.getBuffer(ModRenderTypes.CELESTIAL_RING.apply(ringTex));
            CelestialBodyRenderer.renderRing(guiGraphics.pose(), ringConsumer, LightTexture.FULL_BRIGHT, 0);
            guiGraphics.pose().popPose();
        }

        /// 一次性提交所有渲染层 —— 行星球体在前，半透明大气层和星环在后
        buf.endBatch();

        guiGraphics.pose().popPose();
    }

    /// 在UI中渲染恒星：动画基础模型 + 颜色叠加 + 光晕。
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
        /// 动画灰度恒星模型
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

        /// 颜色叠加 —— 乘法混合
        float[] rgb = CelestialBodyTextureBakery.starColor(star);
        long seed = getMenu().getBlockEntity().getBlockPos().asLong();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.5, 0.5, 0.5);
        guiGraphics.pose().scale(1.005f, 1.005f, 1.005f);
        guiGraphics.pose().translate(-0.5, -0.5, -0.5);
        renderStarColorOverlay(guiGraphics, rgb[0], rgb[1], rgb[2]);
        guiGraphics.pose().popPose();

        /// 光晕
        CelestialBodyRenderer.renderStarHalo(guiGraphics.pose(), buf, star, LightTexture.FULL_BRIGHT, 0, seed);
        buf.endBatch();
    }

    /// 渲染恒星预览的乘法混合颜色叠加层。
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

    /// 通过方块模型渲染复杂模型天体（粉碎、空洞、错误天体等）。
    private void renderComplexModelPreview(GuiGraphics guiGraphics, SpecialCelestialBodyData special) {
        if (minecraft == null) return;
        var modelLoc = special.getModelLocation();
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

        /// 为拥有大气的复杂模型天体渲染大气层
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

    /// 在预览区域渲染中子星或黑洞模型。
    /// 姿态已由调用方设置 —— 仅渲染模型，无颜色叠加和光晕。
    private void renderRemnantModelPreview(GuiGraphics guiGraphics, StarData star) {
        if (minecraft == null) return;
        BakedModel model = minecraft.getModelManager().getModel(getUiStarModel(star));
        if (model == minecraft.getModelManager().getMissingModel()) return;

        /// 黑洞事件视界很小但吸积盘很大 —— 需要放大
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

    /// 在预览窗口中渲染中子星相对论射流。
    /// 射流以1.5倍天体速度旋转，沿磁轴倾斜以产生经典的脉冲星灯塔效应。
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

        /// 显示加速器演化进度而非正常天体信息
        if (be.isAcceleratorActive()) {
            renderAcceleratorProgress(guiGraphics, be);
            return;
        }

        float offsetAge = be.getDisplayOffset(0);
        float offsetRadius = be.getDisplayOffset(1);
        float offsetMass = be.getDisplayOffset(2);
        List<Component> lines = buildInfoLines(body, be.getAgeAnvilCount(), be.getStellarMass(), offsetAge, offsetRadius, offsetMass);
        /// 将 "标签: 值" 拆分为 "标签:" 和 "值" 分别显示在两行
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

        /// 信息面板右侧边缘的滚动条（轨道+滑块样式）
        if (maxScroll > 0) {
            int sbX = PV_INFO_X + PV_INFO_W - 3;
            int sbW = 2;
            guiGraphics.fill(sbX, PV_INFO_Y, sbX + sbW, PV_INFO_Y + PV_INFO_H, 0x40_FFFFFF);
            int thumbH = Math.max(12, PV_INFO_H * maxLines / displayLines.size());
            int thumbY = PV_INFO_Y + (PV_INFO_H - thumbH) * scrollOffset / maxScroll;
            guiGraphics.fill(sbX, thumbY, sbX + sbW, thumbY + thumbH, 0x80_CCCCCC);
        }
    }

    /// 在信息面板中渲染恒星演化加速器进度。
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
        /// 剩余时间（使用客户端本地倒计时，每tick递减）
        int displayTicks = localAcceleratorTicksRemaining > 0 ? localAcceleratorTicksRemaining : be.getAcceleratorTicksRemaining();
        int secondsRemaining = displayTicks / 20;
        lines.add(Component.translatable("screen.anvilcraft.cfa.evolution.time_remaining",
            Component.literal(formatDuration(secondsRemaining))));
        /// 进度条信息
        if (be.getAcceleratorTicksTotal() > 0) {
            int pct = (int) ((1.0f - (float) displayTicks / be.getAcceleratorTicksTotal()) * 100);
            lines.add(Component.literal(pct + "%"));
        }
        /// 无限能量指示器 —— 仅在戴森球提供无限能量时显示
        if (be.isInfinitePower()) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.evolution.infinite_power"));
        }

        int lineHeight = font.lineHeight + 1;
        int y = PV_INFO_Y + 10;
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
    /// 资源类别颜色
    private static final int COLOR_MINERAL = 0xFFFFFF;
    private static final int COLOR_FLUID = 0x55AAFF;
    private static final int COLOR_BIOLOGICAL = 0x55FF55;
    private static final int COLOR_BIOLOGICAL_FLUID = 0xFF88CC;
    private static final int COLOR_GIANT_ITEM = 0x55FFFF;
    private static final int COLOR_GIANT_FLUID = 0x5555FF;
    private static final int COLOR_OFFERING = 0xFFAA00;
    private static final int COLOR_WASTELAND = 0xAA5500;

    private record ColoredEntry(String text, int color) {}

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void renderResourceBar(GuiGraphics guiGraphics) {
        var be = getMenu().getBlockEntity();
        var resources = be.getPlanetaryResourceSet();
        if (resources == null || resources.isEmpty()) return;

        List<ColoredEntry> entries = new ArrayList<>();
        collectItemEntries(entries, resources.getMinerals(), COLOR_MINERAL);
        collectFluidEntries(entries, resources.getFluids(), COLOR_FLUID);
        collectItemEntries(entries, resources.getGiantItems(), COLOR_GIANT_ITEM);
        collectFluidEntries(entries, resources.getGiantFluids(), COLOR_GIANT_FLUID);
        collectItemEntries(entries, resources.getBiologicalItems(), COLOR_BIOLOGICAL);
        collectFluidEntries(entries, resources.getBiologicalFluids(), COLOR_BIOLOGICAL_FLUID);
        collectItemEntries(entries, resources.getOfferings(), COLOR_OFFERING);
        collectItemEntries(entries, resources.getWastelandItems(), COLOR_WASTELAND);
        if (entries.isEmpty()) return;

        /// 计算总文本宽度以确定滚动范围
        int spacing = 10;
        int totalW = -spacing;
        for (ColoredEntry e : entries) totalW += font.width(e.text()) + spacing;
        int contentX = PV_X + 4;
        int contentW = PV_W - 8;
        int maxScroll = Math.max(0, totalW - contentW);
        resourceScrollOffset = Mth.clamp(resourceScrollOffset, 0, maxScroll);

        /// 裁剪区域（绝对屏幕坐标）
        guiGraphics.enableScissor(leftPos + PV_X, topPos + PV_RES_Y, leftPos + PV_X + PV_W, topPos + PV_RES_Y + PV_RES_H);

        /// 标题 —— 居中，样式与条目一致
        Component title = Component.translatable("screen.anvilcraft.cfa.resource_title");
        guiGraphics.drawString(font, title, PV_X + (PV_W - font.width(title)) / 2, PV_RES_Y, 0xFF_AAAAAA, false);

        /// 资源条目（GUI相对坐标）
        int x = contentX - resourceScrollOffset;
        int y = PV_RES_Y + font.lineHeight + 1;
        for (ColoredEntry entry : entries) {
            int w = font.width(entry.text());
            if (x + w >= PV_X && x <= PV_X + PV_W) {
                guiGraphics.drawString(font, entry.text(), x, y, entry.color(), true);
            }
            x += w + spacing;
        }

        guiGraphics.disableScissor();

        /// 水平滚动条
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

    private void collectItemEntries(List<ColoredEntry> out, List<PlanetaryResourceSet.WeightedItemStack> items, int color) {
        int totalW = items.stream().mapToInt(PlanetaryResourceSet.WeightedItemStack::weight).sum();
        for (var entry : items) {
            var it = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(entry.itemId());
            String name = it.getDescription().getString();
            int pct = totalW > 0 ? entry.weight() * 100 / totalW : 0;
            out.add(new ColoredEntry(name + " " + pct + "%", color));
        }
    }

    private void collectFluidEntries(List<ColoredEntry> out, List<PlanetaryResourceSet.WeightedFluidStack> fluids, int color) {
        int totalW = fluids.stream().mapToInt(PlanetaryResourceSet.WeightedFluidStack::weight).sum();
        for (var entry : fluids) {
            String name;
            if (net.minecraft.core.registries.BuiltInRegistries.FLUID.containsKey(entry.fluidId())) {
                var f = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(entry.fluidId());
                name = f.getFluidType().getDescription().getString();
            } else {
                /// 降级方案：牛奶/蜂蜜等作为物品注册，而非流体类型
                var it = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(entry.fluidId());
                name = it.getDescription().getString();
            }
            int pct = totalW > 0 ? entry.weight() * 100 / totalW : 0;
            out.add(new ColoredEntry(name + " " + pct + "%", color));
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
        boolean isError = body instanceof SpecialCelestialBodyData special && special.isErrorPlanet();
        /// 类型名称
        String typeKey;
        if (body instanceof RockyPlanetData rp) {
            typeKey = rockyTypeKey(rp);
        } else if (body instanceof SpecialCelestialBodyData s) {
            typeKey = "screen.anvilcraft.cfa.class.special." + s.name();
        } else {
            typeKey = "screen.anvilcraft.cfa.class." + body.bodyClass().name().toLowerCase();
        }
        lines.add(Component.translatable("screen.anvilcraft.cfa.type", Component.translatable(typeKey)));
        /// 年龄（错误星球显示 "???"）
        if (isError) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.age", Component.literal("???")));
        } else {
            lines.add(Component.translatable(
                "screen.anvilcraft.cfa.age",
                CelestialForgingAnvilMenu.formatAgeOffset(ageAnvilCount, offsetAge)
            ));
        }
        /// 半径（错误星球显示 "???"）
        if (isError) {
            lines.add(Component.translatable("screen.anvilcraft.cfa.radius", Component.literal("???")));
        } else {
            lines.add(Component.translatable(
                "screen.anvilcraft.cfa.radius",
                CelestialForgingAnvilMenu.formatRadiusOffset(body.size(), offsetRadius)
            ));
        }
        /// 质量（错误星球显示 "???"）
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
                /// 仅当轴倾角非零时显示
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

    /// 根据温度、液体覆盖和大气计算岩石行星的类型显示键，
    /// 而非仅依据天体类别。
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
        /// 寒冷、温和、炎热三类
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

    /// 格式化为3位有效数字。
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
        /// 当星图指南可见时，在其上方重新渲染预览按钮
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
                /// 渲染数量（匹配 IFilterScreen.renderSlotLimit 风格）
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
        /// 抑制CFA专有槽位（砧子槽+种子槽）的原版物品提示框。
        /// 玩家背包槽位仍然显示标准物品提示框。
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
        /// 已建造巨构按钮的提示框（格式与重构选项相同：名称 + 材料 + 描述）
        boolean hasMegastructureTip = getMenu().getBlockEntity().getActiveMegastructureIndex() >= 0;
        if (hasMegastructureTip && isLocked() && searchState == SearchState.DONE) {
            if (isHoveringBuiltMegastructureButton(relX, relY)) {
                CelestialRefactorOption activeOption = getMenu().getBlockEntity().getActiveMegastructureOption();
                if (activeOption != null) {
                    Component name = Component.translatable(activeOption.displayName());
                    List<Component> tooltipLines = new ArrayList<>();
                    tooltipLines.add(name);
                    if (activeOption.needsMaterial()) {
                        tooltipLines.add(Component.translatable(
                            "screen.anvilcraft.cfa.material_required",
                            activeOption.material().getDisplayName(),
                            Component.literal(String.valueOf(activeOption.materialCount()))
                        ));
                    }
                    if (hasShiftDown()) {
                        tooltipLines.add(Component.translatable(activeOption.displayName() + ".description")
                            .withStyle(ChatFormatting.DARK_GRAY));
                    } else {
                        tooltipLines.add(Component.translatable(
                            "tooltip.anvilcraft.press_key",
                            Component.literal("Shift").withStyle(ChatFormatting.DARK_GRAY)
                        ).withStyle(ChatFormatting.DARK_GRAY));
                    }
                    guiGraphics.renderTooltip(font, tooltipLines, java.util.Optional.empty(), x, y);
                }
            }
        }
        /// 重构选项提示框
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
                if (hasShiftDown()) {
                    tooltipLines.add(Component.translatable(option.displayName() + ".description").withStyle(ChatFormatting.DARK_GRAY));
                } else {
                    tooltipLines.add(Component.translatable(
                        "tooltip.anvilcraft.press_key",
                        Component.literal("Shift").withStyle(ChatFormatting.DARK_GRAY)
                    ).withStyle(ChatFormatting.DARK_GRAY));
                }
                guiGraphics.renderTooltip(font, tooltipLines, java.util.Optional.empty(), x, y);
            }
        }
        /// 材料槽提示框
        if (isLocked() && searchState == SearchState.DONE && this.hoveredSlot instanceof CelestialForgingAnvilMenu.CFAMaterialSlot) {
            guiGraphics.renderTooltip(font, Component.translatable("screen.anvilcraft.cfa.refactor_materials"), x, y);
        }
        /// 开始重构按钮提示框
        if (isLocked() && searchState == SearchState.DONE && isOverRefactorStart(relX, relY)) {
            guiGraphics.renderTooltip(font, Component.translatable("screen.anvilcraft.cfa.refactor_start_tooltip"), x, y);
        }
        /// 种子槽提示框
        if (this.hoveredSlot instanceof CelestialForgingAnvilMenu.SeedSlot) {
            List<Component> seedTooltip = new ArrayList<>();
            seedTooltip.add(Component.translatable("screen.anvilcraft.cfa.seed_slot.title"));
            if (hasShiftDown()) {
                seedTooltip.add(Component.translatable("screen.anvilcraft.cfa.seed_slot.description").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                seedTooltip.add(Component.translatable(
                    "tooltip.anvilcraft.press_key",
                    Component.literal("[Shift]").withStyle(ChatFormatting.DARK_GRAY)
                ).withStyle(ChatFormatting.DARK_GRAY));
            }
            guiGraphics.renderTooltip(font, seedTooltip, java.util.Optional.empty(), x, y);
        }
        /// 砧子槽范围提示框
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
            /// 发送锁定切换到服务端（按钮ID 200）
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, 200);
            }
            /// 同时在本地切换以获得即时UI反馈
            setLocked(!isLocked());
            return true;
        }
        /// 重构选项点击
        int optIdx = getRefactorOptionAt(relX, relY);
        if (optIdx >= 0) {
            selectedRefactorIndex = optIdx;
            /// 告诉服务端为此选项配置材料槽
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, 9 + optIdx);
            }
            return true;
        }
        /// 开始重构按钮点击
        if (isOverRefactorStart(relX, relY)) {
            handleRefactorStart();
            return true;
        }
        /// 重构滚动条拖动开始
        if (isMouseInRefactorScrollbar(relX, relY)) {
            if (getRefactorMaxScroll() > 0) {
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
        /// 信息区域：滚动文本
        if (relX >= PV_INFO_X && relX < PV_INFO_X + PV_INFO_W && relY >= PV_INFO_Y && relY < PV_INFO_Y + PV_INFO_H) {
            scrollOffset -= (int) scrollY;
            return true;
        }
        /// 资源条：水平像素滚动
        if (relX >= PV_X && relX < PV_X + PV_W && relY >= PV_RES_Y && relY < PV_RES_Y + PV_RES_H) {
            resourceScrollOffset -= (int) scrollY * 30;
            return true;
        }
        /// 重构选项区域：滚动网格（包含巨构建造后的文本）
        if (isMouseInRefactorArea(relX, relY) || isMouseInRefactorScrollbar(relX, relY)) {
            int maxScroll = getRefactorMaxScroll();
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
            int maxScroll = getRefactorMaxScroll();
            if (maxScroll > 0) {
                float scroll = (relY - RF_SCROLL_Y - RF_SCROLL_THUMB_H / 2f) / (RF_SCROLL_H - RF_SCROLL_THUMB_H);
                rfScrollRow = Mth.clamp((int) (scroll * maxScroll + 0.5f), 0, maxScroll);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /// # 搜索

    private void performSearch() {
        if (searchState == SearchState.LOADING) return; /// 已在搜索中
        var be = getMenu().getBlockEntity();
        /// 检查是否有种子物品 —— 有种子物品时跳过图表预检以发现特殊天体
        boolean hasSeedItem = !be.getAnvilInventory().getItem(4).isEmpty();
        /// 客户端预检：匹配不可能时立即失败（有种子物品时跳过）
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
        /// 记住当前天体以检测新结果
        preSearchBody = be.getCelestialBodyData();
        /// 向服务端发送按钮点击
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, 0);
        }
        searchState = SearchState.LOADING;
    }

    /// # 碰撞检测

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
        lockedMsgTick = 60; /// 显示3秒
    }

    private void showUnlockWarning() {
        unlockWarningTick = 60; /// 显示3秒
    }

    /// # 重构界面辅助

    /// 获取给定相对鼠标位置下的重构选项索引。
    /// 如果没有命中任何选项按钮，返回 -1。
    private int getRefactorOptionAt(int rx, int ry) {
        if (refactorOptions.isEmpty()) return -1;
        boolean hasMegastructure = getMenu().getBlockEntity().getActiveMegastructureIndex() >= 0;
        for (int visibleRow = 0; visibleRow < RF_ROWS_VISIBLE; visibleRow++) {
            for (int col = 0; col < RF_COLS; col++) {
                int contentRow = rfScrollRow + visibleRow;
                int optIdx;
                if (hasMegastructure) {
                    /// 第0行第0列为已建造巨构按钮（不可点击）
                    if (contentRow == 0 && col == 0) continue;
                    optIdx = contentRow * RF_COLS + col - 1;
                } else {
                    optIdx = contentRow * RF_COLS + col;
                }
                if (optIdx < 0 || optIdx >= refactorOptions.size()) continue;
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

    /// 计算重构选项网格的最大滚动值。
    /// 普通情况下使用基于行的滚动，巨构建造后使用平面滚动。
    /// 获取上次渲染过程中计算的最大滚动值，该值在巨构建造后
    /// 会同时计入按钮行和文本行。
    private int getRefactorMaxScroll() {
        return refactorMaxScroll;
    }

    private boolean isHoveringBuiltMegastructureButton(int rx, int ry) {
        if (rfScrollRow != 0) return false; /// 已建造按钮固定在第 0 行，滚动后不显示
        return rx >= RF_BTN_X[0] && rx < RF_BTN_X[0] + RF_BTN_W
            && ry >= RF_BTN_Y[0] && ry < RF_BTN_Y[0] + RF_BTN_H;
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
        if (option.needsMaterial()) {
            /// 检查材料槽（槽位索引 = 砧子槽数量 = 5，即容器中的第5个槽位）
            /// 材料槽是CFA的 materialContainer，在菜单中映射
            ItemStack inSlot = be.getMaterialContainer().getItem(0);
            ItemStack required = option.material().copyWithCount(option.materialCount());
            if (!ItemStack.isSameItemSameComponents(inSlot, required) || inSlot.getCount() < required.getCount()) {
                showRefactorError(Component.translatable("screen.anvilcraft.cfa.insufficient_materials"));
                return;
            }
        }
        /// 通过按钮点击向服务端发送建造请求，编码选项索引
        if (minecraft != null && minecraft.gameMode != null) {
            /// 使用按钮ID 100 + optionIndex 发送建造请求
            minecraft.gameMode.handleInventoryButtonClick(getMenu().containerId, 100 + selectedRefactorIndex);
        }
    }

    private void showRefactorError(Component msg) {
        refactorErrorMsg = msg;
        refactorErrorTick = 60;
    }
}
