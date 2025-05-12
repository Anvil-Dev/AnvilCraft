package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.LightTexture;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadView;
import org.embeddedt.embeddium.impl.render.immediate.model.BakedModelEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BakedModelEncoder.class)
public class EmbBakedModelEncoderMixin {
    @WrapOperation(
        method = "writeQuadVertices(Lorg/embeddedt/embeddium/api/vertex/buffer/VertexBufferWriter;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lorg/embeddedt/embeddium/impl/model/quad/ModelQuadView;IIIZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lorg/embeddedt/embeddium/impl/util/ModelQuadUtil;mergeBakedLight(II)I"
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
