package dev.dubhe.anvilcraft.integration.emi.ui;

import dev.dubhe.anvilcraft.util.BlockStateRender;
import dev.emi.emi.api.widget.DrawableWidget.DrawableWidgetConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.state.BlockState;

public class BlockWidget implements DrawableWidgetConsumer {
    private final BlockState blockState;
    private final int posY;

    public BlockWidget(BlockState blockStates, int posY) {
        this.blockState = blockStates;
        this.posY = posY;
    }

    @Override
    public void render(GuiGraphics draw, int mouseX, int mouseY, float delta) {
        BlockStateRender.renderBlocks(blockState, posY, draw, 25);
    }
}
