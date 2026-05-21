package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.client.support.TranslucentVertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemFeatureRenderer.class)
public class SubmitNodeCollectionMixin {
    @WrapOperation(
        method = {
            "renderSolid",
            "renderTranslucent"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer;"
                     + "hasTranslucency(Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)Z"
        )
    )
    private boolean tryOverrideWithTransparent(SubmitNodeStorage.ItemSubmit submit, Operation<Boolean> original) {
        return submit.anvilcraft$isHalfTransparent() || original.call(submit);
    }

    @WrapOperation(
        method = "renderItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"
                     + "getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)"
                     + "Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        )
    )
    private VertexConsumer tryOverrideWithTransparent(
        MultiBufferSource.BufferSource instance,
        RenderType renderType,
        Operation<VertexConsumer> original,
        @Local(argsOnly = true, name = "submit") SubmitNodeStorage.ItemSubmit submit
    ) {
        VertexConsumer consumer = original.call(instance, renderType);
        if (submit.anvilcraft$isHalfTransparent()) consumer = new TranslucentVertexConsumer(consumer, 128);
        return consumer;
    }
}
