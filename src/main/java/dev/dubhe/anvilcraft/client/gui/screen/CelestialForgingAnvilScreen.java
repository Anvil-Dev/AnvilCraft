package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyMatcher;
import dev.dubhe.anvilcraft.block.entity.celestial.GiantPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.RockyPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.CelestialBodyRenderer;
import dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.CelestialBodyTextureBakery;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.CelestialForgingAnvilMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CelestialForgingAnvilScreen extends AbstractContainerScreen<CelestialForgingAnvilMenu> {
    private static final ResourceLocation BACKGROUND =
        SharedTextures.bg("machine", "celestial_forging_anvil");

    private static final int TEX_WIDTH = 512;
    private static final int TEX_HEIGHT = 256;

    // Preview area (0-indexed)
    private static final int PV_X = 98;
    private static final int PV_Y = 15;
    private static final int PV_W = 148;
    private static final int PV_H = 99;
    private static final int PV_BODY_W = 59;
    private static final int PV_BODY_H = 69;
    private static final int PV_INFO_X = 157;
    private static final int PV_INFO_Y = 15;
    private static final int PV_INFO_W = 89;
    private static final int PV_INFO_H = 79;
    private static final int PV_BOT_Y = 84;

    // Search button (sprite: 48x32 = top half normal, bottom half hover)
    private static final int SB_X = 32;
    private static final int SB_Y = 121;
    private static final int SB_W = 48;
    private static final int SB_H = 16;

    private static final String BTN_DIR = "machine/celestial_forging_anvil/";
    private static final ResourceLocation TEX_SEARCH = SharedTextures.textureGui(BTN_DIR + "search");
    private static final ResourceLocation TEX_RESEARCH = SharedTextures.textureGui(BTN_DIR + "re_search");
    private static final ResourceLocation TEX_PREV = SharedTextures.textureGui(BTN_DIR + "previous");
    private static final ResourceLocation TEX_NEXT = SharedTextures.textureGui(BTN_DIR + "next");
    private static final ResourceLocation TEX_UNLOCKED = SharedTextures.textureGui(BTN_DIR + "unlocked");
    private static final ResourceLocation TEX_LOCKED = SharedTextures.textureGui(BTN_DIR + "locked");

    private static final ItemStack[] GHOST_STACKS = {
        new ItemStack(ModBlocks.CONFINED_TIME_ANVILON.asItem()),
        new ItemStack(ModBlocks.CONFINED_SPACE_ANVILON.asItem()),
        new ItemStack(ModBlocks.CONFINED_MASS_ANVILON.asItem()),
        new ItemStack(ModBlocks.CONFINED_ENERGY_ANVILON.asItem())
    };

    // Search state
    private enum SearchState {
        IDLE,
        LOADING,
        DONE,
        FAIL,
        POWER_FAIL
    }

    private SearchState searchState = SearchState.IDLE;
    @Nullable
    private CelestialBodyData preSearchBody = null;

    // History (persisted in BlockEntity)
    private int historyIndex = -1;
    // Saves current body before browsing history, so it can be restored
    @Nullable
    private CelestialBodyData savedCurrentBody = null;

    // Lock state is persisted in BlockEntity
    private boolean isLocked() {
        return getMenu().getBlockEntity().isLocked();
    }

    private void setLocked(boolean v) {
        getMenu().getBlockEntity().setLocked(v);
    }

    private List<CelestialBodyData> searchHistory() {
        return getMenu().getBlockEntity().getSearchHistory();
    }

    // Rotation animation
    private int previewRotTick = 0;

    // Info scroll
    private int scrollOffset = 0;

    public CelestialForgingAnvilScreen(
        CelestialForgingAnvilMenu menu, Inventory playerInventory, Component title
    ) {
        super(menu, playerInventory, title);
        this.imageWidth = 344;
        this.imageHeight = 207;
    }

    @Override
    protected void init() {
        super.init();
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
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        previewRotTick++;
        if (lockedMsgTick > 0) lockedMsgTick--;
        if (searchState == SearchState.LOADING) {
            var be = getMenu().getBlockEntity();
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
        ResourceLocation btnTex = (searchState == SearchState.DONE && !searchHistory().isEmpty())
                                  ? TEX_RESEARCH : TEX_SEARCH;
        boolean hoverSearch = relX >= SB_X && relX < SB_X + SB_W && relY >= SB_Y && relY < SB_Y + SB_H;
        renderButton(guiGraphics, btnTex, i + SB_X, j + SB_Y, SB_W, SB_H, hoverSearch);

        // Preview area bottom buttons
        if (searchState == SearchState.DONE || searchState == SearchState.LOADING) {
            boolean hasPrev = hasPreviousEntry();
            boolean hasNext = hasNextEntry();
            // Previous button
            if (hasPrev) {
                boolean hover = isOverPrevButton(relX, relY);
                renderButton(guiGraphics, TEX_PREV, i + PV_X + 4, j + PV_BOT_Y + 14, 16, 16, hover);
            }
            // Next button
            if (hasNext) {
                boolean hover = isOverNextButton(relX, relY);
                renderButton(guiGraphics, TEX_NEXT, i + PV_X + PV_W - 20, j + PV_BOT_Y + 14, 16, 16, hover);
            }
            // Lock button
            boolean hoverLock = isOverLockButton(relX, relY);
            ResourceLocation lockTex = isLocked() ? TEX_LOCKED : TEX_UNLOCKED;
            renderButton(guiGraphics, lockTex, i + PV_X + PV_W / 2 - 8, j + PV_BOT_Y + 14, 16, 16, hoverLock);
        }
    }

    /**
     * Render a sprite-sheet button: top half = normal, bottom half = hover.
     */
    private void renderButton(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, boolean hovered) {
        RenderSystem.enableDepthTest();
        int v = hovered ? h : 0;
        g.blit(tex, x, y, 0, v, w, h, w, h * 2);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        Component paramsLabel = Component.translatable("screen.anvilcraft.cfa.celestial_params");
        int paramsX = 7 + (72 - this.font.width(paramsLabel)) / 2;
        guiGraphics.drawString(this.font, paramsLabel, paramsX, 18, 0x404040, false);

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
        boolean missingAmplifier = body instanceof StarData
                                   && !getMenu().getBlockEntity().isAmplifierPresent();
        if (missingAmplifier) {
            Component warn = Component.translatable("screen.anvilcraft.cfa.missing_amplifier");
            int cx = PV_X + (PV_W - font.width(warn)) / 2;
            int cy = PV_Y + PV_H / 2 - font.lineHeight / 2;
            guiGraphics.drawString(font, warn, cx, cy, 0xFF5555, true);
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
                CelestialBodyData cur = getCurrentBody();
                if (cur != null) {
                    renderBodyPreview(guiGraphics, cur);
                    renderBodyInfo(guiGraphics, cur);
                }
            }
            // IDLE 与 default 相同
            default -> {
            }
        }
    }

    private void renderBodyPreview(GuiGraphics guiGraphics, CelestialBodyData body) {
        ResourceLocation tex = CelestialBodyTextureBakery.getOrBakeBody(body);
        if (tex == null) return;
        int size = Math.min(PV_BODY_W, PV_BODY_H) - 16;
        float scale = size / 2f;
        int cx = PV_X + PV_BODY_W / 2;
        int cy = PV_Y + PV_BODY_H / 2;
        float rotY = (previewRotTick * 3f) * (float) Math.PI / 180f;

        guiGraphics.pose().pushPose();
        // Position in preview center, flip Y for GUI coords
        guiGraphics.pose().translate(cx, cy, 100);
        guiGraphics.pose().scale(scale, -scale, scale);
        // Same rotations as in-world renderer
        guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(body.axialTilt()));
        guiGraphics.pose().mulPose(Axis.YP.rotation(rotY));
        guiGraphics.pose().translate(-0.5, -0.5, -0.5);

        var buf = guiGraphics.bufferSource();
        var rt = ModRenderTypes.STAR_CUTOUT.apply(tex);
        VertexConsumer vc = buf.getBuffer(rt);

        if (body instanceof StarData) {
            CelestialBodyRenderer.renderStarBody(guiGraphics.pose(), vc, 0x00F000F0, 0);
        } else {
            CelestialBodyRenderer.renderPlanetBody(guiGraphics.pose(), vc, 0x00F000F0, 0);
        }

        buf.endBatch();
        guiGraphics.pose().popPose();
    }

    private void renderBodyInfo(GuiGraphics guiGraphics, CelestialBodyData body) {
        List<Component> lines = buildInfoLines(body);
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

        guiGraphics.enableScissor(
            leftPos + PV_INFO_X, topPos + PV_INFO_Y,
            leftPos + PV_INFO_X + PV_INFO_W, topPos + PV_INFO_Y + PV_INFO_H
        );

        for (int i = scrollOffset; i < Math.min(displayLines.size(), scrollOffset + maxLines); i++) {
            guiGraphics.drawString(font, displayLines.get(i), PV_INFO_X, PV_INFO_Y + (i - scrollOffset) * lineHeight, 0xCCCCCC, false);
        }
        guiGraphics.disableScissor();

        // Thin scrollbar on right edge of info panel
        if (maxScroll > 0) {
            int sbX = PV_INFO_X + PV_INFO_W - 2;
            int sbH = Math.max(8, PV_INFO_H * maxLines / displayLines.size());
            int sbY = PV_INFO_Y + (PV_INFO_H - sbH) * scrollOffset / maxScroll;
            guiGraphics.fill(sbX, sbY, sbX + 2, sbY + sbH, 0x80CCCCCC);
        }
    }

    private List<Component> buildInfoLines(CelestialBodyData body) {
        List<Component> lines = new ArrayList<>();
        // Type name: "类型：XXX星"
        String typeKey = "screen.anvilcraft.cfa.class." + body.bodyClass().name().toLowerCase();
        lines.add(Component.translatable("screen.anvilcraft.cfa.type", Component.translatable(typeKey)));
        // Radius (from space anvil count = size)
        lines.add(Component.translatable("screen.anvilcraft.cfa.radius", CelestialForgingAnvilMenu.formatRadius(body.size())));
        switch (body) {
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
                    Component.translatable(
                        rp.hasAtmosphere()
                        ? "screen.anvilcraft.cfa.atmos.yes"
                        : "screen.anvilcraft.cfa.atmos.no"
                    )
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

    private Component magText(int level) {
        String key = switch (level) {
            case 0 -> "screen.anvilcraft.cfa.mag.none";
            case 1 -> "screen.anvilcraft.cfa.mag.very_weak";
            case 2 -> "screen.anvilcraft.cfa.mag.weak";
            case 3 -> "screen.anvilcraft.cfa.mag.medium";
            case 4 -> "screen.anvilcraft.cfa.mag.strong";
            default -> "screen.anvilcraft.cfa.mag.very_strong";
        };
        return Component.translatable("screen.anvilcraft.cfa.mag", Component.translatable(key));
    }

    private Component spinText(float speed) {
        String key;
        if (speed <= 0.2f) {
            key = "screen.anvilcraft.cfa.spin.very_slow";
        } else if (speed <= 0.6f) {
            key = "screen.anvilcraft.cfa.spin.slow";
        } else if (speed <= 1.2f) {
            key = "screen.anvilcraft.cfa.spin.medium";
        } else if (speed <= 2.0f) {
            key = "screen.anvilcraft.cfa.spin.fast";
        } else {
            key = "screen.anvilcraft.cfa.spin.very_fast";
        }
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
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (lockedMsgTick > 0) {
            Component msg = Component.translatable("screen.anvilcraft.cfa.locked_tooltip");
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
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
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
        if (isOverLockButton(relX, relY)
            && (searchState == SearchState.DONE || searchState == SearchState.LOADING)) {
            guiGraphics.renderTooltip(
                font,
                Component.translatable(isLocked() ? "screen.anvilcraft.cfa.unlock" : "screen.anvilcraft.cfa.lock"), x, y
            );
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
        if (isOverPrevButton(relX, relY) && hasPreviousEntry()) {
            if (isLocked()) {
                showLockedMessage();
                return true;
            }
            goPrevious();
            return true;
        }
        if (isOverNextButton(relX, relY) && hasNextEntry()) {
            if (isLocked()) {
                showLockedMessage();
                return true;
            }
            goNext();
            return true;
        }
        if (isOverLockButton(relX, relY)
            && (searchState == SearchState.DONE || searchState == SearchState.LOADING)) {
            setLocked(!isLocked());
            return true;
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
                this.minecraft.player.connection.send(
                    new ServerboundContainerButtonClickPacket(this.menu.containerId, buttonId)
                );
                return true;
            }
        }
        // Info area: scroll text
        if (relX >= PV_INFO_X && relX < PV_INFO_X + PV_INFO_W
            && relY >= PV_INFO_Y && relY < PV_INFO_Y + PV_INFO_H) {
            scrollOffset -= (int) scrollY;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // === Search ===

    private void performSearch() {
        var be = getMenu().getBlockEntity();
        // Client-side pre-check: immediate fail if match impossible
        if (minecraft != null && minecraft.level != null) {
            var preCheck = CelestialBodyMatcher.match(
                be.getAnvilCount(0), be.getAnvilCount(1), be.getAnvilCount(2), be.getAnvilCount(3),
                be.isAmplify(), minecraft.level.getRandom()
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

    private boolean hasPreviousEntry() {
        int sz = searchHistory().size();
        if (sz <= 1) return false;
        return historyIndex < sz - 1;
    }

    private boolean hasNextEntry() {
        return historyIndex > 0;
    }

    private void goPrevious() {
        if (historyIndex < 0) {
            // Save current body before navigating away
            savedCurrentBody = getMenu().getBlockEntity().getCelestialBodyData();
            historyIndex = 1; // skip index 0 (same as current celestialBodyData)
        } else {
            historyIndex++;
        }
        applyHistoryToWorld();
    }

    private void goNext() {
        historyIndex--;
        if (historyIndex == 0) historyIndex = -1; // back to latest
        if (historyIndex < 0) {
            // Restore original current body
            if (savedCurrentBody != null) {
                getMenu().getBlockEntity().setCelestialBodyData(savedCurrentBody);
                savedCurrentBody = null;
            }
        } else {
            applyHistoryToWorld();
        }
    }

    /**
     * Apply the current history entry to the in-world BlockEntity for rendering.
     */
    private void applyHistoryToWorld() {
        CelestialBodyData body = getCurrentBody();
        if (body != null) {
            getMenu().getBlockEntity().setCelestialBodyData(body);
        }
    }

    @Nullable
    private CelestialBodyData getCurrentBody() {
        if (historyIndex < 0) return getMenu().getBlockEntity().getCelestialBodyData();
        List<CelestialBodyData> hist = searchHistory();
        if (historyIndex < hist.size()) return hist.get(historyIndex);
        return null;
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
}
