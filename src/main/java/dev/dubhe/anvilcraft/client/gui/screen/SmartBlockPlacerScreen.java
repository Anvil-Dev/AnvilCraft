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
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper;
import dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.ToggleButton;
import dev.dubhe.anvilcraft.client.gui.component.TriStateButton;
import dev.dubhe.anvilcraft.client.init.ModShaders;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.SmartBlockPlacerMenu;
import dev.dubhe.anvilcraft.network.SmartBlockPlacerActionPacket;
import dev.dubhe.anvilcraft.util.LevelLike;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

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
    private @Nullable ToggleButton layerModeButton;  // 分层显示切换按钮
    private @Nullable ToggleButton operationModeButton;  // 取物/移动模式切换按钮
    private @Nullable TriStateButton skipMissingButton;  // 跳过缺少方块按钮
    private @Nullable TriStateButton stopMissingButton;  // 停止在缺少方块按钮
    private int currentViewLayer = 0;
    private boolean[] layerPositions = new boolean[SmartBlockPlacerBlockEntity.POSITION_COUNT];
    private boolean showAllLayers = true;
    private boolean isPickupMode = true;
    private boolean isSkipMissingMode = true;  // true=跳过缺少方块, false=停止在缺少方块

    private @Nullable Boolean dragTargetState;

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
    private static final int STRUCTURE_INFO_MAX_WIDTH = 80;

    // LevelLike 缓存
    private @Nullable LevelLike cachedPreviewLevelLike;
    private boolean[] cachedLayerPositions = new boolean[SmartBlockPlacerBlockEntity.POSITION_COUNT];
    private int cachedViewLayer = -1;
    private boolean cachedShowAllLayers = true;
    private boolean cachedPickupMode = true;
    private boolean cachedBlueprintMode = false;  // 缓存蓝图模式状态
    private BlockState[] cachedBlueprintStates = new BlockState[SmartBlockPlacerBlockEntity.POSITION_COUNT];
    private long cachedGameTimeBlockType = -1;  // 用于追踪方块类型的游戏时间

    // 蓝图名字滚动相关
    private long structureNameScrollTime = 0;  // 滚动时间戳
    private String lastRenderedStructureName = "";  // 上次渲染的结构名字

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

        SmartBlockPlacerBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null) {
            this.currentViewLayer = blockEntity.getSelectedLayer();
            this.layerPositions = blockEntity.getLayerPositions().clone();
            this.isPickupMode = blockEntity.isPickupMode();
            this.isSkipMissingMode = blockEntity.isSkipMissingMode();
            // 检查是否处于蓝图模式(直接检查磁盘槽位)
            this.isBlueprintMode = !blockEntity.getBlueprintItemHandler().getStackInSlot(0).isEmpty();
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
                16,
                16,
                LAYER_DEFAULT[i],
                16,
                16,
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

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int positionIndex = row * 5 + col;
                boolean isSelected = this.layerPositions[SmartBlockPlacerBlockEntity.getPositionIndex(
                    this.currentViewLayer,
                    positionIndex
                )];

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

        ToggleButton button = new ToggleButton(
            buttonX,
            buttonY,
            16,
            16,
            this.showAllLayers ? LAYER_ALL : LAYER_SINGLE,
            16,
            16,
            (btn) -> this.onLayerModeButtonClick(),
            List.of(this.getLayerModeTooltip())
        );
        this.layerModeButton = button;
        this.updateLayerModeButtonState();
        this.addRenderableWidget(button);
    }

    private void initOperationModeButton() {
        // 在分层显示切换按钮下方
        int buttonX = this.leftPos + 232;  // 与layerModeButton对齐
        int buttonY = this.topPos + 130;   // layerModeButton的Y坐标(112) + 18像素间距

        ToggleButton button = new ToggleButton(
            buttonX,
            buttonY,
            16,
            16,
            this.isPickupMode ? PICKUP_MODE : MOVE_MODE,
            16,
            16,
            (btn) -> this.onOperationModeButtonClick(),
            List.of(this.getOperationModeTooltip())
        );
        this.operationModeButton = button;
        this.updateOperationModeButtonState();
        this.addRenderableWidget(button);
    }

    private void initMissingModeButton() {
        // 只在蓝图模式下初始化缺少方块处理按钮
        if (!this.isBlueprintMode) {
            return;
        }

        // 在取物/移动模式按钮下方，两个按钮并排
        int buttonStartX = this.leftPos + 8;  // 起始X坐标
        int buttonY = this.topPos + 86;   // operationModeButton的Y坐标(130) + 18像素间距

        // 跳过缺少方块按钮
        TriStateButton skipButton = new TriStateButton(
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
        this.skipMissingButton = skipButton;
        this.addRenderableWidget(skipButton);

        // 停止在缺少方块按钮
        TriStateButton stopButton = new TriStateButton(
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
        this.stopMissingButton = stopButton;
        this.updateMissingModeButtonState();
        this.addRenderableWidget(stopButton);
    }

    private Component getLayerModeTooltip() {
        if (this.showAllLayers) {
            return Component.translatable("screen.anvilcraft.smart_block_placer.layer_mode.all");
        } else {
            return Component.translatable("screen.anvilcraft.smart_block_placer.layer_mode.single", this.currentViewLayer + 1, 5);
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
        this.setSkipMissingMode(true);
    }

    private void onStopMissingButtonClick() {
        this.setSkipMissingMode(false);
    }

    private void setSkipMissingMode(boolean skipMissingMode) {
        if (this.isSkipMissingMode == skipMissingMode) {
            return;
        }
        this.isSkipMissingMode = skipMissingMode;
        this.updateMissingModeButtonState();
        PacketDistributor.sendToServer(new SmartBlockPlacerActionPacket("missingMode", skipMissingMode ? 1 : 0));
    }

    private void updateMissingModeButtonState() {
        TriStateButton skipButton = this.skipMissingButton;
        if (skipButton != null) {
            skipButton.setSelected(this.isSkipMissingMode);
        }
        TriStateButton stopButton = this.stopMissingButton;
        if (stopButton != null) {
            stopButton.setSelected(!this.isSkipMissingMode);
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
            Arrays.fill(this.layerPositions, false);
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
            this.removeWidget(button);
        }
        this.layerButtons.clear();
    }

    /**
     * 移除缺少方块处理按钮
     */
    private void removeMissingModeButtons() {
        TriStateButton skipButton = this.skipMissingButton;
        if (skipButton != null) {
            this.removeWidget(skipButton);
            this.skipMissingButton = null;
        }
        TriStateButton stopButton = this.stopMissingButton;
        if (stopButton != null) {
            this.removeWidget(stopButton);
            this.stopMissingButton = null;
        }
    }

    private TriStateButton createPositionButton(int row, int col, int positionIndex, int startX, int startY, boolean selected) {
        List<Component> tooltipSelected = List.of(Component.translatable(
            "screen.anvilcraft.smart_block_placer.position.selected",
            row + 1,
            col + 1
        ));
        List<Component> tooltipUnselected = List.of(Component.translatable(
            "screen.anvilcraft.smart_block_placer.position.unselected",
            row + 1,
            col + 1
        ));

        int xpos = startX + col * 18;
        int ypos = startY + row * 18;

        TriStateButton button = new TriStateButton(
            xpos,
            ypos,
            16,
            16,
            POSITION_SELECT,
            16,
            16,
            (btn) -> onPositionButtonClick(btn, positionIndex, tooltipSelected, tooltipUnselected),
            selected ? tooltipSelected : tooltipUnselected
        );
        button.setSelected(selected);
        return button;
    }

    private void onLayerButtonClick(int index) {
        this.currentViewLayer = index;

        // 从服务端获取最新配置，创建深拷贝
        SmartBlockPlacerBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null) {
            this.layerPositions = blockEntity.getLayerPositions().clone();
        }

        // 更新layer按钮（互斥）
        this.updateLayerButtonState();

        // 更新棋盘显示
        this.updatePositionButtons();

        // 更新分层显示切换按钮的tooltip
        if (!this.showAllLayers) {
            this.updateLayerModeButtonState();
        }

        // 通知服务端
        PacketDistributor.sendToServer(new SmartBlockPlacerActionPacket("layer", index));
    }

    private void updateLayerButtonState() {
        for (int i = 0; i < 5; i++) {
            this.layerButtons.get(4 - i).setSelected(i == this.currentViewLayer);
        }
    }

    private void onLayerModeButtonClick() {
        this.showAllLayers = !this.showAllLayers;
        this.updateLayerModeButtonState();
    }

    private void onOperationModeButtonClick() {
        this.isPickupMode = !this.isPickupMode;
        this.updateOperationModeButtonState();

        // 发送网络数据包同步到服务端
        PacketDistributor.sendToServer(new SmartBlockPlacerActionPacket("mode", this.isPickupMode ? 1 : 0));
    }

    private void updatePositionButtons() {
        // 蓝图模式下不更新位置按钮
        if (this.isBlueprintMode) {
            return;
        }

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                TriStateButton button = this.positionButtons[row][col];
                if (button == null) continue;

                int positionIndex = row * 5 + col;
                boolean isSelected = this.layerPositions[SmartBlockPlacerBlockEntity.getPositionIndex(
                    this.currentViewLayer,
                    positionIndex
                )];
                button.setSelected(isSelected);

                // 更新tooltip以反映当前层级的选择状态
                List<Component> tooltipSelected = List.of(Component.translatable(
                    "screen.anvilcraft.smart_block_placer.position.selected",
                    row + 1,
                    col + 1
                ));
                List<Component> tooltipUnselected = List.of(Component.translatable(
                    "screen.anvilcraft.smart_block_placer.position.unselected",
                    row + 1,
                    col + 1
                ));
                button.setTooltips(isSelected ? tooltipSelected : tooltipUnselected);
            }
        }
    }

    private void updateLayerModeButtonState() {
        ToggleButton button = this.layerModeButton;
        if (button == null) {
            return;
        }
        button.setSelected(this.showAllLayers);
        button.setTexture(this.showAllLayers ? LAYER_ALL : LAYER_SINGLE);
        button.setTooltips(List.of(this.getLayerModeTooltip()));
    }

    private void updateOperationModeButtonState() {
        ToggleButton button = this.operationModeButton;
        if (button == null) {
            return;
        }
        button.setSelected(this.isPickupMode);
        button.setTexture(this.isPickupMode ? PICKUP_MODE : MOVE_MODE);
        button.setTooltips(List.of(this.getOperationModeTooltip()));
    }

    private void onPositionButtonClick(
        TriStateButton button,
        int positionIndex,
        List<Component> tooltipSelected,
        List<Component> tooltipUnselected
    ) {
        boolean newState = !button.isSelected();
        button.setSelected(newState);
        button.setTooltips(newState ? tooltipSelected : tooltipUnselected);
        this.layerPositions[SmartBlockPlacerBlockEntity.getPositionIndex(this.currentViewLayer, positionIndex)] = newState;

        PacketDistributor.sendToServer(new SmartBlockPlacerActionPacket(
            "position",
            positionIndex,
            this.currentViewLayer + ":" + positionIndex + ":" + newState
        ));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.dragTargetState = null;
        this.isPreviewDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isMouseInPreviewWindow(double mouseX, double mouseY) {
        return mouseX >= this.previewWindowX && mouseX < this.previewWindowX + this.previewWindowWidth && mouseY >= this.previewWindowY
               && mouseY < this.previewWindowY + this.previewWindowHeight;
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
            this.previewRotationX = Math.clamp(this.previewRotationX, MIN_ROTATION_X, MAX_ROTATION_X);

            this.lastMouseX = currentMouseX;
            this.lastMouseY = currentMouseY;
            return true;
        }

        Boolean dragTargetState = this.dragTargetState;
        if (dragTargetState != null) {
            boolean targetState = dragTargetState;
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    TriStateButton btn = this.positionButtons[row][col];
                    if (btn != null && btn.isMouseOver(mouseX, mouseY)) {
                        int positionIndex = row * 5 + col;
                        if (btn.isSelected() != targetState) {
                            btn.setSelected(targetState);
                            this.layerPositions[SmartBlockPlacerBlockEntity.getPositionIndex(
                                this.currentViewLayer,
                                positionIndex
                            )] = targetState;
                            PacketDistributor.sendToServer(new SmartBlockPlacerActionPacket(
                                "position",
                                positionIndex,
                                this.currentViewLayer + ":" + positionIndex + ":" + targetState
                            ));
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
        this.cachedPreviewLevelLike = null;
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
        if (blockEntity != null && blockEntity.getBlueprintItemHandler().getStackInSlot(0).isEmpty()) {
            // 获取结构磁盘物品
            ItemStack diskStack = ModItems.STRUCTURE_DISK.get()
                .getDefaultInstance();
            if (!diskStack.isEmpty()) {
                int diskSlotX = i + 8;
                int diskSlotY = j + 119;
                renderMaskedItem(guiGraphics, diskStack, diskSlotX, diskSlotY);
            }
        }

        // 蓝图模式下渲染书槽位的虚影（当槽位为空时）
        if (this.isBlueprintMode && this.menu.getBookInventory().getItem(0).isEmpty()) {
            // 获取书物品
            ItemStack bookStack = Items.BOOK.getDefaultInstance();
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
                    tooltipsToRender.add(new TooltipRenderInfo(this.font, button.getTooltips(), mouseX, mouseY));
                }
            }
        }

        // 收集位置按钮的tooltip
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                TriStateButton button = this.positionButtons[row][col];
                if (button != null && button.visible && button.isMouseOver(mouseX, mouseY)) {
                    if (!button.getTooltips().isEmpty()) {
                        tooltipsToRender.add(new TooltipRenderInfo(this.font, button.getTooltips(), mouseX, mouseY));
                    }
                }
            }
        }

        // 收集分层显示切换按钮的tooltip
        if (this.layerModeButton != null && this.layerModeButton.visible && this.layerModeButton.isMouseOver(mouseX, mouseY)) {
            if (!this.layerModeButton.getTooltips().isEmpty()) {
                tooltipsToRender.add(new TooltipRenderInfo(this.font, this.layerModeButton.getTooltips(), mouseX, mouseY));
            }
        }

        // 收集取物/移动模式按钮的tooltip
        if (this.operationModeButton != null && this.operationModeButton.visible && this.operationModeButton.isMouseOver(mouseX, mouseY)) {
            if (!this.operationModeButton.getTooltips().isEmpty()) {
                tooltipsToRender.add(new TooltipRenderInfo(this.font, this.operationModeButton.getTooltips(), mouseX, mouseY));
            }
        }

        // 收集跳过缺少方块按钮的tooltip
        if (this.skipMissingButton != null && this.skipMissingButton.visible && this.skipMissingButton.isMouseOver(mouseX, mouseY)) {
            if (!this.skipMissingButton.getTooltips().isEmpty()) {
                tooltipsToRender.add(new TooltipRenderInfo(this.font, this.skipMissingButton.getTooltips(), mouseX, mouseY));
            }
        }

        // 收集停止在缺少方块按钮的tooltip
        if (this.stopMissingButton != null && this.stopMissingButton.visible && this.stopMissingButton.isMouseOver(mouseX, mouseY)) {
            if (!this.stopMissingButton.getTooltips().isEmpty()) {
                tooltipsToRender.add(new TooltipRenderInfo(this.font, this.stopMissingButton.getTooltips(), mouseX, mouseY));
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
    private void renderMaskedItem(GuiGraphics g, ItemStack stack, int x, int y) {
        final int maskColor = 0x99777777;  // 调整透明度，数值越大越透明
        g.renderItem(stack, x, y, 0);
        g.fill(RenderType.guiOverlay(), x, y, x + 16, y + 16, maskColor);
    }

    @SuppressWarnings("deprecation")
    private void renderDisplayedBlock(
        GuiGraphics guiGraphics,
        Either<ItemStack, BlockState> displayedBlock,
        int x,
        int y
    ) {
        displayedBlock
            .ifLeft(stack -> guiGraphics.renderFakeItem(stack, x, y))
            .ifRight(state -> {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(x + 8.0F, y + 12.0F, 100.0F);
                guiGraphics.pose().scale(10.0F, -10.0F, 10.0F);
                guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(30.0F));
                guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(45.0F));
                guiGraphics.pose().translate(-0.5F, -0.5F, -0.5F);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    state,
                    guiGraphics.pose(),
                    guiGraphics.bufferSource(),
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY
                );
                guiGraphics.flush();
                guiGraphics.pose().popPose();
            });
    }

    @Override
    public void containerTick() {
        super.containerTick();

        // 定期从 blockEntity 同步数据到客户端,确保磁盘插入等操作的选区变化能实时更新
        SmartBlockPlacerBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) {
            return;
        }
        this.synchronizeFromBlockEntity(blockEntity);
    }

    private void synchronizeFromBlockEntity(SmartBlockPlacerBlockEntity blockEntity) {
        this.updateBlueprintMode(blockEntity);

        boolean updatePositionButtons = false;
        boolean[] newLayerPositions = blockEntity.getLayerPositions();
        if (!Arrays.equals(this.layerPositions, newLayerPositions)) {
            this.layerPositions = newLayerPositions.clone();
            updatePositionButtons = true;
        }

        int newViewLayer = blockEntity.getSelectedLayer();
        if (newViewLayer != this.currentViewLayer) {
            this.currentViewLayer = newViewLayer;
            this.updateLayerButtonState();
            updatePositionButtons = true;
        }
        if (updatePositionButtons) {
            this.updatePositionButtons();
        }

        boolean newPickupMode = blockEntity.isPickupMode();
        if (newPickupMode != this.isPickupMode) {
            this.isPickupMode = newPickupMode;
            this.updateOperationModeButtonState();
        }

        boolean newSkipMissingMode = blockEntity.isSkipMissingMode();
        if (newSkipMissingMode != this.isSkipMissingMode) {
            this.isSkipMissingMode = newSkipMissingMode;
            this.updateMissingModeButtonState();
        }
    }

    private void updateBlueprintMode(SmartBlockPlacerBlockEntity blockEntity) {
        boolean blueprintMode = !blockEntity.getBlueprintItemHandler().getStackInSlot(0).isEmpty();
        if (blueprintMode == this.isBlueprintMode) {
            return;
        }
        this.isBlueprintMode = blueprintMode;
        this.updateButtonsForBlueprintMode();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 检测蓝图模式变化(containerTick已经处理,这里作为备用)
        SmartBlockPlacerBlockEntity blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null) {
            this.updateBlueprintMode(blockEntity);
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
        boolean isMouseOnDiskSlot =
            mouseX >= diskSlotX && mouseX < diskSlotX + diskSlotWidth && mouseY >= diskSlotY && mouseY < diskSlotY + diskSlotHeight;

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

            if (mouseX >= bookSlotX && mouseX < bookSlotX + bookSlotWidth && mouseY >= bookSlotY && mouseY < bookSlotY + bookSlotHeight) {
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
            Either<ItemStack, BlockState> missingBlock = blockEntity.getMissingBlock();
            if (missingBlock != null) {
                int textX = this.structureInfoBaseX + 4;
                int textY = this.structureInfoBaseY;
                Component missingText = Component.translatable("screen.anvilcraft.smart_block_placer.missing.block");
                int iconX = textX + this.font.width(missingText) + 4;
                int iconY = textY + 18;
                int iconWidth = 16;
                int iconHeight = 16;

                if (mouseX >= iconX && mouseX < iconX + iconWidth && mouseY >= iconY && mouseY < iconY + iconHeight) {
                    List<Component> tooltip = missingBlock.map(
                        this::getTooltipFromContainerItem,
                        state -> List.of(state.getBlock().getName())
                    );
                    tooltipsToRender.add(new TooltipRenderInfo(this.font, tooltip, mouseX, mouseY));
                }
            }
        }

        // 统一渲染所有tooltip，使用高Z轴确保在最上层
        for (TooltipRenderInfo tooltipInfo : tooltipsToRender) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 2000);  // 使用更高的Z轴层级
            guiGraphics.renderTooltip(tooltipInfo.font, tooltipInfo.tooltip, Optional.empty(), tooltipInfo.x, tooltipInfo.y);
            guiGraphics.pose().popPose();
        }
    }

    /**
     * 构建并渲染3D预览（含扫描仪后处理）
     */
    private void renderPreview(GuiGraphics guiGraphics) {
        SmartBlockPlacerBlockEntity blockEntity = this.menu.getBlockEntity();
        Minecraft minecraft = this.minecraft;
        if (blockEntity == null || minecraft == null) {
            return;
        }
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }

        LevelLike previewLevelLike = this.getOrCreateCachedPreviewLevelLike(blockEntity, level);

        // 阶段1: 正常渲染 3D 预览到主帧缓冲
        final double guiScaleD = minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor(
            (int) (this.previewWindowX * guiScaleD),
            (int) ((minecraft.getWindow().getGuiScaledHeight() - this.previewWindowY - this.previewWindowHeight) * guiScaleD),
            (int) (this.previewWindowWidth * guiScaleD),
            (int) (this.previewWindowHeight * guiScaleD)
        );

        this.renderPreviewWithFixedSize(
            previewLevelLike,
            guiGraphics,
            this.previewWindowX + this.previewWindowWidth / 2,
            this.previewWindowY + this.previewWindowHeight / 2 + 5,
            this.previewRotationX,
            this.previewRotationY
        );

        this.renderPlacementRangeBox(guiGraphics, minecraft);

        RenderSystem.disableScissor();

        // 阶段2&3: 扫描着色器后处理（仅在配置启用时）
        if (RenderState.isScanPreviewEffectEnabled()) {
            int guiScale = (int) minecraft.getWindow().getGuiScale();
            int fbWidth = this.previewWindowWidth * guiScale;
            int fbHeight = this.previewWindowHeight * guiScale;

            RenderTarget previewFbo = this.previewFbo;
            if (previewFbo == null) {
                previewFbo = new TextureTarget(fbWidth, fbHeight, true, Minecraft.ON_OSX);
                this.previewFbo = previewFbo;
            } else if (previewFbo.width != fbWidth || previewFbo.height != fbHeight) {
                previewFbo.resize(fbWidth, fbHeight, Minecraft.ON_OSX);
            }

            final RenderTarget mainTarget = minecraft.getMainRenderTarget();
            int srcX = (int) (this.previewWindowX * guiScaleD);
            int srcY = (int) (
                (
                    minecraft.getWindow().getGuiScaledHeight() - this.previewWindowY - this.previewWindowHeight
                ) * guiScaleD
            );

            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mainTarget.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previewFbo.frameBufferId);
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
            RenderSystem.viewport(0, 0, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());

            ShaderInstance shader = ModShaders.getScanPreviewShader();
            float fbW = previewFbo.width;
            float fbH = previewFbo.height;
            shader.setSampler("DiffuseSampler", previewFbo);
            shader.safeGetUniform("ProjMat").set(ModShaders.getOrthoMatrix());
            shader.safeGetUniform("InSize").set(fbW, fbH);
            float screenX = this.previewWindowX * guiScale;
            float screenY = (minecraft.getWindow().getGuiScaledHeight() - this.previewWindowY - this.previewWindowHeight) * guiScale;
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

            previewFbo.unbindRead();
        }

        // 如果没有配置选区位置且不在蓝图模式下，显示提示文本（在裁剪区域外渲染，确保在最上层）
        if (!this.isBlueprintMode && hasNoSelectedPositions(blockEntity.getLayerPositions())) {
            // 禁用深度测试，确保文本在最上层渲染
            RenderSystem.disableDepthTest();
            guiGraphics.pose().pushPose();
            // 将Z轴向前移动，确保文本在最前面
            guiGraphics.pose().translate(0, 0, 1000);
            guiGraphics.pose().scale(0.8f, 0.8f, 0.8f);
            Component emptyText = Component.translatable("screen.anvilcraft.smart_block_placer.preview.empty");
            int textWidth = (int) (this.font.width(emptyText) * 0.8f);
            int textX = this.previewWindowX + (this.previewWindowWidth - textWidth) / 2;
            int textY = this.previewWindowY + (this.previewWindowHeight - (int) (this.font.lineHeight * 0.8f)) / 2;
            guiGraphics.drawString(this.font, emptyText, (int) (textX / 0.8f), (int) (textY / 0.8f), 0xFFFFFF, true);
            guiGraphics.pose().popPose();
            // 恢复深度测试
            RenderSystem.enableDepthTest();
        }

        this.renderStructureInfo(guiGraphics, blockEntity, minecraft);
    }

    private void renderStructureInfo(
        GuiGraphics guiGraphics,
        SmartBlockPlacerBlockEntity blockEntity,
        Minecraft minecraft
    ) {
        String structureName = blockEntity.getBlueprint().name();
        if (!structureName.isEmpty()) {
            this.renderLoadedStructureInfo(guiGraphics, blockEntity, minecraft, structureName);
        } else if (blockEntity.getBlueprint().invalid()
                   && !blockEntity.getBlueprintItemHandler().getStackInSlot(0).isEmpty()) {
            this.renderInvalidStructureInfo(guiGraphics);
        }
    }

    private void renderLoadedStructureInfo(
        GuiGraphics guiGraphics,
        SmartBlockPlacerBlockEntity blockEntity,
        Minecraft minecraft,
        String structureName
    ) {
        RenderSystem.disableDepthTest();
        guiGraphics.pose().pushPose();
        try {
            guiGraphics.pose().translate(0, 0, 1000);
            int textX = this.structureInfoBaseX;
            int textY = this.structureInfoBaseY;
            Component loadedText = Component.translatable("screen.anvilcraft.smart_block_placer.structure.loaded");
            guiGraphics.drawString(this.font, loadedText, textX, textY, 0x00AA00, false);
            this.renderStructureName(guiGraphics, minecraft, structureName, textX, textY + 10);

            Either<ItemStack, BlockState> missingBlock = blockEntity.getMissingBlock();
            if (missingBlock != null) {
                Component missingText = Component.translatable("screen.anvilcraft.smart_block_placer.missing.block");
                guiGraphics.drawString(this.font, missingText, textX, textY + 20, 0xFF5555, false);
                this.renderDisplayedBlock(
                    guiGraphics,
                    missingBlock,
                    textX + this.font.width(missingText) + 4,
                    textY + 18
                );
            }
        } finally {
            guiGraphics.pose().popPose();
            RenderSystem.enableDepthTest();
        }
    }

    private void renderStructureName(
        GuiGraphics guiGraphics,
        Minecraft minecraft,
        String structureName,
        int textX,
        int textY
    ) {
        int textWidth = this.font.width(structureName);
        if (textWidth <= STRUCTURE_INFO_MAX_WIDTH) {
            guiGraphics.drawString(this.font, structureName, textX, textY, 0x5555FF, false);
            return;
        }

        double mouseX = minecraft.mouseHandler.xpos() * (double) this.width / minecraft.getWindow().getWidth();
        double mouseY = minecraft.mouseHandler.ypos() * (double) this.height / minecraft.getWindow().getHeight();
        boolean structureNameHovered = mouseX >= textX && mouseX <= textX + STRUCTURE_INFO_MAX_WIDTH
                                       && mouseY >= textY && mouseY <= textY + 10;
        if (!structureName.equals(this.lastRenderedStructureName)) {
            this.structureNameScrollTime = System.currentTimeMillis();
            this.lastRenderedStructureName = structureName;
        }

        int drawX = textX;
        if (structureNameHovered) {
            long time = System.currentTimeMillis() - this.structureNameScrollTime;
            double progress = time % 8000.0 / 8000.0;
            double maxScroll = textWidth - STRUCTURE_INFO_MAX_WIDTH;
            drawX -= (int) ((Math.sin(progress * Math.PI * 2 - Math.PI / 2) + 1) / 2 * maxScroll);
        } else {
            this.structureNameScrollTime = System.currentTimeMillis();
        }

        guiGraphics.enableScissor(textX, textY - 1, textX + STRUCTURE_INFO_MAX_WIDTH, textY + 10);
        try {
            guiGraphics.drawString(this.font, structureName, drawX, textY, 0x5555FF, false);
        } finally {
            guiGraphics.disableScissor();
        }
    }

    private void renderInvalidStructureInfo(GuiGraphics guiGraphics) {
        RenderSystem.disableDepthTest();
        guiGraphics.pose().pushPose();
        try {
            guiGraphics.pose().translate(0, 0, 1000);
            int textX = this.structureInfoBaseX;
            int textY = this.structureInfoBaseY;
            Component invalidText = Component.translatable("screen.anvilcraft.smart_block_placer.no_structure_record");
            int textWidth = this.font.width(invalidText);
            if (textWidth <= STRUCTURE_INFO_MAX_WIDTH) {
                guiGraphics.drawString(this.font, invalidText, textX, textY, 0xFF5555, false);
                return;
            }

            double scrollSpeed = 30.0;
            double totalScrollDistance = textWidth + STRUCTURE_INFO_MAX_WIDTH;
            double scrollCycle = totalScrollDistance / scrollSpeed * 1000.0;
            double progress = System.currentTimeMillis() % scrollCycle / scrollCycle;
            double scrollOffset = progress * totalScrollDistance - STRUCTURE_INFO_MAX_WIDTH;
            guiGraphics.enableScissor(textX, textY - 1, textX + STRUCTURE_INFO_MAX_WIDTH, textY + 10);
            try {
                guiGraphics.drawString(this.font, invalidText, textX - (int) scrollOffset, textY, 0xFF5555, false);
            } finally {
                guiGraphics.disableScissor();
            }
        } finally {
            guiGraphics.pose().popPose();
            RenderSystem.enableDepthTest();
        }
    }

    /**
     * 使用固定尺寸渲染预览
     */
    private void renderPreviewWithFixedSize(
        LevelLike level,
        GuiGraphics guiGraphics,
        int posX,
        int posY,
        float rotationX,
        float rotationY
    ) {
        RenderSupport.renderLevelLikeWithFixedSize(level, guiGraphics, posX, posY, (float) 80.0, rotationX, rotationY, 5, 5, -0.5f);
    }

    /**
     * 渲染3D放置范围框
     */
    private void renderPlacementRangeBox(GuiGraphics guiGraphics, Minecraft minecraft) {
        // 获取缓冲区
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
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
        float scaleX = 80.0f / (5 * Mth.SQRT_OF_TWO);
        float scaleY = 80.0f / 5.0f;
        float scale = Math.min(scaleY, scaleX);
        poseStack.scale(-scale, -scale, -scale);

        // 3. 平移到中心（5为奇数，加0.5与RenderSupport保持一致）
        poseStack.translate(-(float) 5 / 2, -(float) 5 / 2, 0);

        // 4. 先应用X轴旋转
        poseStack.mulPose(Axis.XP.rotationDegrees(this.previewRotationX));

        // 5. Y轴旋转的中心点 - 固定基于5x5范围计算，忽略放置器
        // 与RenderSupport.renderLevelLikeWithFixedSize保持一致
        float offsetX = (float) -5 / 2 + 0.05f;
        float offsetZ = (float) -5 / 2 + 1;
        poseStack.translate(-offsetX, 0, -offsetZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(this.previewRotationY + 45));
        poseStack.translate(offsetX, 0, offsetZ);

        // 6. 平移Z轴（与方块保持一致，在Z=-1处渲染）
        poseStack.translate(0, 0, -1);

        // 7. 创建范围框（与方块渲染的位置空间一致）
        final VoxelShape rangeShape = Shapes.create(0.0, 0.0, 0.0, 5.0, 5.0, 5.0);

        // 8. 渲染范围框（青色，与世界中一致）
        TooltipRenderHelper.renderOutline(poseStack, consumer, 0, 0, 0, BlockPos.ZERO, rangeShape, 0xFF00FFCC);

        buffers.endBatch(RenderType.lines());
        poseStack.popPose();
    }

    /**
     * 获取或创建缓存的 LevelLike 实例
     * 只在状态改变时重建，避免每帧重新构建
     */
    private LevelLike getOrCreateCachedPreviewLevelLike(
        SmartBlockPlacerBlockEntity blockEntity,
        ClientLevel level
    ) {
        // 检查缓存是否有效
        // 使用客户端本地的 layerPositions 而不是 blockEntity 的，避免网络延迟导致的预览不更新
        // 同时检查游戏时间，确保方块类型能实时切换
        long currentGameTime = level.getGameTime();
        long currentBlockTypeTime = currentGameTime / (PREVIEW_BLOCK_SWITCH_INTERVAL * 2);

        BlockState[] currentBlueprintStates = blockEntity.getBlueprint().states();
        LevelLike cachedLevel = this.cachedPreviewLevelLike;
        if (cachedLevel != null
            && Arrays.equals(this.cachedLayerPositions, this.layerPositions)
            && this.cachedViewLayer == this.currentViewLayer
            && this.cachedShowAllLayers == this.showAllLayers
            && this.cachedPickupMode == this.isPickupMode
            && this.cachedBlueprintMode == this.isBlueprintMode
            && Arrays.equals(this.cachedBlueprintStates, currentBlueprintStates)
            && this.cachedGameTimeBlockType == currentBlockTypeTime) {
            return cachedLevel;
        }

        LevelLike rebuiltLevel = this.buildPreviewLevelLike(blockEntity, level);
        this.cachedPreviewLevelLike = rebuiltLevel;
        this.cachedLayerPositions = this.layerPositions.clone();
        this.cachedViewLayer = this.currentViewLayer;
        this.cachedShowAllLayers = this.showAllLayers;
        this.cachedPickupMode = this.isPickupMode;
        this.cachedBlueprintMode = this.isBlueprintMode;
        this.cachedBlueprintStates = currentBlueprintStates.clone();
        this.cachedGameTimeBlockType = currentBlockTypeTime;
        return rebuiltLevel;
    }

    /**
     * 构建预览用的LevelLike实例
     *
     * @return 预览数据
     */
    private LevelLike buildPreviewLevelLike(SmartBlockPlacerBlockEntity blockEntity, ClientLevel level) {
        LevelLike previewLevelLike = new LevelLike(level);
        previewLevelLike.setAllLayersVisible(this.showAllLayers);

        if (!this.showAllLayers) {
            previewLevelLike.setCurrentVisibleLayer(this.currentViewLayer);
        }

        // 获取放置器的状态
        boolean upsideDown = false;
        boolean powered = false;
        boolean overload = true;
        BlockState placerState = level.getBlockState(blockEntity.getBlockPos());
        if (placerState.getBlock() instanceof SmartBlockPlacerBlock) {
            upsideDown = placerState.getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
            powered = placerState.getValue(SmartBlockPlacerBlock.POWERED);
            overload = placerState.getValue(SmartBlockPlacerBlock.OVERLOAD);
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
            ModBlocks.SMART_BLOCK_PLACER.get()
                .defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(SmartBlockPlacerBlock.UPSIDE_DOWN, upsideDown)
                .setValue(SmartBlockPlacerBlock.POWERED, powered)
                .setValue(SmartBlockPlacerBlock.OVERLOAD, overload)
        );

        // 蓝图模式：渲染磁盘中的结构
        if (this.isBlueprintMode) {
            BlockState[] blueprintStates = blockEntity.getBlueprint().states();
            for (int layer = 0; layer < SmartBlockPlacerBlockEntity.POSITION_GRID_SIZE; layer++) {
                for (int position = 0; position < SmartBlockPlacerBlockEntity.POSITIONS_PER_LAYER; position++) {
                    int index = SmartBlockPlacerBlockEntity.getPositionIndex(layer, position);
                    if (blueprintStates[index].isAir()) {
                        continue;
                    }
                    BlockState blueprintState = blockEntity.getBlueprintStateForPlacement(
                        index,
                        Direction.NORTH,
                        upsideDown
                    );
                    int row = position / SmartBlockPlacerBlockEntity.POSITION_GRID_SIZE;
                    int column = position % SmartBlockPlacerBlockEntity.POSITION_GRID_SIZE;
                    previewLevelLike.setBlockState(
                        new BlockPos(column, layer, row),
                        blueprintState
                    );
                }
            }
            return previewLevelLike;
        }

        // 普通模式：使用客户端本地的 layerPositions，确保快速拖动时预览能及时更新
        if (hasNoSelectedPositions(this.layerPositions)) {
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
        for (int layer = 0; layer < SmartBlockPlacerBlockEntity.POSITION_GRID_SIZE; layer++) {
            for (int position = 0; position < SmartBlockPlacerBlockEntity.POSITIONS_PER_LAYER; position++) {
                if (!this.layerPositions[SmartBlockPlacerBlockEntity.getPositionIndex(layer, position)]) {
                    continue;
                }
                int row = position / 5;
                int col = position % 5;
                previewLevelLike.setBlockState(new BlockPos(col, layer, row), previewBlockState);
            }
        }

        return previewLevelLike;
    }

    private static boolean hasNoSelectedPositions(boolean[] layerPositions) {
        for (boolean selected : layerPositions) {
            if (selected) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tooltip渲染信息记录类
     */
    private record TooltipRenderInfo(
        Font font, List<Component> tooltip, int x, int y
    ) {
    }

}
