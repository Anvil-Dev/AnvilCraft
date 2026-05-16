package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.client.gui.component.ToggleButton;
import dev.dubhe.anvilcraft.client.gui.component.TriStateButton;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.SmartBlockPlacerMenu;
import dev.dubhe.anvilcraft.network.SmartBlockPlacerLayerPacket;
import dev.dubhe.anvilcraft.network.SmartBlockPlacerModePacket;
import dev.dubhe.anvilcraft.network.SmartBlockPlacerPositionPacket;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("checkstyle:LineLength")
public class SmartBlockPlacerScreen extends AbstractContainerScreen<SmartBlockPlacerMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "smart_block_placer");

    private static final ResourceLocation[] LAYER_DEFAULT = {
        SharedTextures.textureGui("machine/smart_block_placer/layer_1"),
        SharedTextures.textureGui("machine/smart_block_placer/layer_2"),
        SharedTextures.textureGui("machine/smart_block_placer/layer_3"),
        SharedTextures.textureGui("machine/smart_block_placer/layer_4"),
        SharedTextures.textureGui("machine/smart_block_placer/layer_5")
    };

    private static final ResourceLocation POSITION_SELECT = SharedTextures.textureGui("machine/smart_block_placer/position_select");

    private static final ResourceLocation LAYER_ALL = SharedTextures.textureGui("machine/smart_block_placer/layer_all");
    private static final ResourceLocation LAYER_SINGLE = SharedTextures.textureGui("machine/smart_block_placer/layer_single");

    private static final ResourceLocation PICKUP_MODE = SharedTextures.textureGui("machine/smart_block_placer/pickup_mode");
    private static final ResourceLocation MOVE_MODE = SharedTextures.textureGui("machine/smart_block_placer/move_mode");

    private final List<TriStateButton> layerButtons = new ArrayList<>();
    private final TriStateButton[][] positionButtons = new TriStateButton[5][5];
    private ToggleButton layerModeButton;  // 分层显示切换按钮
    private ToggleButton operationModeButton;  // 取物/移动模式切换按钮
    private int currentViewLayer = 0;
    private Map<Integer, Set<Integer>> layerPositions = new HashMap<>();
    private boolean showAllLayers = true;
    private boolean isPickupMode = true;

    private Boolean dragTargetState = null;

    private int previewWindowX;
    private int previewWindowY;
    private final int previewWindowWidth = 112;
    private final int previewWindowHeight = 88;
    private boolean isPreviewDragging = false;
    private int lastMouseX = 0;
    private int lastMouseY = 0;
    private float previewRotationY = 45.0f;
    private float previewRotationX = -30.0f;
    private static final float MIN_ROTATION_X = -60.0f;
    private static final float MAX_ROTATION_X = 0.0f;
    private static final float ROTATION_SENSITIVITY = 0.5f;

    private static final int PREVIEW_BLOCK_SWITCH_INTERVAL = 80;
    
    // LevelLike 缓存
    private LevelLike cachedPreviewLevelLike = null;
    private Map<Integer, Set<Integer>> cachedLayerPositions = new HashMap<>();
    private int cachedViewLayer = -1;
    private boolean cachedShowAllLayers = true;
    private boolean cachedPickupMode = true;

    public SmartBlockPlacerScreen(SmartBlockPlacerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 201;
    }

    @SuppressWarnings("checkstyle:LocalVariableName")
    @Override
    protected void init() {
        super.init();
        this.titleLabelY = Constant.SCREEN_TITLE_Y;

        if (this.menu.getBlockEntity() != null) {
            this.currentViewLayer = this.menu.getBlockEntity().getSelectedLayer();
            this.layerPositions = this.menu.getBlockEntity().getLayerPositions();
            this.isPickupMode = this.menu.getBlockEntity().isPickupMode();
        }

        this.previewWindowX = this.leftPos + 136;
        this.previewWindowY = this.topPos + 18;

        this.initLayerButtons();
        this.initPositionButtons();
        this.initLayerModeButton();
        this.initOperationModeButton();
    }
    
    private void initLayerButtons() {
        this.layerButtons.clear();
        int buttonX = this.leftPos + 8;
        int buttonStartY = this.topPos + 18;

        for (int i = 4; i >= 0; i--) {
            int index = i;
            TriStateButton button = new TriStateButton(
                buttonX,
                buttonStartY + (4 - i) * 18,
                16, 16,
                LAYER_DEFAULT[i],
                16, 16,
                (btn) -> this.onLayerButtonClick(index),
                List.of(Component.translatable("screen.anvilcraft.smart_block_placer.layer." + (i + 1)))
            );
            button.setSelected(i == this.currentViewLayer);
            this.layerButtons.add(button);
            this.addRenderableWidget(button);
        }
    }
    
    private void initPositionButtons() {
        int gridStartX = this.leftPos + 33;
        int gridStartY = this.topPos + 18;
        Set<Integer> currentPositions = this.layerPositions.getOrDefault(this.currentViewLayer, new HashSet<>());

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int positionIndex = row * 5 + col;
                boolean isSelected = currentPositions.contains(positionIndex);

                TriStateButton button = this.createPositionButton(row, col, positionIndex, gridStartX, gridStartY, isSelected);
                this.positionButtons[row][col] = button;
                this.addRenderableWidget(button);
            }
        }
    }
    
    private void initLayerModeButton() {
        // 右侧预留区域第一个按钮位置（物品栏右侧）
        int buttonX = this.leftPos + 232;  // 物品栏最右侧(210) + 18像素间距
        int buttonY = this.topPos + 112;   // 与主物品栏第一行对齐

        this.layerModeButton = new ToggleButton(
            buttonX, buttonY, 16, 16,
            this.showAllLayers ? LAYER_ALL : LAYER_SINGLE,
            16, 16,
            (btn) -> this.onLayerModeButtonClick(),
            List.of(this.getLayerModeTooltip())
        );
        this.layerModeButton.setSelected(this.showAllLayers);
        this.addRenderableWidget(this.layerModeButton);
    }
    
    private void initOperationModeButton() {
        // 在分层显示切换按钮下方
        int buttonX = this.leftPos + 232;  // 与layerModeButton对齐
        int buttonY = this.topPos + 130;   // layerModeButton的Y坐标(112) + 18像素间距

        this.operationModeButton = new ToggleButton(
            buttonX, buttonY, 16, 16,
            this.isPickupMode ? PICKUP_MODE : MOVE_MODE,
            16, 16,
            (btn) -> this.onOperationModeButtonClick(),
            List.of(this.getOperationModeTooltip())
        );
        this.operationModeButton.setSelected(this.isPickupMode);
        this.addRenderableWidget(this.operationModeButton);
    }
    
    private Component getLayerModeTooltip() {
        if (this.showAllLayers) {
            return Component.translatable("screen.anvilcraft.smart_block_placer.layer_mode.all");
        } else {
            return Component.translatable("screen.anvilcraft.smart_block_placer.layer_mode.single",
                this.currentViewLayer + 1, 5);
        }
    }
    
    private Component getOperationModeTooltip() {
        if (this.isPickupMode) {
            return Component.translatable("screen.anvilcraft.smart_block_placer.operation_mode.pickup");
        } else {
            return Component.translatable("screen.anvilcraft.smart_block_placer.operation_mode.move");
        }
    }
    
    private TriStateButton createPositionButton(int row, int col, int positionIndex, int startX, int startY, boolean selected) {
        List<Component> tooltipSelected = List.of(
            Component.translatable("screen.anvilcraft.smart_block_placer.position.selected", row + 1, col + 1)
        );
        List<Component> tooltipUnselected = List.of(
            Component.translatable("screen.anvilcraft.smart_block_placer.position.unselected", row + 1, col + 1)
        );
        
        int xpos = startX + col * 18;
        int ypos = startY + row * 18;
        
        TriStateButton button = new TriStateButton(
            xpos, ypos, 16, 16,
            POSITION_SELECT, 16, 16,
            (btn) -> onPositionButtonClick(row, col, positionIndex, tooltipSelected, tooltipUnselected),
            selected ? tooltipSelected : tooltipUnselected
        );
        button.setSelected(selected);
        return button;
    }
    
    private void onLayerButtonClick(int index) {
        this.currentViewLayer = index;

        // 从服务端获取最新配置
        if (this.menu.getBlockEntity() != null) {
            this.layerPositions = this.menu.getBlockEntity().getLayerPositions();
        }

        // 更新layer按钮（互斥）
        for (int i = 0; i < 5; i++) {
            this.layerButtons.get(4 - i).setSelected(i == index);
        }

        // 更新棋盘显示
        this.updatePositionButtons();

        // 更新分层显示切换按钮的tooltip
        if (!this.showAllLayers && this.layerModeButton != null) {
            this.layerModeButton.setTooltips(List.of(this.getLayerModeTooltip()));
        }

        // 通知服务端
        PacketDistributor.sendToServer(new SmartBlockPlacerLayerPacket(index));
    }
    
    private void onLayerModeButtonClick() {
        this.showAllLayers = !this.showAllLayers;
        this.layerModeButton.setSelected(this.showAllLayers);

        // 更新按钮贴图
        this.layerModeButton.setTexture(this.showAllLayers ? LAYER_ALL : LAYER_SINGLE);

        // 更新tooltip
        this.layerModeButton.setTooltips(List.of(this.getLayerModeTooltip()));
    }
    
    private void onOperationModeButtonClick() {
        this.isPickupMode = !this.isPickupMode;
        this.operationModeButton.setSelected(this.isPickupMode);

        // 更新按钮贴图
        this.operationModeButton.setTexture(this.isPickupMode ? PICKUP_MODE : MOVE_MODE);

        // 更新tooltip
        this.operationModeButton.setTooltips(List.of(this.getOperationModeTooltip()));

        // 发送网络数据包同步到服务端
        PacketDistributor.sendToServer(new SmartBlockPlacerModePacket(this.isPickupMode));
    }
    
    private void updatePositionButtons() {
        Set<Integer> positions = this.layerPositions.getOrDefault(this.currentViewLayer, new HashSet<>());
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                this.positionButtons[row][col].setSelected(positions.contains(row * 5 + col));
            }
        }
    }
    
    private void onPositionButtonClick(int row, int col, int positionIndex, List<Component>
        tooltipSelected, List<Component> tooltipUnselected) {
        this.layerPositions.putIfAbsent(this.currentViewLayer, new HashSet<>());

        boolean newState = !this.positionButtons[row][col].isSelected();
        this.positionButtons[row][col].setSelected(newState);
        this.positionButtons[row][col].setTooltips(newState ? tooltipSelected : tooltipUnselected);

        Set<Integer> positions = this.layerPositions.get(this.currentViewLayer);
        if (newState) {
            positions.add(positionIndex);
        } else {
            positions.remove(positionIndex);
        }

        PacketDistributor.sendToServer(new SmartBlockPlacerPositionPacket(this.currentViewLayer, positionIndex, newState));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.dragTargetState = null;
        this.isPreviewDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    private boolean isMouseInPreviewWindow(double mouseX, double mouseY) {
        return mouseX >= this.previewWindowX && mouseX < this.previewWindowX + this.previewWindowWidth
            && mouseY >= this.previewWindowY && mouseY < this.previewWindowY + this.previewWindowHeight;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseInPreviewWindow(mouseX, mouseY)) {
            this.isPreviewDragging = true;
            this.lastMouseX = (int) mouseX;
            this.lastMouseY = (int) mouseY;
            return true;
        }

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                TriStateButton btn = this.positionButtons[row][col];
                if (btn != null && btn.isMouseOver(mouseX, mouseY)) {
                    this.dragTargetState = !btn.isSelected();
                    break;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isPreviewDragging) {
            // 计算鼠标移动距离
            int currentMouseX = (int) mouseX;
            int currentMouseY = (int) mouseY;
            float deltaX = currentMouseX - this.lastMouseX;
            float deltaY = currentMouseY - this.lastMouseY;

            // 更新旋转角度
            // 水平移动 -> Y轴旋转（无限制）
            this.previewRotationY += deltaX * ROTATION_SENSITIVITY;

            // 垂直移动 -> X轴旋转（有限制，反转方向）
            this.previewRotationX -= deltaY * ROTATION_SENSITIVITY;
            this.previewRotationX = Math.max(MIN_ROTATION_X, Math.min(MAX_ROTATION_X, this.previewRotationX));

            this.lastMouseX = currentMouseX;
            this.lastMouseY = currentMouseY;
            return true;
        }

        if (this.dragTargetState != null) {
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    TriStateButton btn = this.positionButtons[row][col];
                    if (btn != null && btn.isMouseOver(mouseX, mouseY)) {
                        int positionIndex = row * 5 + col;
                        if (btn.isSelected() != this.dragTargetState) {
                            btn.setSelected(this.dragTargetState);
                            this.layerPositions.putIfAbsent(this.currentViewLayer, new HashSet<>());
                            Set<Integer> positions = this.layerPositions.get(this.currentViewLayer);
                            if (this.dragTargetState) {
                                positions.add(positionIndex);
                            } else {
                                positions.remove(positionIndex);
                            }
                            PacketDistributor.sendToServer(
                                new SmartBlockPlacerPositionPacket(this.currentViewLayer, positionIndex, this.dragTargetState)
                            );
                        }
                    }
                }
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BACKGROUND, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 只渲染标题（方块名称），不渲染"物品栏"文字
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染3D预览
        this.renderPreview(guiGraphics);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * 构建并渲染3D预览
     */
    private void renderPreview(GuiGraphics guiGraphics) {
        if (this.menu.getBlockEntity() == null || this.minecraft == null || this.minecraft.level == null) {
            return;
        }

        LevelLike previewLevelLike = this.getOrCreateCachedPreviewLevelLike();

        // 启用裁剪，限制渲染区域在预览窗口内
        // 使用浮点数缩放以避免非整数GUI缩放比例（如1.5、2.5）的精度丢失
        double guiScale = this.minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor(
            (int) (this.previewWindowX * guiScale),
            (int) ((this.minecraft.getWindow().getGuiScaledHeight() - this.previewWindowY - this.previewWindowHeight) * guiScale),
            (int) (this.previewWindowWidth * guiScale),
            (int) (this.previewWindowHeight * guiScale)
        );

        // 渲染3D预览（使用固定旋转角度和固定尺寸）
        // 固定尺寸为5x5，忽略放置器的影响
        this.renderPreviewWithFixedSize(previewLevelLike, guiGraphics,
            this.previewWindowX + this.previewWindowWidth / 2,
            this.previewWindowY + this.previewWindowHeight / 2 + 5,
            this.previewRotationX,
            this.previewRotationY
        );  // 固定5x5的尺寸

        // 渲染3D放置范围框
        this.renderPlacementRangeBox(guiGraphics);

        // 如果没有配置选区位置，显示提示文本
        if (this.menu.getBlockEntity().getLayerPositions().isEmpty()) {
            Component emptyText = Component.translatable("screen.anvilcraft.smart_block_placer.preview.empty");
            int textWidth = (int) (this.font.width(emptyText) * 0.8f);
            int textX = this.previewWindowX + (this.previewWindowWidth - textWidth) / 2;
            int textY = this.previewWindowY + (this.previewWindowHeight - (int) (this.font.lineHeight * 0.8f)) / 2;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(0.8f, 0.8f, 0.8f);
            guiGraphics.drawString(this.font, emptyText, (int) (textX / 0.8f), (int) (textY / 0.8f), 0xFFFFFF, true);
            guiGraphics.pose().popPose();
        }

        // 禁用裁剪
        RenderSystem.disableScissor();
    }
    
    /**
     * 使用固定尺寸渲染预览
     */
    private void renderPreviewWithFixedSize(@Nullable LevelLike level, GuiGraphics guiGraphics,
                                            int posX, int posY,
        float rotationX, float rotationY
    ) {
        if (level == null) {
            return;
        }
        RenderSupport.renderLevelLikeWithFixedSize(level, guiGraphics, posX, posY,
            (float) 80.0, rotationX, rotationY, 5, 5
        );
    }
    
    /**
     * 渲染3D放置范围框
     */
    private void renderPlacementRangeBox(GuiGraphics guiGraphics) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        
        // 获取缓冲区
        MultiBufferSource.BufferSource buffers = this.minecraft.renderBuffers().bufferSource();
        final VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
        
        // 设置PoseStack - 完全复制RenderSupport.renderLevelLikeWithFixedSize的变换顺序
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        
        // 1. 平移到预览窗口中心
        poseStack.translate(
            this.previewWindowX + (float) this.previewWindowWidth / 2,
            this.previewWindowY + (float) this.previewWindowHeight / 2 + 5,
            100
        );
        
        // 2. 使用与预览相同的缩放计算
        float scaleX = 80.0f / (5 * net.minecraft.util.Mth.SQRT_OF_TWO);
        float scaleY = 80.0f / 5.0f;
        float scale = Math.min(scaleY, scaleX);
        poseStack.scale(-scale, -scale, -scale);
        
        // 3. 平移到中心
        poseStack.translate(-(float) 5 / 2, -(float) 5 / 2, 0);
        
        // 4. 先应用X轴旋转
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(this.previewRotationX));
        
        // 5. Y轴旋转的中心点和旋转
        float offsetX = (float) -5 / 2 + 0.05f;
        float offsetZ = (float) -5 / 2 + 1 - 0.05f;
        poseStack.translate(-offsetX, 0, -offsetZ);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(this.previewRotationY + 45));
        poseStack.translate(offsetX, 0, offsetZ);
        
        // 6. 平移Z轴（与方块渲染保持一致）
        poseStack.translate(0, 0, -1);
        
        // 7. 创建范围框（与方块渲染的位置空间一致）
        final VoxelShape rangeShape = Shapes.create(0.0, 0.0, 0.0, 5.0, 5.0, 5.0);
        
        // 8. 渲染范围框（青色，与世界中一致）
        TooltipRenderHelper.renderOutline(
            poseStack,
            consumer,
            0, 0, 0,
            BlockPos.ZERO,
            rangeShape,
            0xFF00FFCC
        );
        
        buffers.endBatch(RenderType.lines());
        poseStack.popPose();
    }
    
    /**
     * 获取或创建缓存的 LevelLike 实例
     * 只在状态改变时重建，避免每帧重新构建
     */
    private @Nullable LevelLike getOrCreateCachedPreviewLevelLike() {
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null || this.minecraft == null || this.minecraft.level == null) {
            return null;
        }

        // 检查缓存是否有效
        boolean needsRebuild = this.cachedPreviewLevelLike == null
            || !this.cachedLayerPositions.equals(blockEntity.getLayerPositions())
            || this.cachedViewLayer != blockEntity.getSelectedLayer()
            || this.cachedShowAllLayers != this.showAllLayers
            || this.cachedPickupMode != blockEntity.isPickupMode();

        if (needsRebuild) {
            this.cachedPreviewLevelLike = this.buildPreviewLevelLike();
            this.cachedLayerPositions = new HashMap<>(blockEntity.getLayerPositions());
            this.cachedViewLayer = blockEntity.getSelectedLayer();
            this.cachedShowAllLayers = this.showAllLayers;
            this.cachedPickupMode = blockEntity.isPickupMode();
        }

        return this.cachedPreviewLevelLike;
    }
    
    /**
     * 构建预览用的LevelLike实例
     *
     * @return 预览数据
     */
    private @Nullable LevelLike buildPreviewLevelLike() {
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null || this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        
        ClientLevel level = this.minecraft.level;
        LevelLike previewLevelLike = new LevelLike(level);
        previewLevelLike.setAllLayersVisible(this.showAllLayers);

        if (!this.showAllLayers) {
            previewLevelLike.setCurrentVisibleLayer(this.currentViewLayer);
        }

        // 放置器位置：X居中，Z=6（放置区域后方），Y=0
        int placerX = 2;
        int placerZ = 6;
        int placerY = 0;

        // 放置器始终渲染，不受分层限制，预览窗口中统一朝北
        previewLevelLike.setBlockStateAlwaysRender(
            new BlockPos(placerX, placerY, placerZ),
            dev.dubhe.anvilcraft.init.block.ModBlocks.SMART_BLOCK_PLACER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
        );

        Map<Integer, Set<Integer>> layerPositions = blockEntity.getLayerPositions();
        if (layerPositions.isEmpty()) {
            // 没有选区时只渲染放置器
            return previewLevelLike;
        }

        // 基于游戏时间选择方块类型
        long gameTime = level.getGameTime();
        boolean useGreenGlass = (gameTime / (PREVIEW_BLOCK_SWITCH_INTERVAL * 2)) % 2 == 0;
        BlockState previewBlockState = useGreenGlass
            ? Blocks.LIME_STAINED_GLASS.defaultBlockState()
            : Blocks.LIME_CONCRETE.defaultBlockState();

        // 设置预览方块
        for (Map.Entry<Integer, Set<Integer>> entry : layerPositions.entrySet()) {
            int layer = entry.getKey();
            for (int position : entry.getValue()) {
                int row = position / 5;
                int col = position % 5;
                previewLevelLike.setBlockState(new BlockPos(col, layer, row), previewBlockState);
            }
        }

        return previewLevelLike;
    }
}
