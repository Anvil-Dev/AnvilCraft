package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.client.gui.component.ToggleButton;
import dev.dubhe.anvilcraft.client.gui.component.TriStateButton;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.SmartBlockPlacerMenu;
import dev.dubhe.anvilcraft.network.SmartBlockPlacerLayerPacket;
import dev.dubhe.anvilcraft.network.SmartBlockPlacerPositionPacket;
import dev.dubhe.anvilcraft.util.LevelLike;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("checkstyle:LineLength")
public class SmartBlockPlacerScreen extends AbstractContainerScreen<SmartBlockPlacerMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "smart_block_placer");
    
    // Layer按钮贴图
    private static final ResourceLocation[] LAYER_DEFAULT = {
        SharedTextures.textureGui("machine/smart_block_placer/layer_1"),
        SharedTextures.textureGui("machine/smart_block_placer/layer_2"),
        SharedTextures.textureGui("machine/smart_block_placer/layer_3"),
        SharedTextures.textureGui("machine/smart_block_placer/layer_4"),
        SharedTextures.textureGui("machine/smart_block_placer/layer_5")
    };
    
    // 位置选择按钮贴图
    private static final ResourceLocation POSITION_SELECT = SharedTextures.textureGui("machine/smart_block_placer/position_select");
    
    // 分层显示切换按钮贴图
    private static final ResourceLocation LAYER_ALL = SharedTextures.textureGui("machine/smart_block_placer/layer_all");
    private static final ResourceLocation LAYER_SINGLE = SharedTextures.textureGui("machine/smart_block_placer/layer_single");
    
    private final List<TriStateButton> layerButtons = new ArrayList<>();
    private final TriStateButton[][] positionButtons = new TriStateButton[5][5];
    private ToggleButton layerModeButton;  // 分层显示切换按钮
    private int currentViewLayer = 0;
    private Map<Integer, Set<Integer>> layerPositions = new HashMap<>();
    private boolean showAllLayers = true;  // 是否显示所有层
    
    // 鼠标拖动状态
    private Boolean dragTargetState = null;
    
    // 3D预览窗口
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
    
    // 预览方块动画
    private static final int PREVIEW_BLOCK_SWITCH_INTERVAL = 80;  // 4秒 = 80 tick

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
        
        // 从服务端获取初始状态
        if (this.menu.getBlockEntity() != null) {
            currentViewLayer = this.menu.getBlockEntity().getSelectedLayer();
            layerPositions = this.menu.getBlockEntity().getLayerPositions();
        }
        
        // 初始化预览窗口位置（根据GUI背景图）
        previewWindowX = this.leftPos + 136;
        previewWindowY = this.topPos + 18;
        
        initLayerButtons();
        initPositionButtons();
        initLayerModeButton();
    }
    
    private void initLayerButtons() {
        layerButtons.clear();
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
                (btn) -> onLayerButtonClick(index),
                List.of(Component.translatable("screen.anvilcraft.smart_block_placer.layer." + (i + 1)))
            );
            button.setSelected(i == currentViewLayer);
            layerButtons.add(button);
            this.addRenderableWidget(button);
        }
    }
    
    private void initPositionButtons() {
        int gridStartX = this.leftPos + 33;
        int gridStartY = this.topPos + 18;
        Set<Integer> currentPositions = layerPositions.getOrDefault(currentViewLayer, new HashSet<>());
        
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int positionIndex = row * 5 + col;
                boolean isSelected = currentPositions.contains(positionIndex);
                
                TriStateButton button = createPositionButton(row, col, positionIndex, gridStartX, gridStartY, isSelected);
                positionButtons[row][col] = button;
                this.addRenderableWidget(button);
            }
        }
    }
    
    private void initLayerModeButton() {
        // 右侧预留区域第一个按钮位置（物品栏右侧）
        int buttonX = this.leftPos + 232;  // 物品栏最右侧(210) + 18像素间距
        int buttonY = this.topPos + 112;   // 与主物品栏第一行对齐
        
        layerModeButton = new ToggleButton(
            buttonX, buttonY, 16, 16,
            showAllLayers ? LAYER_ALL : LAYER_SINGLE,
            16, 16,
            (btn) -> onLayerModeButtonClick(),
            List.of(getLayerModeTooltip())
        );
        layerModeButton.setSelected(showAllLayers);
        this.addRenderableWidget(layerModeButton);
    }
    
    private Component getLayerModeTooltip() {
        if (showAllLayers) {
            return Component.translatable("screen.anvilcraft.smart_block_placer.layer_mode.all");
        } else {
            return Component.translatable("screen.anvilcraft.smart_block_placer.layer_mode.single",
                currentViewLayer + 1, 5);
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
        currentViewLayer = index;
        
        // 从服务端获取最新配置
        if (this.menu.getBlockEntity() != null) {
            layerPositions = this.menu.getBlockEntity().getLayerPositions();
        }
        
        // 更新layer按钮（互斥）
        for (int i = 0; i < 5; i++) {
            layerButtons.get(4 - i).setSelected(i == index);
        }
        
        // 更新棋盘显示
        updatePositionButtons();
        
        // 更新分层显示切换按钮的tooltip
        if (!showAllLayers && layerModeButton != null) {
            layerModeButton.setTooltips(List.of(getLayerModeTooltip()));
        }
        
        // 通知服务端
        PacketDistributor.sendToServer(new SmartBlockPlacerLayerPacket(index));
    }
    
    private void onLayerModeButtonClick() {
        showAllLayers = !showAllLayers;
        layerModeButton.setSelected(showAllLayers);
        
        // 更新按钮贴图
        layerModeButton.setTexture(showAllLayers ? LAYER_ALL : LAYER_SINGLE);
        
        // 更新tooltip
        layerModeButton.setTooltips(List.of(getLayerModeTooltip()));
    }
    
    private void updatePositionButtons() {
        Set<Integer> positions = layerPositions.getOrDefault(currentViewLayer, new HashSet<>());
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                positionButtons[row][col].setSelected(positions.contains(row * 5 + col));
            }
        }
    }
    
    private void onPositionButtonClick(int row, int col, int positionIndex, List<Component>
        tooltipSelected, List<Component> tooltipUnselected) {
        layerPositions.putIfAbsent(currentViewLayer, new HashSet<>());
        
        boolean newState = !positionButtons[row][col].isSelected();
        positionButtons[row][col].setSelected(newState);
        positionButtons[row][col].setTooltips(newState ? tooltipSelected : tooltipUnselected);
        
        Set<Integer> positions = layerPositions.get(currentViewLayer);
        if (newState) {
            positions.add(positionIndex);
        } else {
            positions.remove(positionIndex);
        }
        
        PacketDistributor.sendToServer(new SmartBlockPlacerPositionPacket(currentViewLayer, positionIndex, newState));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragTargetState = null;
        isPreviewDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    private boolean isMouseInPreviewWindow(double mouseX, double mouseY) {
        return mouseX >= previewWindowX && mouseX < previewWindowX + previewWindowWidth
            && mouseY >= previewWindowY && mouseY < previewWindowY + previewWindowHeight;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseInPreviewWindow(mouseX, mouseY)) {
            isPreviewDragging = true;
            lastMouseX = (int) mouseX;
            lastMouseY = (int) mouseY;
            return true;
        }
        
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                TriStateButton btn = positionButtons[row][col];
                if (btn != null && btn.isMouseOver(mouseX, mouseY)) {
                    dragTargetState = !btn.isSelected();
                    break;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isPreviewDragging) {
            // 计算鼠标移动距离
            int currentMouseX = (int) mouseX;
            int currentMouseY = (int) mouseY;
            float deltaX = currentMouseX - lastMouseX;
            float deltaY = currentMouseY - lastMouseY;
            
            // 更新旋转角度
            // 水平移动 -> Y轴旋转（无限制）
            previewRotationY += deltaX * ROTATION_SENSITIVITY;
            
            // 垂直移动 -> X轴旋转（有限制，反转方向）
            previewRotationX -= deltaY * ROTATION_SENSITIVITY;
            previewRotationX = Math.max(MIN_ROTATION_X, Math.min(MAX_ROTATION_X, previewRotationX));
            
            lastMouseX = currentMouseX;
            lastMouseY = currentMouseY;
            return true;
        }
        
        if (dragTargetState != null) {
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    TriStateButton btn = positionButtons[row][col];
                    if (btn != null && btn.isMouseOver(mouseX, mouseY)) {
                        int positionIndex = row * 5 + col;
                        if (btn.isSelected() != dragTargetState) {
                            btn.setSelected(dragTargetState);
                            layerPositions.putIfAbsent(currentViewLayer, new HashSet<>());
                            Set<Integer> positions = layerPositions.get(currentViewLayer);
                            if (dragTargetState) {
                                positions.add(positionIndex);
                            } else {
                                positions.remove(positionIndex);
                            }
                            PacketDistributor.sendToServer(
                                new SmartBlockPlacerPositionPacket(currentViewLayer, positionIndex, dragTargetState)
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
        renderPreview(guiGraphics, partialTick);
        
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * 构建并渲染3D预览
     */
    private void renderPreview(GuiGraphics guiGraphics, float partialTick) {
        if (this.menu.getBlockEntity() == null || this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        
        LevelLike previewLevelLike = buildPreviewLevelLike();
        if (previewLevelLike == null) {
            // 没有配置位置时显示提示信息
            Component previewText = Component.translatable("screen.anvilcraft.smart_block_placer.preview.empty");
            
            // 缩小文本到0.8倍
            float scale = 0.8f;
            int textWidth = minecraft.font.width(previewText);
            int centerX = previewWindowX + previewWindowWidth / 2;
            int centerY = previewWindowY + previewWindowHeight / 2;
            
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, centerY, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.pose().translate(-textWidth / 2, -4 / scale, 0);
            guiGraphics.drawString(minecraft.font, previewText, 0, 0, 0xFFFFFFFF, false);
            guiGraphics.pose().popPose();
            
            return;
        }
        
        // 启用裁剪，限制渲染区域在预览窗口内
        RenderSystem.enableScissor(
            previewWindowX * (int) minecraft.getWindow().getGuiScale(),
            (minecraft.getWindow().getGuiScaledHeight() - previewWindowY - previewWindowHeight) * (int) minecraft.getWindow().getGuiScale(),
            previewWindowWidth * (int) minecraft.getWindow().getGuiScale(),
            previewWindowHeight * (int) minecraft.getWindow().getGuiScale()
        );
        
        // 渲染3D预览（使用固定旋转角度和固定尺寸）
        // 固定尺寸为5x5，忽略放置器的影响
        renderPreviewWithFixedSize(previewLevelLike, guiGraphics, 
            previewWindowX + previewWindowWidth / 2, 
            previewWindowY + previewWindowHeight / 2 + 5,
            previewRotationX,
            previewRotationY
        );  // 固定5x5的尺寸
        
        // 禁用裁剪
        RenderSystem.disableScissor();
    }
    
    /**
     * 使用固定尺寸渲染预览
     */
    private void renderPreviewWithFixedSize(LevelLike level, GuiGraphics guiGraphics, 
                                            int posX, int posY,
        float rotationX, float rotationY
    ) {
        RenderSupport.renderLevelLikeWithFixedSize(level, guiGraphics, posX, posY,
            (float) 80.0, rotationX, rotationY, 5, 5
        );
    }
    
    /**
     * 构建预览用的LevelLike实例
     *
     * @return 预览数据，如果没有配置位置则返回null
     */
    @SuppressWarnings("DataFlowIssue")
    private LevelLike buildPreviewLevelLike() {
        var blockEntity = this.menu.getBlockEntity();
        var level = blockEntity.getLevel();
        if (level == null) return null;
        
        Map<Integer, Set<Integer>> layerPositions = blockEntity.getLayerPositions();
        if (layerPositions.isEmpty()) return null;
        
        LevelLike previewLevelLike = new LevelLike(this.minecraft.level);
        previewLevelLike.setAllLayersVisible(showAllLayers);
        
        if (!showAllLayers) {
            previewLevelLike.setCurrentVisibleLayer(currentViewLayer);
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
