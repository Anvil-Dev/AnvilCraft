package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.api.tooltip.HudTooltipManager;
import dev.dubhe.anvilcraft.client.renderer.PowerGridRenderer;
import dev.dubhe.anvilcraft.item.IEngineerGoggles;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
abstract class LevelRendererMixin {
    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;"
                + "render(Lcom/mojang/blaze3d/vertex/PoseStack;"
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
        MultiBufferSource.BufferSource bufferSource = this.renderBuffers.bufferSource();
        double camX = vec3.x();
        double camY = vec3.y();
        double camZ = vec3.z();
        PowerGridRenderer.renderTransmitterLine(poseStack, bufferSource, vec3);
        if (entity instanceof LivingEntity livingEntity) {
            ItemStack mainHandItem = livingEntity.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack offHandItem = livingEntity.getItemInHand(InteractionHand.OFF_HAND);
            ItemStack handItem = mainHandItem.isEmpty() ? offHandItem : mainHandItem;
            if (!handItem.isEmpty()) {
                HudTooltipManager.INSTANCE.renderHandItemLevelTooltip(
                    handItem,
                    poseStack,
                    bufferSource.getBuffer(RenderType.lines()),
                    camX,
                    camY,
                    camZ
                );
            }
        }

        boolean bl = true;
        for (ItemStack slot : entity.getArmorSlots()) {
            if (slot.getItem() instanceof IEngineerGoggles) {
                bl = false;
                break;
            }
        }
        if (bl) return;
        profilerFiller.popPush("grid");
        PowerGridRenderer.render(poseStack, bufferSource, vec3);
        HitResult hit = minecraft.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = ((BlockHitResult) hit).getBlockPos();
            if (minecraft.level == null) return;
            BlockEntity e = minecraft.level.getBlockEntity(blockPos);
            if (e == null) return;
            HudTooltipManager.INSTANCE.renderAffectRange(
                e,
                poseStack,
                bufferSource.getBuffer(RenderType.lines()),
                camX,
                camY,
                camZ
            );
        }
    }
}
