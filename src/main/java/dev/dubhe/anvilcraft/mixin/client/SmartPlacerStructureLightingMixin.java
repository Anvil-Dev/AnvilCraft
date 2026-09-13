package dev.dubhe.anvilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.rendering.gui.renderer.StructurePipRenderer;
import dev.dubhe.anvilcraft.client.gui.screen.SmartPlacerPreviewRenderer;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.LightCoordsUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.OptionalInt;
import java.util.function.Supplier;

@Mixin(value = StructurePipRenderer.class, remap = false)
abstract class SmartPlacerStructureLightingMixin {
    @WrapOperation(
        method = "applyGlitchEffect",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;createRenderPass("
            + "Ljava/util/function/Supplier;Lcom/mojang/blaze3d/textures/GpuTextureView;Ljava/util/OptionalInt;)"
            + "Lcom/mojang/blaze3d/systems/RenderPass;")
    )
    private RenderPass anvilcraft$preserveUnderlyingPreview(
        CommandEncoder encoder, Supplier<String> label, GpuTextureView texture, OptionalInt clear, Operation<RenderPass> original
    ) {
        if ((Object) this instanceof SmartPlacerPreviewRenderer.StructureRenderer) clear = OptionalInt.empty();
        return original.call(encoder, label, texture, clear);
    }

    @WrapOperation(
        method = "lambda$renderToTexture$0",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"
            + "getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;")
    )
    private VertexConsumer anvilcraft$terrainLighting(
        MultiBufferSource.BufferSource buffers, RenderType type, Operation<VertexConsumer> original,
        @Local(argsOnly = true) BakedQuad quad
    ) {
        if ((Object) this instanceof SmartPlacerPreviewRenderer.StructureRenderer) {
            // 方块已做方向明暗处理，不能再叠加物品着色器的实体方向光。
            type = quad.materialInfo().layer() == ChunkSectionLayer.TRANSLUCENT
                ? ModRenderTypes.TRANSLUCENT_BLOCK : ModRenderTypes.CUTOUT_BLOCK;
        }
        return original.call(buffers, type);
    }

    @WrapOperation(
        method = "renderToTexture",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;submit("
            + "Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
    )
    private void anvilcraft$previewEntityLight(
        BlockEntityRenderer<?, ?> renderer, BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector,
        CameraRenderState camera, Operation<Void> original
    ) {
        if ((Object) this instanceof SmartPlacerPreviewRenderer.StructureRenderer) state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
        original.call(renderer, state, pose, collector, camera);
    }
}
