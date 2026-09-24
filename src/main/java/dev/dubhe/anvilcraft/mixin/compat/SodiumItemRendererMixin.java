package dev.dubhe.anvilcraft.mixin.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemRenderer.class)
abstract class SodiumItemRendererMixin {
    @Inject(method = "renderQuadList", at = @At("HEAD"))
    private void anvilcraft$activateFallbackSprites(
        PoseStack pose, VertexConsumer buffer, List<BakedQuad> quads, ItemStack stack,
        int light, int overlay, CallbackInfo ci
    ) {
        for (BakedQuad quad : quads) {
            SpriteUtil.INSTANCE.markSpriteActive(quad.getSprite());
        }
    }
}
