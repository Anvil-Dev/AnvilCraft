package dev.dubhe.anvilcraft.integration.rei;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.util.BlockStateRender;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class DoubleBlockStateIcon implements Renderer {
    private final BlockState state1;
    private final BlockState state2;

    private DoubleBlockStateIcon(BlockState state1, BlockState state2) {
        this.state1 = state1;
        this.state2 = state2;
    }

    public static @NotNull DoubleBlockStateIcon of(BlockState state1, BlockState state2) {
        return new DoubleBlockStateIcon(state1, state2);
    }

    public static @NotNull DoubleBlockStateIcon of(@NotNull Block block1, @NotNull Block block2) {
        return DoubleBlockStateIcon.of(block1.defaultBlockState(), block2.defaultBlockState());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, Rectangle bounds, int mouseX, int mouseY, float delta) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        BlockStateRender.renderBlock(state1, 0, graphics, 16);
        BlockStateRender.renderBlock(state2, 1, graphics, 16);
        pose.popPose();
    }
}
