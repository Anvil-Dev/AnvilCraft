package dev.dubhe.anvilcraft.mixin.compat;

import dev.dubhe.anvilcraft.integration.xaero.OverseerMinimapHighlighter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.highlight.AbstractHighlighter;
import xaero.common.minimap.highlight.HighlighterRegistry;

@Pseudo
@Mixin(value = HighlighterRegistry.class, remap = false)
public abstract class XaeroMinimapHighlighterRegistryMixin {
    @Shadow
    public abstract void register(AbstractHighlighter highlighter);

    @Inject(method = "end", at = @At("HEAD"))
    private void anvilcraft$registerOverseers(CallbackInfo ci) {
        this.register(new OverseerMinimapHighlighter());
    }
}
