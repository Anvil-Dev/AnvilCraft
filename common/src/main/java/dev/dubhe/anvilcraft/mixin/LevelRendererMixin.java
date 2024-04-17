package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.client.renderer.PowerGridRenderer;
import dev.dubhe.anvilcraft.init.ModItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LevelRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class LevelRendererMixin {
    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDD)V"
        ),
        locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    private void renderLevel(
        PoseStack poseStack, float partialTick, long finishNanoTime,
        boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
        LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci,
        @NotNull ProfilerFiller profilerFiller, @NotNull Vec3 vec3
    ) {
        Entity entity = camera.getEntity();
        boolean bl = true;
        for (ItemStack slot : entity.getArmorSlots()) {
            if (slot.is(ModItems.ANVIL_HAMMER.get())) {
                bl = false;
                break;
            }
        }
        if (bl) return;
        MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();
        VertexConsumer vertexConsumer3 = bufferSource.getBuffer(RenderType.lines());
        double camX = vec3.x();
        double camY = vec3.y();
        double camZ = vec3.z();
        profilerFiller.popPush("grid");
        PowerGridRenderer.render(poseStack, vertexConsumer3, camX, camY, camZ);
    }
}
