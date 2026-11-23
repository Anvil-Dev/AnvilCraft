package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(HumanoidArmorLayer.class)
abstract class HumanoidArmorLayerMixin {
    @ModifyArg(
        method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
            + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;"
            + "ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderModel("
                + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
                + "ILnet/minecraft/client/model/Model;ILnet/minecraft/resources/ResourceLocation;)V"),
        index = 1
    )
    private MultiBufferSource anvilcraft$helmetGlass(
        MultiBufferSource buffers, @Local ItemStack stack, @Local ResourceLocation texture
    ) {
        if (stack.is(ModItems.WEATHERPROOF_SPACESUIT_HELMET)) {
            return ignored -> buffers.getBuffer(RenderType.entityTranslucent(texture));
        }
        return buffers;
    }
}
