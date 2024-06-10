package dev.dubhe.anvilcraft.integration.emi.ui;

import dev.dubhe.anvilcraft.util.BlockStateRender;
import dev.emi.emi.api.widget.DrawableWidget.DrawableWidgetConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;

import java.util.List;

public class BlockWidget implements DrawableWidgetConsumer {
    private final BlockState[] blockStates;
    private final Vec2[] vec2s;

    public BlockWidget(BlockState[] blockStates, Vec2[] vec2s) {
        this.blockStates = blockStates;
        this.vec2s = vec2s;
    }

    public BlockWidget(List<BlockState> blockStates, List<Vec2> vec2s) {
        this.blockStates = blockStates.toArray(new BlockState[0]);
        this.vec2s = vec2s.toArray(new Vec2[0]);
    }

    public BlockWidget(BlockState blockState, Vec2 vec2) {
        this.blockStates = new BlockState[]{blockState};
        this.vec2s = new Vec2[]{vec2};
    }

    @Override
    public void render(GuiGraphics draw, int mouseX, int mouseY, float delta) {
        BlockStateRender.renderBlocks(blockStates, vec2s, draw, 25);
    }
}
