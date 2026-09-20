package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.building.BlueprintMultiblocks;
import dev.dubhe.anvilcraft.building.BlueprintPlacement;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.client.building.BlueprintClientFiles;
import dev.dubhe.anvilcraft.client.gui.component.SimpleIconButton;
import dev.dubhe.anvilcraft.client.gui.component.StructureScannerButtonState;
import dev.dubhe.anvilcraft.client.gui.component.TextWidget;
import dev.dubhe.anvilcraft.client.init.ModShaders;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.client.support.DiskDisplaySupport;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import dev.dubhe.anvilcraft.network.StructureScannerActionPacket;
import dev.dubhe.anvilcraft.network.StructureScannerSavePacket;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.StructureSaveUtil;
import dev.dubhe.anvilcraft.util.WatchableCyclingValue;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;

public class StructureScannerScreen extends AbstractContainerScreen<StructureScannerMenu> implements IGhostIngredientScreen {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "structure_scanner");
    private static final ResourceLocation REDO_TEXTURE = scannerTexture("redo");
    private static final ResourceLocation REDO_HIGHLIGHT_TEXTURE = scannerTexture("redo_highlight");
    private static final int REDO_HIGHLIGHT_FRAMES = 8;
    private static final long REDO_HIGHLIGHT_FRAME_MILLIS = 100L;
    private static final ResourceLocation STOP_TEXTURE = scannerTexture("stop");
    private static final ResourceLocation CONFIRM_TEXTURE = scannerTexture("confirm");
    private static final ResourceLocation BLUEPRINT_TEXTURE = scannerTexture("blueprint");
    private final List<AbstractWidget> scanWidgets = new ArrayList<>();
    private final List<AbstractWidget> blueprintWidgets = new ArrayList<>();
    private EditBox importInput;
    private EditBox exportInput;
    private ScannerButton importButton;
    private ScannerButton exportButton;
    private boolean folderMode;
    private ScannerButton confirmButton;
    private ItemStack marker = ItemStack.EMPTY;
    private boolean autoRotate = true;
    private boolean blueprintVisible;
    private boolean importDropdown;
    private List<String> importFiles = List.of();
    private List<String> filteredFiles = List.of();
    private int fileOffset;
    private static final int FILE_ROWS = 6;
    private static final int FILE_ROW_HEIGHT = 12;

    private static ResourceLocation scannerTexture(String name) {
        return SharedTextures.textureGui("machine/structure_scanner/" + name);
    }

    // 预览窗口位置和尺寸
    private int previewWindowX;
    private int previewWindowY;
    private final int previewWindowWidth = 112;
    private final int previewWindowHeight = 88;

    // 预览旋转角度
    private float previewRotationY = 45.0f;
    private float previewRotationX = -30.0f;
    private static final float MIN_ROTATION_X = -60.0f;
    private static final float MAX_ROTATION_X = 0.0f;
    private static final float ROTATION_SENSITIVITY = 0.5f;

    // 鼠标拖拽状态
    private boolean isPreviewDragging = false;
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    // 模式切换按钮
    private ScannerButton modeToggleButton;
    @Nullable private ScannerButton pressedButton;
    private boolean isScanMode = true;  // 默认为 redo 状态
    private long redoHighlightStartedAt = -1L;
    private boolean redoHighlightFinished;

    // 文本输入框
    private EditBox nameInput;

    // 缓存数据
    private StructureScannerBlockEntity cachedBlockEntity;
    @Nullable private String blueprintError;
    @Nullable private Component statusTitle;
    private long statusTitleUntil;
    private boolean cachedHasDisk;
    private StructureScannerBlockEntity.InfoStatus cachedInfoStatus;
    private boolean cachedIsScanComplete;
    private boolean cachedHasStartedScanning;
    private int cachedRangeX = -1;
    private int cachedRangeY = -1;
    private int cachedRangeZ = -1;

    // 预览缓存
    private LevelLike cachedPreviewLevelLike;
    @Nullable private StructureScannerMenu.ImportedStructure cachedImportedStructure;
    @Nullable private LevelLike cachedImportedPreview;
    private AABB cachedPreviewBounds = new AABB(BlockPos.ZERO);
    private AABB cachedImportedPreviewBounds = new AABB(BlockPos.ZERO);
    private Direction cachedPreviewFacing = Direction.NORTH;

    // 扫描数据版本追踪（用于缓存失效）
    private int cachedScannedBlocksSize = -1;

    // 离屏帧缓冲 — 用于扫描预览后处理
    @Nullable
    private RenderTarget previewFbo;

    public StructureScannerScreen(StructureScannerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 201;
    }

    @Override
    protected void init() {
        this.cancelButtonPress();
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;

        // 初始化预览窗口位置（与智能放置器一致）
        this.previewWindowX = this.leftPos + 136;
        this.previewWindowY = this.topPos + 18;

        if (this.minecraft == null) return;

        this.scanWidgets.clear();
        this.blueprintWidgets.clear();

        var scanner = this.menu.getBlockEntity();
        if (scanner != null) {
            this.addRangeControls(scanner.getRangeX(), 68);
            this.addRangeControls(scanner.getRangeZ(), 82);
            this.addRangeControls(scanner.getRangeY(), 96);
        }

        // 添加模式切换按钮（redo/stop）
        this.modeToggleButton = new ScannerButton(
            this.leftPos + 222, this.topPos + 112, 26, 26, REDO_TEXTURE, 3, () -> false,
            button -> this.onModeToggleClick(), Component.translatable("screen.anvilcraft.structure_scanner.scan")
        );
        this.addRenderableWidget(this.modeToggleButton);

        this.confirmButton = new ScannerButton(
            this.leftPos + 44, this.topPos + 85, 68, CONFIRM_TEXTURE,
            button -> this.onConfirmClick(), Component.translatable("screen.anvilcraft.structure_scanner.confirm")
        );
        this.addBlueprintWidget(this.confirmButton);
        this.nameInput = this.createInput(11, 70, 117, "name", 32);
        this.blueprintWidgets.add(this.nameInput);
        this.addBlueprintWidget(new RotationButton(8, true));
        this.addBlueprintWidget(new RotationButton(26, false));
        this.importInput = this.createInput(7, 22, 102, "import_file", 128);
        this.importInput.setResponder(value -> {
            this.filterFiles();
            this.importDropdown = this.importInput.isFocused();
        });
        this.exportInput = this.createInput(7, 42, 102, "export_file", 128);
        this.folderMode = false;
        this.importButton = this.addRenderableWidget(new ScannerButton(
            this.leftPos + 115, this.topPos + 18, 16, scannerTexture("import"), 6, () -> this.folderMode,
            button -> {
                this.updateNameInputEditable();
                this.importDropdown = false;
                if (this.folderMode) {
                    BlueprintClientFiles.openDirectory();
                } else if (button.active) {
                    BlueprintClientFiles.requestImport(this.menu.containerId, this.importInput.getValue());
                }
            }, Component.translatable("screen.anvilcraft.structure_scanner.import")
        ));
        this.exportButton = this.addRenderableWidget(new ScannerButton(
            this.leftPos + 115, this.topPos + 38, 16, scannerTexture("export"), 6, () -> this.folderMode,
            button -> {
                this.updateNameInputEditable();
                if (this.folderMode) {
                    BlueprintClientFiles.openDirectory();
                } else if (button.active) {
                    BlueprintClientFiles.requestExport(this.menu.containerId, this.exportInput.getValue());
                }
            },
            Component.translatable("screen.anvilcraft.structure_scanner.export")
        ));
        this.refreshFiles();
        this.updateCache();
        this.updateNameInputEditable();
    }

    private void addRangeControls(WatchableCyclingValue<Integer> range, int y) {
        this.addScanWidget(new RangeValue(range, y));
        this.addScanWidget(new RangeButton(range, y, -1));
        this.addScanWidget(new RangeButton(range, y, 1));
    }

    private static void stepRange(WatchableCyclingValue<Integer> range, int step) {
        int next = Math.clamp(range.index() + step, 0, range.count() - 1);
        if (next == range.index()) return;
        range.fromIndex(next);
        PacketDistributor.sendToServer(new StructureScannerActionPacket("rangeChange", next, range.getName()));
    }

    private class RangeValue extends TextWidget {
        private final WatchableCyclingValue<Integer> range;

        RangeValue(WatchableCyclingValue<Integer> range, int y) {
            super(StructureScannerScreen.this.leftPos + 97, StructureScannerScreen.this.topPos + y + 1,
                20, 8, StructureScannerScreen.this.font, () -> Component.literal(range.get().toString()));
            this.range = range;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (!this.active || !this.visible || !this.isMouseOver(mouseX, mouseY) || scrollY == 0) return false;
            stepRange(this.range, (int) Math.signum(scrollY));
            return true;
        }
    }

    private class ScannerButton extends Button {
        private ResourceLocation texture;
        private final int frames;
        private final BooleanSupplier selected;
        private final StructureScannerButtonState pressState;
        private int textureWidth;
        private int textureHeight;

        ScannerButton(int x, int y, int width, ResourceLocation texture, OnPress onPress, Component hint) {
            this(x, y, width, texture, 3, () -> false, onPress, hint);
        }

        ScannerButton(int x, int y, int width, ResourceLocation texture, int frames,
                      BooleanSupplier selected, OnPress onPress, Component hint) {
            this(x, y, width, 16, texture, frames, selected, onPress, hint);
        }

        ScannerButton(int x, int y, int width, int height, ResourceLocation texture, int frames,
                      BooleanSupplier selected, OnPress onPress, Component hint) {
            super(x, y, width, height, hint, onPress, DEFAULT_NARRATION);
            this.texture = texture;
            this.frames = frames;
            this.selected = selected;
            this.pressState = new StructureScannerButtonState(frames);
            this.readTextureSize();
            if (!hint.getString().isEmpty()) this.setTooltip(Tooltip.create(hint));
        }

        void setTexture(ResourceLocation texture) {
            if (this.texture.equals(texture)) return;
            this.pressState.cancel();
            this.texture = texture;
            this.readTextureSize();
        }

        private void readTextureSize() {
            this.textureWidth = this.width;
            this.textureHeight = this.height * this.frames;
            try (var stream = Minecraft.getInstance().getResourceManager().open(this.texture);
                 NativeImage atlas = NativeImage.read(stream)) {
                if (atlas.getHeight() % this.frames != 0) throw new IOException("Invalid button atlas height");
                this.textureWidth = atlas.getWidth();
                this.textureHeight = atlas.getHeight();
            } catch (IOException exception) {
                AnvilCraft.LOGGER.warn("Unable to read scanner button atlas {}", this.texture, exception);
            }
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!this.active || !this.visible) this.pressState.cancel();
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (this == StructureScannerScreen.this.modeToggleButton && StructureScannerScreen.this.renderRedoHighlight(graphics)) return;
            int frame = this.pressState.frame(this.active, this.isHovered(), this.selected.getAsBoolean());
            float color = this.active ? 1.0F : 0.45F;
            RenderSystem.setShaderColor(color, color, color, 1.0F);
            int frameHeight = this.textureHeight / this.frames;
            graphics.blit(this.texture, this.getX(), this.getY(), this.width, this.height,
                0, frame * frameHeight, this.textureWidth, frameHeight, this.textureWidth, this.textureHeight);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            StructureScannerScreen.this.cancelButtonPress();
            if (this.pressState.press(0, this.active && this.visible)) StructureScannerScreen.this.pressedButton = this;
        }

        @Override
        public void playDownSound(SoundManager soundManager) {
            // 音效与动作统一在松开时触发。
        }

        private void activate() {
            super.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onPress();
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (button != 0 || !this.pressState.pressedBy(0)) return false;
            if (this.pressState.release(0, this.active && this.visible, this.isMouseOver(mouseX, mouseY))) this.activate();
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!this.active || !this.visible || !CommonInputs.selected(keyCode)) return false;
            this.pressState.press(keyCode, true);
            return true;
        }

        @Override
        public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
            if (!CommonInputs.selected(keyCode) || !this.pressState.pressedBy(keyCode)) return false;
            if (this.pressState.release(keyCode, this.active && this.visible, this.isFocused())) this.activate();
            return true;
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            if (!focused && this.pressState.keyboardPressed()) this.pressState.cancel();
        }
    }

    private boolean renderRedoHighlight(GuiGraphics graphics) {
        if (this.redoHighlightFinished) return false;
        if (this.modeToggleButton.isHovered() || this.modeToggleButton.isFocused() || !this.isScanMode || !this.modeToggleButton.active) {
            this.redoHighlightFinished = true;
            return false;
        }
        long now = Util.getMillis();
        if (this.redoHighlightStartedAt < 0L) this.redoHighlightStartedAt = now;
        long frame = (now - this.redoHighlightStartedAt) / REDO_HIGHLIGHT_FRAME_MILLIS;
        if (frame >= REDO_HIGHLIGHT_FRAMES) {
            this.redoHighlightFinished = true;
            return false;
        }
        graphics.blit(REDO_HIGHLIGHT_TEXTURE, this.modeToggleButton.getX(), this.modeToggleButton.getY(),
            0, (int) frame * 26, 26, 26, 26, 26 * REDO_HIGHLIGHT_FRAMES);
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (this.modeToggleButton.isMouseOver(mouseX, mouseY)) this.redoHighlightFinished = true;
        super.mouseMoved(mouseX, mouseY);
    }

    private class RangeButton extends SimpleIconButton {
        private final WatchableCyclingValue<Integer> range;
        private final int step;

        RangeButton(WatchableCyclingValue<Integer> range, int y, int step) {
            super(StructureScannerScreen.this.leftPos + (step < 0 ? 83 : 121), StructureScannerScreen.this.topPos + y,
                step < 0 ? "minus" : "add", button -> {
                    stepRange(range, step);
                });
            this.range = range;
            this.step = step;
        }

        boolean canStep() {
            int next = this.range.index() + this.step;
            return next >= 0 && next < this.range.count();
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.isHovered = this.active && this.isHovered;
            float color = this.active ? 1.0F : 0.45F;
            RenderSystem.setShaderColor(color, color, color, 1.0F);
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private <T extends AbstractWidget> void addScanWidget(T widget) {
        this.scanWidgets.add(this.addRenderableWidget(widget));
    }

    private <T extends AbstractWidget> void addBlueprintWidget(T widget) {
        this.blueprintWidgets.add(this.addRenderableWidget(widget));
    }

    private EditBox createInput(int x, int y, int width, String key, int maxLength) {
        EditBox input = new ScannerEditBox(this.leftPos + x, this.topPos + y, width,
            Component.translatable("screen.anvilcraft.structure_scanner." + key));
        input.setBordered(false);
        input.setCanLoseFocus(true);
        input.setMaxLength(maxLength);
        input.setTextColor(0xFFFFFF);
        input.setHint(Component.translatable("screen.anvilcraft.structure_scanner." + key));
        return this.addRenderableWidget(input);
    }

    private class ScannerEditBox extends EditBox {
        private long manualScrollUntil;

        ScannerEditBox(int x, int y, int width, Component message) {
            super(StructureScannerScreen.this.font, x, y, width, 12, message);
        }

        @Override
        public void moveCursor(int delta, boolean select) {
            super.moveCursor(delta, select);
            this.manualScrollUntil = Util.getMillis() + 1500L;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            String value = this.getValue();
            Font font = StructureScannerScreen.this.font;
            if (!this.isFocused() && Util.getMillis() >= this.manualScrollUntil && font.width(value) > this.getInnerWidth()) {
                graphics.drawScrollingString(font, Component.literal(value),
                    this.getX(), this.getX() + this.getInnerWidth(), this.getY(), 0xFFFFFF);
            } else {
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private void refreshFiles() {
        BlueprintClientFiles.requestFiles(this.menu.containerId);
    }

    public void onFilesReceived(List<String> files) {
        this.importFiles = List.copyOf(files);
        this.filterFiles();
    }

    private void filterFiles() {
        String query = this.importInput.getValue().toLowerCase(Locale.ROOT);
        this.filteredFiles = this.importFiles.stream().filter(name -> name.toLowerCase(Locale.ROOT).contains(query)).toList();
        this.fileOffset = 0;
    }

    private class RotationButton extends ScannerButton {
        RotationButton(int x, boolean rotation) {
            super(StructureScannerScreen.this.leftPos + x, StructureScannerScreen.this.topPos + 85, 16,
                scannerTexture(rotation ? "auto_rotate_on" : "auto_rotate_off"), 5,
                () -> StructureScannerScreen.this.autoRotate == rotation,
                button -> StructureScannerScreen.this.autoRotate = rotation,
                Component.translatable("screen.anvilcraft.structure_scanner.auto_rotate_" + (rotation ? "on" : "off")));
        }
    }

    public void showStatus(Component message) {
        this.statusTitle = message.copy();
        this.statusTitleUntil = Util.getMillis() + 10000;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.statusTitle != null && Util.getMillis() < this.statusTitleUntil) {
            int available = this.imageWidth - 12;
            if (this.font.width(this.statusTitle) <= available) {
                guiGraphics.drawString(this.font, this.statusTitle,
                    (this.imageWidth - this.font.width(this.statusTitle)) / 2, this.titleLabelY, 0xFF5555, false);
            } else {
                // 滚动文本的裁剪框使用屏幕坐标，不随 renderLabels 的局部 PoseStack 平移。
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(-this.leftPos, -this.topPos, 0);
                guiGraphics.drawScrollingString(this.font, this.statusTitle,
                    this.leftPos + 6, this.leftPos + this.imageWidth - 6, this.topPos + this.titleLabelY, 0xFF5555);
                guiGraphics.pose().popPose();
            }
        } else {
            this.statusTitle = null;
            guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        }
    }

    /**
     * 渲染半透明的物品虚影
     */
    private void renderMaskedItem(GuiGraphics g, ItemStack stack, int x, int y) {
        final int maskColor = 0x99777777;  // 调整透明度，数值越大越透明
        g.renderItem(stack, x, y, 0);
        g.fill(RenderType.guiOverlay(), x, y, x + 16, y + 16, maskColor);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BACKGROUND, i, j, 0, 0, this.imageWidth, this.imageHeight);

        if (this.blueprintVisible) {
            guiGraphics.blit(BLUEPRINT_TEXTURE, i + 4, j + 63, 0, 0, 128, 128, 128, 128);
            if (!this.marker.isEmpty()) guiGraphics.renderItem(this.marker, i + 112, j + 85);
        }

        // 渲染磁盘槽位的虚影（当槽位为空时）
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null && blockEntity.getDiskInventory().getItem(0).isEmpty()) {
            // 获取结构磁盘物品
            ItemStack diskStack = ModItems.STRUCTURE_DISK.get().getDefaultInstance();
            if (!diskStack.isEmpty()) {
                int diskSlotX = i + 8;
                int diskSlotY = j + 112;
                renderMaskedItem(guiGraphics, diskStack, diskSlotX, diskSlotY);
            }
        }
        guiGraphics.flush();
        this.renderPreview(guiGraphics);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 更新缓存数据
        this.updateCache();

        // 根据扫描状态更新按钮
        this.updateModeToggleButton();

        // 根据磁盘状态更新文本框可编辑状态
        this.updateNameInputEditable();
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染信息栏（不渲染tooltip）
        this.renderInfoPanelWithoutTooltip(guiGraphics);

        this.renderFileDropdown(guiGraphics, mouseX, mouseY);
        if (this.blueprintVisible && this.isHovering(112, 85, 16, 16, mouseX, mouseY)) {
            if (this.marker.isEmpty()) {
                guiGraphics.renderTooltip(this.font, Component.translatable("screen.anvilcraft.structure_scanner.marker"), mouseX, mouseY);
            } else {
                guiGraphics.renderTooltip(this.font, this.marker, mouseX, mouseY);
            }
        }

        // 最后统一渲染所有tooltip，确保在所有元素上方
        List<TooltipRenderInfo> tooltipsToRender = new ArrayList<>();

        // 收集默认slot tooltip
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            tooltipsToRender.add(new TooltipRenderInfo(
                this.font,
                this.getTooltipFromContainerItem(this.hoveredSlot.getItem()),
                mouseX,
                mouseY
            ));
        }

        // 收集信息栏叹号tooltip
        TooltipRenderInfo infoPanelTooltip = this.collectInfoPanelTooltip(mouseX, mouseY);
        if (infoPanelTooltip != null) {
            tooltipsToRender.add(infoPanelTooltip);
        }

        // 统一渲染所有tooltip，使用高Z轴确保在最上层
        for (TooltipRenderInfo tooltipInfo : tooltipsToRender) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 2000);  // 使用更高的Z轴层级
            guiGraphics.renderTooltip(tooltipInfo.font, tooltipInfo.tooltip, Optional.empty(), tooltipInfo.x, tooltipInfo.y);
            guiGraphics.pose().popPose();
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
    }

    /**
     * 更新缓存数据，避免每帧重复获取
     */
    private void updateCache() {
        var blockEntity = this.menu.getBlockEntity();

        // 检查blockEntity是否变化
        if (blockEntity != this.cachedBlockEntity) {
            this.cachedBlockEntity = blockEntity;
            this.cachedHasDisk = false;
            this.cachedInfoStatus = StructureScannerBlockEntity.InfoStatus.READY;
            this.cachedIsScanComplete = false;
            this.cachedHasStartedScanning = false;
            this.cachedRangeX = -1;
            this.cachedRangeY = -1;
            this.cachedRangeZ = -1;
        }

        if (blockEntity == null) {
            return;
        }

        // 更新磁盘状态
        boolean newHasDisk = !blockEntity.getDiskInventory().getItem(0).isEmpty();
        if (newHasDisk != this.cachedHasDisk) {
            this.cachedHasDisk = newHasDisk;
        }

        // 更新信息状态
        StructureScannerBlockEntity.InfoStatus newInfoStatus = blockEntity.getInfoStatus();
        if (newInfoStatus != this.cachedInfoStatus) {
            this.cachedInfoStatus = newInfoStatus;
        }

        // 更新扫描完成状态
        boolean newIsScanComplete = blockEntity.isScanComplete();
        if (newIsScanComplete != this.cachedIsScanComplete) {
            this.cachedIsScanComplete = newIsScanComplete;
        }

        // 更新扫描开始状态
        boolean newHasStartedScanning = blockEntity.hasStartedScanning();
        if (newHasStartedScanning != this.cachedHasStartedScanning) {
            this.cachedHasStartedScanning = newHasStartedScanning;
        }

        // 更新范围值
        boolean rangeChanged = false;

        int newRangeX = blockEntity.getRangeX().get();
        if (newRangeX != this.cachedRangeX) {
            this.cachedRangeX = newRangeX;
            rangeChanged = true;
        }

        int newRangeY = blockEntity.getRangeY().get();
        if (newRangeY != this.cachedRangeY) {
            this.cachedRangeY = newRangeY;
            rangeChanged = true;
        }

        int newRangeZ = blockEntity.getRangeZ().get();
        if (newRangeZ != this.cachedRangeZ) {
            this.cachedRangeZ = newRangeZ;
            rangeChanged = true;
        }

        if (rangeChanged) {
            // 任一范围变化时，使预览缓存失效
            this.cachedPreviewLevelLike = null;
        }

        // 检查扫描方块数据是否变化（通过大小比较）
        int currentScannedBlocksSize = blockEntity.getScannedBlocks().size();
        if (currentScannedBlocksSize != this.cachedScannedBlocksSize) {
            // 扫描数据大小变化，使预览缓存失效
            this.cachedScannedBlocksSize = currentScannedBlocksSize;
            this.cachedPreviewLevelLike = null;
        }
    }

    /**
     * 渲染信息栏（不渲染tooltip）
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void renderInfoPanelWithoutTooltip(GuiGraphics guiGraphics) {
        // 使用缓存数据
        if (this.cachedBlockEntity == null) return;

        // 检查是否有磁盘
        if (!this.cachedHasDisk || this.blueprintVisible) return;

        // 获取信息状态
        StructureScannerBlockEntity.InfoStatus status = this.cachedInfoStatus;

        // 信息栏位置（在磁盘槽位上方）
        int infoX = this.leftPos + 9;
        int infoY = this.topPos + 72;

        // 渲染标题（使用缩放）
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(infoX, infoY, 0);
        poseStack.scale(0.75f, 0.75f, 1.0f);
        guiGraphics.drawString(this.font, Component.translatable("screen.anvilcraft.structure_scanner.info_title"), 0, 0, 0xFFFFFF, false);
        poseStack.popPose();

        // 状态信息位置（标题下方）
        int statusY = infoY + 10;

        // 根据状态渲染
        switch (status) {
            case READY -> {
                // 只在扫描完成后显示“结构扫描就绪”
                if (this.cachedIsScanComplete) {
                    // 显示“结构扫描就绪”（使用缩放）
                    poseStack.pushPose();
                    poseStack.translate(infoX, statusY, 0);
                    guiGraphics.drawString(
                        this.font,
                        Component.translatable("screen.anvilcraft.structure_scanner.ready"),
                        0,
                        0,
                        0x40FF40,
                        false
                    );
                    poseStack.popPose();
                }
            }
            case LARGE_STRUCTURE, UNKNOWN_BLOCKS, TOO_LARGE -> {
                // 显示叹号图标
                boolean isWarning = status == StructureScannerBlockEntity.InfoStatus.LARGE_STRUCTURE;
                int iconColor = isWarning ? 0xFFFF55 : 0xFF5555;

                // 叹号图标单独设置位置和大小
                float iconScale = 1.5f;  // 叹号图标缩放比例

                // 绘制叹号（使用缩放）
                poseStack.pushPose();
                poseStack.translate(infoX, statusY, 0);
                poseStack.scale(iconScale, iconScale, 1.0f);
                int textOffsetX = 18;  // 叹号文本的X偏移量
                guiGraphics.drawString(this.font, "!", textOffsetX, 0, iconColor, false);
                poseStack.popPose();
            }
            default -> {
                // 未知状态，不渲染任何内容
            }
        }
    }

    /**
     * 收集信息栏叹号tooltip（不渲染）
     */
    @Nullable
    private TooltipRenderInfo collectInfoPanelTooltip(int mouseX, int mouseY) {
        // 使用缓存数据
        if (this.cachedBlockEntity == null) return null;

        // 检查是否有磁盘
        if (!this.cachedHasDisk || this.blueprintVisible) return null;

        // 获取信息状态
        StructureScannerBlockEntity.InfoStatus status = this.cachedInfoStatus;

        // 只有特定状态才有tooltip
        if (
            status != StructureScannerBlockEntity.InfoStatus.LARGE_STRUCTURE
            && status != StructureScannerBlockEntity.InfoStatus.UNKNOWN_BLOCKS
            && status != StructureScannerBlockEntity.InfoStatus.TOO_LARGE
        ) {
            return null;
        }

        // 信息栏位置（在磁盘槽位上方）
        int infoX = this.leftPos + 9;
        int infoY = this.topPos + 72;
        int statusY = infoY + 10;

        // 叹号图标缩放比例
        float iconScale = 1.5f;
        int textOffsetX = 18;

        // 检查鼠标是否在叹号上（考虑缩放后的实际尺寸和偏移量）
        int scaledWidth = (int) (8 * iconScale);
        int scaledHeight = (int) (10 * iconScale);
        int hoverStartX = infoX + (int) (textOffsetX * iconScale);

        if (mouseX >= hoverStartX && mouseX < hoverStartX + scaledWidth && mouseY >= statusY && mouseY < statusY + scaledHeight) {
            // 收集tooltip
            Component tooltip = switch (status) {
                case LARGE_STRUCTURE -> Component.translatable("screen.anvilcraft.structure_scanner.tooltip.large_structure");
                case UNKNOWN_BLOCKS -> Component.translatable("screen.anvilcraft.structure_scanner.tooltip.unknown_blocks");
                case TOO_LARGE -> Component.translatable("screen.anvilcraft.structure_scanner.tooltip.too_large");
                default -> Component.empty();
            };

            return new TooltipRenderInfo(this.font, List.of(tooltip), mouseX, mouseY);
        }

        return null;
    }

    /**
     * 根据磁盘状态更新文本框可编辑状态
     */
    private void updateNameInputEditable() {
        boolean imported = this.menu.getImportedStructure() != null;
        if (!imported) {
            this.cachedImportedStructure = null;
            this.cachedImportedPreview = null;
        }
        boolean visible = imported || this.cachedHasDisk && this.cachedIsScanComplete;
        if (visible && !imported && !this.blueprintVisible && this.cachedBlockEntity != null) {
            this.marker = DiskDisplaySupport.getScannedDisplay(this.cachedBlockEntity).copy();
        }
        this.blueprintVisible = visible;
        this.blueprintWidgets.forEach(widget -> widget.visible = visible);
        this.scanWidgets.forEach(widget -> {
            widget.visible = !visible;
            widget.active = !visible && this.cachedBlockEntity != null && !this.cachedBlockEntity.isScanning()
                && (!(widget instanceof RangeButton button) || button.canStep());
        });
        this.nameInput.setEditable(visible);
        if (!visible && this.nameInput.isFocused()) this.clearInputFocus();
        this.confirmButton.active = visible && this.cachedHasDisk && this.menu.getSlot(1).getItem().isEmpty()
            && !BlueprintClientFiles.isBusy();
        boolean openFolder = hasShiftDown() && BlueprintClientFiles.canOpenDirectory();
        if (this.folderMode != openFolder) {
            this.folderMode = openFolder;
            this.importButton.setTooltip(Tooltip.create(Component.translatable(
                "screen.anvilcraft.structure_scanner." + (openFolder ? "open_folder" : "import"))));
            this.exportButton.setTooltip(Tooltip.create(Component.translatable(
                "screen.anvilcraft.structure_scanner." + (openFolder ? "open_folder" : "export"))));
        }
        this.importButton.active = openFolder || this.importFiles.contains(this.importInput.getValue()) && !BlueprintClientFiles.isBusy();
        this.exportButton.active = openFolder || BlueprintClientFiles.isValidExportName(this.exportInput.getValue())
            && (imported || this.cachedIsScanComplete || this.menu.getSlot(0).hasItem() || this.menu.getSlot(1).hasItem())
            && !BlueprintClientFiles.isBusy();
    }

    private void renderFileDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.importDropdown) return;
        int rows = Math.min(FILE_ROWS, this.filteredFiles.size());
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 500);
        int x = this.leftPos + 6;
        int y = this.topPos + 35;
        graphics.fill(x, y, x + 106, y + Math.max(1, rows) * FILE_ROW_HEIGHT, 0xFF101010);
        for (int row = 0; row < rows; row++) {
            int top = y + row * FILE_ROW_HEIGHT;
            if (mouseX >= x && mouseX < x + 106 && mouseY >= top && mouseY < top + FILE_ROW_HEIGHT) {
                graphics.fill(x, top, x + 106, top + FILE_ROW_HEIGHT, 0xFF555555);
            }
            graphics.drawString(this.font, this.font.plainSubstrByWidth(this.filteredFiles.get(this.fileOffset + row), 102),
                x + 2, top + 2, 0xFFFFFF, false);
        }
        if (rows == 0) {
            graphics.drawString(this.font, Component.translatable("screen.anvilcraft.structure_scanner.no_files"),
                x + 2, y + 2, 0xAAAAAA, false);
        }
        graphics.pose().popPose();
    }

    @Override
    public Collection<Integer> getGhostSlots() {
        return this.blueprintVisible && !this.importDropdown ? List.of(0) : List.of();
    }

    @Override
    public int[] getGhostSlotArea(int slotIndex) {
        return new int[]{112, 85, 16, 16};
    }

    @Override
    public void acceptGhost(Slot slot, ItemStack ingredient) {
        if (this.blueprintVisible) this.marker = ingredient.copyWithCount(1);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.importDropdown && this.isHovering(6, 35, 106, FILE_ROWS * FILE_ROW_HEIGHT, mouseX, mouseY)) {
            this.fileOffset = Mth.clamp(this.fileOffset - (int) Math.signum(scrollY), 0,
                Math.max(0, this.filteredFiles.size() - FILE_ROWS));
            return true;
        }
        EditBox input = this.inputAt(mouseX, mouseY);
        if (input != null) {
            double amount = scrollX == 0 ? -scrollY : scrollX;
            input.moveCursor((int) Math.signum(amount) * 3, false);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /**
     * 根据扫描状态更新模式切换按钮
     */
    private void updateScanButton(boolean scanning) {
        this.modeToggleButton.setTexture(scanning ? STOP_TEXTURE : REDO_TEXTURE);
        Component hint = Component.translatable("screen.anvilcraft.structure_scanner." + (scanning ? "scanning" : "scan"));
        if (!hint.equals(this.modeToggleButton.getMessage())) {
            this.modeToggleButton.setMessage(hint);
            this.modeToggleButton.setTooltip(Tooltip.create(hint));
        }
    }

    private void updateModeToggleButton() {
        // 使用缓存数据
        if (this.cachedBlockEntity == null) {
            return;
        }

        if (this.menu.getImportedStructure() != null) {
            this.isScanMode = true;
            this.updateScanButton(false);
            return;
        }

        // 如果正在扫描，切换为 stop 状态
        if (this.cachedBlockEntity.isScanning()) {
            if (this.isScanMode) {
                this.autoRotate = true;
                this.isScanMode = false;
                this.updateScanButton(true);
            }
            // 如果扫描完成，切换回 redo 状态
        } else {
            if (!this.isScanMode) {
                this.isScanMode = true;
                this.updateScanButton(false);
            }
        }
    }

    /**
     * 渲染3D预览（含扫描仪后处理）
     */
    private void renderPreview(GuiGraphics guiGraphics) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }

        // 阶段1: 正常渲染 3D 预览到主帧缓冲
        final double guiScaleD = this.minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor(
            (int) (this.previewWindowX * guiScaleD), (int) (
                (
                    this.minecraft.getWindow().getGuiScaledHeight() - this.previewWindowY - this.previewWindowHeight
                ) * guiScaleD
            ), (int) (this.previewWindowWidth * guiScaleD), (int) (this.previewWindowHeight * guiScaleD)
        );

        this.renderPreviewContent(
            guiGraphics,
            this.previewWindowX + this.previewWindowWidth / 2,
            this.previewWindowY + this.previewWindowHeight / 2
        );

        RenderSystem.disableScissor();
        guiGraphics.flush();

        // 阶段2&3: 扫描着色器后处理（仅在配置启用时）
        if (!RenderState.isScanPreviewEffectEnabled()) return;

        int guiScale = (int) this.minecraft.getWindow().getGuiScale();
        int fbWidth = this.previewWindowWidth * guiScale;
        int fbHeight = this.previewWindowHeight * guiScale;

        if (this.previewFbo == null) {
            this.previewFbo = new TextureTarget(fbWidth, fbHeight, true, Minecraft.ON_OSX);
        } else if (this.previewFbo.width != fbWidth || this.previewFbo.height != fbHeight) {
            this.previewFbo.resize(fbWidth, fbHeight, Minecraft.ON_OSX);
        }

        final RenderTarget mainTarget = this.minecraft.getMainRenderTarget();
        int srcX = (int) (this.previewWindowX * guiScaleD);
        int srcY = (int) (
            (
                this.minecraft.getWindow().getGuiScaledHeight() - this.previewWindowY - this.previewWindowHeight
            ) * guiScaleD
        );

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainTarget.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.previewFbo.frameBufferId);
        GL30.glBlitFramebuffer(
            srcX,
            srcY,
            srcX + fbWidth,
            srcY + fbHeight,
            0,
            0,
            fbWidth,
            fbHeight,
            GL11.GL_COLOR_BUFFER_BIT,
            GL11.GL_NEAREST
        );
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);

        mainTarget.bindWrite(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.viewport(0, 0, this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());

        float fbW = this.previewFbo.width;
        float fbH = this.previewFbo.height;

        ShaderInstance shader = ModShaders.getScanPreviewShader();
        shader.setSampler("DiffuseSampler", this.previewFbo);
        shader.safeGetUniform("ProjMat").set(ModShaders.getOrthoMatrix());
        shader.safeGetUniform("InSize").set(fbW, fbH);

        float screenX = this.previewWindowX * guiScale;
        float screenY = (
                            this.minecraft.getWindow().getGuiScaledHeight() - this.previewWindowY - this.previewWindowHeight
                        ) * guiScale;

        shader.safeGetUniform("OutPos").set(screenX, screenY);
        shader.safeGetUniform("OutSize").set(fbW, fbH);
        shader.safeGetUniform("GameTime").set((float) (System.currentTimeMillis() % 100000) / 1000.0f);

        RenderSystem.depthMask(false);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        shader.apply();

        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferbuilder.addVertex(0.0F, 0.0F, 0.0F);
        bufferbuilder.addVertex(fbW, 0.0F, 0.0F);
        bufferbuilder.addVertex(fbW, fbH, 0.0F);
        bufferbuilder.addVertex(0.0F, fbH, 0.0F);
        BufferUploader.draw(bufferbuilder.buildOrThrow());

        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        ProgramManager.glUseProgram(0);
        RenderSystem.disableBlend();

        this.previewFbo.unbindRead();
    }

    /**
     * 渲染3D预览内容
     */
    private void renderPreviewContent(GuiGraphics guiGraphics, int posX, int posY) {
        if (this.minecraft == null || this.minecraft.level == null) return;

        var imported = this.menu.getImportedStructure();
        LevelLike preview;
        AABB bounds;
        if (imported != null) {
            preview = this.buildImportedPreview(imported, this.minecraft.level);
            bounds = this.cachedImportedPreviewBounds;
        } else {
            if (this.cachedBlockEntity == null) return;
            var state = this.minecraft.level.getBlockState(this.cachedBlockEntity.getBlockPos());
            LevelLike scannedPreview = this.buildPreviewLevelLike(state.getValue(HorizontalDirectionalBlock.FACING));
            if (this.blueprintError != null) {
                guiGraphics.drawWordWrap(this.font, Component.translatable(
                    "screen.anvilcraft.structure_scanner.file_failed", this.blueprintError),
                    this.previewWindowX + 4, this.previewWindowY + 4, this.previewWindowWidth - 8, 0xFFFF5555);
                return;
            }
            if (scannedPreview == null) return;
            preview = scannedPreview;
            bounds = this.cachedPreviewBounds;
        }

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        this.applyPreviewTransform(pose, bounds, posX, posY);
        RenderSupport.renderLevelLikeBlocks(preview, pose, BlockPos.betweenClosed(
            BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
            BlockPos.containing(bounds.maxX - 1, bounds.maxY - 1, bounds.maxZ - 1)));
        if (imported == null) this.renderScannerBorder(pose);
        pose.popPose();
    }

    private void applyPreviewTransform(PoseStack pose, AABB bounds, int posX, int posY) {
        Quaternionf rotation = Axis.XP.rotationDegrees(this.previewRotationX)
            .mul(Axis.YP.rotationDegrees(this.previewRotationY + 315.0F));
        Matrix3f matrix = new Matrix3f().rotation(rotation);
        float sizeX = (float) bounds.getXsize();
        float sizeY = (float) bounds.getYsize();
        float sizeZ = (float) bounds.getZsize();
        float projectedWidth = Math.abs(matrix.m00()) * sizeX + Math.abs(matrix.m10()) * sizeY + Math.abs(matrix.m20()) * sizeZ;
        float projectedHeight = Math.abs(matrix.m01()) * sizeX + Math.abs(matrix.m11()) * sizeY + Math.abs(matrix.m21()) * sizeZ;
        float scale = Math.min((this.previewWindowWidth - 8.0F) / projectedWidth,
            (this.previewWindowHeight - 8.0F) / projectedHeight);
        pose.translate(posX, posY, 100);
        pose.scale(-scale, -scale, -scale);
        pose.mulPose(rotation);
        pose.translate(-(bounds.minX + bounds.maxX) / 2, -(bounds.minY + bounds.maxY) / 2, -(bounds.minZ + bounds.maxZ) / 2);
    }

    private LevelLike buildImportedPreview(StructureScannerMenu.ImportedStructure imported, ClientLevel level) {
        if (imported == this.cachedImportedStructure && this.cachedImportedPreview != null) return this.cachedImportedPreview;
        LevelLike preview = new LevelLike(level);
        var blocks = BlueprintMultiblocks.expand(imported.snapshot(),
            new BlueprintPlacement(BlockPos.ZERO, Rotation.NONE, Mirror.NONE), -1);
        int minX = blocks.stream().mapToInt(block -> block.pos().getX()).min().orElse(0);
        int minY = blocks.stream().mapToInt(block -> block.pos().getY()).min().orElse(0);
        int minZ = blocks.stream().mapToInt(block -> block.pos().getZ()).min().orElse(0);
        BlockPos origin = new BlockPos(minX, minY, minZ);
        this.cachedImportedPreviewBounds = new AABB(BlockPos.ZERO);
        for (var block : blocks) {
            BlockPos pos = block.pos().subtract(origin);
            preview.setBlockState(pos, block.state());
            this.cachedImportedPreviewBounds = this.cachedImportedPreviewBounds.minmax(new AABB(pos));
            var entity = preview.getBlockEntity(pos);
            if (entity != null) block.nbt().ifPresent(tag -> entity.loadWithComponents(tag, level.registryAccess()));
        }
        this.cachedImportedStructure = imported;
        this.cachedImportedPreview = preview;
        return preview;
    }

    /**
     * 构建预览用的LevelLike实例（带缓存）
     */
    private @Nullable LevelLike buildPreviewLevelLike(Direction facing) {
        if (this.cachedBlockEntity == null || this.minecraft == null || this.minecraft.level == null) {
            return null;
        }

        // 如果缓存有效，直接返回
        if (this.cachedPreviewLevelLike != null && this.cachedPreviewFacing == facing) {
            return this.cachedPreviewLevelLike;
        }

        ClientLevel level = this.minecraft.level;
        LevelLike previewLevelLike = new LevelLike(level);

        // 获取扫描范围
        int rangeX = this.cachedRangeX;
        int rangeY = this.cachedRangeY;
        this.cachedPreviewBounds = new AABB(0, 0, 0, Math.max(1, rangeX),
            Math.max(1, rangeY), Math.max(1, this.cachedRangeZ) + 2);

        boolean upsideDown = false;
        if (this.cachedBlockEntity.getBlockState().hasProperty(dev.dubhe.anvilcraft.block.StructureScannerBlock.UPSIDE_DOWN)) {
            upsideDown = this.cachedBlockEntity.getBlockState().getValue(dev.dubhe.anvilcraft.block.StructureScannerBlock.UPSIDE_DOWN);
        }

        // Scanner在预览中的位置：X居中，Y=0，Z=0（选区前面）
        int scannerX = rangeX / 2;
        int scannerY = upsideDown ? Math.max(1, rangeY) - 1 : 0;
        int scannerZ = 0;  // 选区前面

        // 放置Scanner（始终渲染），在预览中统一朝北
        previewLevelLike.setBlockStateAlwaysRender(
            new BlockPos(scannerX, scannerY, scannerZ),
            ModBlocks.STRUCTURE_SCANNER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(dev.dubhe.anvilcraft.block.StructureScannerBlock.UPSIDE_DOWN, upsideDown)
        );

        // 使用缓存的扫描结果渲染方块
        List<StructureScannerBlockEntity.CachedBlockData> scannedBlocks = this.cachedBlockEntity.getScannedBlocks();

        this.blueprintError = null;
        if (!scannedBlocks.isEmpty()) {
            try {
                var result = StructureSaveUtil.buildSnapshot(this.cachedBlockEntity, scannedBlocks);
                var snapshot = result.snapshot();
                for (var entry : snapshot.blocks()) {
                    BlockPos local = entry.pos().subtract(result.offset());
                    int x = local.getX();
                    int z = local.getZ();
                    BlockPos pos = switch (facing) {
                        case SOUTH -> new BlockPos(rangeX - 1 - x, local.getY(), this.cachedRangeZ + 1 - z);
                        case WEST -> new BlockPos(rangeX - 1 - z, local.getY(), x + 2);
                        case EAST -> new BlockPos(z, local.getY(), this.cachedRangeZ + 1 - x);
                        default -> new BlockPos(x, local.getY(), z + 2);
                    };
                    previewLevelLike.setBlockState(pos, snapshot.stateOf(entry).rotate(this.rotationForPreview(facing)));
                    this.cachedPreviewBounds = this.cachedPreviewBounds.minmax(new AABB(pos));
                    var entity = previewLevelLike.getBlockEntity(pos);
                    if (entity != null) entry.nbt().ifPresent(tag -> entity.loadWithComponents(tag, level.registryAccess()));
                }
            } catch (IllegalArgumentException exception) {
                this.blueprintError = exception.getMessage();
            }
        }

        // 更新缓存
        this.cachedPreviewLevelLike = previewLevelLike;
        this.cachedPreviewFacing = facing;

        return previewLevelLike;
    }

    /**
     * 根据 Scanner 朝向旋转方块状态
     */
    private Rotation rotationForPreview(Direction scannerFacing) {
        return switch (scannerFacing) {
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.CLOCKWISE_90;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private void renderScannerBorder(PoseStack pose) {
        if (this.minecraft == null || this.cachedBlockEntity == null) return;
        MultiBufferSource.BufferSource buffers = this.minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
        VoxelShape border = Shapes.create(0, 0, 2, this.cachedRangeX, this.cachedRangeY, this.cachedRangeZ + 2);
        TooltipRenderHelper.renderOutline(pose, consumer, 0, 0, 0, BlockPos.ZERO, border, 0xFF00FFCC);
        buffers.endBatch(RenderType.lines());
    }

    /**
     * 鼠标按下事件 - 支持拖拽旋转预览
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.updateNameInputEditable();
        if (this.importDropdown && this.isHovering(6, 35, 106,
            Math.max(1, Math.min(FILE_ROWS, this.filteredFiles.size())) * FILE_ROW_HEIGHT, mouseX, mouseY)) {
            int row = (int) (mouseY - this.topPos - 35) / FILE_ROW_HEIGHT + this.fileOffset;
            if (button == 0 && row < this.filteredFiles.size()) {
                this.importInput.setValue(this.filteredFiles.get(row));
                this.importInput.moveCursorToStart(false);
            }
            this.clearInputFocus();
            this.importDropdown = false;
            return true;
        }
        EditBox input = this.inputAt(mouseX, mouseY);
        this.clearInputFocus();
        this.importDropdown = false;
        if (input != null) {
            this.setFocused(input);
            input.setFocused(true);
            input.mouseClicked(mouseX, Mth.clamp(mouseY, input.getY(), input.getY() + input.getHeight() - 1), button);
            if (input == this.importInput) {
                this.refreshFiles();
                this.importDropdown = true;
            }
            return true;
        }
        if (this.blueprintVisible && this.isHovering(112, 85, 16, 16, mouseX, mouseY)) {
            this.marker = button == 1 ? ItemStack.EMPTY : this.menu.getCarried().copyWithCount(1);
            return true;
        }
        // 如果鼠标在预览窗口内，开始拖拽
        if (this.isMouseInPreviewWindow(mouseX, mouseY)) {
            this.isPreviewDragging = true;
            this.lastMouseX = (int) mouseX;
            this.lastMouseY = (int) mouseY;
            return true;
        }

        // 让父类和子组件处理点击事件（包括文本框的焦点处理）
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Nullable
    private EditBox inputAt(double mouseX, double mouseY) {
        for (EditBox input : List.of(this.nameInput, this.importInput, this.exportInput)) {
            int padding = input == this.nameInput ? 3 : 4;
            if (input.visible && this.isHovering(input.getX() - this.leftPos - 1,
                input.getY() - this.topPos - padding, input.getWidth() + 2, 16, mouseX, mouseY)) {
                return input;
            }
        }
        return null;
    }

    private void clearInputFocus() {
        for (EditBox input : List.of(this.nameInput, this.importInput, this.exportInput)) {
            input.setFocused(false);
            input.setHighlightPos(input.getCursorPosition());
        }
        if (this.getFocused() instanceof EditBox) this.setFocused(null);
    }

    public void onImportComplete(String name, StructureSnapshot snapshot) {
        this.menu.setImportedStructure(name, snapshot);
        this.nameInput.setValue(name);
        this.marker = DiskDisplaySupport.getImportedDisplay(snapshot).copy();
        this.cachedImportedPreview = null;
        this.autoRotate = false;
        this.clearInputFocus();
        this.importDropdown = false;
    }

    /**
     * 鼠标释放事件
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isPreviewDragging = false;
        if (button == 0 && this.pressedButton != null) {
            ScannerButton pressed = this.pressedButton;
            this.pressedButton = null;
            this.setDragging(false);
            pressed.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void cancelButtonPress() {
        if (this.pressedButton != null) this.pressedButton.pressState.cancel();
        this.pressedButton = null;
        if (this.getFocused() instanceof ScannerButton focused) focused.pressState.cancel();
    }

    /**
     * 鼠标拖拽事件 - 旋转预览
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isPreviewDragging) {
            int currentMouseX = (int) mouseX;
            int currentMouseY = (int) mouseY;
            float deltaX = currentMouseX - this.lastMouseX;
            float deltaY = currentMouseY - this.lastMouseY;

            // 水平移动 -> Y轴旋转
            this.previewRotationY += deltaX * ROTATION_SENSITIVITY;

            // 垂直移动 -> X轴旋转（有限制，反转方向）
            this.previewRotationX -= deltaY * ROTATION_SENSITIVITY;
            this.previewRotationX = Math.clamp(this.previewRotationX, MIN_ROTATION_X, MAX_ROTATION_X);

            this.lastMouseX = currentMouseX;
            this.lastMouseY = currentMouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * 检查鼠标是否在预览窗口内
     */
    private boolean isMouseInPreviewWindow(double mouseX, double mouseY) {
        return mouseX >= this.previewWindowX
               && mouseX < this.previewWindowX + this.previewWindowWidth
               && mouseY >= this.previewWindowY
               && mouseY < this.previewWindowY + this.previewWindowHeight;
    }

    /**
     * 模式切换按钮点击事件
     */
    private void onModeToggleClick() {
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) {
            return;
        }

        // 如果是 redo 状态，点击后开始/重新开始扫描
        if (this.isScanMode) {
            BlueprintClientFiles.cancelImport(this.menu.containerId);
            this.menu.clearImportedStructure();
            this.nameInput.setValue("");
            this.autoRotate = true;
            PacketDistributor.sendToServer(new StructureScannerActionPacket("start"));

            this.isScanMode = false;
            this.updateScanButton(true);
            // 如果正在扫描（stop 状态），点击后停止扫描
        } else {
            PacketDistributor.sendToServer(new StructureScannerActionPacket("stop"));

            this.isScanMode = true;
            this.updateScanButton(false);
        }
    }

    /**
     * 确认按钮点击事件
     */
    private void onConfirmClick() {
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null || !this.blueprintVisible || !this.confirmButton.active || BlueprintClientFiles.isBusy()) {
            return;
        }

        // 获取输入的结构名称
        String structureName = this.nameInput.getValue().trim();
        if (structureName.isEmpty()) {
            structureName = "structure_" + System.currentTimeMillis();
        }

        // 发送确认数据包到服务器（包含结构名称）
        PacketDistributor.sendToServer(new StructureScannerSavePacket(this.menu.containerId, structureName, this.autoRotate, this.marker));
    }

    @Override
    public void removed() {
        this.cancelButtonPress();
        BlueprintClientFiles.cancelImport(this.menu.containerId);
        this.menu.clearImportedStructure();
        if (this.previewFbo != null) {
            this.previewFbo.destroyBuffers();
            this.previewFbo = null;
        }
        super.removed();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String name = this.nameInput.getValue();
        String imported = this.importInput.getValue();
        final String exported = this.exportInput.getValue();
        this.init(minecraft, width, height);
        this.nameInput.setValue(name);
        this.importInput.setValue(imported);
        this.exportInput.setValue(exported);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.importDropdown) {
            this.importDropdown = false;
            return true;
        }
        for (EditBox input : List.of(this.nameInput, this.importInput, this.exportInput)) {
            if (input.isFocused() && keyCode != 256) {
                input.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * Tooltip渲染信息记录类
     */
    private record TooltipRenderInfo(
        Font font, List<Component> tooltip, int x, int y
    ) {
    }
}
