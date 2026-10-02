package dev.dubhe.anvilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.anvilcraft.resource.ageratum.client.gui.GuideScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = GuideScreen.class, remap = false)
abstract class HandbookLayoutMixin {
    @ModifyConstant(method = {"<init>", "extractBackgroundRenderState"}, constant = @Constant(intValue = 360))
    private int anvilcraft$pageWidth(int width) {
        return 256;
    }

    @ModifyConstant(method = "init", constant = @Constant(floatValue = 360.0F / 232.0F))
    private float anvilcraft$pageAspect(float ratio) {
        return 256.0F / 232.0F;
    }

    @ModifyConstant(method = {"getBgImageScale", "extractBackgroundRenderState"}, constant = @Constant(floatValue = 360.0F))
    private float anvilcraft$pageScale(float width) {
        return 256.0F;
    }

    @ModifyConstant(method = "extractBackgroundRenderState", constant = @Constant(intValue = 512))
    private int anvilcraft$pageTexture(int size) {
        return 256;
    }

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 60))
    private int anvilcraft$labelWidth(int width) {
        return 128;
    }

    @ModifyConstant(method = "getLabelImageScale", constant = @Constant(floatValue = 60.0F))
    private float anvilcraft$labelScale(float width) {
        return 128.0F;
    }

    @ModifyConstant(method = "getLabelBaseX", constant = @Constant(intValue = -30))
    private int anvilcraft$labelOrigin(int x) {
        return -64;
    }

    @ModifyConstant(method = "extractBookmarksRenderState", constant = @Constant(intValue = 50))
    private int anvilcraft$bookmarkText(int x) {
        return 118;
    }

    @WrapOperation(method = {"renderSingleLabel", "extractBookmarksRenderState"}, at = @At(value = "INVOKE", target =
        "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;"
        + "Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
    private void anvilcraft$labelTexture(
        GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v,
        int width, int height, int textureWidth, int textureHeight, Operation<Void> original
    ) {
        original.call(graphics, pipeline, texture, x, y, u, v, 128, 16, 128, 16);
    }
}
