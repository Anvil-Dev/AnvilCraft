package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.client.gui.component.ItemCollectorButton;
import dev.dubhe.anvilcraft.client.gui.component.TextWidget;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import dev.dubhe.anvilcraft.util.LevelLike;
import net.minecraft.client.Minecraft;
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
import org.jetbrains.annotations.Nullable;

public class StructureScannerScreen extends AbstractContainerScreen<StructureScannerMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "structure_scanner");
    
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
            () -> Component.literal(this.menu.getBlockEntity().getRangeX().get().toString())
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 84, this.topPos + 48, "minus", (b) -> {
            this.menu.getBlockEntity().getRangeX().previous();
            this.menu.getBlockEntity().getRangeX().notifyServer();
        }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 122, this.topPos + 48, "add", (b) -> {
            this.menu.getBlockEntity().getRangeX().next();
            this.menu.getBlockEntity().getRangeX().notifyServer();
        }
        ));
        
        // 添加Z轴范围控制按钮和数值显示
        this.addRenderableWidget(new TextWidget(
            this.leftPos + 97,
            this.topPos + 63,
            20,
            8,
            this.minecraft.font,
            () -> Component.literal(this.menu.getBlockEntity().getRangeZ().get().toString())
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 84, this.topPos + 62, "minus", (b) -> {
            this.menu.getBlockEntity().getRangeZ().previous();
            this.menu.getBlockEntity().getRangeZ().notifyServer();
        }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 122, this.topPos + 62, "add", (b) -> {
            this.menu.getBlockEntity().getRangeZ().next();
            this.menu.getBlockEntity().getRangeZ().notifyServer();
        }
        ));
        
        // 添加Y轴范围控制按钮和数值显示
        this.addRenderableWidget(new TextWidget(
            this.leftPos + 97,
            this.topPos + 77,
            20,
            8,
            this.minecraft.font,
            () -> Component.literal(this.menu.getBlockEntity().getRangeY().get().toString())
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 84, this.topPos + 76, "minus", (b) -> {
            this.menu.getBlockEntity().getRangeY().previous();
            this.menu.getBlockEntity().getRangeY().notifyServer();
        }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 122, this.topPos + 76, "add", (b) -> {
            this.menu.getBlockEntity().getRangeY().next();
            this.menu.getBlockEntity().getRangeY().notifyServer();
        }
        ));
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
        
        // 渲染3D预览
        this.renderPreview(guiGraphics);
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
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
            RenderSupport.renderLevelLikeWithFixedSize(previewLevelLike, guiGraphics,
                posX, posY,
                (float) 80.0, this.previewRotationX, this.previewRotationY + getFacingYawOffset(facing), 3, 3
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
        
        // Structure Scanner 位置：居中（1, 1, 1）
        // 在预览中统一朝北
        previewLevelLike.setBlockStateAlwaysRender(
            new BlockPos(1, 1, 1),
            ModBlocks.STRUCTURE_SCANNER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
        );
        
        return previewLevelLike;
    }
    
    /**
     * 获取朝向对应的Y轴偏移角度
     */
    private float getFacingYawOffset(Direction facing) {
        return switch (facing) {
            case NORTH -> 0f;
            case EAST -> 90f;
            case SOUTH -> 180f;
            case WEST -> 270f;
            default -> 0f;
        };
    }
    
    /**
     * 渲染Structure Scanner的边框（紧贴背后）
     */
    private void renderScannerBorder(GuiGraphics guiGraphics, int posX, int posY, net.minecraft.core.Direction facing) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        
        // 获取缓冲区
        MultiBufferSource.BufferSource buffers = this.minecraft.renderBuffers().bufferSource();
        final VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
        
        // 设置PoseStack
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        
        // 1. 平移到预览窗口中心
        poseStack.translate(posX, posY, 100);
        
        // 2. 缩放（与智能放置器一致）
        float scaleX = 80.0f / (5 * net.minecraft.util.Mth.SQRT_OF_TWO);
        float scaleY = 80.0f / 5.0f;
        float scale = Math.min(scaleY, scaleX);
        poseStack.scale(-scale, -scale, -scale);
        
        // 3. 平移到中心
        poseStack.translate(-2.5f, -2.5f, 0);
        
        // 4. 应用X轴旋转
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(this.previewRotationX));
        
        // 5. Y轴旋转
        float offsetX = -2.5f + 0.05f;
        float offsetZ = -2.5f + 1;
        poseStack.translate(-offsetX, 0, -offsetZ);
        // 根据方块朝向调整初始旋转角度
        float yawOffset = switch (facing) {
            case NORTH -> 0f;
            case EAST -> 90f;
            case SOUTH -> 180f;
            case WEST -> 270f;
            default -> 0f;
        };
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(this.previewRotationY + 45 + yawOffset));
        poseStack.translate(offsetX, 0, offsetZ);
        
        // 6. 平移Z轴（与方块保持一致）
        poseStack.translate(0, 0, -1);
        
        // 7. 创建边框形状（与世界内渲染完全一致）
        // 世界内: behindPos 在 scannerPos 背后2格，边框是 behindPos ± 1（XZ轴），Y轴 +0 到 +3
        // GUI预览: Scanner在(1,1,1)，背后2格是(1,1,-1)
        // 边框: X(0到2), Y(1到4), Z(-2到0) - 3x3x3
        final VoxelShape borderShape = Shapes.create(0.0, 1.0, -2.0, 3.0, 4.0, 0.0);
        
        // 8. 渲染边框（青色，与智能放置器一致）
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
        if (this.isMouseInPreviewWindow(mouseX, mouseY)) {
            this.isPreviewDragging = true;
            this.lastMouseX = (int) mouseX;
            this.lastMouseY = (int) mouseY;
            return true;
        }
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
}
