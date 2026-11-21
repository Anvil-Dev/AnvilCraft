package dev.dubhe.anvilcraft.integration.jei.drawable;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DrawableBlockStateIcon implements IDrawable {
    private static final long SWAP_INTERVAL_MS = 1000L;

    private final BlockState upState;
    private final BlockState downState;
    private final @Nullable BlockState alternateUpState;
    private final @Nullable BlockState alternateDownState;

    public DrawableBlockStateIcon(BlockState upState, BlockState downState) {
        this(upState, downState, null, null);
    }

    private DrawableBlockStateIcon(
        BlockState upState,
        BlockState downState,
        @Nullable BlockState alternateUpState,
        @Nullable BlockState alternateDownState
    ) {
        this.upState = upState;
        this.downState = downState;
        this.alternateUpState = alternateUpState;
        this.alternateDownState = alternateDownState;
    }

    public static DrawableBlockStateIcon alternatingUp(
        BlockState upState,
        BlockState downState,
        BlockState alternateUpState
    ) {
        return new DrawableBlockStateIcon(upState, downState, alternateUpState, null);
    }

    public static DrawableBlockStateIcon alternatingDown(
        BlockState upState,
        BlockState downState,
        BlockState alternateDownState
    ) {
        return new DrawableBlockStateIcon(upState, downState, null, alternateDownState);
    }

    public static DrawableBlockStateIcon alternatingBoth(
        BlockState upState,
        BlockState downState,
        BlockState alternateUpState,
        BlockState alternateDownState
    ) {
        return new DrawableBlockStateIcon(upState, downState, alternateUpState, alternateDownState);
    }

    @Override
    public int getWidth() {
        return 16;
    }

    @Override
    public int getHeight() {
        return 16;
    }

    @Override
    public void draw(GuiGraphics guiGraphics, int offsetX, int offsetY) {
        RenderSupport.renderBlock(
            guiGraphics,
            this.current(this.upState, this.alternateUpState),
            offsetX + 8,
            offsetY + 3,
            10,
            7,
            RenderSupport.SINGLE_BLOCK
        );
        RenderSupport.renderBlock(
            guiGraphics,
            this.current(this.downState, this.alternateDownState),
            offsetX + 8,
            offsetY + 9,
            0,
            7,
            RenderSupport.SINGLE_BLOCK
        );
    }

    private BlockState current(BlockState state, @Nullable BlockState alternate) {
        if (alternate == null || System.currentTimeMillis() / DrawableBlockStateIcon.SWAP_INTERVAL_MS % 2 == 0) {
            return state;
        }
        return alternate;
    }
}
