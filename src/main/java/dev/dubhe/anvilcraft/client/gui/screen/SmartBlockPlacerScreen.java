package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.preview.ViewportCameraRig;
import dev.anvilcraft.lib.v2.rendering.preview.gui.StructureGuiPreview;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.building.BlueprintBlockConfiguration;
import dev.dubhe.anvilcraft.building.BlueprintPlacement;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.gui.component.StructureScannerButtonState;
import dev.dubhe.anvilcraft.client.gui.component.TriStateButton;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.client.support.StructurePreviewScene;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.SmartBlockPlacerMenu;
import dev.dubhe.anvilcraft.network.SmartBlockPlacerActionPacket;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SmartBlockPlacerScreen extends AbstractContainerScreen<SmartBlockPlacerMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "smart_block_placer");

    private static final ResourceLocation[] LAYER_DEFAULT = {
        SmartBlockPlacerScreen.texture("layer_1"),
        SmartBlockPlacerScreen.texture("layer_2"),
        SmartBlockPlacerScreen.texture("layer_3"),
        SmartBlockPlacerScreen.texture("layer_4"),
        SmartBlockPlacerScreen.texture("layer_5")
    };

    private static final ResourceLocation POSITION_SELECT = SmartBlockPlacerScreen.texture("position_select");

    private static final ResourceLocation LAYER_ALL = SmartBlockPlacerScreen.texture("layer_all");
    private static final ResourceLocation LAYER_SINGLE = SmartBlockPlacerScreen.texture("layer_single");

    private static final ResourceLocation PICKUP_MODE = SmartBlockPlacerScreen.texture("pickup_mode");
    private static final ResourceLocation MOVE_MODE = SmartBlockPlacerScreen.texture("move_mode");

    private static final ResourceLocation BLUEPRINT_MODE_BG = SmartBlockPlacerScreen.texture("blueprint_mode");

    // 跳过/停止缺少方块按钮贴图
    private static final ResourceLocation SKIP_MISSING = SmartBlockPlacerScreen.texture("skip_missing");
    private static final ResourceLocation STOP_MISSING = SmartBlockPlacerScreen.texture("stop_missing");

    private static ResourceLocation texture(String name) {
        return SharedTextures.textureGui("machine/smart_block_placer/" + name);
    }

    private final List<TriStateButton> layerButtons = new ArrayList<>();
    private final TriStateButton[][] positionButtons = new TriStateButton[5][5];
    private @Nullable ModeButton layerModeButton;  // 分层显示切换按钮
    private @Nullable ModeButton operationModeButton;  // 取物/移动模式切换按钮
    private @Nullable ModeButton skipMissingButton;  // 跳过缺少方块按钮
    private @Nullable ModeButton pressedModeButton;
    private @Nullable ModeButton stopMissingButton;  // 停止在缺少方块按钮
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
    private int previewDragButton = -1;
    private boolean previewDragged;
    private float previewZoom = 1;
    private float previewPanX;
    private float previewPanY;
    private float handbookPanX;
    private float handbookPanY;
    private final ViewportCameraRig previewCamera = new ViewportCameraRig();
    private final StructureGuiPreview structureGuiPreview = new StructureGuiPreview();
    @Nullable private StructurePreviewScene previewScene;
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    private static final int PREVIEW_BLOCK_SWITCH_INTERVAL = 80;
    private static final int STRUCTURE_INFO_MAX_WIDTH = 80;
    private static final float PREVIEW_PADDING = 4.0F;
    private static final int PLACER_PREVIEW_X = 2;
    private static final int PLACER_PREVIEW_Y = 0;
    private static final int PLACER_PREVIEW_Y_UPSIDE_DOWN = 4;
    private static final int PLACER_PREVIEW_Z = 6;
    private static final float STRUCTURE_PREVIEW_Z = 300;

    // LevelLike 缓存
    private @Nullable LevelLike cachedPreviewLevelLike;
    private BlockPlacementUtil.BlueprintExtent cachedBlueprintExtent =
        BlockPlacementUtil.BlueprintExtent.of(new BlockState[0], SmartBlockPlacerBlockEntity.POSITION_GRID_SIZE);
    private BlockState cachedPlacerState = Blocks.AIR.defaultBlockState();
    private ItemStack cachedBlueprintItem = ItemStack.EMPTY;
    @Nullable private StructureSnapshot cachedBlueprintSnapshot;
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

    @Override
    protected void init() {
        this.previewDragButton = -1;
        this.structureGuiPreview.clear();
        this.cancelModeButtonPress();
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

        ModeButton button = new ModeButton(
            buttonX,
            buttonY,
            this.showAllLayers ? LAYER_ALL : LAYER_SINGLE,
            3,
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

        ModeButton button = new ModeButton(
            buttonX,
            buttonY,
            this.isPickupMode ? PICKUP_MODE : MOVE_MODE,
            3,
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
        ModeButton skipButton = new ModeButton(
            buttonStartX,
            buttonY,
            SKIP_MISSING,
            5,
            (btn) -> this.onSkipMissingButtonClick(),
            List.of(Component.translatable("screen.anvilcraft.smart_block_placer.missing_mode.skip"))
        );
        this.skipMissingButton = skipButton;
        this.addRenderableWidget(skipButton);

        // 停止在缺少方块按钮
        ModeButton stopButton = new ModeButton(
            buttonStartX + 18,
            buttonY,
            STOP_MISSING,
            5,
            (btn) -> this.onStopMissingButtonClick(),
            List.of(Component.translatable("screen.anvilcraft.smart_block_placer.missing_mode.stop"))
        );
        this.stopMissingButton = stopButton;
        this.updateMissingModeButtonState();
        this.addRenderableWidget(stopButton);
    }

    private void cancelModeButtonPress() {
        if (this.getFocused() instanceof ModeButton focused) focused.pressState.cancel();
        if (this.pressedModeButton != null) this.pressedModeButton.pressState.cancel();
        this.pressedModeButton = null;
    }

    private class ModeButton extends Button {
        private ResourceLocation texture;
        private final int frames;
        private final StructureScannerButtonState pressState;
        @Setter
        private boolean selected;
        @Getter
        @Setter
        private List<Component> tooltips;

        ModeButton(int x, int y, ResourceLocation texture, int frames, OnPress onPress, List<Component> tooltips) {
            super(x, y, 16, 16, Component.empty(), onPress, DEFAULT_NARRATION);
            this.texture = texture;
            this.frames = frames;
            this.pressState = new StructureScannerButtonState(frames);
            this.tooltips = tooltips;
        }

        void setTexture(ResourceLocation texture) {
            if (this.texture.equals(texture)) return;
            this.pressState.cancel();
            this.texture = texture;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!this.active || !this.visible) this.pressState.cancel();
            super.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int frame = this.pressState.frame(this.active, this.isHovered(), this.selected);
            float color = this.active ? 1.0F : 0.45F;
            RenderSystem.setShaderColor(color, color, color, 1.0F);
            graphics.blit(this.texture, this.getX(), this.getY(), 0, frame * 16, this.width, this.height, 16, 16 * this.frames);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            SmartBlockPlacerScreen.this.cancelModeButtonPress();
            if (this.pressState.press(0, this.active && this.visible)) SmartBlockPlacerScreen.this.pressedModeButton = this;
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
        ModeButton skipButton = this.skipMissingButton;
        if (skipButton != null) {
            skipButton.setSelected(this.isSkipMissingMode);
        }
        ModeButton stopButton = this.stopMissingButton;
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
        this.cancelModeButtonPress();
        ModeButton skipButton = this.skipMissingButton;
        if (skipButton != null) {
            this.removeWidget(skipButton);
            this.skipMissingButton = null;
        }
        ModeButton stopButton = this.stopMissingButton;
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
            (btn) -> this.onPositionButtonClick((TriStateButton) btn, positionIndex, tooltipSelected, tooltipUnselected),
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
        ModeButton button = this.layerModeButton;
        if (button == null) {
            return;
        }
        button.setSelected(this.showAllLayers);
        button.setTexture(this.showAllLayers ? LAYER_ALL : LAYER_SINGLE);
        button.setTooltips(List.of(this.getLayerModeTooltip()));
    }

    private void updateOperationModeButtonState() {
        ModeButton button = this.operationModeButton;
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
        if (this.structureGuiPreview.mouseReleased(button)) {
            return true;
        }
        this.dragTargetState = null;
        if (button == this.previewDragButton) {
            if (!this.previewDragged && this.isMouseInPreviewWindow(mouseX, mouseY) && this.previewScene != null) {
                var pos = StructurePreviewScene.pick(this.previewScene.level(), this.previewCamera,
                    this.previewWindowWidth, this.previewWindowHeight, mouseX - this.previewWindowX, mouseY - this.previewWindowY,
                    Integer.MIN_VALUE, Integer.MAX_VALUE, this.handbookPanX, this.handbookPanY);
                if (pos != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
                    if (button == 1 && (!this.isBlueprintMode || this.cachedBlueprintSnapshot != null)) {
                        var anchor = this.previewBlockCenter(pos);
                        this.structureGuiPreview.open(this, this.previewScene.template(), pos, anchor.x, anchor.y);
                    }
                }
            }
            this.previewDragButton = -1;
            return true;
        }
        if (button == 0 && this.pressedModeButton != null) {
            ModeButton pressed = this.pressedModeButton;
            this.pressedModeButton = null;
            this.setDragging(false);
            pressed.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isMouseInPreviewWindow(double mouseX, double mouseY) {
        return mouseX >= this.previewWindowX && mouseX < this.previewWindowX + this.previewWindowWidth && mouseY >= this.previewWindowY
               && mouseY < this.previewWindowY + this.previewWindowHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.structureGuiPreview.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) this.cancelModeButtonPress();
        if ((button == 0 || button == 1) && this.previewDragButton == -1 && this.isMouseInPreviewWindow(mouseX, mouseY)) {
            this.previewDragButton = button;
            this.previewDragged = false;
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
        if (this.structureGuiPreview.mouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        if (button == this.previewDragButton && this.previewDragButton != -1) {
            int currentMouseX = (int) mouseX;
            int currentMouseY = (int) mouseY;
            if (!this.previewDragged && Math.abs(currentMouseX - this.lastMouseX) + Math.abs(currentMouseY - this.lastMouseY) < 3) {
                return true;
            }
            this.previewDragged = true;
            float deltaX = currentMouseX - this.lastMouseX;
            float deltaY = currentMouseY - this.lastMouseY;
            if (button == 1) {
                this.previewPanX += deltaX;
                this.previewPanY += deltaY;
            } else {
                this.previewCamera.setRotationY(this.previewCamera.getRotationY()
                    + deltaX * (float) AnvilCraft.CLIENT_CONFIG.controls.previewRotationSensitivity);
                this.previewCamera.setRotationX(Mth.clamp(this.previewCamera.getRotationX()
                    + deltaY * (float) AnvilCraft.CLIENT_CONFIG.controls.previewRotationSensitivity, -89.9f, 89.9f));
            }
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
        this.structureGuiPreview.clear();
        this.previewScene = null;
        this.cancelModeButtonPress();
        this.cachedPreviewLevelLike = null;
        super.removed();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BACKGROUND, i, j, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

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
                this.renderMaskedItem(guiGraphics, diskStack, diskSlotX, diskSlotY);
            }
        }

        // 蓝图模式下渲染书槽位的虚影（当槽位为空时）
        if (this.isBlueprintMode && this.menu.getBookInventory().getItem(0).isEmpty()) {
            // 获取书物品
            ItemStack bookStack = Items.BOOK.getDefaultInstance();
            if (!bookStack.isEmpty()) {
                int bookSlotX = i + 46;
                int bookSlotY = j + 86;
                this.renderMaskedItem(guiGraphics, bookStack, bookSlotX, bookSlotY);
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
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    private void renderMaskedItem(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.flush();
        graphics.pose().pushPose();
        // 虚影属于界面背景，深度必须低于结构方块的悬浮界面。
        graphics.pose().translate(0, 0, -100);
        RenderSupport.renderItemWithTransparency(stack, graphics.pose(), x, y, 0.4f);
        graphics.pose().popPose();
    }

    @SuppressWarnings("deprecation")
    private void renderDisplayedBlock(GuiGraphics graphics, Either<ItemStack, BlockState> displayedBlock, int x, int y) {
        displayedBlock.ifLeft(stack -> graphics.renderItem(stack, x, y)).ifRight(state -> {
            PoseStack pose = new PoseStack();
            pose.mulPose(Axis.XP.rotationDegrees(30));
            pose.mulPose(Axis.YP.rotationDegrees(45));
            LevelLike icon = new LevelLike(Minecraft.getInstance().level);
            icon.setBlockState(BlockPos.ZERO, state);
            StructurePreviewScene.of(icon).submit(graphics, new Matrix4f(pose.last().pose()),
                x, y, x + 16, y + 16, 10, net.minecraft.world.phys.shapes.Shapes.empty(), 0);
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
        this.structureGuiPreview.beginFrame();
        if (this.structureGuiPreview.contains(mouseX, mouseY)) {
            mouseX = -100000;
            mouseY = -100000;
        }
        for (var child : this.children()) {
            if (child instanceof ModeButton button && (!button.active || !button.visible)) button.pressState.cancel();
        }
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
                    mouseY,
                    this.hoveredSlot.getItem()
                ));
            }
        }

        // 检查Disk槽位tooltip
        if (isMouseOnDiskSlot) {
            tooltipsToRender.add(new TooltipRenderInfo(
                this.font,
                List.of(Component.translatable("screen.anvilcraft.smart_block_placer.disk_slot")),
                mouseX,
                mouseY,
                blockEntity == null ? ItemStack.EMPTY : blockEntity.getBlueprintItemHandler().getStackInSlot(0)
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
            guiGraphics.renderTooltip(tooltipInfo.font, tooltipInfo.tooltip, Optional.empty(),
                tooltipInfo.stack, tooltipInfo.x, tooltipInfo.y);
            guiGraphics.pose().popPose();
        }
        BlockPos anchor = this.structureGuiPreview.blockFor(this);
        if (anchor != null) {
            var point = this.previewBlockCenter(anchor);
            this.structureGuiPreview.anchor(point.x, point.y, this.isMouseInPreviewWindow(point.x, point.y));
        }
        // 悬浮界面内部按 z=100 绘制，低于原版物品的 150 与 tooltip 的 400，会被槽位物品盖住。
        // 这里整体抬高到两者之间，既压住槽位物品，又不会盖住 tooltip。
        guiGraphics.flush();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, STRUCTURE_PREVIEW_Z);
        this.structureGuiPreview.render(guiGraphics);
        guiGraphics.flush();
        guiGraphics.pose().popPose();
    }

    /**
     * 构建并渲染3D预览（含扫描仪后处理）
     */
    private void renderPreview(GuiGraphics graphics) {
        SmartBlockPlacerBlockEntity blockEntity = this.menu.getBlockEntity();
        Minecraft client = this.minecraft;
        if (blockEntity == null || client == null || client.level == null) return;
        LevelLike preview = this.getOrCreateCachedPreviewLevelLike(blockEntity, client.level);
        this.renderPreviewAutoScaled(preview, graphics);
        if (!this.isBlueprintMode && hasNoSelectedPositions(this.layerPositions)) {
            Component empty = Component.translatable("screen.anvilcraft.smart_block_placer.preview.empty");
            graphics.pose().pushPose();
            graphics.pose().scale(0.8F, 0.8F, 1);
            int x = this.previewWindowX + (this.previewWindowWidth - (int) (this.font.width(empty) * 0.8F)) / 2;
            int y = this.previewWindowY + (this.previewWindowHeight - (int) (this.font.lineHeight * 0.8F)) / 2;
            graphics.drawString(this.font, empty, (int) (x / 0.8F), (int) (y / 0.8F), 0xFFFFFFFF, true);
            graphics.pose().popPose();
        }
        this.renderStructureInfo(graphics, blockEntity, client);
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
        guiGraphics.pose().pushPose();
        try {
            guiGraphics.pose().translate(0, 0, 0);
            int textX = this.structureInfoBaseX;
            int textY = this.structureInfoBaseY;
            Component loadedText = Component.translatable("screen.anvilcraft.smart_block_placer.structure.loaded");
            guiGraphics.drawString(this.font, loadedText, textX, textY, 0xFF00AA00, false);
            this.renderStructureName(guiGraphics, minecraft, structureName, textX, textY + 10);

            Either<ItemStack, BlockState> missingBlock = blockEntity.getMissingBlock();
            if (missingBlock != null) {
                Component missingText = Component.translatable("screen.anvilcraft.smart_block_placer.missing.block");
                guiGraphics.drawString(this.font, missingText, textX, textY + 20, 0xFFFF5555, false);
                this.renderDisplayedBlock(
                    guiGraphics,
                    missingBlock,
                    textX + this.font.width(missingText) + 4,
                    textY + 18
                );
            }
        } finally {
            guiGraphics.pose().popPose();
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
            guiGraphics.drawString(this.font, structureName, textX, textY, 0xFF5555FF, false);
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
            guiGraphics.drawString(this.font, structureName, drawX, textY, 0xFF5555FF, false);
        } finally {
            guiGraphics.disableScissor();
        }
    }

    private void renderInvalidStructureInfo(GuiGraphics guiGraphics) {
        guiGraphics.pose().pushPose();
        try {
            guiGraphics.pose().translate(0, 0, 0);
            int textX = this.structureInfoBaseX;
            int textY = this.structureInfoBaseY;
            Component invalidText = Component.translatable("screen.anvilcraft.smart_block_placer.no_structure_record");
            int textWidth = this.font.width(invalidText);
            if (textWidth <= STRUCTURE_INFO_MAX_WIDTH) {
                guiGraphics.drawString(this.font, invalidText, textX, textY, 0xFFFF5555, false);
                return;
            }

            double scrollSpeed = 30.0;
            double totalScrollDistance = textWidth + STRUCTURE_INFO_MAX_WIDTH;
            double scrollCycle = totalScrollDistance / scrollSpeed * 1000.0;
            double progress = System.currentTimeMillis() % scrollCycle / scrollCycle;
            double scrollOffset = progress * totalScrollDistance - STRUCTURE_INFO_MAX_WIDTH;
            guiGraphics.enableScissor(textX, textY - 1, textX + STRUCTURE_INFO_MAX_WIDTH, textY + 10);
            try {
                guiGraphics.drawString(this.font, invalidText, textX - (int) scrollOffset, textY, 0xFFFF5555, false);
            } finally {
                guiGraphics.disableScissor();
            }
        } finally {
            guiGraphics.pose().popPose();
        }
    }

    private void renderPreviewAutoScaled(LevelLike level, GuiGraphics graphics) {
        VoxelShape rangeShape = this.createPlacementRangeShape();
        AABB bounds = this.getPreviewBounds(rangeShape);
        StructurePreviewScene scene = StructurePreviewScene.of(level);
        if (this.previewScene != null && !this.previewScene.offset().equals(scene.offset())) {
            this.structureGuiPreview.clear();
        }
        this.previewScene = scene;
        Quaternionf rotation = new Quaternionf().rotateX(Mth.DEG_TO_RAD * this.previewCamera.getRotationX())
            .rotateY(Mth.DEG_TO_RAD * this.previewCamera.getRotationY());
        float scale = this.previewZoom * RenderSupport.autoScale(rotation, bounds,
            this.previewWindowWidth, this.previewWindowHeight, PREVIEW_PADDING);
        this.previewCamera.setZoom(scale / 10);
        Vector3f center = new Matrix3f().rotation(rotation).transform(new Vector3f(
            (float) bounds.getCenter().x + scene.offset().getX(), (float) bounds.getCenter().y + scene.offset().getY(),
            (float) bounds.getCenter().z + scene.offset().getZ()));
        this.handbookPanX = this.previewPanX - center.x * scale;
        this.handbookPanY = -this.previewPanY - center.y * scale;
        graphics.enableScissor(this.previewWindowX, this.previewWindowY,
            this.previewWindowX + this.previewWindowWidth, this.previewWindowY + this.previewWindowHeight);
        graphics.pose().pushPose();
        graphics.pose().translate(this.previewWindowX, this.previewWindowY, 0);
        StructurePreviewScene.render(scene.level(), this.previewCamera, graphics, this.previewWindowWidth, this.previewWindowHeight,
            this.handbookPanX, this.handbookPanY, rangeShape.move(scene.offset().getX(), scene.offset().getY(), scene.offset().getZ()),
            AnvilCraftClient.CONFIG.effects.renderScanPreviewExtraEffect ? 0.75f : 0);
        graphics.pose().popPose();
        graphics.disableScissor();
    }

    private org.joml.Vector2f previewBlockCenter(BlockPos pos) {
        return StructurePreviewScene.projectCenter(pos, this.previewCamera,
            this.previewWindowWidth, this.previewWindowHeight, this.handbookPanX, this.handbookPanY)
            .add(this.previewWindowX, this.previewWindowY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.structureGuiPreview.contains(mouseX, mouseY)) {
            return true;
        }
        if (this.isMouseInPreviewWindow(mouseX, mouseY) && scrollY != 0) {
            this.previewZoom = Mth.clamp(this.previewZoom * (float) Math.pow(1.15, scrollY), 0.25f, 8);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!this.structureGuiPreview.contains(mouseX, mouseY)) {
            super.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private AABB getPreviewBounds(VoxelShape rangeShape) {
        int placerY = this.isUpsideDownPreview() ? PLACER_PREVIEW_Y_UPSIDE_DOWN : PLACER_PREVIEW_Y;
        return rangeShape.bounds()
            .minmax(new AABB(new BlockPos(PLACER_PREVIEW_X, placerY, PLACER_PREVIEW_Z)));
    }

    private VoxelShape createPlacementRangeShape() {
        SmartBlockPlacerBlockEntity blockEntity = this.menu.getBlockEntity();
        if (!this.isBlueprintMode || blockEntity == null) {
            return Shapes.create(0.0, 0.0, 0.0, 5.0, 5.0, 5.0);
        }
        boolean upsideDown = this.isUpsideDownPreview();
        BlockPos placerPos = blockEntity.getBlockPos();
        return Shapes.create(blockEntity.getPlacementRangeBounds(Direction.NORTH, upsideDown).move(
            PLACER_PREVIEW_X - placerPos.getX(),
            (upsideDown ? PLACER_PREVIEW_Y_UPSIDE_DOWN : PLACER_PREVIEW_Y) - placerPos.getY(),
            PLACER_PREVIEW_Z - placerPos.getZ()
        ));
    }

    private boolean isUpsideDownPreview() {
        ClientLevel level = this.minecraft.level;
        SmartBlockPlacerBlockEntity blockEntity = this.menu.getBlockEntity();
        if (level == null || blockEntity == null) return false;
        BlockState state = level.getBlockState(blockEntity.getBlockPos());
        return state.getBlock() instanceof SmartBlockPlacerBlock && state.getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
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

        var blueprint = blockEntity.getBlueprint();
        BlockState[] currentBlueprintStates = blueprint.states();
        LevelLike cachedLevel = this.cachedPreviewLevelLike;
        BlockState placerState = level.getBlockState(blockEntity.getBlockPos());
        ItemStack blueprintItem = blockEntity.getBlueprintItemHandler().getStackInSlot(0);
        var data = this.isBlueprintMode ? StructureLoadUtil.loadStructureFromDiskForPreview(level, blueprintItem) : null;
        var snapshot = data == null ? null : data.snapshot;
        if (cachedLevel != null && this.cachedBlueprintSnapshot == snapshot
            && ItemStack.isSameItemSameComponents(this.cachedBlueprintItem, blueprintItem)
            && this.cachedPlacerState == placerState
            && Arrays.equals(this.cachedLayerPositions, this.layerPositions)
            && this.cachedViewLayer == this.currentViewLayer
            && this.cachedShowAllLayers == this.showAllLayers
            && this.cachedPickupMode == this.isPickupMode
            && this.cachedBlueprintMode == this.isBlueprintMode
            && Arrays.equals(this.cachedBlueprintStates, currentBlueprintStates)
            && blueprint.extent().equals(this.cachedBlueprintExtent)
            && this.cachedGameTimeBlockType == currentBlockTypeTime) {
            return cachedLevel;
        }

        if (!ItemStack.isSameItemSameComponents(this.cachedBlueprintItem, blueprintItem)
            || this.cachedViewLayer != this.currentViewLayer || this.cachedShowAllLayers != this.showAllLayers) {
            this.structureGuiPreview.clear();
        }
        this.cachedBlueprintSnapshot = snapshot;
        LevelLike rebuiltLevel = this.buildPreviewLevelLike(blockEntity, level);
        this.cachedPreviewLevelLike = rebuiltLevel;
        this.cachedLayerPositions = this.layerPositions.clone();
        this.cachedViewLayer = this.currentViewLayer;
        this.cachedShowAllLayers = this.showAllLayers;
        this.cachedPickupMode = this.isPickupMode;
        this.cachedBlueprintMode = this.isBlueprintMode;
        this.cachedBlueprintStates = currentBlueprintStates.clone();
        this.cachedBlueprintExtent = blueprint.extent();
        this.cachedPlacerState = placerState;
        this.cachedBlueprintItem = blueprintItem.copy();
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
        int placerX = PLACER_PREVIEW_X;
        int placerZ = PLACER_PREVIEW_Z;
        int placerY = upsideDown ? PLACER_PREVIEW_Y_UPSIDE_DOWN : PLACER_PREVIEW_Y;
        if (!this.showAllLayers) {
            int visibleLayer = this.currentViewLayer;
            if (this.isBlueprintMode) {
                int index = SmartBlockPlacerBlockEntity.getPositionIndex(this.currentViewLayer, 0);
                visibleLayer = blockEntity.getBlueprintPosition(index, Direction.NORTH, upsideDown).getY()
                    - blockEntity.getBlockPos().getY() + placerY;
            }
            previewLevelLike.setCurrentVisibleLayer(visibleLayer);
        }

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
            Map<Integer, CompoundTag> blockData = this.blueprintBlockData(blockEntity, upsideDown, new BlockPos(placerX, placerY, placerZ));
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
                    BlockPos previewPos = blockEntity.getBlueprintPosition(index, Direction.NORTH, upsideDown)
                        .subtract(blockEntity.getBlockPos()).offset(placerX, placerY, placerZ);
                    previewLevelLike.setBlockState(previewPos, blueprintState, blockData.get(index));
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

    private Map<Integer, CompoundTag> blueprintBlockData(SmartBlockPlacerBlockEntity blockEntity, boolean upsideDown, BlockPos placerPos) {
        var snapshot = this.cachedBlueprintSnapshot;
        var disk = blockEntity.getBlueprintItemHandler().getStackInSlot(0).get(ModComponents.STRUCTURE_DISK_DATA);
        if (snapshot == null || disk == null) {
            return Map.of();
        }
        var frame = new BlueprintPlacement(BlockPos.ZERO,
            BlueprintPlacement.facingPlayer(disk.direction(), Direction.SOUTH), Mirror.NONE);
        var bounds = frame.bounds(snapshot.size());
        int gridSize = SmartBlockPlacerBlockEntity.POSITION_GRID_SIZE;
        int half = gridSize / 2;
        int centerIndex = half * gridSize + half;
        BlockPos center = blockEntity.getBlueprintPosition(centerIndex, Direction.NORTH, upsideDown);
        BlockPos columnStep = blockEntity.getBlueprintPosition(centerIndex + 1, Direction.NORTH, upsideDown).subtract(center);
        BlockPos rowStep = blockEntity.getBlueprintPosition(centerIndex + gridSize, Direction.NORTH, upsideDown).subtract(center);
        BlockPos gridOrigin = this.blueprintGridPos(BlockPos.ZERO, frame, bounds);
        BlockPos origin = center.subtract(blockEntity.getBlockPos()).offset(placerPos)
            .offset(columnStep.multiply(gridOrigin.getX() - half)).offset(rowStep.multiply(gridOrigin.getZ() - half));
        BlockPos gridStep = this.blueprintGridPos(new BlockPos(1, 0, 0), frame, bounds).subtract(gridOrigin);
        BlockPos delta = columnStep.multiply(gridStep.getX()).offset(rowStep.multiply(gridStep.getZ()));
        Rotation rotation = delta.getX() == 1 ? Rotation.NONE : delta.getX() == -1 ? Rotation.CLOCKWISE_180
            : delta.getZ() == 1 ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
        var placement = new BlueprintPlacement(origin, rotation, Mirror.NONE);
        BlockPos sourceOrigin = BlueprintBlockConfiguration.sourceOrigin(snapshot);
        Map<Integer, CompoundTag> result = new HashMap<>();
        for (var entry : snapshot.blocks()) {
            if (entry.nbt().isEmpty()) {
                continue;
            }
            BlockPos grid = this.blueprintGridPos(entry.pos(), frame, bounds);
            int size = SmartBlockPlacerBlockEntity.POSITION_GRID_SIZE;
            if (grid.getX() < 0 || grid.getX() >= size || grid.getY() < 0 || grid.getY() >= size
                || grid.getZ() < 0 || grid.getZ() >= size) {
                continue;
            }
            int index = grid.getY() * size * size + grid.getZ() * size + grid.getX();
            CompoundTag tag = entry.nbt().orElseThrow().copy();
            BlueprintBlockConfiguration.transform(tag, placement, sourceOrigin);
            result.put(index, tag);
        }
        return result;
    }

    private BlockPos blueprintGridPos(BlockPos pos, BlueprintPlacement frame, BoundingBox bounds) {
        BlockPos local = frame.localOf(pos).offset(-bounds.minX(), -bounds.minY(), -bounds.minZ());
        int width = bounds.getXSpan();
        int depth = bounds.getZSpan();
        int column = (5 - width) / 2 + width - local.getX() - 1;
        int row = (5 - depth) / 2 + depth - local.getZ() - 1;
        return new BlockPos(column, local.getY(), row);
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
        Font font, List<Component> tooltip, int x, int y, ItemStack stack
    ) {
        TooltipRenderInfo(Font font, List<Component> tooltip, int x, int y) {
            this(font, tooltip, x, y, ItemStack.EMPTY);
        }
    }

}
