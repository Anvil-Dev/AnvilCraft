package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.client.renderer.mun.MunSurfaceRenderer;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.world.level.CardinalLighting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderSectionRegion.class)
public class MunTerrainShadeMixin {
    @Unique
    private static final CardinalLighting ANVILCRAFT_UNSHADED = new CardinalLighting(1, 1, 1, 1, 1, 1);

    @ModifyReturnValue(method = "cardinalLighting", at = @At("RETURN"))
    private CardinalLighting anvilcraft$moonShade(CardinalLighting original) {
        return MunSurfaceRenderer.lightingRequested() ? ANVILCRAFT_UNSHADED : original;
    }
}
