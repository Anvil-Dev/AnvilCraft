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
    private static final ResourceLocation BACKGROUND = SharedTextures.SMART_BLOCK_PLACER_BACKGROUND;

    private static final ResourceLocation[] LAYER_DEFAULT = {
        SharedTextures.SMART_BLOCK_PLACER_LAYER_1,
        SharedTextures.SMART_BLOCK_PLACER_LAYER_2,
        SharedTextures.SMART_BLOCK_PLACER_LAYER_3,
        SharedTextures.SMART_BLOCK_PLACER_LAYER_4,
        SharedTextures.SMART_BLOCK_PLACER_LAYER_5
    };

    private static final ResourceLocation POSITION_SELECT = SharedTextures.SMART_BLOCK_PLACER_POSITION_SELECT;

    private static final ResourceLocation LAYER_ALL = SharedTextures.SMART_BLOCK_PLACER_LAYER_ALL;
    private static final ResourceLocation LAYER_SINGLE = SharedTextures.SMART_BLOCK_PLACER_LAYER_SINGLE;

    private static final ResourceLocation PICKUP_MODE = SharedTextures.SMART_BLOCK_PLACER_PICKUP_MODE;
    private static final ResourceLocation MOVE_MODE = SharedTextures.SMART_BLOCK_PLACER_MOVE_MODE;
    
    // 蓝图模式贴图（已在SharedTextures中注册）
    private static final ResourceLocation BLUEPRINT_MODE_BG = SharedTextures.SMART_BLOCK_PLACER_BLUEPRINT_MODE;

    private final List<TriStateButton> layerButtons = new ArrayList<>();
    private final TriStateButton[][] positionButtons = new TriStateButton[5][5];
    private ToggleButton layerModeButton;  // 分层显示切换按钮
    private ToggleButton operationModeButton;  // 取物/移动模式切换按钮
    private int currentViewLayer = 0;
    private Map<Integer, Set<Integer>> layerPositions = new HashMap<>();
    private boolean showAllLayers = true;
    private boolean isPickupMode = true;

    private Boolean dragTargetState = null;
    
    // 蓝图模式标志（当加载了结构磁盘时为 true）
    private boolean isBlueprintMode = false;

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
    private boolean cachedBlueprintMode = false;  // 缓存蓝图模式状态
    private String cachedStructureUuid = "";  // 缓存结构UUID，用于检测结构变化
    private long cachedGameTimeBlockType = -1;  // 用于追踪方块类型的游戏时间

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
            // 创建深拷贝，避免与 blockEntity 共享内部 Set 引用
            this.layerPositions = new HashMap<>();
            for (Map.Entry<Integer, Set<Integer>> entry : this.menu.getBlockEntity().getLayerPositions().entrySet()) {
                this.layerPositions.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
            this.isPickupMode = this.menu.getBlockEntity().isPickupMode();
            // 检查是否处于蓝图模式(直接检查磁盘槽位)
            this.isBlueprintMode = !this.menu.getBlockEntity().getDiskInventory().getItem(0).isEmpty();
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
        // 蓝图模式下向右移动105像素
        int buttonX = this.leftPos + 8 + (this.isBlueprintMode ? 97 : 0);
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
            // Layer 按钮始终可用，蓝图模式下也可以分层查看结构
            button.active = true;
            this.layerButtons.add(button);
            this.addRenderableWidget(button);
        }
    }
    
    private void initPositionButtons() {
        int gridStartX = this.leftPos + 33;
        int gridStartY = this.topPos + 18;
        
        // 蓝图模式下不渲染位置选择按钮
        if (this.isBlueprintMode) {
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    this.positionButtons[row][col] = null;
                }
            }
            return;
        }
        
        // 正常模式下初始化位置按钮
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
    
    /**
     * 根据蓝图模式更新按钮状态
     */
    private void updateButtonsForBlueprintMode() {
        // 重新初始化Layer按钮（蓝图模式下向右移动105像素）
        this.removeLayerButtons();
        this.initLayerButtons();
        
        // 蓝图模式下移除位置按钮
        if (this.isBlueprintMode) {
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    TriStateButton button = this.positionButtons[row][col];
                    if (button != null) {
                        this.removeWidget(button);
                        this.positionButtons[row][col] = null;
                    }
                }
            }
        } else {
            // 正常模式下重新初始化位置按钮
            this.initPositionButtons();
        }
        
        // 蓝图模式下清空本地 layerPositions
        if (this.isBlueprintMode) {
            this.layerPositions.clear();
        }
    }
    
    /**
     * 移除所有Layer按钮
     */
    private void removeLayerButtons() {
        for (TriStateButton button : this.layerButtons) {
            if (button != null) {
                this.removeWidget(button);
            }
        }
        this.layerButtons.clear();
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

        // 从服务端获取最新配置，创建深拷贝
        if (this.menu.getBlockEntity() != null) {
            this.layerPositions = new HashMap<>();
            for (Map.Entry<Integer, Set<Integer>> entry : this.menu.getBlockEntity().getLayerPositions().entrySet()) {
                this.layerPositions.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
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
        // 蓝图模式下不更新位置按钮
        if (this.isBlueprintMode) {
            return;
        }
        
        Set<Integer> positions = this.layerPositions.getOrDefault(this.currentViewLayer, new HashSet<>());
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                TriStateButton button = this.positionButtons[row][col];
                if (button == null) continue;
                
                int positionIndex = row * 5 + col;
                boolean isSelected = positions.contains(positionIndex);
                button.setSelected(isSelected);
                
                // 更新tooltip以反映当前层级的选择状态
                List<Component> tooltipSelected = List.of(
                    Component.translatable("screen.anvilcraft.smart_block_placer.position.selected", row + 1, col + 1)
                );
                List<Component> tooltipUnselected = List.of(
                    Component.translatable("screen.anvilcraft.smart_block_placer.position.unselected", row + 1, col + 1)
                );
                button.setTooltips(isSelected ? tooltipSelected : tooltipUnselected);
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
    
    /**
     * 渲染Disk槽位的tooltip
     */
    private void renderDiskSlotTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Disk槽位的位置（与Menu中一致）
        int diskSlotX = this.leftPos + 8;
        int diskSlotY = this.topPos + 119;
        int diskSlotWidth = 16;
        int diskSlotHeight = 16;
        
        // 检查鼠标是否在Disk槽位上
        if (mouseX >= diskSlotX && mouseX < diskSlotX + diskSlotWidth
            && mouseY >= diskSlotY && mouseY < diskSlotY + diskSlotHeight) {
            // 渲染tooltip，确保在所有元素上方
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 1500);
            guiGraphics.renderTooltip(
                this.font,
                List.of(Component.translatable("screen.anvilcraft.smart_block_placer.disk_slot")),
                java.util.Optional.empty(),
                mouseX,
                mouseY
            );
            guiGraphics.pose().popPose();
        }
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
        
        // 蓝图模式下渲染额外贴图（128×128）
        if (this.isBlueprintMode) {
            int blueprintX = i + (this.imageWidth - 128) / 2 - 60;
            int blueprintY = j + (this.imageHeight - 128) / 2 - 19;
            guiGraphics.blit(BLUEPRINT_MODE_BG, blueprintX, blueprintY, 0, 0, 128, 128, 128, 128);
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 只渲染标题（方块名称），不渲染"物品栏"文字
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }
    
    @Override
    public void containerTick() {
        super.containerTick();
        
        // 定期从 blockEntity 同步数据到客户端,确保磁盘插入等操作的选区变化能实时更新
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null) {
            // 先同步蓝图模式状态(优先级最高,因为会影响按钮的可交互性)
            // 直接检查磁盘槽位是否有物品,而不是依赖 loadedStructure(只在服务端设置)
            boolean newBlueprintMode = !blockEntity.getDiskInventory().getItem(0).isEmpty();
            if (newBlueprintMode != this.isBlueprintMode) {
                this.isBlueprintMode = newBlueprintMode;
                this.updateButtonsForBlueprintMode();
            }
            
            // 同步 layerPositions
            Map<Integer, Set<Integer>> newLayerPositions = blockEntity.getLayerPositions();
            if (!this.layerPositions.equals(newLayerPositions)) {
                // 深拷贝,避免共享引用
                this.layerPositions = new HashMap<>();
                for (Map.Entry<Integer, Set<Integer>> entry : newLayerPositions.entrySet()) {
                    this.layerPositions.put(entry.getKey(), new HashSet<>(entry.getValue()));
                }
                // 数据变化时更新按钮贴图
                this.updatePositionButtons();
            }
            
            // 同步 selectedLayer
            int newViewLayer = blockEntity.getSelectedLayer();
            if (newViewLayer != this.currentViewLayer) {
                this.currentViewLayer = newViewLayer;
                // 层级变化时更新按钮贴图
                this.updatePositionButtons();
            }
            
            // 同步 isPickupMode
            boolean newPickupMode = blockEntity.isPickupMode();
            if (newPickupMode != this.isPickupMode) {
                this.isPickupMode = newPickupMode;
            }
        }
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 检测蓝图模式变化(containerTick已经处理,这里作为备用)
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null) {
            // 直接检查磁盘槽位是否有物品
            boolean newBlueprintMode = !blockEntity.getDiskInventory().getItem(0).isEmpty();
            if (newBlueprintMode != this.isBlueprintMode) {
                this.isBlueprintMode = newBlueprintMode;
                this.updateButtonsForBlueprintMode();
            }
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 渲染3D预览
        this.renderPreview(guiGraphics);

        // 检查鼠标是否在Disk槽位上
        int diskSlotX = this.leftPos + 8;
        int diskSlotY = this.topPos + 119;
        int diskSlotWidth = 16;
        int diskSlotHeight = 16;
        boolean isMouseOnDiskSlot = mouseX >= diskSlotX && mouseX < diskSlotX + diskSlotWidth
            && mouseY >= diskSlotY && mouseY < diskSlotY + diskSlotHeight;
        
        // 如果鼠标在Disk槽位上，不渲染默认tooltip
        if (!isMouseOnDiskSlot) {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
        
        // 渲染Disk槽位的tooltip
        this.renderDiskSlotTooltip(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * 构建并渲染3D预览
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
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

        // 渲染3D放置范围框（在方块之后渲染，会被方块遮挡）
        this.renderPlacementRangeBox(guiGraphics);

        // 禁用裁剪
        RenderSystem.disableScissor();

        // 如果没有配置选区位置且不在蓝图模式下，显示提示文本（在裁剪区域外渲染，确保在最上层）
        if (!this.isBlueprintMode && this.menu.getBlockEntity().getLayerPositions().isEmpty()) {
            Component emptyText = Component.translatable("screen.anvilcraft.smart_block_placer.preview.empty");
            int textWidth = (int) (this.font.width(emptyText) * 0.8f);
            int textX = this.previewWindowX + (this.previewWindowWidth - textWidth) / 2;
            int textY = this.previewWindowY + (this.previewWindowHeight - (int) (this.font.lineHeight * 0.8f)) / 2;
            
            // 禁用深度测试，确保文本在最上层渲染
            RenderSystem.disableDepthTest();
            guiGraphics.pose().pushPose();
            // 将Z轴向前移动，确保文本在最前面
            guiGraphics.pose().translate(0, 0, 1000);
            guiGraphics.pose().scale(0.8f, 0.8f, 0.8f);
            guiGraphics.drawString(this.font, emptyText, (int) (textX / 0.8f), (int) (textY / 0.8f), 0xFFFFFF, true);
            guiGraphics.pose().popPose();
            // 恢复深度测试
            RenderSystem.enableDepthTest();
        }
        
        // 渲染已加载的结构名称（提高图层，与"没有选区"文本一致）
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null) {
            String structureName = blockEntity.getLoadedStructureName();
            if (!structureName.isEmpty()) {
                Component structureText = Component.translatable("screen.anvilcraft.smart_block_placer.structure.loaded", structureName);
                int textX = this.titleLabelX + 216;
                int textY = this.titleLabelY + 50;
                
                // 禁用深度测试，确保文本在最上层渲染
                RenderSystem.disableDepthTest();
                guiGraphics.pose().pushPose();
                // 将Z轴向前移动，确保文本在最前面
                guiGraphics.pose().translate(0, 0, 1000);
                guiGraphics.drawString(this.font, structureText, textX, textY, 0x00AA00, false);
                guiGraphics.pose().popPose();
                // 恢复深度测试
                RenderSystem.enableDepthTest();
            }
        }
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
        
        // 5. Y轴旋转的中心点 - 固定基于5x5范围计算，忽略放置器
        // 与RenderSupport.renderLevelLikeWithFixedSize保持一致
        float offsetX = (float) -5 / 2 + 0.05f;
        float offsetZ = (float) -5 / 2 + 1;
        poseStack.translate(-offsetX, 0, -offsetZ);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(this.previewRotationY + 45));
        poseStack.translate(offsetX, 0, offsetZ);
        
        // 6. 平移Z轴（与方块保持一致，在Z=-1处渲染）
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
        // 使用客户端本地的 layerPositions 而不是 blockEntity 的，避免网络延迟导致的预览不更新
        // 同时检查游戏时间，确保方块类型能实时切换
        long currentGameTime = this.minecraft.level.getGameTime();
        long currentBlockTypeTime = currentGameTime / (PREVIEW_BLOCK_SWITCH_INTERVAL * 2);
        
        // 获取当前结构UUID（用于检测结构变化）
        String currentStructureUuid = "";
        if (this.isBlueprintMode && blockEntity.getLoadedStructure() != null) {
            currentStructureUuid = blockEntity.getLoadedStructure().uuid;
        }
        
        boolean needsRebuild = this.cachedPreviewLevelLike == null
            || !this.cachedLayerPositions.equals(this.layerPositions)
            || this.cachedViewLayer != this.currentViewLayer
            || this.cachedShowAllLayers != this.showAllLayers
            || this.cachedPickupMode != this.isPickupMode
            || this.cachedBlueprintMode != this.isBlueprintMode
            || !this.cachedStructureUuid.equals(currentStructureUuid)
            || this.cachedGameTimeBlockType != currentBlockTypeTime;

        if (needsRebuild) {
            this.cachedPreviewLevelLike = this.buildPreviewLevelLike();
            // 深拷贝 layerPositions，避免共享 Set 引用导致缓存判断失效
            this.cachedLayerPositions = new HashMap<>();
            for (Map.Entry<Integer, Set<Integer>> entry : this.layerPositions.entrySet()) {
                this.cachedLayerPositions.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
            this.cachedViewLayer = this.currentViewLayer;
            this.cachedShowAllLayers = this.showAllLayers;
            this.cachedPickupMode = this.isPickupMode;
            this.cachedBlueprintMode = this.isBlueprintMode;
            this.cachedStructureUuid = currentStructureUuid;
            this.cachedGameTimeBlockType = currentBlockTypeTime;
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

        // 获取放置器的状态
        boolean upsideDown = false;
        boolean powered = false;
        boolean overload = true;
        if (this.minecraft.level != null) {
            BlockState placerState = this.minecraft.level.getBlockState(this.menu.getBlockEntity().getBlockPos());
            if (placerState.getBlock() instanceof dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock) {
                upsideDown = placerState.getValue(dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock.UPSIDE_DOWN);
                powered = placerState.getValue(dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock.POWERED);
                overload = placerState.getValue(dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock.OVERLOAD);
            }
        }

        // 放置器位置：X居中，Z=6（放置区域后方）
        // 倒挂时Y=4（顶部），正常时Y=0（底部）
        int placerX = 2;
        int placerZ = 6;
        int placerY = upsideDown ? 4 : 0;

        // 放置器始终渲染，不受分层限制，预览窗口中统一朝北
        // 应用实际的 POWERED 和 OVERLOAD 状态以显示正确的贴图
        previewLevelLike.setBlockStateAlwaysRender(
            new BlockPos(placerX, placerY, placerZ),
            dev.dubhe.anvilcraft.init.block.ModBlocks.SMART_BLOCK_PLACER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock.UPSIDE_DOWN, upsideDown)
                .setValue(dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock.POWERED, powered)
                .setValue(dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock.OVERLOAD, overload)
        );

        // 蓝图模式：渲染磁盘中的结构
        if (this.isBlueprintMode) {
            var loadedStructure = blockEntity.getLoadedStructure();
            if (loadedStructure != null && !loadedStructure.isEmpty()) {
                // 获取放置器朝向
                Direction placerFacing = Direction.NORTH;  // 默认
                if (this.minecraft.level != null) {
                    BlockState placerState = this.minecraft.level.getBlockState(blockEntity.getBlockPos());
                    if (placerState.hasProperty(HorizontalDirectionalBlock.FACING)) {
                        placerFacing = placerState.getValue(HorizontalDirectionalBlock.FACING);
                    }
                }
                
                // 对结构方块应用旋转（与服务端放置逻辑保持一致）
                List<dev.dubhe.anvilcraft.util.StructureLoadUtil.BlockPosition> rotatedBlocks = 
                    this.rotateStructureForPreview(loadedStructure, placerFacing);
                
                // 渲染旋转后的结构方块
                for (dev.dubhe.anvilcraft.util.StructureLoadUtil.BlockPosition blockPos : rotatedBlocks) {
                    int x = blockPos.x();
                    int y = blockPos.y();
                    int z = blockPos.z();
                    
                    // 只渲染在预览范围内的方块（5x5x5）
                    if (x >= 0 && x < 5 && y >= 0 && y < 5 && z >= 0 && z < 5) {
                        previewLevelLike.setBlockState(new BlockPos(x, y, z), blockPos.state());
                    }
                }
            }
            return previewLevelLike;
        }

        // 普通模式：使用客户端本地的 layerPositions，确保快速拖动时预览能及时更新
        Map<Integer, Set<Integer>> layerPositions = this.layerPositions;
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
    
    /**
     * 为预览旋转结构方块（与SmartBlockPlacerBlockEntity.rotateStructureData保持一致）
     */
    private List<dev.dubhe.anvilcraft.util.StructureLoadUtil.BlockPosition> rotateStructureForPreview(
        dev.dubhe.anvilcraft.util.StructureLoadUtil.StructureData data,
        Direction placerFacing
    ) {
        // 获取扫描器的朝向
        int scannerFacingValue = data.scannerFacing;
        
        // 计算相对旋转角度
        // Scanner朝向与放置器朝向是镜像对应的，需要转换
        int scannerToPlacerMapping = switch (scannerFacingValue) {
            case 2 -> 3;  // Scanner北 → 放置器南
            case 3 -> 2;  // Scanner南 → 放置器北
            case 4 -> 5;  // Scanner西 → 放置器东
            case 5 -> 4;  // Scanner东 → 放置器西
            default -> scannerFacingValue;
        };
        
        // 转换为0-3的索引用于计算旋转 (NORTH=0, EAST=1, SOUTH=2, WEST=3)
        // 并根据放置器朝向应用修正：东+3, 西+1, 南+2, 北+0
        int placerIndex = switch (placerFacing) {
            case NORTH -> 0;  // 北：无修正
            case EAST -> (1 + 3) % 4;  // 东：+3
            case SOUTH -> (2 + 2) % 4;  // 南：+2
            case WEST -> (3 + 1) % 4;   // 西：+1
            default -> 0;
        };
        
        int scannerIndex = switch (scannerToPlacerMapping) {
            case 2 -> 0;  // NORTH
            case 5 -> 1;  // EAST
            case 3 -> 2;  // SOUTH
            case 4 -> 3;  // WEST
            default -> 0;
        };
        
        // 计算旋转步数（顺时针）
        int rotationSteps = (placerIndex - scannerIndex + 4) % 4;
        if (rotationSteps == 0) {
            // 不需要旋转，直接返回原始数据
            return data.blocks;
        }
        
        // 旋转所有方块
        List<dev.dubhe.anvilcraft.util.StructureLoadUtil.BlockPosition> rotatedBlocks = new ArrayList<>();
        int centerX = data.sizeX / 2;
        int centerZ = data.sizeZ / 2;
        
        for (dev.dubhe.anvilcraft.util.StructureLoadUtil.BlockPosition block : data.blocks) {
            // 计算相对于中心的坐标
            int relX = block.x() - centerX;
            int relZ = block.z() - centerZ;
            
            // 根据旋转步数旋转坐标
            int rotatedX = relX;
            int rotatedZ = relZ;
            
            switch (rotationSteps) {
                case 1 -> {  // 90度顺时针: (x, z) -> (-z, x)
                    rotatedX = -relZ;
                    rotatedZ = relX;
                }
                case 2 -> {  // 180度: (x, z) -> (-x, -z)
                    rotatedX = -relX;
                    rotatedZ = -relZ;
                }
                case 3 -> {  // 270度顺时针: (x, z) -> (z, -x)
                    rotatedX = relZ;
                    rotatedZ = -relX;
                }
            }
            
            // 转换回绝对坐标
            int newX = rotatedX + centerX;
            int newZ = rotatedZ + centerZ;
            
            // 旋转方块朝向
            BlockState rotatedState = this.rotateBlockStateForPreview(block.state(), rotationSteps);
            
            rotatedBlocks.add(new dev.dubhe.anvilcraft.util.StructureLoadUtil.BlockPosition(newX, block.y(), newZ, rotatedState));
        }
        
        return rotatedBlocks;
    }
    
    /**
     * 旋转方块的朝向属性（用于预览）
     */
    private BlockState rotateBlockStateForPreview(BlockState state, int rotationSteps) {
        if (rotationSteps == 0) return state;
        
        // 获取方块的FACING属性
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
            
            // 转换为0-3的索引 (NORTH=0, EAST=1, SOUTH=2, WEST=3)
            int facingIndex = switch (facing) {
                case NORTH -> 0;
                case EAST -> 1;
                case SOUTH -> 2;
                case WEST -> 3;
                default -> -1;
            };
            
            if (facingIndex >= 0) {
                // 旋转
                int rotatedIndex = (facingIndex + rotationSteps) % 4;
                Direction rotatedFacing = switch (rotatedIndex) {
                    case 0 -> Direction.NORTH;
                    case 1 -> Direction.EAST;
                    case 2 -> Direction.SOUTH;
                    case 3 -> Direction.WEST;
                    default -> facing;
                };
                
                return state.setValue(HorizontalDirectionalBlock.FACING, rotatedFacing);
            }
        }
        
        return state;
    }
}
