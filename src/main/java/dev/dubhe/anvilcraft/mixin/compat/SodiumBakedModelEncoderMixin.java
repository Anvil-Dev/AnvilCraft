package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.render.immediate.model.BakedModelEncoder;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BakedModelEncoder.class)
public class SodiumBakedModelEncoderMixin {
    @WrapOperation(
        method = "writeQuadVertices(Lnet/caffeinemc/mods/sodium/api/vertex/buffer/VertexBufferWriter;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/caffeinemc/mods/sodium/client/model/quad/ModelQuadView;IIIZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/immediate/model/BakedModelEncoder;mergeLighting(II)I"
        )
    )
    private static int modifyLightForEmissiveItems(
        int stored,
        int calculated,
        Operation<Integer> original,
        @Local(argsOnly = true) ModelQuadView modelQuadView
    ) {
        if (modelQuadView instanceof BakedQuadView view && !view.hasShade()) {
            return LightTexture.FULL_BRIGHT;
        }
        return original.call(stored, calculated);
    }

}
