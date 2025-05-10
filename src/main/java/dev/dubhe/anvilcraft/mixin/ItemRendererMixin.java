package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemRenderer.class)
@Debug(export = true)
public class ItemRendererMixin {
    @WrapOperation(
        method = "renderQuadList",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIZ)V"
        )
    )
    void modifyLightForEmissiveItems(
        VertexConsumer instance,
        PoseStack.Pose pose,
        BakedQuad bakedQuad,
        float r,
        float g,
        float b,
        float a,
        int packedLight,
        int packedOverlay,
        boolean readExistingColor,
        Operation<Void> original
    ) {
        packedLight = bakedQuad.isShade() ? packedLight : LightTexture.FULL_BRIGHT;
        original.call(instance, pose, bakedQuad, r, g, b, a, packedLight, packedOverlay, readExistingColor);
    }
}
