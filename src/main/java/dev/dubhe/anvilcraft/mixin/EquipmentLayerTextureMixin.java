package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.client.renderer.item.EquipmentPoweredProperty;
import dev.dubhe.anvilcraft.item.armor.IonoCraftBackpackItem;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EquipmentLayerRenderer.class)
abstract class EquipmentLayerTextureMixin {
    @ModifyExpressionValue(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;"
        + "Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;"
        + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
        + "ILnet/minecraft/resources/Identifier;II)V",
        at = @At(value = "INVOKE",
        target = "Lnet/neoforged/neoforge/client/ClientHooks;getArmorTexture(Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;"
            + "Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;Lnet/minecraft/resources/Identifier;)"
            + "Lnet/minecraft/resources/Identifier;"))
    private Identifier anvilcraft$poweredArmorTexture(Identifier original, @Local(argsOnly = true) Object state,
                                                     @Local(argsOnly = true) ItemStack stack) {
        if (!(stack.getItem() instanceof IonoCraftBackpackItem backpack)) return original;
        boolean inGrid = state instanceof LivingEntityRenderState living
            && Boolean.TRUE.equals(living.getRenderData(EquipmentPoweredProperty.IN_GRID));
        return backpack.getArmorTexture(stack, inGrid);
    }
}
