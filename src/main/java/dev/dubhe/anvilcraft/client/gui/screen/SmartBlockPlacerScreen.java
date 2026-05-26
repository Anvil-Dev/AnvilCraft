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
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.client.gui.component.ToggleButton;
import dev.dubhe.anvilcraft.client.gui.component.TriStateButton;
import dev.dubhe.anvilcraft.client.init.ModShaders;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.SmartBlockPlacerMenu;
import dev.dubhe.anvilcraft.network.SmartBlockPlacerActionPacket;
import dev.dubhe.anvilcraft.util.LevelLike;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

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
    
    // 跳过/停止缺少方块按钮贴图
    private static final ResourceLocation SKIP_MISSING = SharedTextures.SMART_BLOCK_PLACER_SKIP_MISSING;
    private static final ResourceLocation STOP_MISSING = SharedTextures.SMART_BLOCK_PLACER_STOP_MISSING;

    private final List<TriStateButton> layerButtons = new ArrayList<>();
    private final TriStateButton[][] positionButtons = new TriStateButton[5][5];
    private ToggleButton layerModeButton;  // 分层显示切换按钮
    private ToggleButton operationModeButton;  // 取物/移动模式切换按钮
    private TriStateButton skipMissingButton;  // 跳过缺少方块按钮
    private TriStateButton stopMissingButton;  // 停止在缺少方块按钮
    private int currentViewLayer = 0;
    private Map<Integer, Set<Integer>> layerPositions = new HashMap<>();
    private boolean showAllLayers = true;
    private boolean isPickupMode = true;
    private boolean isSkipMissingMode = true;  // true=跳过缺少方块, false=停止在缺少方块

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

    // 离屏帧缓冲 — 用于扫描预览后处理
    @Nullable
    private RenderTarget previewFbo;
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
    
    // 蓝图名字滚动相关
    private long structureNameScrollTime = 0;  // 滚动时间戳
    private String lastRenderedStructureName = "";  // 上次渲染的结构名字
    private boolean isStructureNameHovered = false;  // 鼠标是否悬停在文本上
    
    // 结构信息文本基础位置（统一计算）
    private int structureInfoBaseX;
    private int structureInfoBaseY;

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
            this.isSkipMissingMode = this.menu.getBlockEntity().isSkipMissingMode();
            // 检查是否处于蓝图模式(直接检查磁盘槽位)
            this.isBlueprintMode = !this.menu.getBlockEntity().getDiskInventory().getItem(0).isEmpty();
        }

        this.previewWindowX = this.leftPos + 136;
        this.previewWindowY = this.topPos + 18;
        
        // 计算结构信息文本的基础位置
        this.structureInfoBaseX = this.leftPos + 12;
        this.structureInfoBaseY = this.topPos + 36;

        this.initLayerButtons();
        this.initPositionButtons();
        this.initLayerModeButton();
        this.initOperationModeButton();
        this.initMissingModeButton();
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
    
    private void initMissingModeButton() {
        // 只在蓝图模式下初始化缺少方块处理按钮
        if (!this.isBlueprintMode) {
            this.skipMissingButton = null;
            this.stopMissingButton = null;
            return;
        }
        
        // 在取物/移动模式按钮下方，两个按钮并排
        int buttonStartX = this.leftPos + 8;  // 起始X坐标
        int buttonY = this.topPos + 86;   // operationModeButton的Y坐标(130) + 18像素间距

        // 跳过缺少方块按钮
        this.skipMissingButton = new TriStateButton(
            buttonStartX,
            buttonY,
            16,
            16,
            SKIP_MISSING,
            16,
            16,
            (btn) -> this.onSkipMissingButtonClick(),
            List.of(Component.translatable("screen.anvilcraft.smart_block_placer.missing_mode.skip"))
        );
        this.skipMissingButton.setSelected(this.isSkipMissingMode);
        this.addRenderableWidget(this.skipMissingButton);
        
        // 停止在缺少方块按钮
        this.stopMissingButton = new TriStateButton(
            buttonStartX + 18,
            buttonY,
            16,
            16,
            STOP_MISSING,
            16,
            16,
            (btn) -> this.onStopMissingButtonClick(),
            List.of(Component.translatable("screen.anvilcraft.smart_block_placer.missing_mode.stop"))
        );
        this.stopMissingButton.setSelected(!this.isSkipMissingMode);
        this.addRenderableWidget(this.stopMissingButton);
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
    
    private void onSkipMissingButtonClick() {
        if (!this.isSkipMissingMode) {
            this.isSkipMissingMode = true;
            
            // 互斥逻辑：选中skip，取消stop
            if (this.skipMissingButton != null) {
                this.skipMissingButton.setSelected(true);
            }
            if (this.stopMissingButton != null) {
                this.stopMissingButton.setSelected(false);
            }
            
            // 发送网络数据包同步到服务端
            PacketDistributor.sendToServer(new SmartBlockPlacerActionPacket("missingMode", 1));
        }
    }
    
    private void onStopMissingButtonClick() {
        if (this.isSkipMissingMode) {
            this.isSkipMissingMode = false;
            
            // 互斥逻辑：选中stop，取消skip
            if (this.skipMissingButton != null) {
                this.skipMissingButton.setSelected(false);
            }
            if (this.stopMissingButton != null) {
                this.stopMissingButton.setSelected(true);
            }
            
            // 发送网络数据包同步到服务端
            PacketDistributor.sendToServer(new SmartBlockPlacerActionPacket("missingMode", 0));
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
        
        // 更新缺少方块处理按钮（只在蓝图模式下显示）
        this.removeMissingModeButtons();
        this.initMissingModeButton();
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
    
    /**
     * 移除缺少方块处理按钮
     */
    private void removeMissingModeButtons() {
        if (this.skipMissingButton != null) {
            this.removeWidget(this.skipMissingButton);
            this.skipMissingButton = null;
        }
        if (this.stopMissingButton != null) {
            this.removeWidget(this.stopMissingButton);
            this.stopMissingButton = null;
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
        PacketDistributor.sendToServer(new SmartBlockPlacerActionPacket("layer", index));
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
        PacketDistributor.sendToServer(new SmartBlockPlacerActionPacket("mode", this.isPickupMode ? 1 : 0));
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

        PacketDistributor.sendToServer(new SmartBlockPlacerActionPacket("position", positionIndex, this.currentViewLayer + ":" + positionIndex + ":" + newState));
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
                                new SmartBlockPlacerActionPacket("position", positionIndex, this.currentViewLayer + ":" + positionIndex + ":" + this.dragTargetState)
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
    public void removed() {
        if (this.previewFbo != null) {
            this.previewFbo.destroyBuffers();
            this.previewFbo = null;
        }
        super.removed();
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
        
        // 渲染磁盘槽位的虚影（当槽位为空时）
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null && blockEntity.getDiskInventory().getItem(0).isEmpty()) {
            // 获取结构磁盘物品
            net.minecraft.world.item.ItemStack diskStack = dev.dubhe.anvilcraft.init.item.ModItems.STRUCTURE_DISK.get().getDefaultInstance();
            if (!diskStack.isEmpty()) {
                int diskSlotX = i + 8;
                int diskSlotY = j + 119;
                renderMaskedItem(guiGraphics, diskStack, diskSlotX, diskSlotY);
            }
        }
        
        // 蓝图模式下渲染书槽位的虚影（当槽位为空时）
        if (this.isBlueprintMode && blockEntity != null && blockEntity.getBookInventory().getItem(0).isEmpty()) {
            // 获取书物品
            net.minecraft.world.item.ItemStack bookStack = net.minecraft.world.item.Items.BOOK.getDefaultInstance();
            if (!bookStack.isEmpty()) {
                int bookSlotX = i + 46;
                int bookSlotY = j + 86;
                renderMaskedItem(guiGraphics, bookStack, bookSlotX, bookSlotY);
            }
        }
    }
    
    /**
     * 收集所有按钮的tooltip信息
     */
    private void collectButtonTooltips(List<TooltipRenderInfo> tooltipsToRender, int mouseX, int mouseY) {
        // 收集Layer按钮的tooltip
        for (TriStateButton button : this.layerButtons) {
            if (button != null && button.visible && button.isMouseOver(mouseX, mouseY)) {
                if (!button.getTooltips().isEmpty()) {
                    tooltipsToRender.add(new TooltipRenderInfo(
                        this.font,
                        button.getTooltips(),
                        mouseX,
                        mouseY
                    ));
                }
            }
        }
        
        // 收集位置按钮的tooltip
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                TriStateButton button = this.positionButtons[row][col];
                if (button != null && button.visible && button.isMouseOver(mouseX, mouseY)) {
                    if (!button.getTooltips().isEmpty()) {
                        tooltipsToRender.add(new TooltipRenderInfo(
                            this.font,
                            button.getTooltips(),
                            mouseX,
                            mouseY
                        ));
                    }
                }
            }
        }
        
        // 收集分层显示切换按钮的tooltip
        if (this.layerModeButton != null && this.layerModeButton.visible 
            && this.layerModeButton.isMouseOver(mouseX, mouseY)) {
            if (!this.layerModeButton.getTooltips().isEmpty()) {
                tooltipsToRender.add(new TooltipRenderInfo(
                    this.font,
                    this.layerModeButton.getTooltips(),
                    mouseX,
                    mouseY
                ));
            }
        }
        
        // 收集取物/移动模式按钮的tooltip
        if (this.operationModeButton != null && this.operationModeButton.visible 
            && this.operationModeButton.isMouseOver(mouseX, mouseY)) {
            if (!this.operationModeButton.getTooltips().isEmpty()) {
                tooltipsToRender.add(new TooltipRenderInfo(
                    this.font,
                    this.operationModeButton.getTooltips(),
                    mouseX,
                    mouseY
                ));
            }
        }
        
        // 收集跳过缺少方块按钮的tooltip
        if (this.skipMissingButton != null && this.skipMissingButton.visible 
            && this.skipMissingButton.isMouseOver(mouseX, mouseY)) {
            if (!this.skipMissingButton.getTooltips().isEmpty()) {
                tooltipsToRender.add(new TooltipRenderInfo(
                    this.font,
                    this.skipMissingButton.getTooltips(),
                    mouseX,
                    mouseY
                ));
            }
        }
        
        // 收集停止在缺少方块按钮的tooltip
        if (this.stopMissingButton != null && this.stopMissingButton.visible 
            && this.stopMissingButton.isMouseOver(mouseX, mouseY)) {
            if (!this.stopMissingButton.getTooltips().isEmpty()) {
                tooltipsToRender.add(new TooltipRenderInfo(
                    this.font,
                    this.stopMissingButton.getTooltips(),
                    mouseX,
                    mouseY
                ));
            }
        }
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 只渲染标题（方块名称），不渲染“物品栏”文字
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }
        
    /**
     * 渲染半透明的物品虚影
     */
    private void renderMaskedItem(GuiGraphics g, net.minecraft.world.item.ItemStack stack, int x, int y) {
        final int maskColor = 0x99777777;  // 调整透明度，数值越大越透明
        g.renderItem(stack, x, y, 0);
        g.fill(net.minecraft.client.renderer.RenderType.guiOverlay(), x, y, x + 16, y + 16, maskColor);
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
        
        // 最后统一渲染所有tooltip，确保在所有元素上方
        // 收集所有需要渲染的tooltip
        List<TooltipRenderInfo> tooltipsToRender = new ArrayList<>();
        
        // 收集所有按钮的tooltip
        this.collectButtonTooltips(tooltipsToRender, mouseX, mouseY);
        
        // 检查鼠标是否在Disk槽位上
        int diskSlotX = this.leftPos + 8;
        int diskSlotY = this.topPos + 119;
        int diskSlotWidth = 16;
        int diskSlotHeight = 16;
        boolean isMouseOnDiskSlot = mouseX >= diskSlotX && mouseX < diskSlotX + diskSlotWidth
            && mouseY >= diskSlotY && mouseY < diskSlotY + diskSlotHeight;
        
        // 如果鼠标不在Disk槽位上，添加默认tooltip
        if (!isMouseOnDiskSlot) {
            // 获取鼠标悬停位置的slot的tooltip
            if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
                tooltipsToRender.add(new TooltipRenderInfo(
                    this.font,
                    this.getTooltipFromContainerItem(this.hoveredSlot.getItem()),
                    mouseX,
                    mouseY
                ));
            }
        }
        
        // 检查Disk槽位tooltip
        if (isMouseOnDiskSlot) {
            tooltipsToRender.add(new TooltipRenderInfo(
                this.font,
                List.of(Component.translatable("screen.anvilcraft.smart_block_placer.disk_slot")),
                mouseX,
                mouseY
            ));
        }
        
        // 检查书槽位tooltip（仅在蓝图模式下）
        if (this.isBlueprintMode) {
            int bookSlotX = this.leftPos + 8;
            int bookSlotY = this.topPos + 101;
            int bookSlotWidth = 16;
            int bookSlotHeight = 16;
            
            if (mouseX >= bookSlotX && mouseX < bookSlotX + bookSlotWidth
                && mouseY >= bookSlotY && mouseY < bookSlotY + bookSlotHeight) {
                tooltipsToRender.add(new TooltipRenderInfo(
                    this.font,
                    List.of(Component.translatable("screen.anvilcraft.smart_block_placer.book_slot")),
                    mouseX,
                    mouseY
                ));
            }
        }
        
        // 检查缺失方块图标的tooltip
        if (blockEntity != null) {
            ItemStack missingItem = blockEntity.getMissingBlockItem();
            if (!missingItem.isEmpty()) {
                int textX = this.structureInfoBaseX + 4;
                int textY = this.structureInfoBaseY;
                Component missingText = Component.translatable("screen.anvilcraft.smart_block_placer.missing.block");
                int iconX = textX + this.font.width(missingText) + 4;
                int iconY = textY + 18;
                int iconWidth = 16;
                int iconHeight = 16;
                
                if (mouseX >= iconX && mouseX < iconX + iconWidth
                    && mouseY >= iconY && mouseY < iconY + iconHeight) {
                    tooltipsToRender.add(new TooltipRenderInfo(
                        this.font,
                        this.getTooltipFromContainerItem(missingItem),
                        mouseX,
                        mouseY
                    ));
                }
            }
        }
        
        // 统一渲染所有tooltip，使用高Z轴确保在最上层
        for (TooltipRenderInfo tooltipInfo : tooltipsToRender) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 2000);  // 使用更高的Z轴层级
            guiGraphics.renderTooltip(
                tooltipInfo.font,
                tooltipInfo.tooltip,
                java.util.Optional.empty(),
                tooltipInfo.x,
                tooltipInfo.y
            );
            guiGraphics.pose().popPose();
        }
    }
    
    /**
     * 构建并渲染3D预览（含扫描仪后处理）
     */
    @SuppressWarnings({"checkstyle:VariableDeclarationUsageDistance", "checkstyle:LineLength"})
    private void renderPreview(GuiGraphics guiGraphics) {
        if (this.menu.getBlockEntity() == null || this.minecraft == null || this.minecraft.level == null) {
            return;
        }

        LevelLike previewLevelLike = this.getOrCreateCachedPreviewLevelLike();

        // 阶段1: 正常渲染 3D 预览到主帧缓冲
        final double guiScaleD = this.minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor(
            (int) (this.previewWindowX * guiScaleD),
            (int) ((this.minecraft.getWindow().getGuiScaledHeight() - this.previewWindowY - this.previewWindowHeight) * guiScaleD),
            (int) (this.previewWindowWidth * guiScaleD),
            (int) (this.previewWindowHeight * guiScaleD)
        );

        this.renderPreviewWithFixedSize(previewLevelLike, guiGraphics,
            this.previewWindowX + this.previewWindowWidth / 2,
            this.previewWindowY + this.previewWindowHeight / 2 + 5,
            this.previewRotationX,
            this.previewRotationY
        );

        this.renderPlacementRangeBox(guiGraphics);

        RenderSystem.disableScissor();

        // 阶段2&3: 扫描着色器后处理（仅在配置启用时）
        if (RenderState.isScanPreviewEffectEnabled()) {
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
            int srcY = (int) ((this.minecraft.getWindow().getGuiScaledHeight()
                - this.previewWindowY - this.previewWindowHeight) * guiScaleD);

            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainTarget.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.previewFbo.frameBufferId);
            GL30.glBlitFramebuffer(
                srcX, srcY, srcX + fbWidth, srcY + fbHeight,
                0, 0, fbWidth, fbHeight,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST
            );
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, 0);

            mainTarget.bindWrite(false);

            float fbW = this.previewFbo.width;
            float fbH = this.previewFbo.height;
            float screenX = this.previewWindowX * guiScale;
            float screenY = (this.minecraft.getWindow().getGuiScaledHeight()
                - this.previewWindowY - this.previewWindowHeight) * guiScale;

            ShaderInstance shader = ModShaders.getScanPreviewShader();
            if (shader != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.viewport(0, 0,
                    this.minecraft.getWindow().getWidth(),
                    this.minecraft.getWindow().getHeight());

                shader.setSampler("DiffuseSampler", this.previewFbo);
                shader.safeGetUniform("ProjMat").set(ModShaders.getOrthoMatrix());
                shader.safeGetUniform("InSize").set(fbW, fbH);
                shader.safeGetUniform("OutPos").set(screenX, screenY);
                shader.safeGetUniform("OutSize").set(fbW, fbH);
                shader.safeGetUniform("GameTime").set(
                    (float) (System.currentTimeMillis() % 100000) / 1000.0f
                );

                RenderSystem.depthFunc(GL11.GL_ALWAYS);
                shader.apply();

                BufferBuilder bufferbuilder = Tesselator.getInstance()
                    .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
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
        }

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
        
        // 渲染已加载的结构名称（提高图层，与“没有选区”文本一致）
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null) {
            String structureName = blockEntity.getLoadedStructureName();
            if (!structureName.isEmpty()) {
                Component loadedText = Component.translatable("screen.anvilcraft.smart_block_placer.structure.loaded");
                int textX = this.structureInfoBaseX;
                int textY = this.structureInfoBaseY;
                        
                // 禁用深度测试，确保文本在最上层渲染
                RenderSystem.disableDepthTest();
                guiGraphics.pose().pushPose();
                // 将Z轴向前移动，确保文本在最前面
                guiGraphics.pose().translate(0, 0, 1000);
                guiGraphics.drawString(this.font, loadedText, textX, textY, 0x00AA00, false);
                        
                // 渲染蓝图名字（单独一行，带滚动效果）
                int nameY = textY + 10;
                int maxWidth = 80;  // 最大显示宽度
                int textWidth = this.font.width(structureName);
                        
                // 检测鼠标是否悬停在文本区域
                // 使用 Screen 的 hovered 字段获取鼠标位置
                if (this.minecraft != null && this.minecraft.screen != null) {
                    double mouseX = this.minecraft.mouseHandler.xpos() * (double)
                        this.width / (double)
                                        this.minecraft.getWindow().getWidth();
                    double mouseY = this.minecraft.mouseHandler.ypos() * (double)
                        this.height / (double)
                                        this.minecraft.getWindow().getHeight();
                    this.isStructureNameHovered = mouseX >= textX && mouseX <= textX + maxWidth && mouseY >= nameY && mouseY <= nameY + 10;
                }
                        
                if (textWidth > maxWidth) {
                    // 名字太长，需要滚动
                    if (!structureName.equals(lastRenderedStructureName)) {
                        // 切换了新的结构名，重置滚动时间
                        structureNameScrollTime = System.currentTimeMillis();
                        lastRenderedStructureName = structureName;
                    }
                            
                    // 只有鼠标悬停时才滚动
                    if (this.isStructureNameHovered) {
                        // 计算滚动偏移（每8秒一个来回周期）
                        long time = System.currentTimeMillis() - structureNameScrollTime;
                        double scrollCycle = 8000.0;  // 8秒一个完整周期
                        double progress = (time % scrollCycle) / scrollCycle;
                                
                        // 使用正弦波实现来回滚动，但保持左侧始终有文本
                        // 只在 0 和最大偏移之间滚动，避免出现空白
                        double maxScroll = textWidth - maxWidth;
                        double scrollOffset = (Math.sin(progress * Math.PI * 2 - Math.PI / 2) + 1) / 2 * maxScroll;
                                
                        // 应用裁剪区域
                        guiGraphics.enableScissor(textX, nameY - 1, textX + maxWidth, nameY + 10);
                        guiGraphics.drawString(this.font, structureName, textX - (int)
                            scrollOffset, nameY, 0x5555FF, false);
                        guiGraphics.disableScissor();
                    } else {
                        // 鼠标未悬停，重置滚动时间并显示开头部分
                        structureNameScrollTime = System.currentTimeMillis();
                        guiGraphics.enableScissor(textX, nameY - 1, textX + maxWidth, nameY + 10);
                        guiGraphics.drawString(this.font, structureName, textX, nameY, 0x5555FF, false);
                        guiGraphics.disableScissor();
                    }
                } else {
                    // 名字不长，直接显示
                    guiGraphics.drawString(this.font, structureName, textX, nameY, 0x5555FF, false);
                }
                        
                // 显示缺失方块信息（服务端同步）
                ItemStack missingItem = blockEntity.getMissingBlockItem();
                if (!missingItem.isEmpty()) {
                    Component missingText = Component.translatable("screen.anvilcraft.smart_block_placer.missing.block");
                    guiGraphics.drawString(this.font, missingText, textX, textY + 20, 0xFF5555, false);
                    // 渲染缺失方块图标
                    guiGraphics.renderFakeItem(missingItem, textX + this.font.width(missingText) + 4, textY + 18);
                }
                        
                guiGraphics.pose().popPose();
                // 恢复深度测试
                RenderSystem.enableDepthTest();
            } else if (blockEntity.hasInvalidStructure() && !blockEntity.getDiskInventory().getItem(0).isEmpty()) {
                // 磁盘存在但结构数据无效，显示提示信息（带滚动效果）
                // 额外检查磁盘槽位是否为空，确保拿走磁盘后提示消失
                Component invalidText = Component.translatable("screen.anvilcraft.smart_block_placer.no_structure_record");
                int textX = this.structureInfoBaseX;
                int textY = this.structureInfoBaseY;
                int maxWidth = 80;  // 最大显示宽度
                int textWidth = this.font.width(invalidText);
                            
                // 禁用深度测试，确保文本在最上层渲染
                RenderSystem.disableDepthTest();
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 1000);
                            
                if (textWidth > maxWidth) {
                    // 文本太长，需要一直滚动（匀速）
                    long time = System.currentTimeMillis();
                    double scrollSpeed = 30.0;  // 像素/秒，控制滚动速度
                    double totalScrollDistance = textWidth + maxWidth;  // 完整滚动距离（文本宽度+显示宽度）
                    double scrollCycle = totalScrollDistance / scrollSpeed * 1000.0;  // 完整周期时间（毫秒）
                    double progress = (time % scrollCycle) / scrollCycle;
                    
                    // 匀速滚动：从左到右，然后循环
                    double scrollOffset = progress * totalScrollDistance - maxWidth;
                    
                    // 应用裁剪区域
                    guiGraphics.enableScissor(textX, textY - 1, textX + maxWidth, textY + 10);
                    guiGraphics.drawString(this.font, invalidText, textX - (int)
                        scrollOffset, textY, 0xFF5555, false);
                    guiGraphics.disableScissor();
                } else {
                    // 文本不长，直接显示
                    guiGraphics.drawString(this.font, invalidText, textX, textY, 0xFF5555, false);
                }
                            
                guiGraphics.pose().popPose();
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
        
        // 3. 平移到中心（5为奇数，加0.5与RenderSupport保持一致）
        poseStack.translate(-(float) 5 / 2 + 0.5f, -(float) 5 / 2, 0);

        // 4. 先应用X轴旋转
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(this.previewRotationX));

        // 5. Y轴旋转的中心点 - 固定基于5x5范围计算，忽略放置器
        // 与RenderSupport.renderLevelLikeWithFixedSize保持一致
        float offsetX = (float) -5 / 2 + 0.05f + 0.5f;
        float offsetZ = (float) -5 / 2 + 1 + 0.5f;
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
                // 预览中放置器固定朝北，所以使用NORTH作为forward参数
                // 这样旋转计算才能与预览中的朝向一致
                Direction previewFacing = Direction.NORTH;
                
                // 对结构方块应用旋转和倒挂翻转（与服务端放置逻辑保持一致）
                List<dev.dubhe.anvilcraft.util.StructureLoadUtil.BlockPosition> rotatedBlocks = 
                    this.rotateStructureForPreview(loadedStructure, previewFacing, upsideDown);
                
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
     * 为预览旋转结构方块（直接使用 SmartBlockPlacerBlockEntity.rotateStructureDataStatic 确保一致性）
     * 注意：使用Minecraft原生的Rotation API进行旋转
     */
    private List<dev.dubhe.anvilcraft.util.StructureLoadUtil.BlockPosition> rotateStructureForPreview(
        dev.dubhe.anvilcraft.util.StructureLoadUtil.StructureData data,
        Direction forward,
        boolean upsideDown
    ) {
        // 直接使用服务端的旋转逻辑，确保预览和实际放置完全一致
        if (this.minecraft == null || this.minecraft.level == null) {
            return data.blocks;  // 无法获取 level，返回原始数据
        }
        
        // 获取 blockEntity 的位置
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) {
            return data.blocks;
        }
        
        // 计算旋转步数（与 buildBlueprintPositions 保持一致）
        int scannerFacingValue = data.scannerFacing;
        
        // 1. 计算放置器朝向的基础旋转
        int placerRotation = switch (forward) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        
        // 2. 根据Scanner朝向计算额外修正
        int scannerCorrection = switch (scannerFacingValue) {
            case 2 -> 2;  // Scanner北 → +180度
            case 3 -> 2;  // Scanner南 → +180度
            case 4 -> 3;  // Scanner西 → +270度
            case 5 -> 1;  // Scanner东 → +90度
            default -> 0;
        };
        
        // 3. Scanner朝南时额外+180度（在修正基础上再翻180）
        int extraFlip = (scannerFacingValue == 3) ? 2 : 0;
        
        // 4. 总旋转步数 = 基础旋转 + Scanner修正 + Scanner朝南额外翻转
        int rotationSteps = (placerRotation + scannerCorrection + extraFlip) % 4;
        
        // 转换为Minecraft原生Rotation
        net.minecraft.world.level.block.Rotation rotation = switch (rotationSteps) {
            case 1 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90;
            case 2 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_180;
            case 3 -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90;
            default -> net.minecraft.world.level.block.Rotation.NONE;
        };
        
        // 计算结构中心点
        int centerX = data.sizeX / 2;
        int centerZ = data.sizeZ / 2;
        
        // 预览中的基准位置（居中显示）
        int baseX = 2;  // 5x5的中心
        int baseZ = 2;
        
        // 计算left和forward方向（与服务端一致）
        Direction left = forward.getCounterClockWise();

        // 应用旋转和坐标变换
        List<dev.dubhe.anvilcraft.util.StructureLoadUtil.BlockPosition> rotatedBlocks = new ArrayList<>();
        for (var blueprintBlock : data.blocks) {
            // 旋转方块朝向（与服务端一致）
            net.minecraft.world.level.block.state.BlockState rotatedState = blueprintBlock.state().rotate(rotation);
            
            // 倒挂情况下，翻转 half 属性
            if (upsideDown) {
                rotatedState = dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity.flipHalfPropertyStatic(rotatedState);
            }
            
            // 计算相对于中心的偏移（与服务端一致）
            int offsetX = blueprintBlock.x() - centerX;
            int offsetZ = blueprintBlock.z() - centerZ;
            
            // 使用relative方式计算新坐标（与服务端buildBlueprintPositions完全一致）
            // 服务端：basePos.relative(left, offsetX).relative(forward, offsetZ)
            // 预览中：从中心点开始，同样的relative计算
            int newX = baseX;
            int newZ = baseZ;
            
            // 应用left方向偏移
            switch (left) {
                case NORTH -> newZ -= offsetX;
                case SOUTH -> newZ += offsetX;
                case EAST -> newX += offsetX;
                case WEST -> newX -= offsetX;
                default -> {}
            }
            
            // 应用forward方向偏移
            switch (forward) {
                case NORTH -> newZ -= offsetZ;
                case SOUTH -> newZ += offsetZ;
                case EAST -> newX += offsetZ;
                case WEST -> newX -= offsetZ;
                default -> {}
            }
            
            // 倒挂情况下，翻转 y 坐标，并添加偏移（相对于放置器的Y=4）
            int newY = upsideDown ? (4 - blueprintBlock.y()) : blueprintBlock.y();
            
            rotatedBlocks.add(new dev.dubhe.anvilcraft.util.StructureLoadUtil.BlockPosition(
                newX, newY, newZ, rotatedState
            ));
        }
        
        return rotatedBlocks;
    }
    
    /**
     * Tooltip渲染信息记录类
     */
    private record TooltipRenderInfo(
        net.minecraft.client.gui.Font font,
        java.util.List<Component> tooltip,
        int x,
        int y
    ) {}

}
