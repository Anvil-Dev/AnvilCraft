package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
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
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.SimpleIconButton;
import dev.dubhe.anvilcraft.client.gui.component.TextWidget;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.gui.component.ToggleButton;
import dev.dubhe.anvilcraft.client.init.ModShaders;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import dev.dubhe.anvilcraft.network.StructureScannerActionPacket;
import dev.dubhe.anvilcraft.util.LevelLike;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StructureScannerScreen extends AbstractContainerScreen<StructureScannerMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "structure_scanner");
    private static final ResourceLocation REDO_TEXTURE = SharedTextures.BUTTON_REDO;
    private static final ResourceLocation STOP_TEXTURE = SharedTextures.BUTTON_STOP;
    private static final ResourceLocation CONFIRM_TEXTURE = SharedTextures.BUTTON_CONFIRM;
    private static final ResourceLocation STRUCTURE_TOOL_LOCKED_TEXTURE = SharedTextures.STRUCTURE_TOOL_LOCKED;

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
    private ToggleButton modeToggleButton;
    private boolean isScanMode = true;  // 默认为 redo 状态

    // 文本输入框
    private EditBox nameInput;

    // 缓存数据
    private StructureScannerBlockEntity cachedBlockEntity;
    private boolean cachedHasDisk;
    private StructureScannerBlockEntity.InfoStatus cachedInfoStatus;
    private boolean cachedIsScanComplete;
    private boolean cachedHasStartedScanning;
    private int cachedRangeX = -1;
    private int cachedRangeY = -1;
    private int cachedRangeZ = -1;

    // 预览缓存
    private LevelLike cachedPreviewLevelLike;
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
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;

        // 初始化预览窗口位置（与智能放置器一致）
        this.previewWindowX = this.leftPos + 136;
        this.previewWindowY = this.topPos + 18;

        if (this.minecraft == null) return;

        // 添加X轴范围控制按钮和数值显示
        this.addRenderableWidget(new TextWidget(
            this.leftPos + 97, this.topPos + 49, 20, 8, this.minecraft.font, () -> {
            var blockEntity = this.menu.getBlockEntity();
            return Component.literal(blockEntity != null ? blockEntity.getRangeX().get().toString() : "?");
        }
        ));
        this.addRenderableWidget(new SimpleIconButton(
            this.leftPos + 84, this.topPos + 48, "minus", (b) -> {
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.getRangeX().previous();
                PacketDistributor.sendToServer(new StructureScannerActionPacket("rangeChange", blockEntity.getRangeX().index(), "rangeX"));
            }
        }
        ));
        this.addRenderableWidget(new SimpleIconButton(
            this.leftPos + 122, this.topPos + 48, "add", (b) -> {
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.getRangeX().next();
                PacketDistributor.sendToServer(new StructureScannerActionPacket("rangeChange", blockEntity.getRangeX().index(), "rangeX"));
            }
        }
        ));

        // 添加Z轴范围控制按钮和数值显示
        this.addRenderableWidget(new TextWidget(
            this.leftPos + 97, this.topPos + 63, 20, 8, this.minecraft.font, () -> {
            var blockEntity = this.menu.getBlockEntity();
            return Component.literal(blockEntity != null ? blockEntity.getRangeZ().get().toString() : "?");
        }
        ));
        this.addRenderableWidget(new SimpleIconButton(
            this.leftPos + 84, this.topPos + 62, "minus", (b) -> {
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.getRangeZ().previous();
                PacketDistributor.sendToServer(new StructureScannerActionPacket("rangeChange", blockEntity.getRangeZ().index(), "rangeZ"));
            }
        }
        ));
        this.addRenderableWidget(new SimpleIconButton(
            this.leftPos + 122, this.topPos + 62, "add", (b) -> {
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.getRangeZ().next();
                PacketDistributor.sendToServer(new StructureScannerActionPacket("rangeChange", blockEntity.getRangeZ().index(), "rangeZ"));
            }
        }
        ));

        // 添加Y轴范围控制按钮和数值显示
        this.addRenderableWidget(new TextWidget(
            this.leftPos + 97, this.topPos + 77, 20, 8, this.minecraft.font, () -> {
            var blockEntity = this.menu.getBlockEntity();
            return Component.literal(blockEntity != null ? blockEntity.getRangeY().get().toString() : "?");
        }
        ));
        this.addRenderableWidget(new SimpleIconButton(
            this.leftPos + 84, this.topPos + 76, "minus", (b) -> {
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.getRangeY().previous();
                PacketDistributor.sendToServer(new StructureScannerActionPacket("rangeChange", blockEntity.getRangeY().index(), "rangeY"));
            }
        }
        ));
        this.addRenderableWidget(new SimpleIconButton(
            this.leftPos + 122, this.topPos + 76, "add", (b) -> {
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.getRangeY().next();
                PacketDistributor.sendToServer(new StructureScannerActionPacket("rangeChange", blockEntity.getRangeY().index(), "rangeY"));
            }
        }
        ));

        // 添加模式切换按钮（redo/stop）
        this.modeToggleButton = new ToggleButton(
            this.leftPos + 232,
            this.topPos + 119,
            16,
            16,
            REDO_TEXTURE,
            16,
            16,
            (btn) -> this.onModeToggleClick(),
            List.of()
        );
        this.modeToggleButton.setSelected(false);
        this.addRenderableWidget(this.modeToggleButton);

        // 添加确认按钮
        TexturedButton confirmButton = new TexturedButton(
            this.leftPos + 8,
            this.topPos + 90,
            16,
            16,
            CONFIRM_TEXTURE,
            16,
            16,
            32,
            (btn) -> this.onConfirmClick()
        );
        this.addRenderableWidget(confirmButton);

        // 添加文本输入框（完全参考铁砧的实现）
        this.nameInput = new EditBox(this.font, this.leftPos + 28, this.topPos + 94, 101, 16, Component.literal(""));
        this.nameInput.setCanLoseFocus(true);
        this.nameInput.setTextColor(-1);
        this.nameInput.setTextColorUneditable(-1);
        this.nameInput.setBordered(false);
        this.nameInput.setMaxLength(50);
        this.nameInput.setResponder(this::onNameInputChanged);
        this.nameInput.setValue("");
        this.nameInput.setMaxLength(32);
        this.addRenderableWidget(this.nameInput);
        this.setInitialFocus(this.nameInput);
        this.nameInput.setEditable(true);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
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
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 更新缓存数据
        this.updateCache();

        // 根据扫描状态更新按钮
        this.updateModeToggleButton();

        // 根据磁盘状态更新文本框可编辑状态
        this.updateNameInputEditable();

        // 渲染3D预览
        this.renderPreview(guiGraphics);

        // 渲染信息栏（不渲染tooltip）
        this.renderInfoPanelWithoutTooltip(guiGraphics);

        // 渲染STRUCTURE_TOOL_LOCKED贴图（一直显示）
        guiGraphics.blit(STRUCTURE_TOOL_LOCKED_TEXTURE, this.leftPos + 6, this.topPos + 18, 0, 0, 126, 26, 126, 26);

        // 渲染文本输入框（参考铁砧的实现）
        this.nameInput.render(guiGraphics, mouseX, mouseY, partialTick);

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
        if (!this.cachedHasDisk) return;

        // 获取信息状态
        StructureScannerBlockEntity.InfoStatus status = this.cachedInfoStatus;

        // 信息栏位置（在磁盘槽位上方）
        int infoX = this.leftPos + 9;
        int infoY = this.topPos + 52;

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
        if (!this.cachedHasDisk) return null;

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
        int infoY = this.topPos + 52;
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
        // 使用缓存数据
        if (this.cachedBlockEntity == null) {
            this.nameInput.setEditable(false);
            return;
        }

        // 检查磁盘槽位是否有物品
        this.nameInput.setEditable(this.cachedHasDisk);

        // 如果没有磁盘且文本框有焦点，移除焦点
        if (!this.cachedHasDisk && this.nameInput.isFocused()) {
            this.nameInput.setFocused(false);
        }
    }

    /**
     * 根据扫描状态更新模式切换按钮
     */
    private void updateModeToggleButton() {
        // 使用缓存数据
        if (this.cachedBlockEntity == null) {
            return;
        }

        // 如果正在扫描，切换为 stop 状态
        if (this.cachedHasStartedScanning && !this.cachedIsScanComplete) {
            if (this.isScanMode) {
                this.isScanMode = false;
                this.modeToggleButton.setSelected(false);
                this.modeToggleButton.setTexture(STOP_TEXTURE);
            }
            // 如果扫描完成，切换回 redo 状态
        } else if (this.cachedIsScanComplete) {
            if (!this.isScanMode) {
                this.isScanMode = true;
                this.modeToggleButton.setSelected(true);
                this.modeToggleButton.setTexture(REDO_TEXTURE);
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
            this.previewWindowY + this.previewWindowHeight / 2 + 5
        );

        RenderSystem.disableScissor();

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

        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        shader.apply();

        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferbuilder.addVertex(0.0F, 0.0F, 0.0F);
        bufferbuilder.addVertex(fbW, 0.0F, 0.0F);
        bufferbuilder.addVertex(fbW, fbH, 0.0F);
        bufferbuilder.addVertex(0.0F, fbH, 0.0F);
        BufferUploader.draw(bufferbuilder.buildOrThrow());

        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        ProgramManager.glUseProgram(0);
        RenderSystem.disableBlend();

        this.previewFbo.unbindRead();
    }

    /**
     * 渲染3D预览内容
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void renderPreviewContent(GuiGraphics guiGraphics, int posX, int posY) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }

        // 获取Structure Scanner方块的状态
        if (this.cachedBlockEntity == null) {
            return;
        }

        var level = this.minecraft.level;
        var blockState = level.getBlockState(this.cachedBlockEntity.getBlockPos());
        var facing = blockState.getValue(HorizontalDirectionalBlock.FACING);

        // 构建并渲染 LevelLike（使用缓存）
        LevelLike previewLevelLike = this.buildPreviewLevelLike(facing);
        if (previewLevelLike != null) {
            // 计算选区的实际尺寸（忽略 Scanner）
            int rangeX = this.cachedRangeX;
            int rangeY = this.cachedRangeY;

            // 使用选区范围作为缩放基准，忽略 Scanner 的影响
            int sizeX = Math.max(1, rangeX);
            int sizeY = Math.max(1, rangeY);

            // 应用朝向旋转偏移，让预览根据Scanner的实际朝向旋转
            RenderSupport.renderLevelLikeWithFixedSize(
                previewLevelLike,
                guiGraphics,
                posX,
                posY,
                (float) 80.0,
                this.previewRotationX,
                this.previewRotationY + getFacingYawOffset(facing),
                sizeX,
                sizeY,
                -0.5f
            );
        }

        // 渲染边框
        this.renderScannerBorder(guiGraphics, posX, posY, facing);
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

        if (!scannedBlocks.isEmpty()) {
            for (StructureScannerBlockEntity.CachedBlockData data : scannedBlocks) {
                // 根据 Scanner 朝向旋转方块状态
                BlockState rotatedState = rotateBlockStateForPreview(data.state(), facing);
                int renderY = upsideDown ? (Math.max(1, rangeY) - 1 - data.y()) : data.y();
                previewLevelLike.setBlockState(new BlockPos(data.x(), renderY, data.z() + 1), rotatedState);
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
    private BlockState rotateBlockStateForPreview(BlockState state, Direction scannerFacing) {
        if (scannerFacing == Direction.NORTH) {
            return state;
        }

        // 处理水平朝向属性（HORIZONTAL_FACING）
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction blockFacing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            Direction rotatedFacing = rotateDirection(blockFacing, scannerFacing);
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotatedFacing);
        }

        // 处理水平朝向属性（HorizontalDirectionalBlock.FACING）
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            Direction blockFacing = state.getValue(HorizontalDirectionalBlock.FACING);
            Direction rotatedFacing = rotateDirection(blockFacing, scannerFacing);
            return state.setValue(HorizontalDirectionalBlock.FACING, rotatedFacing);
        }

        // 处理六向朝向属性（FACING）- 用于活塞、发射器等
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction blockFacing = state.getValue(BlockStateProperties.FACING);
            Direction rotatedFacing = rotateDirection6Way(blockFacing, scannerFacing);
            return state.setValue(BlockStateProperties.FACING, rotatedFacing);
        }

        return state;
    }

    /**
     * 根据 Scanner 朝向旋转方向（水平4向）
     */
    private Direction rotateDirection(Direction blockFacing, Direction scannerFacing) {
        return switch (scannerFacing) {
            case SOUTH -> blockFacing.getOpposite();
            case WEST -> blockFacing.getClockWise();
            case EAST -> blockFacing.getCounterClockWise();
            default -> blockFacing;
        };
    }

    /**
     * 根据 Scanner 朝向旋转方向（六向，包括UP和DOWN）
     * 用于活塞、发射器等方块
     */
    private Direction rotateDirection6Way(Direction blockFacing, Direction scannerFacing) {
        // UP和DOWN不受水平旋转影响
        if (blockFacing == Direction.UP || blockFacing == Direction.DOWN) {
            return blockFacing;
        }

        // 水平方向正常旋转
        return switch (scannerFacing) {
            case SOUTH -> blockFacing.getOpposite();
            case WEST -> blockFacing.getClockWise();
            case EAST -> blockFacing.getCounterClockWise();
            default -> blockFacing;
        };
    }

    @SuppressWarnings("unused")
    private float getFacingYawOffset(Direction scannerFacing) {
        return 270f;
    }

    /**
     * 渲染Structure Scanner的边框（与世界渲染一致）
     */
    private void renderScannerBorder(GuiGraphics guiGraphics, int posX, int posY, Direction facing) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }

        if (this.cachedBlockEntity == null) return;

        // 使用缓存的扫描范围
        int rangeX = this.cachedRangeX;
        int rangeY = this.cachedRangeY;
        int rangeZ = this.cachedRangeZ;

        // 使用选区范围作为缩放基准，忽略 Scanner
        int sizeX = Math.max(1, rangeX);
        int sizeY = Math.max(1, rangeY);

        // 获取缓冲区
        MultiBufferSource.BufferSource buffers = this.minecraft.renderBuffers().bufferSource();
        final VertexConsumer consumer = buffers.getBuffer(RenderType.lines());

        // 设置PoseStack
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // 1. 平移到预览窗口中心
        poseStack.translate(posX, posY, 100);

        // 2. 缩放（与方块渲染保持一致，使用选区范围）
        float scaleX = 80.0f / (sizeX * Mth.SQRT_OF_TWO);
        float scaleY = 80.0f / (float) sizeY;
        float scale = Math.min(scaleY, scaleX);
        poseStack.scale(-scale, -scale, -scale);

        // 3. 平移到中心
        poseStack.translate(-(float) sizeX / 2, -(float) sizeY / 2, 0);

        // 4. 应用X轴旋转
        poseStack.mulPose(Axis.XP.rotationDegrees(this.previewRotationX));

        // 5. Y轴旋转
        float offsetX = (float) -sizeX / 2 + 0.05f;
        float offsetZ = (float) -sizeX / 2 + 1;
        poseStack.translate(-offsetX, 0, -offsetZ);
        // 应用朝向旋转偏移
        float yawOffset = getFacingYawOffset(facing);
        poseStack.mulPose(Axis.YP.rotationDegrees(this.previewRotationY + 45 + yawOffset));
        poseStack.translate(offsetX, 0, offsetZ);

        // 6. 平移Z轴
        poseStack.translate(0, 0, -1);

        // 7. 创建边框形状 - 与世界中渲染的边框完全一致
        // 在预览坐标系中：
        // - Scanner 在 Z=0
        // - 选区从 Z=2 到 Z=rangeZ+1
        // (内部渲染已规整化为正数纵向区间，无需使用负Y向下延伸边框)
        final VoxelShape borderShape = Shapes.create(0.0, 0.0, 2.0, rangeX, rangeY, rangeZ + 2);

        // 8. 渲染边框（青色）
        TooltipRenderHelper.renderOutline(poseStack, consumer, 0, 0, 0, BlockPos.ZERO, borderShape, 0xFF00FFCC);

        buffers.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    /**
     * 鼠标按下事件 - 支持拖拽旋转预览
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

    /**
     * 鼠标释放事件
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isPreviewDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
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
            PacketDistributor.sendToServer(new StructureScannerActionPacket("start"));

            this.isScanMode = false;
            this.modeToggleButton.setSelected(false);
            this.modeToggleButton.setTexture(STOP_TEXTURE);
            // 如果正在扫描（stop 状态），点击后停止扫描
        } else {
            PacketDistributor.sendToServer(new StructureScannerActionPacket("stop"));

            this.isScanMode = true;
            this.modeToggleButton.setSelected(true);
            this.modeToggleButton.setTexture(REDO_TEXTURE);
        }
    }

    /**
     * 确认按钮点击事件
     */
    private void onConfirmClick() {
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) {
            return;
        }

        // 获取输入的结构名称
        String structureName = this.nameInput.getValue().trim();
        if (structureName.isEmpty()) {
            structureName = "structure_" + System.currentTimeMillis();
        }

        // 发送确认数据包到服务器（包含结构名称）
        PacketDistributor.sendToServer(new StructureScannerActionPacket("confirm", structureName));
    }

    /**
     * 文本输入框内容变化响应
     */
    private void onNameInputChanged(String text) {
        // 可以在这里处理文本变化逻辑
    }

    @Override
    public void removed() {
        if (this.previewFbo != null) {
            this.previewFbo.destroyBuffers();
            this.previewFbo = null;
        }
        super.removed();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String string = this.nameInput.getValue();
        this.init(minecraft, width, height);
        this.nameInput.setValue(string);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 当文本框有焦点时，优先处理所有按键输入
        if (this.nameInput.isFocused()) {
            // 拦截ESC键，关闭GUI
            if (keyCode == 256 && this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.closeContainer();
                return true;
            }
            // 其他所有按键都交给文本框处理
            return this.nameInput.keyPressed(keyCode, scanCode, modifiers);
        }

        // 文本框没有焦点时，ESC键关闭GUI
        if (keyCode == 256 && this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
            return true;
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
