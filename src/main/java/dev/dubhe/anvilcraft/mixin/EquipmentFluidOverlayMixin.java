package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class EquipmentFluidOverlayMixin {
    @Inject(method = "renderFluid", at = @At("HEAD"), cancellable = true)
    private static void anvilcraft$clearFluidOverlay(Minecraft client, PoseStack pose, MultiBufferSource buffers,
                                                    Identifier texture, CallbackInfo callback) {
        if (client.player != null && client.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.WEATHERPROOF_SPACESUIT_HELMET)) {
            callback.cancel();
        }
    }
}
