package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameRenderer.class)
abstract class ItemFrameRendererMixin {
    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;"
                + "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"
        )
    )
    private void anvilcraft$verticalItem(
        ItemFrameRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
        CameraRenderState camera, CallbackInfo ci
    ) {
        if (!AnvilCraftClient.CONFIG.verticalItemFrame) return;
        if (state.direction == Direction.UP) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        } else if (state.direction == Direction.DOWN) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
    }
}
