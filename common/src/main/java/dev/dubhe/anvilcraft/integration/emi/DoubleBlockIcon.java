package dev.dubhe.anvilcraft.integration.emi;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.integration.emi.ui.BlockWidget;
import dev.emi.emi.api.render.EmiRenderable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DoubleBlockIcon implements EmiRenderable {
    private final @Nullable BlockWidget widget1;
    private final @Nullable BlockWidget widget2;

    private DoubleBlockIcon(@Nullable BlockWidget widget1, @Nullable BlockWidget widget2) {
        this.widget1 = widget1;
        this.widget2 = widget2;
    }

    /**
     * EMI
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
    public void render(@NotNull GuiGraphics draw, int x, int y, float delta) {
        PoseStack pose = draw.pose();
        pose.pushPose();
        pose.translate(0.0d, 2.0d, 0.0d);
        if (this.widget1 != null) {
            this.widget1.setSize(14);
            this.widget1.setOffsetX(x + 1);
            this.widget1.render(draw, x, y, delta);
        }
        if (this.widget2 != null) {
            this.widget2.setSize(14);
            this.widget2.setOffsetX(x + 1);
            this.widget2.render(draw, x, y, delta);
        }
        pose.popPose();
    }
}
