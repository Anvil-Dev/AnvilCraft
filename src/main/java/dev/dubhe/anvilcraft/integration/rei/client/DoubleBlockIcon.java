package dev.dubhe.anvilcraft.integration.rei.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.integration.rei.client.widget.BlockWidget;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DoubleBlockIcon implements Renderer {
    private final @Nullable BlockWidget widget1;
    private final @Nullable BlockWidget widget2;

    private DoubleBlockIcon(@Nullable BlockWidget widget1, @Nullable BlockWidget widget2) {
        this.widget1 = widget1;
        this.widget2 = widget2;
    }

    /**
     * REI
     */
    public static @NotNull DoubleBlockIcon of(@Nullable BlockState state1, @Nullable BlockState state2) {
        return new DoubleBlockIcon(
                state1 != null ? new BlockWidget(state1, 0, 0, 0) : null,
                state2 != null ? new BlockWidget(state2, -1, 0, 0) : null
        );
    }

    public static @NotNull DoubleBlockIcon of(@NotNull Block block1, @NotNull Block block2) {
        return DoubleBlockIcon.of(block1.defaultBlockState(), block2.defaultBlockState());
    }

    @Override
    public void render(GuiGraphics graphics, Rectangle bounds, int mouseX, int mouseY, float delta) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(bounds.x + 1, bounds.y - 0.5d, 0.0d);
        if (this.widget1 != null) {
            this.widget1.setSize(12);
            this.widget1.setOffsetX(1);
            this.widget1.render(graphics, 0, 0, delta);
        }
        if (this.widget2 != null) {
            this.widget2.setSize(12);
            this.widget2.setOffsetX(1);
            this.widget2.render(graphics, 0, 0, delta);
        }
        pose.popPose();
    }
}
