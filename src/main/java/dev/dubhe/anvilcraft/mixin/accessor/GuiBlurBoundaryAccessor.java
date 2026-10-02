package dev.dubhe.anvilcraft.mixin.accessor;

import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiRenderState.class)
public interface GuiBlurBoundaryAccessor {
    @Accessor("firstStratumAfterBlur")
    int anvilcraft$firstStratumAfterBlur();
}
