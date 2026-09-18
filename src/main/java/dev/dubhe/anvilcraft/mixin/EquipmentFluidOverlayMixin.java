package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ScreenEffectRenderer.class)
abstract class EquipmentFluidOverlayMixin {
    @WrapOperation(method = "renderScreenEffect", at = @At(value = "INVOKE", target =
        "Lnet/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions;"
            + "renderOverlay(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private static void anvilcraft$clearFluidOverlay(IClientFluidTypeExtensions fluid, Minecraft client, PoseStack pose,
                                                   Operation<Void> original) {
        if (client.player != null && client.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.WEATHERPROOF_SPACESUIT_HELMET)) return;
        original.call(fluid, client, pose);
    }
}
