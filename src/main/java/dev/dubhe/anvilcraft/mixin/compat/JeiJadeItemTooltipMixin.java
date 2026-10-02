package dev.dubhe.anvilcraft.mixin.compat;

import dev.dubhe.anvilcraft.integration.jade.JadeItemTooltipSupport;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "mezz.jei.library.render.ItemStackRenderer", remap = false)
abstract class JeiJadeItemTooltipMixin {
    @Inject(
        method = "getTooltip(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;",
        at = @At("RETURN"), cancellable = true
    )
    private void anvilcraft$modNameBeforeRecipeHints(
        ItemStack stack, TooltipFlag flags, CallbackInfoReturnable<List<Component>> callback
    ) {
        callback.setReturnValue(JadeItemTooltipSupport.appendModName(stack, callback.getReturnValue()));
    }
}
