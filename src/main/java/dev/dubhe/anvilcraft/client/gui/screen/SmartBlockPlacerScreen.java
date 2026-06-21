package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.ToggleButton;
import dev.dubhe.anvilcraft.client.gui.component.TriStateButton;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.SmartBlockPlacerMenu;
import dev.dubhe.anvilcraft.network.SmartBlockPlacerActionPacket;
import dev.dubhe.anvilcraft.util.LevelLike;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("checkstyle:LineLength")
public class SmartBlockPlacerScreen extends AbstractContainerScreen<SmartBlockPlacerMenu> {
    private static final Identifier BACKGROUND = SharedTextures.SMART_BLOCK_PLACER_BACKGROUND;

    private static final Identifier[] LAYER_DEFAULT = {
        SharedTextures.SMART_BLOCK_PLACER_LAYER_1,
        SharedTextures.SMART_BLOCK_PLACER_LAYER_2,
        SharedTextures.SMART_BLOCK_PLACER_LAYER_3,
        SharedTextures.SMART_BLOCK_PLACER_LAYER_4,
        SharedTextures.SMART_BLOCK_PLACER_LAYER_5
    };

    private static final Identifier POSITION_SELECT = SharedTextures.SMART_BLOCK_PLACER_POSITION_SELECT;

    private static final Identifier LAYER_ALL = SharedTextures.SMART_BLOCK_PLACER_LAYER_ALL;
    private static final Identifier LAYER_SINGLE = SharedTextures.SMART_BLOCK_PLACER_LAYER_SINGLE;

    private static final Identifier PICKUP_MODE = SharedTextures.SMART_BLOCK_PLACER_PICKUP_MODE;
    private static final Identifier MOVE_MODE = SharedTextures.SMART_BLOCK_PLACER_MOVE_MODE;

    private static final Identifier BLUEPRINT_MODE_BG = SharedTextures.SMART_BLOCK_PLACER_BLUEPRINT_MODE;

    private static final Identifier SKIP_MISSING = SharedTextures.SMART_BLOCK_PLACER_SKIP_MISSING;
    private static final Identifier STOP_MISSING = SharedTextures.SMART_BLOCK_PLACER_STOP_MISSING;

    private final List<TriStateButton> layerButtons = new ArrayList<>();
    private final TriStateButton[][] positionButtons = new TriStateButton[5][5];
    @Nullable
    private ToggleButton layerModeButton;
    @Nullable
    private ToggleButton operationModeButton;
    @Nullable
    private TriStateButton skipMissingButton;
    @Nullable
    private TriStateButton stopMissingButton;
    private int currentViewLayer = 0;
    private Map<Integer, Set<Integer>> layerPositions = new HashMap<>();
    private boolean showAllLayers = true;
    private boolean isPickupMode = true;
    private boolean isSkipMissingMode = true;

    @Nullable
    private Boolean dragTargetState = null;

    private boolean isBlueprintMode = false;

    private boolean showPreviewConcrete = false;
    private int previewSwapTimer = 0;

    private int previewWindowX;
    private int previewWindowY;
    private final int previewWindowWidth = 112;
    private final int previewWindowHeight = 88;
    private boolean isPreviewDragging = false;
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    @Nullable
    private RenderTarget previewFbo;
    private float previewRotationY = 45.0f;
    private float previewRotationX = 30.0f;

    @Nullable
    private LevelLike cachedPreviewLevelLike = null;
    private Map<Integer, Set<Integer>> cachedLayerPositions = new HashMap<>();
    private int cachedViewLayer = -1;
    private boolean cachedShowAllLayers = true;
    private boolean cachedPickupMode = true;
    private boolean cachedBlueprintMode = false;
    private @Nullable UUID cachedStructureUuid = null;
    private final long cachedGameTimeBlockType = -1;

    private long structureNameScrollTime = 0;
    private String lastRenderedStructureName = "";
    private final boolean isStructureNameHovered = false;

    private int structureInfoBaseX;
    private int structureInfoBaseY;

    public SmartBlockPlacerScreen(SmartBlockPlacerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 256, 201);
    }

    @SuppressWarnings("checkstyle:LocalVariableName")
    @Override
    protected void init() {
        super.init();
        this.titleLabelY = Constant.SCREEN_TITLE_Y;

        if (this.menu.getBlockEntity() != null) {
            this.currentViewLayer = this.menu.getBlockEntity().getSelectedLayer();
            this.layerPositions = new HashMap<>();
            for (Map.Entry<Integer, Set<Integer>> entry : this.menu.getBlockEntity().getLayerPositions().entrySet()) {
                this.layerPositions.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
            this.isPickupMode = this.menu.getBlockEntity().isPickupMode();
            this.isSkipMissingMode = this.menu.getBlockEntity().isSkipMissingMode();
            this.isBlueprintMode = !this.menu.getBlockEntity().getDiskInventory().getItem(0).isEmpty();
        }

        this.previewWindowX = this.leftPos + 136;
        this.previewWindowY = this.topPos + 18;

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
                48,
                _ -> this.onLayerButtonClick(index),
                List.of(Component.translatable("screen.anvilcraft.smart_block_placer.layer." + (i + 1)))
            );
            button.setSelected(i == this.currentViewLayer);
            button.active = true;
            this.layerButtons.add(button);
            this.addRenderableWidget(button);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private void initPositionButtons() {
        int gridStartX = this.leftPos + 33;
        int gridStartY = this.topPos + 18;

        if (this.isBlueprintMode) {
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    this.positionButtons[row][col] = null;
                }
            }
            return;
        }

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
        int buttonX = this.leftPos + 232;
        int buttonY = this.topPos + 112;

        this.layerModeButton = new ToggleButton(
            buttonX,
            buttonY,
            16,
            16,
            this.showAllLayers ? LAYER_ALL : LAYER_SINGLE,
            16,
            32,
            (btn) -> this.onLayerModeButtonClick(),
            List.of(this.getLayerModeTooltip())
        );
        this.layerModeButton.setSelected(this.showAllLayers);
        this.addRenderableWidget(this.layerModeButton);
    }

    private void initOperationModeButton() {
        int buttonX = this.leftPos + 232;
        int buttonY = this.topPos + 130;

        this.operationModeButton = new ToggleButton(
            buttonX,
            buttonY,
            16,
            16,
            this.isPickupMode ? PICKUP_MODE : MOVE_MODE,
            16,
            32,
            (btn) -> this.onOperationModeButtonClick(),
            List.of(this.getOperationModeTooltip())
        );
        this.operationModeButton.setSelected(this.isPickupMode);
        this.addRenderableWidget(this.operationModeButton);
    }

    private void initMissingModeButton() {
        if (!this.isBlueprintMode) {
            this.skipMissingButton = null;
            this.stopMissingButton = null;
            return;
        }

        int buttonStartX = this.leftPos + 8;
        int buttonY = this.topPos + 86;

        this.skipMissingButton = new TriStateButton(
            buttonStartX,
            buttonY,
            16,
            16,
            SKIP_MISSING,
            16,
            48,
            (btn) -> this.onSkipMissingButtonClick(),
            List.of(Component.translatable("screen.anvilcraft.smart_block_placer.missing_mode.skip"))
        );
        this.skipMissingButton.setSelected(this.isSkipMissingMode);
        this.addRenderableWidget(this.skipMissingButton);

        this.stopMissingButton = new TriStateButton(
            buttonStartX + 18,
            buttonY,
            16,
            16,
            STOP_MISSING,
            16,
            48,
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
        if (!this.isSkipMissingMode) {
            this.isSkipMissingMode = true;
            if (this.skipMissingButton != null) this.skipMissingButton.setSelected(true);
            if (this.stopMissingButton != null) this.stopMissingButton.setSelected(false);
            ClientPacketDistributor.sendToServer(new SmartBlockPlacerActionPacket("missingMode", 1));
        }
    }

    private void onStopMissingButtonClick() {
        if (this.isSkipMissingMode) {
            this.isSkipMissingMode = false;
            if (this.skipMissingButton != null) this.skipMissingButton.setSelected(false);
            if (this.stopMissingButton != null) this.stopMissingButton.setSelected(true);
            ClientPacketDistributor.sendToServer(new SmartBlockPlacerActionPacket("missingMode", 0));
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private void updateButtonsForBlueprintMode() {
        this.removeLayerButtons();
        this.initLayerButtons();

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
            this.initPositionButtons();
        }

        if (this.isBlueprintMode) {
            this.layerPositions.clear();
        }

        this.removeMissingModeButtons();
        this.initMissingModeButton();
    }

    private void removeLayerButtons() {
        for (TriStateButton button : this.layerButtons) {
            this.removeWidget(button);
        }
        this.layerButtons.clear();
    }

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
            xpos, ypos, 16, 16,
            POSITION_SELECT, 16, 48,
            (btn) -> this.onPositionButtonClick(row, col, positionIndex, tooltipSelected, tooltipUnselected),
            selected ? tooltipSelected : tooltipUnselected
        );
        button.setSelected(selected);
        return button;
    }

    private void onLayerButtonClick(int index) {
        this.currentViewLayer = index;

        for (int i = 0; i < 5; i++) {
            this.layerButtons.get(4 - i).setSelected(i == index);
        }

        this.updatePositionButtons();

        if (!this.showAllLayers && this.layerModeButton != null) {
            this.layerModeButton.setTooltips(List.of(this.getLayerModeTooltip()));
        }

        ClientPacketDistributor.sendToServer(new SmartBlockPlacerActionPacket("layer", index));
    }

    private void onLayerModeButtonClick() {
        this.showAllLayers = !this.showAllLayers;
        if (this.layerModeButton != null) {
            this.layerModeButton.setSelected(this.showAllLayers);
            this.layerModeButton.setTexture(this.showAllLayers ? LAYER_ALL : LAYER_SINGLE);
            this.layerModeButton.setTooltips(List.of(this.getLayerModeTooltip()));
        }
    }

    private void onOperationModeButtonClick() {
        this.isPickupMode = !this.isPickupMode;
        if (this.operationModeButton != null) {
            this.operationModeButton.setSelected(this.isPickupMode);
            this.operationModeButton.setTexture(this.isPickupMode ? PICKUP_MODE : MOVE_MODE);
            this.operationModeButton.setTooltips(List.of(this.getOperationModeTooltip()));
        }
        ClientPacketDistributor.sendToServer(new SmartBlockPlacerActionPacket("mode", this.isPickupMode ? 1 : 0));
    }

    private void updatePositionButtons() {
        if (this.isBlueprintMode) return;

        Set<Integer> positions = this.layerPositions.getOrDefault(this.currentViewLayer, new HashSet<>());
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                TriStateButton button = this.positionButtons[row][col];
                if (button == null) continue;

                int positionIndex = row * 5 + col;
                boolean isSelected = positions.contains(positionIndex);
                button.setSelected(isSelected);

                List<Component> tooltipSelected = List.of(Component.translatable(
                    "screen.anvilcraft.smart_block_placer.position.selected",
                    row + 1, col + 1
                ));
                List<Component> tooltipUnselected = List.of(Component.translatable(
                    "screen.anvilcraft.smart_block_placer.position.unselected",
                    row + 1, col + 1
                ));
                button.setTooltips(isSelected ? tooltipSelected : tooltipUnselected);
            }
        }
    }

    private void onPositionButtonClick(int row, int col, int positionIndex, List<Component> tooltipSelected, List<Component> tooltipUnselected) {
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

        ClientPacketDistributor.sendToServer(new SmartBlockPlacerActionPacket(
            "position", positionIndex,
            this.currentViewLayer + ":" + positionIndex + ":" + newState
        ));
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.dragTargetState = null;
        this.isPreviewDragging = false;
        return super.mouseReleased(event);
    }

    private boolean isMouseInPreviewWindow(double mouseX, double mouseY) {
        return mouseX >= this.previewWindowX
            && mouseX < this.previewWindowX + this.previewWindowWidth
            && mouseY >= this.previewWindowY
            && mouseY < this.previewWindowY + this.previewWindowHeight;
    }

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

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                TriStateButton btn = this.positionButtons[row][col];
                if (btn != null && btn.isMouseOver(mouseX, mouseY)) {
                    this.dragTargetState = !btn.isSelected();
                    break;
                }
            }
        }
        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (this.isPreviewDragging) {
            int currentMouseX = (int) mouseX;
            int currentMouseY = (int) mouseY;
            float deltaX = currentMouseX - this.lastMouseX;
            float deltaY = currentMouseY - this.lastMouseY;

            this.previewRotationY += deltaX * 1;
            this.previewRotationX += deltaY * 1;
            this.previewRotationX = Math.clamp(this.previewRotationX, -60, 60);

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
                            ClientPacketDistributor.sendToServer(new SmartBlockPlacerActionPacket(
                                "position", positionIndex,
                                this.currentViewLayer + ":" + positionIndex + ":" + this.dragTargetState
                            ));
                        }
                    }
                }
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int i = this.leftPos;
        int j = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, i, j, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

        if (this.isBlueprintMode) {
            int blueprintX = i + (this.imageWidth - 128) / 2 - 60;
            int blueprintY = j + (this.imageHeight - 128) / 2 - 19;
            graphics.blit(RenderPipelines.GUI_TEXTURED, BLUEPRINT_MODE_BG, blueprintX, blueprintY, 0, 0, 128, 128, 128, 128);
        }

        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity != null && blockEntity.getDiskInventory().getItem(0).isEmpty()) {
            ItemStack diskStack = ModItems.STRUCTURE_DISK.get().getDefaultInstance();
            if (!diskStack.isEmpty()) {
                int diskSlotX = i + 8;
                int diskSlotY = j + 119;
                this.renderMaskedItem(graphics, diskStack, diskSlotX, diskSlotY);
            }
        }

        if (this.isBlueprintMode && blockEntity != null && blockEntity.getBookInventory().getItem(0).isEmpty()) {
            ItemStack bookStack = Items.BOOK.getDefaultInstance();
            if (!bookStack.isEmpty()) {
                int bookSlotX = i + 46;
                int bookSlotY = j + 86;
                this.renderMaskedItem(graphics, bookStack, bookSlotX, bookSlotY);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    private void renderMaskedItem(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        final int maskColor = 0x99777777;
        g.item(stack, x, y);
        g.fill(x, y, x + 16, y + 16, maskColor);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);

        // 渲染3D预览
        this.renderPreview(graphics);

        // 渲染结构信息
        this.renderStructureInfo(graphics, mouseX, mouseY);

        // 收集并渲染所有tooltip
        List<TooltipRenderInfo> tooltipsToRender = new ArrayList<>();

        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            tooltipsToRender.add(new TooltipRenderInfo(
                this.font,
                this.getTooltipFromContainerItem(this.hoveredSlot.getItem()),
                mouseX,
                mouseY
            ));
        }

        // 收集缺失方块图标的tooltip
        this.collectMissingBlockItemTooltip(tooltipsToRender, mouseX, mouseY);

        for (TooltipRenderInfo info : tooltipsToRender) {
            graphics.setTooltipForNextFrame(
                info.tooltip.stream().map(Component::getVisualOrderText).toList(),
                info.x,
                info.y
            );
        }
    }

    /**
     * 渲染3D预览
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    private void renderPreview(GuiGraphicsExtractor graphics) {
        if (this.minecraft.level == null) return;

        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) return;

        // 构建并渲染 LevelLike 预览
        LevelLike previewLevelLike = this.getOrCreateCachedPreviewLevelLike();
        if (previewLevelLike != null) {
            this.renderPreviewWithFixedSize(
                previewLevelLike,
                graphics,
                this.previewRotationX,
                this.previewRotationY
            );
        }

        // 渲染放置范围边框
        // this.renderPlacementRangeBox(graphics);
    }

    /**
     * 渲染结构名称（带滚动效果，仅鼠标悬停时滚动）
     */
    private void renderStructureInfo(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) return;

        String structureName = blockEntity.getLoadedStructureName();
        if (!structureName.isEmpty()) {
            // 检测名称是否变化，重置滚动时间
            if (!structureName.equals(this.lastRenderedStructureName)) {
                this.lastRenderedStructureName = structureName;
                this.structureNameScrollTime = 0;
            }

            int textX = this.structureInfoBaseX;
            int textY = this.structureInfoBaseY;

            // 渲染"加载："标签（绿色）
            Component loadedText = Component.translatable("screen.anvilcraft.smart_block_placer.structure.loaded");
            graphics.text(this.font, loadedText, textX, textY, 0xFF00AA00, false);

            // 渲染结构名（蓝色，换行显示，仅鼠标悬停时滚动）
            int nameX = textX;
            int nameY = textY + 10;
            int maxWidth = 80;
            int textWidth = this.font.width(structureName);

            // 检测鼠标是否悬停在结构名区域
            boolean isHovered = mouseX >= nameX && mouseX <= nameX + maxWidth
                && mouseY >= nameY && mouseY <= nameY + this.font.lineHeight;

            if (textWidth > maxWidth) {
                int scrollSpeed = 2;
                int totalScrollDistance = textWidth + 20;

                int scrollOffset;
                if (isHovered) {
                    // 鼠标悬停时滚动
                    scrollOffset = (int) (this.structureNameScrollTime * scrollSpeed) % totalScrollDistance;
                } else {
                    // 鼠标未悬停，显示开头
                    scrollOffset = 0;
                }

                graphics.enableScissor(nameX, nameY, nameX + maxWidth, nameY + this.font.lineHeight);
                graphics.text(this.font, structureName, nameX - scrollOffset, nameY, 0xFF5555FF, false);

                if (scrollOffset > textWidth) {
                    int secondTextX = nameX - scrollOffset + textWidth + 20;
                    graphics.text(this.font, structureName, secondTextX, nameY, 0xFF5555FF, false);
                }
                graphics.disableScissor();
            } else {
                graphics.text(this.font, structureName, nameX, nameY, 0xFF5555FF, false);
            }

            // 渲染缺失方块信息
            ItemStack missingItem = blockEntity.getMissingBlockItem();
            if (!missingItem.isEmpty()) {
                Component missingText = Component.translatable("screen.anvilcraft.smart_block_placer.missing.block");
                int missingY = textY + 20;
                graphics.text(this.font, missingText, textX, missingY, 0xFFFF5555, false);
                // 渲染缺失方块图标
                graphics.item(missingItem, textX + this.font.width(missingText) + 4, missingY - 2);
            }
        } else if (blockEntity.hasInvalidStructure() && !blockEntity.getDiskInventory().getItem(0).isEmpty()) {
            // 磁盘存在但结构数据无效
            int textX = this.structureInfoBaseX;
            int textY = this.structureInfoBaseY;
            int maxWidth = 80;
            Component invalidText = Component.translatable("screen.anvilcraft.smart_block_placer.no_structure_record");
            int textWidth = this.font.width(invalidText);

            // 检测鼠标是否悬停
            boolean isHovered = mouseX >= textX && mouseX <= textX + maxWidth
                && mouseY >= textY && mouseY <= textY + this.font.lineHeight;

            if (textWidth > maxWidth) {
                int scrollSpeed = 2;
                int totalScrollDistance = textWidth + 20;

                int scrollOffset;
                if (isHovered) {
                    scrollOffset = (int) (this.structureNameScrollTime * scrollSpeed) % totalScrollDistance;
                } else {
                    scrollOffset = 0;
                }

                graphics.enableScissor(textX, textY, textX + maxWidth, textY + this.font.lineHeight);
                graphics.text(this.font, invalidText, textX - scrollOffset, textY, 0xFFFF5555, false);

                if (scrollOffset > textWidth) {
                    int secondTextX = textX - scrollOffset + textWidth + 20;
                    graphics.text(this.font, invalidText, secondTextX, textY, 0xFFFF5555, false);
                }
                graphics.disableScissor();
            } else {
                graphics.text(this.font, invalidText, textX, textY, 0xFFFF5555, false);
            }
        }
    }

    /**
     * 收集缺失方块图标的tooltip
     */
    private void collectMissingBlockItemTooltip(List<TooltipRenderInfo> tooltips, int mouseX, int mouseY) {
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) return;

        String structureName = blockEntity.getLoadedStructureName();
        if (structureName.isEmpty()) return;

        ItemStack missingItem = blockEntity.getMissingBlockItem();
        if (missingItem.isEmpty()) return;

        int textX = this.structureInfoBaseX;
        int textY = this.structureInfoBaseY;
        Component missingText = Component.translatable("screen.anvilcraft.smart_block_placer.missing.block");
        int iconX = textX + this.font.width(missingText) + 4;
        int iconY = textY + 18;

        if (mouseX >= iconX && mouseX < iconX + 16 && mouseY >= iconY && mouseY < iconY + 16) {
            tooltips.add(new TooltipRenderInfo(
                this.font,
                this.getTooltipFromContainerItem(missingItem),
                mouseX,
                mouseY
            ));
        }
    }

    /**
     * 以固定大小渲染 LevelLike 预览
     */
    @SuppressWarnings("SameParameterValue")
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
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationY));

        poseStack.translate(-2, 0, -4);

        GuiRenderExtras.submitStructure(
            graphics,
            level,
            minPos.get(),
            maxPos.get(),
            this.previewWindowX,
            this.previewWindowY,
            this.previewWindowX + this.previewWindowWidth,
            this.previewWindowY + this.previewWindowHeight,
            10,
            false,
            false,
            poseStack
        );
    }

    /**
     * 渲染放置范围边框（通过 GUI 提取系统绘制 2D 边框）
     */
    private void renderPlacementRangeBox(GuiGraphicsExtractor graphics) {
        // 在预览窗口四周绘制 2D 边框
        int x0 = this.previewWindowX;
        int y0 = this.previewWindowY;
        int x1 = this.previewWindowX + this.previewWindowWidth;
        int y1 = this.previewWindowY + this.previewWindowHeight;
        int borderColor = 0x88FFAA00;

        // 上
        graphics.fill(x0 - 1, y0 - 1, x1 + 1, y0, borderColor);
        // 下
        graphics.fill(x0 - 1, y1, x1 + 1, y1 + 1, borderColor);
        // 左
        graphics.fill(x0 - 1, y0, x0, y1, borderColor);
        // 右
        graphics.fill(x1, y0, x1 + 1, y1, borderColor);
    }

    @Override
    public void containerTick() {
        super.containerTick();

        // 更新结构名称滚动时间
        this.structureNameScrollTime++;

        // 预览方块颜色切换（每80tick=4秒互换）
        this.previewSwapTimer++;
        if (this.previewSwapTimer >= 80) {
            this.previewSwapTimer = 0;
            this.showPreviewConcrete = !this.showPreviewConcrete;
            this.cachedPreviewLevelLike = null;
        }

        // 检查结构数据变化，使预览缓存失效
        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) return;

        var currentStructure = blockEntity.getLoadedStructure();
        UUID currentUuid = null;
        if (currentStructure != null) {
            currentUuid = currentStructure.diskData.uuid();
        }

        if (!Objects.equals(currentUuid, this.cachedStructureUuid)) {
            this.cachedStructureUuid = currentUuid;
            this.cachedPreviewLevelLike = null;
        }

        // 检查蓝图模式状态变化
        boolean newBlueprintMode = !blockEntity.getDiskInventory().getItem(0).isEmpty();
        if (newBlueprintMode != this.isBlueprintMode) {
            this.isBlueprintMode = newBlueprintMode;
            this.cachedPreviewLevelLike = null;
            // 立即刷新按钮布局以匹配当前模式
            this.updateButtonsForBlueprintMode();
        }

        // 检查层数据变化
        if (!this.layerPositions.equals(this.cachedLayerPositions)
            || this.currentViewLayer != this.cachedViewLayer
            || this.showAllLayers != this.cachedShowAllLayers
            || this.isPickupMode != this.cachedPickupMode
            || this.isBlueprintMode != this.cachedBlueprintMode) {

            this.cachedLayerPositions = new HashMap<>();
            for (Map.Entry<Integer, Set<Integer>> entry : this.layerPositions.entrySet()) {
                this.cachedLayerPositions.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
            this.cachedViewLayer = this.currentViewLayer;
            this.cachedShowAllLayers = this.showAllLayers;
            this.cachedPickupMode = this.isPickupMode;
            this.cachedBlueprintMode = this.isBlueprintMode;
            this.cachedPreviewLevelLike = null;
        }
    }

    /**
     * 获取或创建缓存的预览 LevelLike
     */
    @Nullable
    private LevelLike getOrCreateCachedPreviewLevelLike() {
        if (this.cachedPreviewLevelLike == null) {
            this.cachedPreviewLevelLike = this.buildPreviewLevelLike();
        }
        return this.cachedPreviewLevelLike;
    }

    /**
     * 构建预览用的 LevelLike 实例
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @Nullable
    private LevelLike buildPreviewLevelLike() {
        if (this.minecraft.level == null) return null;

        var blockEntity = this.menu.getBlockEntity();
        if (blockEntity == null) return null;

        ClientLevel level = this.minecraft.level;
        LevelLike previewLevelLike = new LevelLike(level);

        BlockPos placerPos = blockEntity.getBlockPos();
        BlockState blockState = level.getBlockState(placerPos);
        if (!(blockState.getBlock() instanceof SmartBlockPlacerBlock)) return null;

        Direction facing = blockState.getValue(HorizontalDirectionalBlock.FACING);
        boolean upsideDown = blockState.getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);

        if (this.isBlueprintMode) {
            // 蓝图模式：显示结构方块
            var structure = blockEntity.getLoadedStructure();
            if (structure == null || structure.isEmpty()) return previewLevelLike;

            int placerY = upsideDown ? 4 : 0;
            previewLevelLike.setBlockState(
                new BlockPos(2, placerY - 2, 7),
                blockState.getBlock().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                    .setValue(SmartBlockPlacerBlock.UPSIDE_DOWN, upsideDown)
            );

            var rotatedData = SmartBlockPlacerBlockEntity.rotateStructureDataStatic(structure);
            for (var bp : rotatedData.blocks) {
                BlockState rotatedState = this.rotateStructureForPreview(bp.state());
                int renderY = bp.y();
                previewLevelLike.setBlockState(new BlockPos(bp.x(), renderY - 2, bp.z() + 1), rotatedState);
            }
        } else {
            // 普通模式：显示 UI 中的选区模式（不读取世界方块）
            // 黄绿色玻璃和黄绿色混凝土每4s互换
            BlockState glassBlock = this.showPreviewConcrete
                ? Blocks.LIME_CONCRETE.defaultBlockState()
                : Blocks.LIME_STAINED_GLASS.defaultBlockState();

            int placerY = upsideDown ? 4 : 0;
            previewLevelLike.setBlockState(
                new BlockPos(2, placerY - 2, 7),
                blockState.getBlock().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                    .setValue(SmartBlockPlacerBlock.UPSIDE_DOWN, upsideDown)
            );

            for (Map.Entry<Integer, Set<Integer>> entry : this.layerPositions.entrySet()) {
                int layer = entry.getKey();

                if (!this.showAllLayers && layer != this.currentViewLayer) continue;

                for (int posIndex : entry.getValue()) {
                    int row = posIndex / 5;
                    int previewX = posIndex % 5;
                    int previewZ = row + 1;

                    previewLevelLike.setBlockState(
                        new BlockPos(previewX, layer - 2, previewZ),
                        glassBlock
                    );
                }
            }
        }

        return previewLevelLike;
    }

    /**
     * 为预览旋转方块状态
     */
    private BlockState rotateStructureForPreview(BlockState state) {
        // 桌面预览统一朝北显示
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction blockFacing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            Direction rotatedFacing = switch (blockFacing) {
                case SOUTH -> Direction.NORTH;
                case WEST -> Direction.EAST;
                case EAST -> Direction.WEST;
                default -> blockFacing;
            };
            return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotatedFacing);
        }
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            Direction blockFacing = state.getValue(HorizontalDirectionalBlock.FACING);
            Direction rotatedFacing = switch (blockFacing) {
                case SOUTH -> Direction.NORTH;
                case WEST -> Direction.EAST;
                case EAST -> Direction.WEST;
                default -> blockFacing;
            };
            return state.setValue(HorizontalDirectionalBlock.FACING, rotatedFacing);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction blockFacing = state.getValue(BlockStateProperties.FACING);
            Direction rotatedFacing = switch (blockFacing) {
                case SOUTH -> Direction.NORTH;
                case WEST -> Direction.EAST;
                case EAST -> Direction.WEST;
                default -> blockFacing;
            };
            return state.setValue(BlockStateProperties.FACING, rotatedFacing);
        }
        return state;
    }

    /**
     * Tooltip渲染信息记录类
     */
    private record TooltipRenderInfo(
        Font font, List<Component> tooltip, int x, int y
    ) {
    }
}
