package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.util.ModEnchantmentHelper;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoeItem.class)
public class HoeItemMixin {
    @Inject(method = "useOn", at = @At("RETURN"))
    void onUse(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel().isClientSide) return;
        ModEnchantmentHelper.onUseOnBlock(
            (ServerLevel) context.getLevel(),
            context.getItemInHand(),
            context.getPlayer(),
            Util.convertToSlot(context.getHand()),
            context.getClickedPos().getCenter(),
            context.getLevel().getBlockState(context.getClickedPos())
        );
    }
}
