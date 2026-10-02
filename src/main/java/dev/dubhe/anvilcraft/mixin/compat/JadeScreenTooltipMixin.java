package dev.dubhe.anvilcraft.mixin.compat;

import dev.dubhe.anvilcraft.integration.jade.JadeItemTooltipSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Screen.class)
abstract class JadeScreenTooltipMixin {
    @Inject(method = "getTooltipFromItem", at = @At("RETURN"), cancellable = true)
    private static void anvilcraft$modNameBeforeGuide(
        Minecraft client, ItemStack stack, CallbackInfoReturnable<List<Component>> callback
    ) {
        callback.setReturnValue(JadeItemTooltipSupport.appendModName(stack, callback.getReturnValue()));
    }
}
