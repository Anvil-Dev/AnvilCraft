package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.block.StructureScannerBlock;
import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.ItemCollectorButton;
import dev.dubhe.anvilcraft.client.gui.component.TextWidget;
import dev.dubhe.anvilcraft.client.gui.component.TexturedButton;
import dev.dubhe.anvilcraft.client.gui.component.ToggleButton;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import dev.dubhe.anvilcraft.network.StructureScannerActionPacket;
import dev.dubhe.anvilcraft.network.StructureScannerActionPacket.Action;
import dev.dubhe.anvilcraft.network.StructureScannerActionPacket.RangeAxis;
import dev.dubhe.anvilcraft.util.LevelLike;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StructureScannerScreen extends AbstractContainerScreen<StructureScannerMenu> {
    private static final Identifier BACKGROUND = SharedTextures.STRUCTURE_SCANNER_BACKGROUND;
    private static final Identifier REDO_TEXTURE = SharedTextures.REDO;
    private static final Identifier STOP_TEXTURE = SharedTextures.STOP;
    private static final Identifier CONFIRM_TEXTURE = SharedTextures.CONFIRM;
    private static final Identifier STRUCTURE_TOOL_LOCKED_TEXTURE = SharedTextures.STRUCTURE_TOOL_LOCKED;

    // 预览窗口位置和尺寸
    private int previewWindowX;
    private int previewWindowY;
    private final int previewWindowWidth = 112;
    private final int previewWindowHeight = 88;

    // 预览旋转角度
    private float previewRotationY = 45.0f;
    private float previewRotationX = 30.0f;
    private static final float MIN_ROTATION_X = -60.0f;
    private static final float MAX_ROTATION_X = 60.0f;
    private static final float ROTATION_SENSITIVITY = 0.5f;

    // 鼠标拖拽状态
    private boolean isPreviewDragging = false;
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    // 模式切换按钮
    @Nullable
    private ToggleButton modeToggleButton;
    private boolean isScanMode = true;

    // 文本输入框
    @Nullable
    private EditBox nameInput;

    // 缓存数据
    @Nullable
    private StructureScannerBlockEntity cachedBlockEntity;
    private boolean cachedHasDisk;
    private StructureScannerBlockEntity.InfoStatus cachedInfoStatus = StructureScannerBlockEntity.InfoStatus.READY;
    private boolean cachedIsScanComplete;
    private boolean cachedHasStartedScanning;
    private int cachedRangeX = -1;
    private int cachedRangeY = -1;
    private int cachedRangeZ = -1;

    // 预览缓存
    @Nullable
    private LevelLike cachedPreviewLevelLike;
    private Direction cachedPreviewFacing = Direction.NORTH;

    // 扫描数据版本追踪（用于缓存失效）
    private int cachedScannedBlocksSize = -1;

    public StructureScannerScreen(StructureScannerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 256, 201);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.getImageWidth() - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;

        // 初始化预览窗口位置
        this.previewWindowX = this.leftPos + 136;
        this.previewWindowY = this.topPos + 18;

        // 添加X轴范围控制按钮和数值显示
        this.addRenderableWidget(new TextWidget(
            this.leftPos + 97, this.topPos + 49, 20, 8, this.minecraft.font, () -> {
            var blockEntity = this.menu.getBlockEntity();
            return Component.literal(blockEntity != null ? blockEntity.getRangeX().get().toString() : "?");
        }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 84, this.topPos + 48, "minus",
            _ -> {
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.getRangeX().previous();
                ClientPacketDistributor.sendToServer(
                    new StructureScannerActionPacket(Action.RANGE_CHANGE, blockEntity.getRangeX().index(), RangeAxis.X));
            }
        }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 122, this.topPos + 48, "add",
            _ -> {
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.getRangeX().next();
                ClientPacketDistributor.sendToServer(
                    new StructureScannerActionPacket(Action.RANGE_CHANGE, blockEntity.getRangeX().index(), RangeAxis.X));
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
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 84, this.topPos + 62, "minus",
            _ -> {
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.getRangeZ().previous();
                ClientPacketDistributor.sendToServer(
                    new StructureScannerActionPacket(Action.RANGE_CHANGE, blockEntity.getRangeZ().index(), RangeAxis.Z)
                );
            }
        }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 122, this.topPos + 62, "add",
            _ -> {
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.getRangeZ().next();
                ClientPacketDistributor.sendToServer(
                    new StructureScannerActionPacket(Action.RANGE_CHANGE, blockEntity.getRangeZ().index(), RangeAxis.Z)
                );
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
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 84, this.topPos + 76, "minus",
            _ -> {
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.getRangeY().previous();
                ClientPacketDistributor.sendToServer(
                    new StructureScannerActionPacket(Action.RANGE_CHANGE, blockEntity.getRangeY().index(), RangeAxis.Y)
                );
            }
        }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 122, this.topPos + 76, "add",
            _ -> {
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.getRangeY().next();
                ClientPacketDistributor.sendToServer(
                    new StructureScannerActionPacket(Action.RANGE_CHANGE, blockEntity.getRangeY().index(), RangeAxis.Y)
                );
            }
        }
        ));

        // 添加模式切换按钮
        this.modeToggleButton = new ToggleButton(
            this.leftPos + 232,
            this.topPos + 119,
            16,
            16,
            REDO_TEXTURE,
            16,
            32,
            (_) -> this.onModeToggleClick(),
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
            (_) -> this.onConfirmClick()
        );
        this.addRenderableWidget(confirmButton);

        // 添加文本输入框
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
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    /// 渲染半透明的物品虚影
    private void renderMaskedItem(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        final int maskColor = 0x99777777;
        g.item(stack, x, y);
        g.fill(x, y, x + 16, y + 16, maskColor);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines
            .GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, 0, 0, this.getImageWidth(), this.getImageHeight(), 256, 256);

        // 渲染磁盘槽位的虚影（当槽位为空时）
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null && blockEntity.getDiskInventory().getItem(0).isEmpty()) {
            ItemStack diskStack = ModItems.STRUCTURE_DISK.get().getDefaultInstance();
            if (!diskStack.isEmpty()) {
                int diskSlotX = this.leftPos + 8;
                int diskSlotY = this.topPos + 112;
                this.renderMaskedItem(graphics, diskStack, diskSlotX, diskSlotY);
            }
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);

        // 更新缓存数据
        this.updateCache();

        // 根据扫描状态更新按钮
        this.updateModeToggleButton();

        // 根据磁盘状态更新文本框可编辑状态
        this.updateNameInputEditable();

        // 渲染3D预览
        this.renderPreview(graphics);

        // 渲染信息栏
        this.renderInfoPanel(graphics);

        // 渲染STRUCTURE_TOOL_LOCKED贴图
        graphics.blit(RenderPipelines
            .GUI_TEXTURED, STRUCTURE_TOOL_LOCKED_TEXTURE, this.leftPos + 6, this.topPos + 18, 0, 0, 126, 26, 126, 26);

        // 收集并渲染所有tooltip
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

        // 统一渲染所有tooltip
        for (TooltipRenderInfo info : tooltipsToRender) {
            graphics.setTooltipForNextFrame(
                info.tooltip.stream().map(Component::getVisualOrderText).toList(),
                info.x,
                info.y
            );
        }
    }

    /// 更新缓存数据，避免每帧重复获取
    private void updateCache() {
        var blockEntity = this.menu.getBlockEntity();

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

        if (blockEntity == null) return;

        boolean newHasDisk = !blockEntity.getDiskInventory().getItem(0).isEmpty();
        if (newHasDisk != this.cachedHasDisk) {
            this.cachedHasDisk = newHasDisk;
        }

        StructureScannerBlockEntity.InfoStatus newInfoStatus = blockEntity.getInfoStatus();
        if (newInfoStatus != this.cachedInfoStatus) {
            this.cachedInfoStatus = newInfoStatus;
        }

        boolean newIsScanComplete = blockEntity.isScanComplete();
        if (newIsScanComplete != this.cachedIsScanComplete) {
            this.cachedIsScanComplete = newIsScanComplete;
        }

        boolean newHasStartedScanning = blockEntity.hasStartedScanning();
        if (newHasStartedScanning != this.cachedHasStartedScanning) {
            this.cachedHasStartedScanning = newHasStartedScanning;
        }

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
            this.cachedPreviewLevelLike = null;
        }

        int currentScannedBlocksSize = blockEntity.getScannedBlocks().size();
        if (currentScannedBlocksSize != this.cachedScannedBlocksSize) {
            this.cachedScannedBlocksSize = currentScannedBlocksSize;
            this.cachedPreviewLevelLike = null;
        }
    }

    /// 渲染信息栏
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void renderInfoPanel(GuiGraphicsExtractor graphics) {
        if (!this.cachedHasDisk) return;

        StructureScannerBlockEntity.InfoStatus status = this.cachedInfoStatus;

        int infoX = this.leftPos + 9;
        int infoY = this.topPos + 52;

        graphics.pose().pushMatrix();
        graphics.pose().translate(infoX, infoY);
        graphics.pose().scale(0.75f, 0.75f);
        graphics.text(this.font, Component.translatable("screen.anvilcraft.structure_scanner.info_title"), 0, 0, 0xFFFFFFFF, false);
        graphics.pose().popMatrix();

        int statusY = infoY + 10;

        switch (status) {
            case READY -> {
                if (this.cachedIsScanComplete) {
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(infoX, statusY);
                    graphics.text(
                        this.font,
                        Component.translatable("screen.anvilcraft.structure_scanner.ready"),
                        0,
                        0,
                        0xFF40FF40,
                        false
                    );
                    graphics.pose().popMatrix();
                }
            }
            case LARGE_STRUCTURE, UNKNOWN_BLOCKS, TOO_LARGE -> {
                boolean isWarning = status == StructureScannerBlockEntity.InfoStatus.LARGE_STRUCTURE;
                int iconColor = isWarning ? 0xFFFFFF55 : 0xFFFF5555;
                float iconScale = 1.5f;

                graphics.pose().pushMatrix();
                graphics.pose().translate(infoX, statusY);
                graphics.pose().scale(iconScale, iconScale);
                int textOffsetX = 18;
                graphics.text(this.font, "!", textOffsetX, 0, iconColor, false);
                graphics.pose().popMatrix();
            }
            default -> {

            }
        }
    }

    @Nullable
    private TooltipRenderInfo collectInfoPanelTooltip(int mouseX, int mouseY) {
        if (!this.cachedHasDisk) return null;

        StructureScannerBlockEntity.InfoStatus status = this.cachedInfoStatus;

        if (
            status != StructureScannerBlockEntity.InfoStatus.LARGE_STRUCTURE
            && status != StructureScannerBlockEntity.InfoStatus.UNKNOWN_BLOCKS
            && status != StructureScannerBlockEntity.InfoStatus.TOO_LARGE
        ) {
            return null;
        }

        int infoX = this.leftPos + 9;
        int infoY = this.topPos + 52;
        int statusY = infoY + 10;

        float iconScale = 1.5f;
        int textOffsetX = 18;

        int scaledWidth = (int) (8 * iconScale);
        int scaledHeight = (int) (10 * iconScale);
        int hoverStartX = infoX + (int) (textOffsetX * iconScale);

        if (mouseX >= hoverStartX && mouseX < hoverStartX + scaledWidth && mouseY >= statusY && mouseY < statusY + scaledHeight) {
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

    /// 根据磁盘状态更新文本框可编辑状态
    private void updateNameInputEditable() {
        if (this.nameInput == null) return;

        this.nameInput.setEditable(this.cachedHasDisk);

        if (!this.cachedHasDisk && this.nameInput.isFocused()) {
            this.nameInput.setFocused(false);
        }
    }

    /// 根据扫描状态更新模式切换按钮
    private void updateModeToggleButton() {
        if (this.modeToggleButton == null) return;

        if (this.cachedHasStartedScanning && !this.cachedIsScanComplete) {
            if (this.isScanMode) {
                this.isScanMode = false;
                this.modeToggleButton.setSelected(false);
                this.modeToggleButton.setTexture(STOP_TEXTURE);
            }
        } else if (this.cachedIsScanComplete) {
            if (!this.isScanMode) {
                this.isScanMode = true;
                this.modeToggleButton.setSelected(true);
                this.modeToggleButton.setTexture(REDO_TEXTURE);
            }
        }
    }

    /// 渲染3D预览
    private void renderPreview(GuiGraphicsExtractor graphics) {
        if (this.minecraft.level == null) return;

        // 使用 scissor 裁剪预览窗口区域
        graphics.enableScissor(
            this.previewWindowX,
            this.previewWindowY,
            this.previewWindowX + this.previewWindowWidth,
            this.previewWindowY + this.previewWindowHeight
        );

        this.renderPreviewContent(
            graphics,
            this.previewWindowX + this.previewWindowWidth / 2,
            this.previewWindowY + this.previewWindowHeight / 2 + 5
        );

        graphics.disableScissor();
    }

    /// 渲染3D预览内容
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void renderPreviewContent(GuiGraphicsExtractor graphics, int posX, int posY) {
        if (this.minecraft.level == null) return;
        if (this.cachedBlockEntity == null) return;

        var level = this.minecraft.level;
        var blockState = level.getBlockState(this.cachedBlockEntity.getBlockPos());
        var facing = blockState.getValue(HorizontalDirectionalBlock.FACING);

        // 构建并渲染 LevelLike（使用缓存）
        LevelLike previewLevelLike = this.buildPreviewLevelLike(facing);
        if (previewLevelLike != null) {
            this.renderPreviewWithFixedSize(
                previewLevelLike,
                graphics,
                this.previewRotationX,
                this.previewRotationY + this.getFacingYawOffset(facing)
            );
        }

        // TODO: 渲染边框
        // this.renderScannerBorder(graphics, posX, posY, facing);
    }

    /// 以固定大小渲染 LevelLike 预览
    private void renderPreviewWithFixedSize(
        LevelLike level,
        GuiGraphicsExtractor graphics,
        float rotationX,
        float rotationY
    ) {
        var minPos = level.getMinPos();
        var maxPos = level.getMaxPos();
        if (minPos.isEmpty() || maxPos.isEmpty()) return;

        PoseStack poseStack = new PoseStack();

        poseStack.mulPose(Axis.XP.rotationDegrees(rotationX));

        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY + 45));

        poseStack.translate(-this.cachedRangeX / 2f + 1, -this.cachedRangeY / 2f + 1, -this.cachedRangeZ / 2f);

        int maxSize = Math.max(1, Math.max(this.cachedRangeX, Math.max(this.cachedRangeY, this.cachedRangeZ)));
        float scale = 55.0f / maxSize;

        GuiRenderExtras.submitStructure(
            graphics,
            level,
            minPos.get(),
            maxPos.get(),
            this.previewWindowX,
            this.previewWindowY,
            this.previewWindowX + this.previewWindowWidth,
            this.previewWindowY + this.previewWindowHeight,
            scale,
            true,
            false,
            poseStack
        );
    }

    /// 构建预览用的LevelLike实例（带缓存）
    private @Nullable LevelLike buildPreviewLevelLike(Direction facing) {
        if (this.minecraft.level == null || this.cachedBlockEntity == null) {
            return null;
        }

        if (this.cachedPreviewFacing == facing) {
            return this.cachedPreviewLevelLike;
        }

        ClientLevel level = this.minecraft.level;
        LevelLike previewLevelLike = new LevelLike(level);

        int rangeX = this.cachedRangeX;
        int rangeY = this.cachedRangeY;

        boolean upsideDown = false;
        if (this.cachedBlockEntity.getBlockState().hasProperty(StructureScannerBlock.UPSIDE_DOWN)) {
            upsideDown = this.cachedBlockEntity.getBlockState().getValue(StructureScannerBlock.UPSIDE_DOWN);
        }

        // Scanner在预览中的位置：X居中，Y=0，Z=0
        int scannerX = rangeX / 2;
        int scannerY = upsideDown ? Math.max(1, rangeY) - 1 : 0;
        int scannerZ = 0;

        previewLevelLike.setBlockState(
            new BlockPos(scannerX, scannerY, scannerZ),
            ModBlocks.STRUCTURE_SCANNER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(StructureScannerBlock.UPSIDE_DOWN, upsideDown)
        );

        List<StructureScannerBlockEntity.CachedBlockData> scannedBlocks = this.cachedBlockEntity.getScannedBlocks();

        if (!scannedBlocks.isEmpty()) {
            for (StructureScannerBlockEntity.CachedBlockData data : scannedBlocks) {
                BlockState rotatedState = this.rotateBlockStateForPreview(data.state(), facing);
                int renderY = upsideDown ? (Math.max(1, rangeY) - 1 - data.y()) : data.y();
                previewLevelLike.setBlockState(new BlockPos(data.x(), renderY, data.z() + 1), rotatedState);
            }
        }

        this.cachedPreviewLevelLike = previewLevelLike;
        this.cachedPreviewFacing = facing;

        return previewLevelLike;
    }

    /// 根据 Scanner 朝向旋转方块状态
    private BlockState rotateBlockStateForPreview(BlockState state, Direction scannerFacing) {
        if (scannerFacing == Direction.NORTH) return state;

        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction blockFacing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            Direction rotatedFacing = this.rotateDirection(blockFacing, scannerFacing);
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotatedFacing);
        }

        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            Direction blockFacing = state.getValue(HorizontalDirectionalBlock.FACING);
            Direction rotatedFacing = this.rotateDirection(blockFacing, scannerFacing);
            return state.setValue(HorizontalDirectionalBlock.FACING, rotatedFacing);
        }

        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction blockFacing = state.getValue(BlockStateProperties.FACING);
            Direction rotatedFacing = this.rotateDirection6Way(blockFacing, scannerFacing);
            return state.setValue(BlockStateProperties.FACING, rotatedFacing);
        }

        return state;
    }

    private Direction rotateDirection(Direction blockFacing, Direction scannerFacing) {
        return switch (scannerFacing) {
            case SOUTH -> blockFacing.getOpposite();
            case WEST -> blockFacing.getClockWise();
            case EAST -> blockFacing.getCounterClockWise();
            default -> blockFacing;
        };
    }

    private Direction rotateDirection6Way(Direction blockFacing, Direction scannerFacing) {
        if (blockFacing == Direction.UP || blockFacing == Direction.DOWN) return blockFacing;
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

    /// 渲染Scanner边框
    private void renderScannerBorder(GuiGraphicsExtractor graphics, int posX, int posY, Direction facing) {
        if (this.minecraft.level == null) return;

        int rangeX = this.cachedRangeX;
        int rangeY = this.cachedRangeY;
        int rangeZ = this.cachedRangeZ;

        int sizeX = Math.max(1, rangeX);
        int sizeY = Math.max(1, rangeY);

        MultiBufferSource.BufferSource buffers = this.minecraft.renderBuffers().bufferSource();

        PoseStack poseStack = new PoseStack();

        // 1. 平移到预览窗口中心
        poseStack.translate(posX, posY, 100);

        // 2. 缩放
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
        float yawOffset = this.getFacingYawOffset(facing);
        poseStack.mulPose(Axis.YP.rotationDegrees(this.previewRotationY + 45 + yawOffset));
        poseStack.translate(offsetX, 0, offsetZ);

        // 6. 平移Z轴
        poseStack.translate(0, 0, -1);

        // 7. 创建边框形状
        VoxelShape borderShape = Shapes.create(0.0, 0.0, 2.0, rangeX, rangeY, rangeZ + 2);

        // 8. 渲染边框
        VertexConsumer consumer = buffers.getBuffer(RenderTypes.LINES);
        TooltipRenderHelper.renderOutline(poseStack, consumer, 0, 0, 0, BlockPos.ZERO, borderShape, 0xFF00FFCC);

        buffers.endBatch(RenderTypes.LINES);
    }

    /// 鼠标按下事件 - 支持拖拽旋转预览
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (this.isMouseInPreviewWindow(mouseX, mouseY)) {
            this.isPreviewDragging = true;
            this.lastMouseX = (int) mouseX;
            this.lastMouseY = (int) mouseY;
            return true;
        }

        return super.mouseClicked(event, handled);
    }

    /// 鼠标释放事件
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isPreviewDragging = false;
        return super.mouseReleased(event);
    }

    /// 鼠标拖拽事件 - 旋转预览
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (this.isPreviewDragging) {
            int currentMouseX = (int) mouseX;
            int currentMouseY = (int) mouseY;
            float deltaX = currentMouseX - this.lastMouseX;
            float deltaY = currentMouseY - this.lastMouseY;

            this.previewRotationY += deltaX * ROTATION_SENSITIVITY;
            this.previewRotationX += deltaY * ROTATION_SENSITIVITY;
            this.previewRotationX = Math.clamp(this.previewRotationX, MIN_ROTATION_X, MAX_ROTATION_X);

            this.lastMouseX = currentMouseX;
            this.lastMouseY = currentMouseY;
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    private boolean isMouseInPreviewWindow(double mouseX, double mouseY) {
        return mouseX >= this.previewWindowX
               && mouseX < this.previewWindowX + this.previewWindowWidth
               && mouseY >= this.previewWindowY
               && mouseY < this.previewWindowY + this.previewWindowHeight;
    }

    /// 模式切换按钮点击事件
    private void onModeToggleClick() {
        if (this.modeToggleButton == null) return;

        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) return;

        if (this.isScanMode) {
            ClientPacketDistributor.sendToServer(new StructureScannerActionPacket(Action.START));

            this.isScanMode = false;
            this.modeToggleButton.setSelected(false);
            this.modeToggleButton.setTexture(STOP_TEXTURE);
        } else {
            ClientPacketDistributor.sendToServer(new StructureScannerActionPacket(Action.STOP));

            this.isScanMode = true;
            this.modeToggleButton.setSelected(true);
            this.modeToggleButton.setTexture(REDO_TEXTURE);
        }
    }

    /// 确认按钮点击事件
    private void onConfirmClick() {
        if (this.nameInput == null) return;

        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) return;

        String structureName = this.nameInput.getValue().trim();
        if (structureName.isEmpty()) {
            structureName = "structure_" + System.currentTimeMillis();
        }

        ClientPacketDistributor.sendToServer(new StructureScannerActionPacket(Action.CONFIRM, structureName));
    }

    private void onNameInputChanged(String text) {
    }

    @Override
    public void removed() {
        super.removed();
    }

    public void resize(int width, int height) {
        if (this.nameInput == null) {
            this.init(width, height);
            return;
        }
        String string = this.nameInput.getValue();
        this.init(width, height);
        this.nameInput.setValue(string);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.nameInput != null && this.nameInput.isFocused()) {
            if (event.key() == 256 && this.minecraft.player != null) {
                this.minecraft.player.closeContainer();
                return true;
            }
            return this.nameInput.keyPressed(event);
        }

        if (event.key() == 256 && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
            return true;
        }

        return super.keyPressed(event);
    }

    /// Tooltip渲染信息记录类
    private record TooltipRenderInfo(
        Font font, List<Component> tooltip, int x, int y
    ) {
    }
}
