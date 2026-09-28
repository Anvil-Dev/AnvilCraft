package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

public class SpacetimeSupercomputerRenderState extends BlockEntityRenderState {
    public @Nullable FormattedCharSequence text;
    public int width;
    public int lineHeight;
    public int size;
}
