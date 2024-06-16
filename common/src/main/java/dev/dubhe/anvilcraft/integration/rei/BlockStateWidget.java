package dev.dubhe.anvilcraft.integration.rei;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.util.BlockStateRender;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BlockStateWidget extends Widget {
    private final BlockState state;
    private final int posY;
    private final int offsetX;
    private final int offsetY;

    private BlockStateWidget(BlockState state, int posY, int offsetX, int offsetY) {
        this.state = state;
        this.posY = posY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public static void of(BlockState blockState, int posY, int offsetX, int offsetY) {
        new BlockStateWidget(blockState, posY, offsetX, offsetY);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(this.offsetX, 25.0, 0.0);
        BlockStateRender.renderBlock(this.state, this.posY, graphics, 25);
        pose.popPose();
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return Lists.newArrayList();
    }
}
