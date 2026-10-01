package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameRenderer.class)
public class ItemFrameRendererMixin<T extends ItemFrame> {
    @Inject(
        method = "render("
                 + "Lnet/minecraft/world/entity/decoration/ItemFrame;"
                 + "FFLcom/mojang/blaze3d/vertex/PoseStack;"
                 + "Lnet/minecraft/client/renderer/MultiBufferSource;I"
                 + ")V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemFrameRenderer;"
                     + "getLightVal(Lnet/minecraft/world/entity/decoration/ItemFrame;II)I",
            ordinal = 1
        )
    )
    void render(
        T entity,
        float entityYaw,
        float partialTicks,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        CallbackInfo ci
    ) {
        if (!AnvilCraftClient.CONFIG.graphics.renderVerticalModelsInHorizontalItemFrame) return;
        Direction direction = entity.getDirection();
        if (direction == Direction.UP) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        } else if (direction == Direction.DOWN) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
    }
}
