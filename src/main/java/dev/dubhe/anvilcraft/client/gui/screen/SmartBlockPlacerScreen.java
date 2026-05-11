package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.client.gui.component.TriStateButton;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.SmartBlockPlacerMenu;
import dev.dubhe.anvilcraft.network.SmartBlockPlacerLayerPacket;
import dev.dubhe.anvilcraft.network.SmartBlockPlacerPositionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
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
    
    private final List<TriStateButton> layerButtons = new ArrayList<>();
    private final TriStateButton[][] positionButtons = new TriStateButton[5][5];
    private int currentViewLayer = 0;
    private Map<Integer, Set<Integer>> layerPositions = new HashMap<>();
    
    // 鼠标拖动状态
    private Boolean dragTargetState = null;
    
    // 3D预览窗口
    private int previewWindowX;
    private int previewWindowY;
    private final int previewWindowWidth = 112;
    private final int previewWindowHeight = 88;
    private boolean isPreviewDragging = false;
    private int lastMouseX = 0;

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
        
        // 通知服务端
        PacketDistributor.sendToServer(new SmartBlockPlacerLayerPacket(index));
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
            lastMouseX = (int) mouseX;
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
    
    private void renderPreview(GuiGraphics guiGraphics, float partialTick) {
        Component previewText = Component.literal("Preview");
        guiGraphics.drawString(minecraft.font, previewText, 
            previewWindowX + previewWindowWidth / 2 - minecraft.font.width(previewText) / 2,
            previewWindowY + previewWindowHeight / 2 - 4,
            0xFFFFFFFF, false);
    }
}
