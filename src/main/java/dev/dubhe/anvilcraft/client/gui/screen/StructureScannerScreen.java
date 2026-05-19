package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.block.entity.StructureScannerBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.ItemCollectorButton;
import dev.dubhe.anvilcraft.client.gui.component.TextWidget;
import dev.dubhe.anvilcraft.client.gui.component.ToggleButton;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import dev.dubhe.anvilcraft.network.StructureScannerActionPacket;
import dev.dubhe.anvilcraft.util.LevelLike;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StructureScannerScreen extends AbstractContainerScreen<StructureScannerMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "structure_scanner");
    private static final ResourceLocation REDO_TEXTURE = SharedTextures.BUTTON_REDO;
    private static final ResourceLocation STOP_TEXTURE = SharedTextures.BUTTON_STOP;
    
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
            this.leftPos + 97,
            this.topPos + 49,
            20,
            8,
            this.minecraft.font,
            () -> {
                var blockEntity = this.menu.getBlockEntity();
                return Component.literal(blockEntity != null ? blockEntity.getRangeX().get().toString() : "?");
            }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 84, this.topPos + 48, "minus", (b) -> {
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.getRangeX().previous();
                PacketDistributor.sendToServer(new StructureScannerActionPacket("rangeChange", blockEntity.getRangeX().index(), "rangeX"));
            }
        }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
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
            this.leftPos + 97,
            this.topPos + 63,
            20,
            8,
            this.minecraft.font,
            () -> {
                var blockEntity = this.menu.getBlockEntity();
                return Component.literal(blockEntity != null ? blockEntity.getRangeZ().get().toString() : "?");
            }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 84, this.topPos + 62, "minus", (b) -> {
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.getRangeZ().previous();
                PacketDistributor.sendToServer(new StructureScannerActionPacket("rangeChange", blockEntity.getRangeZ().index(), "rangeZ"));
            }
        }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
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
            this.leftPos + 97,
            this.topPos + 77,
            20,
            8,
            this.minecraft.font,
            () -> {
                var blockEntity = this.menu.getBlockEntity();
                return Component.literal(blockEntity != null ? blockEntity.getRangeY().get().toString() : "?");
            }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 84, this.topPos + 76, "minus", (b) -> {
            var blockEntity = this.menu.getBlockEntity();
            if (blockEntity != null) {
                blockEntity.getRangeY().previous();
                PacketDistributor.sendToServer(new StructureScannerActionPacket("rangeChange", blockEntity.getRangeY().index(), "rangeY"));
            }
        }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
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
            16, 16,
            REDO_TEXTURE,
            16, 16,
            (btn) -> this.onModeToggleClick(),
            List.of()
        );
        this.modeToggleButton.setSelected(false);
        this.addRenderableWidget(this.modeToggleButton);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BACKGROUND, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // 根据扫描状态更新按钮
        this.updateModeToggleButton();
        
        // 渲染3D预览
        this.renderPreview(guiGraphics);
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
    }
    
    /**
     * 根据扫描状态更新模式切换按钮
     */
    private void updateModeToggleButton() {
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) {
            return;
        }
        
        // 如果正在扫描，切换为 stop 状态
        if (blockEntity.hasStartedScanning() && !blockEntity.isScanComplete()) {
            if (this.isScanMode) {
                this.isScanMode = false;
                this.modeToggleButton.setSelected(false);
                this.modeToggleButton.setTexture(STOP_TEXTURE);
            }
        // 如果扫描完成，切换回 redo 状态
        } else if (blockEntity.isScanComplete()) {
            if (!this.isScanMode) {
                this.isScanMode = true;
                this.modeToggleButton.setSelected(true);
                this.modeToggleButton.setTexture(REDO_TEXTURE);
            }
        }
    }
    
    /**
     * 渲染3D预览（包含Structure Scanner方块和边框）
     */
    private void renderPreview(GuiGraphics guiGraphics) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        
        // 启用裁剪，限制渲染区域在预览窗口内
        double guiScale = this.minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor(
            (int) (this.previewWindowX * guiScale),
            (int) ((this.minecraft.getWindow().getGuiScaledHeight() - this.previewWindowY - this.previewWindowHeight) * guiScale),
            (int) (this.previewWindowWidth * guiScale),
            (int) (this.previewWindowHeight * guiScale)
        );
        
        // 渲染3D预览
        this.renderPreviewContent(guiGraphics,
            this.previewWindowX + this.previewWindowWidth / 2,
            this.previewWindowY + this.previewWindowHeight / 2 + 5
        );
        
        // 禁用裁剪
        RenderSystem.disableScissor();
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
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) {
            return;
        }
        
        var level = this.minecraft.level;
        var blockState = level.getBlockState(blockEntity.getBlockPos());
        var facing = blockState.getValue(HorizontalDirectionalBlock.FACING);
        
        // 构建并渲染 LevelLike
        LevelLike previewLevelLike = this.buildPreviewLevelLike();
        if (previewLevelLike != null) {
            // 计算选区的实际尺寸（忽略 Scanner）
            int rangeX = blockEntity.getRangeX().get();
            int rangeY = blockEntity.getRangeY().get();
            
            // 使用选区范围作为缩放基准，忽略 Scanner 的影响
            int sizeX = Math.max(1, rangeX);
            int sizeY = Math.max(1, rangeY);
            
            // 应用朝向旋转偏移，让预览根据Scanner的实际朝向旋转
            RenderSupport.renderLevelLikeWithFixedSize(previewLevelLike, guiGraphics,
                posX, posY,
                (float) 80.0, this.previewRotationX, this.previewRotationY + getFacingYawOffset(facing), 
                sizeX, sizeY
            );
        }
        
        // 渲染边框
        this.renderScannerBorder(guiGraphics, posX, posY, facing);
    }
    
    /**
     * 构建预览用的LevelLike实例
     */
    private @Nullable LevelLike buildPreviewLevelLike() {
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null || this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        
        ClientLevel level = this.minecraft.level;
        LevelLike previewLevelLike = new LevelLike(level);
        
        // 获取 Structure Scanner 的实际状态
        BlockState scannerState = this.minecraft.level.getBlockState(blockEntity.getBlockPos());
        Direction facing = scannerState.getValue(HorizontalDirectionalBlock.FACING);
        
        // 获取扫描范围
        int rangeX = blockEntity.getRangeX().get();
        
        // Scanner在预览中的位置：X居中，Y=0，Z=0（选区前面）
        int scannerX = rangeX / 2;
        int scannerY = 0;
        int scannerZ = 0;  // 选区前面
        
        // 放置Scanner（始终渲染），在预览中统一朝北
        previewLevelLike.setBlockStateAlwaysRender(
            new BlockPos(scannerX, scannerY, scannerZ),
            ModBlocks.STRUCTURE_SCANNER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
        );
        
        // 使用缓存的扫描结果渲染方块
        List<StructureScannerBlockEntity.CachedBlockData> scannedBlocks = blockEntity.getScannedBlocks();
        
        if (!scannedBlocks.isEmpty()) {
            for (StructureScannerBlockEntity.CachedBlockData data : scannedBlocks) {
                // 根据 Scanner 朝向旋转方块状态
                BlockState rotatedState = rotateBlockStateForPreview(data.state(), facing);
                previewLevelLike.setBlockState(
                    new BlockPos(data.x(), data.y(), data.z()),
                    rotatedState
                );
            }
        } else if (blockEntity.hasStartedScanning()) {
            org.slf4j.LoggerFactory.getLogger(StructureScannerScreen.class)
                .debug("Preview: scanning in progress but no blocks yet, isScanning={}, currentLayer={}",
                    blockEntity.isScanning() ? "yes" : "no", blockEntity.getCurrentScanLayer());
        }
        
        return previewLevelLike;
    }
    
    /**
     * 根据 Scanner 朝向旋转方块状态
     */
    private BlockState rotateBlockStateForPreview(BlockState state, Direction scannerFacing) {
        if (scannerFacing == Direction.NORTH) {
            return state;
        }
        
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            Direction blockFacing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
            Direction rotatedFacing = rotateDirection(blockFacing, scannerFacing);
            return state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, rotatedFacing);
        }
        
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            Direction blockFacing = state.getValue(HorizontalDirectionalBlock.FACING);
            Direction rotatedFacing = rotateDirection(blockFacing, scannerFacing);
            return state.setValue(HorizontalDirectionalBlock.FACING, rotatedFacing);
        }
        
        return state;
    }
    
    /**
     * 根据 Scanner 朝向旋转方向
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
     * 获取朝向对应的Y轴偏移角度
     */
    @SuppressWarnings("unused")
    private float getFacingYawOffset(Direction scannerFacing) {
        return 270f;
    }
    
    /**
     * 渲染Structure Scanner的边框（与世界渲染一致）
     */
    private void renderScannerBorder(GuiGraphics guiGraphics, int posX, int posY, net.minecraft.core.Direction facing) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
            
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) return;
            
        // 获取扫描范围
        int rangeX = blockEntity.getRangeX().get();
        int rangeY = blockEntity.getRangeY().get();
        int rangeZ = blockEntity.getRangeZ().get();
        
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
        float scaleX = 80.0f / (sizeX * net.minecraft.util.Mth.SQRT_OF_TWO);
        float scaleY = 80.0f / (float) sizeY;
        float scale = Math.min(scaleY, scaleX);
        poseStack.scale(-scale, -scale, -scale);
            
        // 3. 平移到中心
        poseStack.translate(-(float) sizeX / 2, -(float) sizeY / 2, 0);
            
        // 4. 应用X轴旋转
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(this.previewRotationX));
            
        // 5. Y轴旋转
        float offsetX = (float) -sizeX / 2 + 0.05f;
        float offsetZ = (float) -sizeX / 2 + 1;
        poseStack.translate(-offsetX, 0, -offsetZ);
        // 应用朝向旋转偏移
        float yawOffset = getFacingYawOffset(facing);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(this.previewRotationY + 45 + yawOffset));
        poseStack.translate(offsetX, 0, offsetZ);
            
        // 6. 平移Z轴
        poseStack.translate(0, 0, -1);
            
        // 7. 创建边框形状 - 与世界中渲染的边框完全一致
        // 在预览坐标系中：
        // - Scanner 在 Z=0
        // - 选区从 Z=1 到 Z=rangeZ
        // 所以边框应该从 Z=1 到 Z=rangeZ+1
        final VoxelShape borderShape = Shapes.create(
            0.0, 0.0, 1.0,
            rangeX, rangeY, rangeZ + 1
        );
            
        // 8. 渲染边框（青色）
        TooltipRenderHelper.renderOutline(
            poseStack,
            consumer,
            0, 0, 0,
            BlockPos.ZERO,
            borderShape,
            0xFF00FFCC
        );
            
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
        
        // 否则让父类处理（按钮点击等）
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
            this.previewRotationX = Math.max(MIN_ROTATION_X, Math.min(MAX_ROTATION_X, this.previewRotationX));
            
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
        return mouseX >= this.previewWindowX && mouseX < this.previewWindowX + this.previewWindowWidth
            && mouseY >= this.previewWindowY && mouseY < this.previewWindowY + this.previewWindowHeight;
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
}
